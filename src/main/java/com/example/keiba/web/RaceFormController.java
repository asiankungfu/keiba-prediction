package com.example.keiba.web;

import com.example.keiba.domain.Race;
import com.example.keiba.domain.Surface;
import com.example.keiba.repository.RaceRepository;
import com.example.keiba.service.EvaluationService;
import com.example.keiba.service.RaceNotFoundException;
import com.example.keiba.service.RaceRegistrationService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 手動入力でレース・出走馬を登録する画面。
 */
@Controller
public class RaceFormController {

    private final RaceRegistrationService registrationService;
    private final RaceRepository raceRepository;
    private final EvaluationService evaluationService;

    public RaceFormController(RaceRegistrationService registrationService,
                              RaceRepository raceRepository,
                              EvaluationService evaluationService) {
        this.registrationService = registrationService;
        this.raceRepository = raceRepository;
        this.evaluationService = evaluationService;
    }

    /** 新規レース作成フォーム。 */
    @GetMapping("/races/new")
    public String newRace(Model model) {
        model.addAttribute("surfaces", Surface.values());
        return "race_new";
    }

    /** レース作成 → 出走馬の入力画面へ。 */
    @PostMapping("/races")
    public String createRace(@RequestParam String name,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate raceDate,
                             @RequestParam String course,
                             @RequestParam int distanceMeters,
                             @RequestParam Surface surface,
                             @RequestParam(required = false) String grade) {
        Race race = registrationService.createRace(name, raceDate, course, distanceMeters, surface, grade);
        return "redirect:/races/" + race.getId() + "/edit";
    }

    /** 出走馬の入力画面（現在の登録一覧＋追加フォーム）。 */
    @GetMapping("/races/{id}/edit")
    public String editRace(@PathVariable Long id, Model model) {
        Race race = raceRepository.findWithEntriesById(id);
        if (race == null) {
            throw new RaceNotFoundException(id);
        }
        model.addAttribute("race", race);
        return "race_edit";
    }

    /** 出走馬を1頭追加。 */
    @PostMapping("/races/{id}/entries")
    public String addEntry(@PathVariable Long id,
                           @RequestParam int horseNo,
                           @RequestParam(defaultValue = "0") int frameNo,
                           @RequestParam String horseName,
                           @RequestParam(required = false) String sex,
                           @RequestParam(required = false) String jockeyName,
                           @RequestParam(defaultValue = "55.0") double weightKg,
                           @RequestParam(defaultValue = "0") int careerStarts,
                           @RequestParam(defaultValue = "0") int careerWins,
                           @RequestParam(defaultValue = "0") double jockeyWinPercent,
                           @RequestParam(defaultValue = "50") double conditionPercent) {
        registrationService.addEntry(id, horseNo, frameNo, horseName, sex, jockeyName,
                weightKg, careerStarts, careerWins, jockeyWinPercent, conditionPercent);
        return "redirect:/races/" + id + "/edit";
    }

    /** 出馬表を一括登録（カンマ区切りテキストを貼り付け）。 */
    @PostMapping("/races/{id}/entries/bulk")
    public String addEntriesBulk(@PathVariable Long id, @RequestParam String bulk) {
        registrationService.addEntriesBulk(id, bulk);
        return "redirect:/races/" + id + "/edit";
    }

    /** 結果照合（バックテスト）画面：予想 vs 実着順、着順入力フォーム。 */
    @GetMapping("/races/{id}/result")
    public String result(@PathVariable Long id, Model model) {
        Race race = raceRepository.findWithEntriesById(id);
        if (race == null) {
            throw new RaceNotFoundException(id);
        }
        model.addAttribute("race", race);
        model.addAttribute("evaluation", evaluationService.evaluate(id));
        return "result";
    }

    /** 実着順を保存（finish_<entryId> 形式のパラメータ）。 */
    @PostMapping("/races/{id}/results")
    public String saveResults(@PathVariable Long id, @RequestParam Map<String, String> params) {
        Map<Long, Integer> finishByEntryId = new HashMap<>();
        for (Map.Entry<String, String> en : params.entrySet()) {
            if (en.getKey().startsWith("finish_") && en.getValue() != null && !en.getValue().isBlank()) {
                try {
                    Long entryId = Long.parseLong(en.getKey().substring("finish_".length()));
                    finishByEntryId.put(entryId, Integer.parseInt(en.getValue().trim()));
                } catch (NumberFormatException ignored) {
                    // 数値以外は無視
                }
            }
        }
        registrationService.setResults(id, finishByEntryId);
        return "redirect:/races/" + id + "/result";
    }
}
