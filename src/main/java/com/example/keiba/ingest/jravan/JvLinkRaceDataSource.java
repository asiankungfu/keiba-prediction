package com.example.keiba.ingest.jravan;

import com.example.keiba.domain.Race;
import com.example.keiba.ingest.RaceDataSource;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * JRA-VAN Data Lab.（JV-Link）からレースデータを取得する {@link RaceDataSource} 実装。
 *
 * <p><b>前提条件</b></p>
 * <ul>
 *   <li>JRA-VAN Data Lab. の会員契約（利用には契約とキーが必要）。</li>
 *   <li>JV-Link は Windows 用 ActiveX(COM) コンポーネント。Java からは
 *       <a href="https://sourceforge.net/projects/jacob-project/">JACOB</a>
 *       （Java-COM Bridge）等を介して呼び出す。Linux/Mac では動作しない。</li>
 * </ul>
 *
 * <p><b>JV-Link を用いたデータ取得の標準的な流れ</b>（JV-Data 仕様書準拠）</p>
 * <ol>
 *   <li>{@code JVInit(sid)} … サービスキー検証・初期化</li>
 *   <li>{@code JVOpen(dataspec, fromtime, option, ...)} … 蓄積系データの読み出し開始
 *       （リアルタイム系は {@code JVRTOpen}）</li>
 *   <li>{@code JVRead(...)} … レコードを1件ずつバイナリで読み出し、
 *       レコード種別IDに応じて JV-Data 構造体へパースする</li>
 *   <li>{@code JVClose()} … クローズ</li>
 * </ol>
 *
 * <p>本クラスはアーキテクチャ上の差し込み口（ポート）であり、
 * {@code keiba.datasource=jvlink} 指定時に有効化される。実際の COM 連携は
 * 別途 JACOB を依存に追加して実装する。</p>
 */
@Component
@ConditionalOnProperty(name = "keiba.datasource", havingValue = "jvlink")
public class JvLinkRaceDataSource implements RaceDataSource {

    @Override
    public List<Race> loadRaces() {
        // TODO: JACOB 経由で JVInit → JVOpen → JVRead → JVClose を実装し、
        //       読み出したレコードを Race / RaceEntry / Horse にマッピングする。
        throw new UnsupportedOperationException(
                "JV-Link 連携は Windows + JACOB + JRA-VAN 契約が必要です。"
                        + "デモ実行では keiba.datasource=csv（デフォルト）を使用してください。");
    }
}
