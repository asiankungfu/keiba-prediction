package com.example.keiba.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WinProbabilityCalculatorTest {

    private final WinProbabilityCalculator calculator =
            new WinProbabilityCalculator(ModelWeights.defaults());

    @Test
    @DisplayName("勝率の合計はほぼ1.0になる")
    void probabilitiesSumToOne() {
        List<ScoringInput> inputs = List.of(
                new ScoringInput(1, 0.30, 0.20, 0.80, 57.0),
                new ScoringInput(2, 0.10, 0.10, 0.50, 55.0),
                new ScoringInput(3, 0.20, 0.15, 0.60, 56.0));

        Map<Long, Double> probs = calculator.calculateWinProbabilities(inputs);

        double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("成績が良い馬ほど勝率が高くなる")
    void strongerHorseHasHigherProbability() {
        ScoringInput strong = new ScoringInput(1, 0.40, 0.25, 0.90, 55.0);
        ScoringInput weak = new ScoringInput(2, 0.05, 0.05, 0.30, 55.0);

        Map<Long, Double> probs =
                calculator.calculateWinProbabilities(List.of(strong, weak));

        assertThat(probs.get(1L)).isGreaterThan(probs.get(2L));
    }

    @Test
    @DisplayName("全頭の特徴量が同じなら勝率は均等になる")
    void identicalHorsesHaveEqualProbability() {
        List<ScoringInput> inputs = List.of(
                new ScoringInput(1, 0.2, 0.1, 0.5, 56.0),
                new ScoringInput(2, 0.2, 0.1, 0.5, 56.0),
                new ScoringInput(3, 0.2, 0.1, 0.5, 56.0),
                new ScoringInput(4, 0.2, 0.1, 0.5, 56.0));

        Map<Long, Double> probs = calculator.calculateWinProbabilities(inputs);

        probs.values().forEach(p ->
                assertThat(p).isCloseTo(0.25, org.assertj.core.data.Offset.offset(1e-9)));
    }

    @Test
    @DisplayName("空入力では空の結果を返す")
    void emptyInputReturnsEmpty() {
        assertThat(calculator.calculateWinProbabilities(List.of())).isEmpty();
    }
}
