package com.example.keiba.config;

import com.example.keiba.service.ModelWeights;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 勝率算出モデルの重みを application.yml からバインドする設定クラス。
 *
 * <pre>
 * keiba:
 *   model:
 *     intercept: 0.0
 *     career-win-rate: 4.0
 *     ...
 * </pre>
 */
@ConfigurationProperties(prefix = "keiba.model")
public class ModelProperties {

    private double intercept = 0.0;
    private double careerWinRate = 4.0;
    private double jockeyWinRate = 2.5;
    private double conditionAptitude = 2.0;
    private double weightPenalty = 0.05;
    private double baseWeightKg = 55.0;

    public ModelWeights toWeights() {
        return new ModelWeights(
                intercept, careerWinRate, jockeyWinRate,
                conditionAptitude, weightPenalty, baseWeightKg);
    }

    public double getIntercept() {
        return intercept;
    }

    public void setIntercept(double intercept) {
        this.intercept = intercept;
    }

    public double getCareerWinRate() {
        return careerWinRate;
    }

    public void setCareerWinRate(double careerWinRate) {
        this.careerWinRate = careerWinRate;
    }

    public double getJockeyWinRate() {
        return jockeyWinRate;
    }

    public void setJockeyWinRate(double jockeyWinRate) {
        this.jockeyWinRate = jockeyWinRate;
    }

    public double getConditionAptitude() {
        return conditionAptitude;
    }

    public void setConditionAptitude(double conditionAptitude) {
        this.conditionAptitude = conditionAptitude;
    }

    public double getWeightPenalty() {
        return weightPenalty;
    }

    public void setWeightPenalty(double weightPenalty) {
        this.weightPenalty = weightPenalty;
    }

    public double getBaseWeightKg() {
        return baseWeightKg;
    }

    public void setBaseWeightKg(double baseWeightKg) {
        this.baseWeightKg = baseWeightKg;
    }
}
