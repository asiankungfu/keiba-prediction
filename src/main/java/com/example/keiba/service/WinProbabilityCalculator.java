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
}
