#!/usr/bin/env python3
"""履歴データから勝率モデル（条件付きロジット）を学習し、的中率を評価する。

- 学習: レース内 softmax の交差エントロピーを勾配降下で最小化（= 多クラスロジスティック回帰）。
- 評価: レース単位で train/test を分割し、テスト集合で
        Top-1 的中率（予想本命が実際に1着）、Top-3 的中率、log loss を算出。
- 比較: 「通算勝率だけで本命を選ぶ」素朴なベースラインと比べる。

学習済み重みは Java アプリの application.yml(keiba.model.*) にそのまま転記して使う。
依存ライブラリ無し（標準ライブラリのみ）。
"""
import csv
import math
import random
from collections import defaultdict

DATA = "src/main/resources/sample-data/historical_races.csv"
SEED = 7
TEST_RATIO = 0.25
EPOCHS = 300
LR = 0.3

FEATURES = ["career_win_rate", "jockey_win_rate", "condition_aptitude"]
BASE_W = 55.0


def load_races():
    races = defaultdict(list)
    with open(DATA, encoding="utf-8") as f:
        for row in csv.DictReader(f):
            races[row["race_id"]].append({
                "career": float(row["career_win_rate"]),
                "jockey": float(row["jockey_win_rate"]),
                "apt": float(row["condition_aptitude"]),
                "weight": float(row["weight"]),
                "win": 1 if int(row["finish_position"]) == 1 else 0,
                "finish": int(row["finish_position"]),
            })
    return list(races.values())


def feats(h):
    # [career, jockey, apt, -(weight-base)]  ← 斤量は負の特徴として扱う
    return [h["career"], h["jockey"], h["apt"], -(h["weight"] - BASE_W)]


def softmax(scores):
    m = max(scores)
    exps = [math.exp(s - m) for s in scores]
    tot = sum(exps)
    return [e / tot for e in exps]


def train(races, dim):
    w = [0.0] * dim
    for _ in range(EPOCHS):
        grad = [0.0] * dim
        for race in races:
            xs = [feats(h) for h in race]
            scores = [sum(wi * xi for wi, xi in zip(w, x)) for x in xs]
            probs = softmax(scores)
            for i, h in enumerate(race):
                err = probs[i] - h["win"]   # softmax交差エントロピーの勾配
                for k in range(dim):
                    grad[k] += err * xs[i][k]
        n = len(races)
        w = [wi - LR * g / n for wi, g in zip(w, grad)]
    return w


def evaluate(races, w):
    top1 = top3 = total = 0
    ll = 0.0
    for race in races:
        xs = [feats(h) for h in race]
        scores = [sum(wi * xi for wi, xi in zip(w, x)) for x in xs]
        probs = softmax(scores)
        pred_order = sorted(range(len(race)), key=lambda i: probs[i], reverse=True)
        actual_winner = next(i for i, h in enumerate(race) if h["win"] == 1)
        if pred_order[0] == actual_winner:
            top1 += 1
        if actual_winner in pred_order[:3]:
            top3 += 1
        ll += -math.log(probs[actual_winner] + 1e-12)
        total += 1
    return top1 / total, top3 / total, ll / total


def baseline_top1(races):
    """通算勝率が最も高い馬を本命にする素朴な方法の Top-1 的中率。"""
    hit = 0
    for race in races:
        pick = max(range(len(race)), key=lambda i: race[i]["career"])
        if race[pick]["win"] == 1:
            hit += 1
    return hit / len(races)


def random_top1(races):
    """ランダムに本命を選んだ場合の期待 Top-1 的中率（= 平均 1/頭数）。"""
    return sum(1.0 / len(r) for r in races) / len(races)


def main():
    races = load_races()
    rng = random.Random(SEED)
    rng.shuffle(races)
    cut = int(len(races) * (1 - TEST_RATIO))
    train_races, test_races = races[:cut], races[cut:]

    w = train(train_races, dim=4)
    tr = evaluate(train_races, w)
    te = evaluate(test_races, w)
    base = baseline_top1(test_races)
    rnd = random_top1(test_races)

    names = ["career-win-rate", "jockey-win-rate", "condition-aptitude", "weight-penalty"]
    print("=== 学習済み重み（application.yml にそのまま転記可能） ===")
    for nm, val in zip(names, w):
        print(f"  {nm}: {val:.4f}")
    print()
    print(f"レース数: 学習 {len(train_races)} / テスト {len(test_races)}")
    print("=== Top-1 的中率（予想本命が実際に1着） ===")
    print(f"  学習データ : {tr[0]*100:5.1f}%")
    print(f"  テストデータ: {te[0]*100:5.1f}%   <- 汎化性能")
    print(f"  ベースライン(通算勝率のみ): {base*100:5.1f}%")
    print(f"  ランダム期待値           : {rnd*100:5.1f}%")
    print("=== その他指標（テストデータ） ===")
    print(f"  Top-3 的中率: {te[1]*100:5.1f}%")
    print(f"  log loss    : {te[2]:.4f}")

    # 機械可読の結果も出力（ドキュメント生成・CIで使える）
    with open("scripts/metrics.txt", "w", encoding="utf-8") as f:
        f.write(f"weights={dict(zip(names, [round(x,4) for x in w]))}\n")
        f.write(f"test_top1={te[0]:.4f}\ntest_top3={te[1]:.4f}\ntest_logloss={te[2]:.4f}\n")
        f.write(f"baseline_top1={base:.4f}\nrandom_top1={rnd:.4f}\n")
        f.write(f"train_top1={tr[0]:.4f}\nn_train={len(train_races)}\nn_test={len(test_races)}\n")


if __name__ == "__main__":
    main()
