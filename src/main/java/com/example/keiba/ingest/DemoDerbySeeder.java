package com.example.keiba.ingest;

import com.example.keiba.domain.Race;
import com.example.keiba.domain.RaceEntry;
import com.example.keiba.domain.Surface;
import com.example.keiba.repository.RaceRepository;
import com.example.keiba.service.RaceRegistrationService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * デモ用に「2026年 日本ダービー（東京優駿・G1）」を出走馬＋実着順つきで起動時に投入する。
 *
 * <p>コース特徴を反映した条件適性の例:
 * ダービーは「東京・芝2400・左回り」なので、各馬の
 * <b>「芝・左回り(東京/中京/新潟)・中長距離(1800〜2400m)の3着内率」</b>を条件適性に用いている。
 * 通算勝率はベイズ縮小（{@code RaceRegistrationService}）で小標本の過信を補正。</p>
 */
@Component
@Order(3)
public class DemoDerbySeeder implements CommandLineRunner {

    private static final String RACE_NAME = "日本ダービー";

    private static final String ENTRIES_CSV = """
            17,ロブチェン,牡,松山,57,4,3,12,70
            13,パントルナイーフ,牡,ルメール,57,4,2,12,70
            5,バステール,牡,川田,57,4,2,12,50
            14,ゴーイントゥスカイ,牡,武豊,57,4,2,12,80
            2,マテンロウゲイル,牡,横山和,57,6,2,12,45
            4,アルトラムス,牡,横山武,57,3,2,12,30
            11,リアライズシリウス,牡,津村,57,5,3,12,65
            1,ライヒスアドラー,牡,佐々木,57,4,1,12,55
            6,コンジェスタス,牡,西村淳,57,3,3,12,45
            12,アスクエジンバラ,牡,岩田康,57,8,2,12,40
            9,アウダーシア,牡,レーン,57,4,2,12,50
            15,フォルテアンジェロ,牡,荻野極,57,4,1,12,50
            7,メイショウハチコウ,牡,ディー,57,5,3,12,55
            18,エムズビギン,牡,ゴンサルベス,57,4,1,12,45
            3,ケントン,牡,丹内,57,6,2,12,25
            16,グリーンエナジー,牡,戸崎圭,57,4,2,12,55
            8,ショウナンガルフ,牡,浜中,57,4,2,12,30
            10,ジャスティンビスタ,牡,坂井,57,3,2,12,30
            """;

    /** 確定着順（馬番 -> 着順）。 */
    private static final Map<Integer, Integer> FINISH_BY_HORSE_NO = Map.ofEntries(
            Map.entry(17, 1), Map.entry(13, 2), Map.entry(5, 3), Map.entry(14, 4),
            Map.entry(2, 5), Map.entry(4, 6), Map.entry(11, 7), Map.entry(1, 8),
            Map.entry(6, 9), Map.entry(12, 10), Map.entry(9, 11), Map.entry(15, 12),
            Map.entry(7, 13), Map.entry(18, 14), Map.entry(3, 15), Map.entry(16, 16),
            Map.entry(8, 17), Map.entry(10, 18)
    );

    private final RaceRepository raceRepository;
    private final RaceRegistrationService registrationService;

    public DemoDerbySeeder(RaceRepository raceRepository,
                           RaceRegistrationService registrationService) {
        this.raceRepository = raceRepository;
        this.registrationService = registrationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean exists = raceRepository.findAll().stream()
                .anyMatch(r -> RACE_NAME.equals(r.getName()));
        if (exists) {
            return;
        }

        Race race = registrationService.createRace(
                RACE_NAME, LocalDate.of(2026, 5, 31), "東京", 2400, Surface.TURF, "G1");
        registrationService.addEntriesBulk(race.getId(), ENTRIES_CSV);

        Race saved = raceRepository.findWithEntriesById(race.getId());
        for (RaceEntry e : saved.getEntries()) {
            Integer pos = FINISH_BY_HORSE_NO.get(e.getHorseNo());
            if (pos != null) {
                e.setFinishPosition(pos);
            }
        }
        raceRepository.save(saved);
    }
}
