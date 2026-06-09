#!/usr/bin/env python3
"""結果ラベル付きの合成「履歴レース」データを生成する。

実データ（JRA-VAN）が無い環境で、勝率モデルの学習・的中率評価を再現可能に行うための
サンプルデータ生成スクリプト。データ生成過程(DGP)は条件付きロジット：

    効用 u_i = Σ b_k * x_ik + Gumbel雑音
    着順     = 効用の降順（1着 = 効用最大）

真の重み TRUE_WEIGHTS から生成しているため、学習で重みを概ね復元できることも確認できる。
※ あくまで合成データであり、実在のレース結果ではない。
"""
import csv
import math
import random

SEED = 42
N_RACES = 600
OUT = "src/main/resources/sample-data/historical_races.csv"

# データ生成に用いる「真の」重み（学習でこれを復元できるかも確認できる）
# 特徴量の取りうる幅(career~0.18, jockey~0.13, apt~0.55)に対し、効用への寄与が
# Gumbel雑音(std≈1.28)を上回るよう設定し、能力差が着順に反映される構造にしている。
TRUE_WEIGHTS = {
    "career": 14.0,
    "jockey": 9.0,
    "apt": 8.0,
    "wpen": 0.12,   # 斤量ペナルティ
    "base_w": 55.0,
}

COURSES = ["東京", "中山", "京都", "阪神", "中京"]
SURFACES = ["TURF", "DIRT"]
JOCKEYS = ["田中 一郎", "鈴木 花子", "佐藤 健", "高橋 大輔", "伊藤 翼",
           "渡辺 美咲", "中村 駿", "小林 蓮", "加藤 楓", "山本 陽"]
SEXES = ["牡", "牝", "セ"]


def gumbel(rng):
    # 標準Gumbel乱数（逆関数法）
    u = rng.random()
    return -math.log(-math.log(u + 1e-12) + 1e-12)


def utility(career, jockey, apt, weight, rng):
    w = TRUE_WEIGHTS
    return (w["career"] * career
            + w["jockey"] * jockey
            + w["apt"] * apt
            - w["wpen"] * (weight - w["base_w"])
            + gumbel(rng))


def main():
    rng = random.Random(SEED)
    rows = []
    header = ["race_id", "race_name", "race_date", "course", "distance", "surface", "grade",
              "horse_no", "frame_no", "horse_reg", "horse_name", "sex", "birth_year",
              "jockey", "weight", "career_starts", "career_win_rate", "jockey_win_rate",
              "condition_aptitude", "finish_position"]

    horse_seq = 1
    for r in range(1, N_RACES + 1):
        n = rng.randint(8, 16)
        course = rng.choice(COURSES)
        surface = rng.choice(SURFACES)
        distance = rng.choice([1200, 1400, 1600, 1800, 2000, 2400])
        grade = rng.choice(["G1", "G2", "G3", "OP", "3勝", "2勝", "1勝", "未勝利"])
        # 開催日（過去日付）
        month = rng.randint(1, 12)
        day = rng.randint(1, 28)
        race_date = f"2025-{month:02d}-{day:02d}"

        entries = []
        for h in range(1, n + 1):
            career = round(min(0.45, max(0.0, rng.gauss(0.18, 0.09))), 3)
            jockey_wr = round(min(0.30, max(0.03, rng.gauss(0.13, 0.05))), 3)
            apt = round(min(1.0, max(0.0, rng.gauss(0.55, 0.18))), 3)
            weight = round(rng.choice([54.0, 55.0, 56.0, 57.0, 58.0]), 1)
            u = utility(career, jockey_wr, apt, weight, rng)
            entries.append({
                "horse_no": h, "frame_no": (h + 1) // 2,
                "horse_reg": f"H{horse_seq:07d}", "horse_name": f"ウマ{horse_seq:04d}",
                "sex": rng.choice(SEXES), "birth_year": rng.choice([2020, 2021, 2022]),
                "jockey": rng.choice(JOCKEYS), "weight": weight,
                "career_starts": rng.randint(3, 30),
                "career_win_rate": career, "jockey_win_rate": jockey_wr,
                "condition_aptitude": apt, "utility": u,
            })
            horse_seq += 1

        # 効用降順で着順を確定
        entries.sort(key=lambda e: e["utility"], reverse=True)
        for pos, e in enumerate(entries, start=1):
            e["finish_position"] = pos

        # 馬番順に戻して出力
        entries.sort(key=lambda e: e["horse_no"])
        for e in entries:
            rows.append([
                r, f"レース{r:04d}", race_date, course, distance, surface, grade,
                e["horse_no"], e["frame_no"], e["horse_reg"], e["horse_name"], e["sex"],
                e["birth_year"], e["jockey"], e["weight"], e["career_starts"],
                e["career_win_rate"], e["jockey_win_rate"], e["condition_aptitude"],
                e["finish_position"],
            ])

    with open(OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerows(rows)
    print(f"生成: {N_RACES} レース / {len(rows)} 出走 -> {OUT}")


if __name__ == "__main__":
    main()
