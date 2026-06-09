package com.example.keiba.web;

import com.example.keiba.domain.Race;
import com.example.keiba.service.HorsePrediction;
import com.example.keiba.service.PredictionService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 画面（Thymeleaf）を返すコントローラ。
 */
@Controller
public class RaceViewController {

    private final PredictionService predictionService;

    public RaceViewController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    /** トップ：予想対象レースの一覧。 */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("races", predictionService.upcomingRaces());
        return "index";
    }

    /** レース詳細：各馬の勝率を表示。 */
    @GetMapping("/races/{id}")
    public String race(@PathVariable Long id, Model model) {
        Race race = predictionService.getRace(id);
        List<HorsePrediction> predictions = predictionService.predict(id);
        model.addAttribute("race", race);
        model.addAttribute("predictions", predictions);
        return "race";
    }
}
