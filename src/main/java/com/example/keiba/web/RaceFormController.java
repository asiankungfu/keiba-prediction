package com.example.keiba.web;

import com.example.keiba.domain.Race;
import com.example.keiba.domain.Surface;
import com.example.keiba.repository.RaceRepository;
import com.example.keiba.service.RaceNotFoundException;
import com.example.keiba.service.RaceRegistrationService;
import java.time.LocalDate;
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

    public RaceFormController(RaceRegistrationService registrationService,
                              RaceRepository raceRepository) {
        this.registrationService = registrationService;
        this.raceRepository = raceRepository;
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
}
