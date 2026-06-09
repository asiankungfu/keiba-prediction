package com.example.keiba.web;

import com.example.keiba.service.HorsePrediction;
import com.example.keiba.service.PredictionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 勝率予想を JSON で返す REST API。
 *
 * <p>例: {@code GET /api/races/1/predictions}</p>
 */
@RestController
@RequestMapping("/api")
public class PredictionApiController {

    private final PredictionService predictionService;

    public PredictionApiController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/races/{id}/predictions")
    public List<HorsePrediction> predictions(@PathVariable Long id) {
        return predictionService.predict(id);
    }
}
