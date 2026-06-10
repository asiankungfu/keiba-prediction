#!/usr/bin/env python3
"""履歴データから勝率モデル（条件付きロジット）を学習し、的中率を評価する。

改良版（v2）:
- 特徴量の標準化（z-score）: スケールの違う特徴量を公平に扱う。
- L2 正則化: 過学習を抑え、重みを安定させる。
- k分割交差検証(CV): 1回の分割に依存せず、平均±標準偏差で性能を頑健に評価。
- 学習は標準化空間で行い、最後に「生の特徴量空間の重み」に変換して出力する
  （Java アプリは生の特徴量をそのまま使うため、アプリ側は無改修で済む）。

依存ライブラリ無し（標準ライブラリのみ）。
"""
import csv
import math
import random
from collections import defaultdict

DATA = "src/main/resources/sample-data/historical_races.csv"
SEED = 7
K_FOLDS = 5
EPOCHS = 400
LR = 0.3
L2 = 0.05          # L2 正則化の強さ
BASE_W = 55.0
FEATURE_NAMES = ["career-win-rate", "jockey-win-rate", "condition-aptitude", "weight-penalty"]


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
            })
    return list(races.values())


def raw_feats(h):
    # [通算勝率, 騎手勝率, 条件適性, -(斤量-基準)]  ← 斤量は重いほどマイナス
    return [h["career"], h["jockey"], h["apt"], -(h["weight"] - BASE_W)]


def standardizer(train_races):
    """学習データから各特徴量の平均・標準偏差を求める。"""
    cols = [[], [], [], []]
    for race in train_races:
        for h in race:
            x = raw_feats(h)
            for k in range(4):
                cols[k].append(x[k])
    mean = [sum(c) / len(c) for c in cols]
    std = []
    for k in range(4):
        m = mean[k]
        var = sum((v - m) ** 2 for v in cols[k]) / len(cols[k])
        std.append(math.sqrt(var) if var > 1e-12 else 1.0)
    return mean, std


def std_feats(h, mean, std):
    x = raw_feats(h)
    return [(x[k] - mean[k]) / std[k] for k in range(4)]


def softmax(scores):
    m = max(scores)
    exps = [math.exp(s - m) for s in scores]
    tot = sum(exps)
    return [e / tot for e in exps]


def train(races, mean, std, dim=4):
    """標準化＋L2正則化つき条件付きロジットの学習。標準化空間の重みを返す。"""
    w = [0.0] * dim
    for _ in range(EPOCHS):
        grad = [0.0] * dim
        for race in races:
            xs = [std_feats(h, mean, std) for h in race]
            scores = [sum(wi * xi for wi, xi in zip(w, x)) for x in xs]
            probs = softmax(scores)
            for i, h in enumerate(race):
                err = probs[i] - h["win"]
                for k in range(dim):
                    grad[k] += err * xs[i][k]
        n = len(races)
        # 平均勾配 + L2正則化項
        w = [wi - LR * (g / n + L2 * wi) for wi, g in zip(w, grad)]
    return w


def evaluate(races, w, mean, std):
    top1 = top3 = total = 0
    ll = 0.0
    for race in races:
        xs = [std_feats(h, mean, std) for h in race]
        scores = [sum(wi * xi for wi, xi in zip(w, x)) for x in xs]
        probs = softmax(scores)
        order = sorted(range(len(race)), key=lambda i: probs[i], reverse=True)
        winner = next(i for i, h in enumerate(race) if h["win"] == 1)
        if order[0] == winner:
            top1 += 1
        if winner in order[:3]:
            top3 += 1
        ll += -math.log(probs[winner] + 1e-12)
        total += 1
    return top1 / total, top3 / total, ll / total


def baseline_top1(races):
    hit = 0
    for race in races:
        pick = max(range(len(race)), key=lambda i: race[i]["career"])
        if race[pick]["win"] == 1:
            hit += 1
    return hit / len(races)


def mean_std(xs):
    m = sum(xs) / len(xs)
    v = sum((x - m) ** 2 for x in xs) / len(xs)
    return m, math.sqrt(v)


def main():
    races = load_races()
    rng = random.Random(SEED)
    rng.shuffle(races)

    # ---- k分割交差検証 ----
    folds = [races[i::K_FOLDS] for i in range(K_FOLDS)]
    t1s, t3s, lls, bases = [], [], [], []
    for i in range(K_FOLDS):
        test = folds[i]
        train_races = [r for j in range(K_FOLDS) if j != i for r in folds[j]]
        mean, std = standardizer(train_races)
        w = train(train_races, mean, std)
        t1, t3, ll = evaluate(test, w, mean, std)
        t1s.append(t1); t3s.append(t3); lls.append(ll); bases.append(baseline_top1(test))

    m_t1, s_t1 = mean_std(t1s)
    m_t3, s_t3 = mean_std(t3s)
    m_ll, s_ll = mean_std(lls)
    m_b, _ = mean_std(bases)

    # ---- 全データで最終学習 → 生の特徴量空間の重みに変換 ----
    mean, std = standardizer(races)
    w_std = train(races, mean, std)
    # score_std = Σ w_k (x_k-μ_k)/σ_k。softmaxはレース内一定項に不変なので、
    # 生空間の係数は w_k/σ_k（定数項 -Σ w_k μ_k/σ_k はレース内で打ち消される）。
    w_raw = [w_std[k] / std[k] for k in range(4)]

    print("=== 交差検証（{}分割）の結果 ===".format(K_FOLDS))
    print(f"  Top-1 的中率: {m_t1*100:5.1f}% ± {s_t1*100:.1f}")
    print(f"  Top-3 的中率: {m_t3*100:5.1f}% ± {s_t3*100:.1f}")
    print(f"  log loss    : {m_ll:.4f} ± {s_ll:.4f}")
    print(f"  ベースライン(通算勝率のみ): {m_b*100:5.1f}%")
    print()
    print("=== 学習済み重み（生の特徴量空間・application.yml に転記） ===")
    for nm, v in zip(FEATURE_NAMES, w_raw):
        print(f"  {nm}: {v:.4f}")

    with open("scripts/metrics.txt", "w", encoding="utf-8") as f:
        f.write("model=standardized+L2(conditional logit), {}-fold CV\n".format(K_FOLDS))
        f.write(f"cv_top1={m_t1:.4f}+-{s_t1:.4f}\ncv_top3={m_t3:.4f}+-{s_t3:.4f}\n")
        f.write(f"cv_logloss={m_ll:.4f}+-{s_ll:.4f}\nbaseline_top1={m_b:.4f}\n")
        f.write("weights_raw=" + ",".join(f"{nm}:{v:.4f}" for nm, v in zip(FEATURE_NAMES, w_raw)) + "\n")


if __name__ == "__main__":
    main()
