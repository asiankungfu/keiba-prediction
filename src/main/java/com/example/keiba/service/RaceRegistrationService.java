package com.example.keiba.service;

import com.example.keiba.domain.Horse;
import com.example.keiba.domain.Race;
import com.example.keiba.domain.RaceEntry;
import com.example.keiba.domain.Surface;
import com.example.keiba.repository.HorseRepository;
import com.example.keiba.repository.RaceRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 手動入力でレース・出走馬を登録するサービス。
 *
 * <p>出馬表を画面から手入力して予想に使うための機能。スクレイピングに依存せず、
 * 公式の出馬表などを見ながらユーザー自身が入力する（規約・法的リスクを回避）。</p>
 *
 * <p>勝率算出に必要な特徴量は、入力しやすい値（通算出走数・勝利数、騎手勝率%、適性%）から
 * 内部で 0.0〜1.0 のレートに変換して保存する。</p>
 */
@Service
public class RaceRegistrationService {

    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;

    public RaceRegistrationService(RaceRepository raceRepository,
                                   HorseRepository horseRepository) {
        this.raceRepository = raceRepository;
        this.horseRepository = horseRepository;
    }

    @Transactional
    public Race createRace(String name, LocalDate raceDate, String course,
                           int distanceMeters, Surface surface, String grade) {
        Race race = new Race();
        race.setName(name);
        race.setRaceDate(raceDate);
        race.setCourse(course);
        race.setDistanceMeters(distanceMeters);
        race.setSurface(surface);
        race.setGrade(grade);
        race.setFinished(false);
        return raceRepository.save(race);
    }

    /**
     * 既存レースに1頭の出走馬を追加する。
     *
     * @param careerStarts          通算出走数
     * @param careerWins            通算勝利数（勝率 = 勝利数 / 出走数）
     * @param jockeyWinPercent      騎手勝率（%）
     * @param conditionPercent      条件適性（0〜100）
     */
    @Transactional
    public Race addEntry(Long raceId, int horseNo, int frameNo, String horseName, String sex,
                         String jockeyName, double weightKg,
                         int careerStarts, int careerWins,
                         double jockeyWinPercent, double conditionPercent) {
        Race race = raceRepository.findWithEntriesById(raceId);
        if (race == null) {
            throw new RaceNotFoundException(raceId);
        }

        Horse horse = new Horse();
        horse.setRegistrationNumber("MANUAL-" + UUID.randomUUID());
        horse.setName(horseName);
        horse.setSex(sex);
        horse = horseRepository.save(horse);

        RaceEntry e = new RaceEntry();
        e.setHorse(horse);
        e.setHorseNo(horseNo);
        e.setFrameNo(frameNo > 0 ? frameNo : (horseNo + 1) / 2);
        e.setJockeyName(jockeyName);
        e.setWeightKg(weightKg);
        e.setCareerStarts(careerStarts);
        e.setCareerWinRate(careerStarts > 0 ? (double) careerWins / careerStarts : 0.0);
        e.setJockeyWinRate(clamp01(jockeyWinPercent / 100.0));
        e.setConditionAptitude(clamp01(conditionPercent / 100.0));

        race.addEntry(e);
        return raceRepository.save(race);
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
