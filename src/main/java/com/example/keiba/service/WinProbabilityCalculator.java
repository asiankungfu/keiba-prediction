package com.example.keiba.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 各出走馬の勝率を算出するドメインロジック。
 *
 * <p>アルゴリズムは「特徴量の線形和でスコアを出し、レース内で softmax 正規化する」
 * 多クラスロジスティック回帰相当。出力はレース内で必ず合計1.0になるため、
 * 「そのレースで勝つ確率」として解釈できる。</p>
 *
 * <pre>
 *   score_i = w0
 *           + w1 * 通算勝率_i
 *           + w2 * 騎手勝率_i
 *           + w3 * 条件適性_i
 *           - w4 * (斤量_i - 基準斤量)
 *
 *   p_i = softmax(score)_i = exp(score_i) / Σ_j exp(score_j)
 * </pre>
 *
 * <p>フレームワーク非依存・副作用なしの純粋な計算クラスにしているため、
 * 単体テストが容易で、重み({@link ModelWeights})を差し替えるだけで
 * モデルを学習済みパラメータに置き換えられる。</p>
 */
public class WinProbabilityCalculator {

    private final ModelWeights weights;

    public WinProbabilityCalculator(ModelWeights weights) {
        this.weights = weights;
    }

    /**
     * 1頭分の線形スコアを算出する。
     */
    public double score(ScoringInput in) {
        return weights.intercept()
                + weights.careerWinRate() * in.careerWinRate()
                + weights.jockeyWinRate() * in.jockeyWinRate()
                + weights.conditionAptitude() * in.conditionAptitude()
                - weights.weightPenalty() * (in.weightKg() - weights.baseWeightKg());
    }

    /**
     * レース内の全出走馬について勝率を算出する。
     *
     * @param inputs 出走馬の特徴量（順序は保持される）
     * @return entryId をキー、勝率(0.0〜1.0)を値とする Map。合計はほぼ1.0。
     */
    public Map<Long, Double> calculateWinProbabilities(List<ScoringInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 1) 各馬のスコア
        List<Double> scores = new ArrayList<>(inputs.size());
        double maxScore = Double.NEGATIVE_INFINITY;
        for (ScoringInput in : inputs) {
            double s = score(in);
            scores.add(s);
            if (s > maxScore) {
                maxScore = s;
            }
        }

        // 2) softmax（最大値を引いてオーバーフローを防ぐ＝数値安定化）
        double sumExp = 0.0;
        List<Double> exps = new ArrayList<>(inputs.size());
        for (double s : scores) {
            double e = Math.exp(s - maxScore);
            exps.add(e);
            sumExp += e;
        }

        // 3) 正規化して entryId にひも付け
        Map<Long, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            result.put(inputs.get(i).entryId(), exps.get(i) / sumExp);
        }
        return result;
    }

    /**
     * 各馬の「3着以内に入る確率（複勝率）」を算出する。
     *
     * <p>勝率 p_i をもとに <b>ハーヴィル・モデル</b>（Harville, 1973）で
     * 1着・2着・3着になる確率を順に求め、その和をとる。直感的には
     * 「勝率に比例して1着→2着→3着を非復元抽出していく」モデル。</p>
     *
     * <pre>
     *   P(iが1着) = p_i
     *   P(iが2着) = Σ_j p_j · p_i/(1−p_j)
     *   P(iが3着) = Σ_j Σ_k p_j · p_k/(1−p_j) · p_i/(1−p_j−p_k)
     *   複勝率_i  = P(1着)+P(2着)+P(3着)
     * </pre>
     *
     * <p>全馬の複勝率の合計は約3.0（3頭が3着以内に入るため）になる。</p>
     */
    public Map<Long, Double> calculateTop3Probabilities(List<ScoringInput> inputs) {
        Map<Long, Double> winMap = calculateWinProbabilities(inputs);
        Map<Long, Double> result = new LinkedHashMap<>();
        if (winMap.isEmpty()) {
            return result;
        }

        int n = inputs.size();
        long[] ids = new long[n];
        double[] p = new double[n];
        for (int i = 0; i < n; i++) {
            ids[i] = inputs.get(i).entryId();
            p[i] = winMap.get(ids[i]);
        }

        for (int i = 0; i < n; i++) {
            double place1 = p[i];
            double place2 = 0.0;
            double place3 = 0.0;
            for (int j = 0; j < n; j++) {
                if (j == i) {
                    continue;
                }
                double denom1 = 1.0 - p[j];
                if (denom1 <= 1e-12) {
                    continue;
                }
                place2 += p[j] * (p[i] / denom1);          // j が1着, i が2着
                for (int k = 0; k < n; k++) {
                    if (k == i || k == j) {
                        continue;
                    }
                    double denom2 = 1.0 - p[j] - p[k];
                    if (denom2 <= 1e-12) {
                        continue;
                    }
                    // j,k が1,2着, i が3着
                    place3 += p[j] * (p[k] / denom1) * (p[i] / denom2);
                }
            }
            result.put(ids[i], Math.min(1.0, place1 + place2 + place3));
        }
        return result;
    }
}
