package com.example.keiba.ingest;

import com.example.keiba.domain.Horse;
import com.example.keiba.domain.Race;
import com.example.keiba.domain.RaceEntry;
import com.example.keiba.repository.HorseRepository;
import com.example.keiba.repository.RaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 起動時に {@link RaceDataSource} からレースを取り込み、DBへ投入するランナー。
 *
 * <p>DBが空のときだけ実行する（冪等）。馬は血統登録番号で重複排除する。</p>
 */
@Component
public class DataIngestionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionRunner.class);

    private final RaceDataSource dataSource;
    private final RaceRepository raceRepository;
    private final HorseRepository horseRepository;

    public DataIngestionRunner(RaceDataSource dataSource,
                               RaceRepository raceRepository,
                               HorseRepository horseRepository) {
        this.dataSource = dataSource;
        this.raceRepository = raceRepository;
        this.horseRepository = horseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (raceRepository.count() > 0) {
            log.info("既にデータが存在するため取り込みをスキップします。");
            return;
        }

        var races = dataSource.loadRaces();
        for (Race race : races) {
            for (RaceEntry entry : race.getEntries()) {
                Horse incoming = entry.getHorse();
                Horse persisted = horseRepository
                        .findByRegistrationNumber(incoming.getRegistrationNumber())
                        .orElseGet(() -> horseRepository.save(incoming));
                entry.setHorse(persisted);
            }
        }
        raceRepository.saveAll(races);
        log.info("{} 件のレースを取り込みました。", races.size());
    }
}
