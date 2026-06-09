package com.example.keiba.service;

import com.example.keiba.domain.Race;
import com.example.keiba.domain.RaceEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 予想と実際の着順を照合する評価（バックテスト）サービス。
 *
 * <p>「本命（予想1位）が1着だったか」「実際の1着が予想上位3頭に入っていたか」を判定し、
 * 手入力した実レースに対してモデルの的中を確認できるようにする。</p>
 */
@Service
public class EvaluationService {

    private final PredictionService predictionService;

    public EvaluationService(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @Transactional(readOnly = true)
    public RaceEvaluation evaluate(Long raceId) {
        Race race = predictionService.getRace(raceId);

        // 実着順マップ（馬番 -> 着順）
        Map<Integer, Integer> finishByHorseNo = new HashMap<>();
        Map<Integer, String> nameByHorseNo = new HashMap<>();
        Integer actualWinnerNo = null;
        String actualWinnerName = null;
        for (RaceEntry e : race.getEntries()) {
            nameByHorseNo.put(e.getHorseNo(), e.getHorse().getName());
            if (e.getFinishPosition() != null) {
                finishByHorseNo.put(e.getHorseNo(), e.getFinishPosition());
                if (e.getFinishPosition() == 1) {
                    actualWinnerNo = e.getHorseNo();
                    actualWinnerName = e.getHorse().getName();
                }
            }
        }
        boolean hasResults = !finishByHorseNo.isEmpty();

        // 予想（勝率降順）
        List<HorsePrediction> predictions = predictionService.predict(raceId);

        boolean top1Hit = false;
        boolean top3Hit = false;
        if (actualWinnerNo != null && !predictions.isEmpty()) {
            top1Hit = predictions.get(0).horseNo() == actualWinnerNo;
            for (int i = 0; i < Math.min(3, predictions.size()); i++) {
                if (predictions.get(i).horseNo() == actualWinnerNo) {
                    top3Hit = true;
                    break;
                }
            }
        }

        List<RaceEvaluation.Row> rows = new ArrayList<>();
        for (HorsePrediction p : predictions) {
            Integer finish = finishByHorseNo.get(p.horseNo());
            boolean isWinner = actualWinnerNo != null && p.horseNo() == actualWinnerNo;
            rows.add(new RaceEvaluation.Row(
                    p.rank(), p.horseNo(), p.horseName(), p.winPercentLabel(), finish, isWinner));
        }

        return new RaceEvaluation(hasResults, actualWinnerNo, actualWinnerName, top1Hit, top3Hit, rows);
    }
}
