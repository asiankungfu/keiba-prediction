package com.example.keiba.ingest;

import com.example.keiba.domain.Race;
import com.example.keiba.domain.RaceEntry;
import com.example.keiba.domain.Surface;
import com.example.keiba.repository.RaceRepository;
import com.example.keiba.service.RaceRegistrationService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * デモ用に「2026年 安田記念（G1）」を出走馬＋実着順つきで起動時に自動投入するランナー。
 *
 * <p>手入力を不要にし、結果照合（バックテスト）まで即座に確認できるようにするためのもの。
 * 特徴量は公開の出馬表・戦績を見て手入力した値（スクレイピングには非依存）。
 * 既に同名レースがあれば何もしない（冪等）。</p>
 */
@Component
@Order(2)
public class DemoYasudaSeeder implements CommandLineRunner {

    private static final String RACE_NAME = "安田記念";

    /**
     * 出馬表（馬番,馬名,性別,騎手,斤量,通算出走,通算勝利,騎手勝率%,条件適性）。
     *
     * <p>条件適性は主観ではなく、コース特徴を反映した<b>データ駆動値</b>:
     * 安田記念は「東京・芝1600・左回り」なので、各馬の
     * <b>「同じ馬場(芝)・同じ回り(左回り＝東京/中京/新潟)・似た距離(1400〜1800m)の3着内率」</b>
     * を100倍して用いる。東京単独だとサンプルが少ないため、似たコースまで広げて安定させている。
     * 坂・直線長・回りといった特徴は、その競馬場の実績にすでに織り込まれている
     * （詳細は docs/course-profiles.md）。</p>
     */
    private static final String ENTRIES_CSV = """
            1,レーベンスティール,牡,戸崎圭,58,17,7,12,85
            2,ロングラン,セ,ゴンサルベス,58,25,5,12,25
            3,オフトレイル,牡,菅原明,58,18,3,12,35
            4,シックスペンス,牡,武豊,58,12,5,12,50
            5,サクラトゥジュール,セ,佐々木,58,28,6,12,30
            6,ステレンボッシュ,牝,レーン,56,14,3,12,30
            7,スズハローム,牡,藤懸,58,20,5,12,30
            8,シャンパンカラー,牡,岩田康,58,18,3,12,45
            9,ウォーターリヒト,牡,高杉,58,14,3,12,50
            10,ルクソールカフェ,牡,岩田望,58,12,6,12,20
            11,ワールズエンド,牡,津村,58,10,6,12,70
            12,シリウスコルト,牡,横山和,58,21,4,12,20
            13,セイウンハーデス,牡,幸,58,18,5,12,25
            14,ガイアフォース,牡,横山武,58,21,5,12,80
            15,ドラゴンブースト,牡,丹内,58,12,3,12,30
            16,パンジャタワー,牡,松山,58,9,4,12,80
            17,トロヴァトーレ,牡,ルメール,58,16,9,12,68
            """;

    /** 戦績・結果から読み取った実着順（馬番 -> 着順）。判明分のみ（3・15・16着は未確定）。 */
    private static final Map<Integer, Integer> FINISH_BY_HORSE_NO = Map.ofEntries(
            Map.entry(4, 1),    // シックスペンス（1着）
            Map.entry(11, 2),   // ワールズエンド（2着）
            Map.entry(13, 4),   // セイウンハーデス
            Map.entry(16, 5),   // パンジャタワー
            Map.entry(3, 6),    // オフトレイル
            Map.entry(1, 7),    // レーベンスティール
            Map.entry(15, 8),   // ドラゴンブースト
            Map.entry(17, 9),   // トロヴァトーレ
            Map.entry(6, 10),   // ステレンボッシュ
            Map.entry(10, 11),  // ルクソールカフェ
            Map.entry(8, 12),   // シャンパンカラー
            Map.entry(9, 13),   // ウォーターリヒト
            Map.entry(12, 14),  // シリウスコルト
            Map.entry(7, 17)    // スズハローム
    );

    private final RaceRepository raceRepository;
    private final RaceRegistrationService registrationService;

    public DemoYasudaSeeder(RaceRepository raceRepository,
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
                RACE_NAME, LocalDate.of(2026, 6, 7), "東京", 1600, Surface.TURF, "G1");
        registrationService.addEntriesBulk(race.getId(), ENTRIES_CSV);

        // 判明している実着順をセット（結果照合がすぐ動くように）
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
