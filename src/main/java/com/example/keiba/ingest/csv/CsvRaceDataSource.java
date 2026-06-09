package com.example.keiba.ingest.csv;

import com.example.keiba.domain.Horse;
import com.example.keiba.domain.Race;
import com.example.keiba.domain.RaceEntry;
import com.example.keiba.domain.Surface;
import com.example.keiba.ingest.RaceDataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 同梱の CSV からレースデータを読み込む {@link RaceDataSource} 実装。
 *
 * <p>JRA-VAN の契約やWindows環境が無くてもアプリを起動・デモできるようにするための
 * 取得元。{@code keiba.datasource=csv}（デフォルト）のとき有効になる。</p>
 */
@Component
@ConditionalOnProperty(name = "keiba.datasource", havingValue = "csv", matchIfMissing = true)
public class CsvRaceDataSource implements RaceDataSource {

    private static final String RESOURCE = "sample-data/races.csv";

    @Override
    public List<Race> loadRaces() {
        // レースキー(レース名+日付+競馬場) -> Race
        Map<String, Race> raceByKey = new LinkedHashMap<>();

        ClassPathResource resource = new ClassPathResource(RESOURCE);
        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // ヘッダ行を読み飛ばす
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] c = line.split(",", -1);
                Row row = Row.parse(c);

                Race race = raceByKey.computeIfAbsent(row.raceKey(), k -> row.toRace());
                race.addEntry(row.toEntry());
            }
        } catch (Exception e) {
            throw new IllegalStateException("サンプルCSVの読み込みに失敗しました: " + RESOURCE, e);
        }

        return new ArrayList<>(raceByKey.values());
    }

    /** CSV 1行を表す内部レコード。 */
    private record Row(
            String raceName, LocalDate raceDate, String course, int distance, Surface surface,
            String grade, int horseNo, int frameNo, String horseReg, String horseName,
            String sex, int birthYear, String jockey, double weight, int careerStarts,
            double careerWinRate, double jockeyWinRate, double conditionAptitude) {

        static Row parse(String[] c) {
            return new Row(
                    c[0].trim(),
                    LocalDate.parse(c[1].trim()),
                    c[2].trim(),
                    Integer.parseInt(c[3].trim()),
                    Surface.valueOf(c[4].trim()),
                    c[5].trim(),
                    Integer.parseInt(c[6].trim()),
                    Integer.parseInt(c[7].trim()),
                    c[8].trim(),
                    c[9].trim(),
                    c[10].trim(),
                    Integer.parseInt(c[11].trim()),
                    c[12].trim(),
                    Double.parseDouble(c[13].trim()),
                    Integer.parseInt(c[14].trim()),
                    Double.parseDouble(c[15].trim()),
                    Double.parseDouble(c[16].trim()),
                    Double.parseDouble(c[17].trim()));
        }

        String raceKey() {
            return raceName + "|" + raceDate + "|" + course;
        }

        Race toRace() {
            Race r = new Race();
            r.setName(raceName);
            r.setRaceDate(raceDate);
            r.setCourse(course);
            r.setDistanceMeters(distance);
            r.setSurface(surface);
            r.setGrade(grade);
            r.setFinished(false);
            return r;
        }

        RaceEntry toEntry() {
            Horse horse = new Horse();
            horse.setRegistrationNumber(horseReg);
            horse.setName(horseName);
            horse.setSex(sex);
            horse.setBirthYear(birthYear);

            RaceEntry e = new RaceEntry();
            e.setHorse(horse);
            e.setHorseNo(horseNo);
            e.setFrameNo(frameNo);
            e.setJockeyName(jockey);
            e.setWeightKg(weight);
            e.setCareerStarts(careerStarts);
            e.setCareerWinRate(careerWinRate);
            e.setJockeyWinRate(jockeyWinRate);
            e.setConditionAptitude(conditionAptitude);
            return e;
        }
    }
}
