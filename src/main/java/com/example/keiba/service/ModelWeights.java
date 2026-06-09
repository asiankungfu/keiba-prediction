package com.example.keiba.service;

/**
 * 勝率算出モデルの重み係数。
 *
 * <p>線形スコアの各特徴量にかかる重み。値を入れ替えるだけでモデルの挙動を
 * 調整できるよう、設定値として外出しできる構造にしている
 * （{@code application.yml} の {@code keiba.model.*} にバインド）。</p>
 */
public final class ModelWeights {

    private final double intercept;
    private final double careerWinRate;
    private final double jockeyWinRate;
    private final double conditionAptitude;
    private final double weightPenalty;
    private final double baseWeightKg;

    public ModelWeights(double intercept,
                        double careerWinRate,
                        double jockeyWinRate,
                        double conditionAptitude,
                        double weightPenalty,
                        double baseWeightKg) {
        this.intercept = intercept;
        this.careerWinRate = careerWinRate;
        this.jockeyWinRate = jockeyWinRate;
        this.conditionAptitude = conditionAptitude;
        this.weightPenalty = weightPenalty;
        this.baseWeightKg = baseWeightKg;
    }

    /**
     * 妥当な初期値。実運用では過去データでロジスティック回帰により学習し、
     * 学習済みの重みに差し替える想定。
     */
    public static ModelWeights defaults() {
        return new ModelWeights(
                /* intercept        */ 0.0,
                /* careerWinRate    */ 4.0,
                /* jockeyWinRate    */ 2.5,
                /* conditionAptitude*/ 2.0,
                /* weightPenalty    */ 0.05,
                /* baseWeightKg     */ 55.0);
    }

    public double intercept() {
        return intercept;
    }

    public double careerWinRate() {
        return careerWinRate;
    }

    public double jockeyWinRate() {
        return jockeyWinRate;
    }

    public double conditionAptitude() {
        return conditionAptitude;
    }

    public double weightPenalty() {
        return weightPenalty;
    }

    public double baseWeightKg() {
        return baseWeightKg;
    }
}
