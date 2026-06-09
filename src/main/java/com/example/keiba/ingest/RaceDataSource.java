package com.example.keiba.ingest;

import com.example.keiba.domain.Race;
import java.util.List;

/**
 * レースデータの取得元を抽象化するインターフェース。
 *
 * <p>「どこからデータを取るか」を実装の差し替えで切り替えられるようにする
 * （依存性逆転の原則）。本番は {@code JvLinkRaceDataSource}（JRA-VAN Data Lab.）、
 * デモ/CI は {@code CsvRaceDataSource}（同梱サンプルCSV）を使う。</p>
 */
public interface RaceDataSource {

    /**
     * 取り込み可能なレース一覧を取得する。
     *
     * @return 出走表(entries)・馬(horse)・特徴量がセットされた Race のリスト
     */
    List<Race> loadRaces();
}
