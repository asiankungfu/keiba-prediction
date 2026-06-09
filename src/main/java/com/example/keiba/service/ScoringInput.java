package com.example.keiba.service;

/**
 * 勝率算出モデルへの入力（1頭分の特徴量）。
 *
 * <p>JPA エンティティから切り離した純粋な値オブジェクトにすることで、
 * 算出ロジックを永続化層やフレームワークから独立して単体テストできる。</p>
 */
public final class ScoringInput {

    private final long entryId;
    private final double careerWinRate;
    private final double jockeyWinRate;
    private final double conditionAptitude;
    private final double weightKg;

    public ScoringInput(long entryId,
                        double careerWinRate,
                        double jockeyWinRate,
                        double conditionAptitude,
                        double weightKg) {
        this.entryId = entryId;
        this.careerWinRate = careerWinRate;
        this.jockeyWinRate = jockeyWinRate;
        this.conditionAptitude = conditionAptitude;
        this.weightKg = weightKg;
    }

    public long entryId() {
        return entryId;
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

    public double weightKg() {
        return weightKg;
    }
}
