package com.example.keiba.service;

import com.example.keiba.domain.Race;
import com.example.keiba.domain.RaceEntry;
import com.example.keiba.repository.RaceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * レース単位の勝率予想を取りまとめるアプリケーションサービス。
 *
 * <p>永続化層(JPA)から取得した出走表を計算用の値オブジェクトに変換し、
 * {@link WinProbabilityCalculator} で勝率を算出して、画面表示用 DTO に整形する。</p>
 */
@Service
public class PredictionService {

    private final RaceRepository raceRepository;
    private final WinProbabilityCalculator calculator;

    public PredictionService(RaceRepository raceRepository,
                             WinProbabilityCalculator calculator) {
        this.raceRepository = raceRepository;
        this.calculator = calculator;
    }

    /** 予想対象（結果未確定）のレース一覧。 */
    @Transactional(readOnly = true)
    public List<Race> upcomingRaces() {
        return raceRepository.findByFinishedFalseOrderByRaceDateAsc();
    }

    @Transactional(readOnly = true)
    public Race getRace(Long raceId) {
        Race race = raceRepository.findWithEntriesById(raceId);
        if (race == null) {
            throw new RaceNotFoundException(raceId);
        }
        return race;
    }

    /**
     * 指定レースの各馬の勝率を算出し、勝率の高い順に並べて返す。
     */
    @Transactional(readOnly = true)
    public List<HorsePrediction> predict(Long raceId) {
        Race race = getRace(raceId);

        List<ScoringInput> inputs = new ArrayList<>();
        for (RaceEntry e : race.getEntries()) {
            inputs.add(new ScoringInput(
                    e.getId(),
                    e.getCareerWinRate(),
                    e.getJockeyWinRate(),
                    e.getConditionAptitude(),
                    e.getWeightKg()));
        }

        Map<Long, Double> probabilities = calculator.calculateWinProbabilities(inputs);
        Map<Long, Double> top3Probabilities = calculator.calculateTop3Probabilities(inputs);

        List<HorsePrediction> predictions = new ArrayList<>();
        for (RaceEntry e : race.getEntries()) {
            double p = probabilities.getOrDefault(e.getId(), 0.0);
            double t3 = top3Probabilities.getOrDefault(e.getId(), 0.0);
            predictions.add(new HorsePrediction(
                    e.getHorseNo(),
                    e.getHorse().getName(),
                    e.getJockeyName(),
                    p,
                    t3,
                    0 /* rank は後で付与 */));
        }

        // 勝率降順に並べ、順位を採番し直す
        predictions.sort(Comparator.comparingDouble(HorsePrediction::winProbability).reversed());
        List<HorsePrediction> ranked = new ArrayList<>(predictions.size());
        for (int i = 0; i < predictions.size(); i++) {
            HorsePrediction p = predictions.get(i);
            ranked.add(new HorsePrediction(
                    p.horseNo(), p.horseName(), p.jockeyName(),
                    p.winProbability(), p.top3Probability(), i + 1));
        }
        return ranked;
    }
}
