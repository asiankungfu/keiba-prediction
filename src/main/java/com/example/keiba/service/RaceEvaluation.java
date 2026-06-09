package com.example.keiba.service;

import java.util.List;

/**
 * 予想と実際の結果を照合した評価（バックテスト）結果。
 *
 * @param hasResults          実着順が入力済みか
 * @param actualWinnerHorseNo 実際の1着の馬番（未確定は null）
 * @param actualWinnerName    実際の1着の馬名
 * @param top1Hit             本命（予想1位）が実際に1着だったか
 * @param top3Hit             実際の1着が予想上位3頭以内だったか
 * @param rows                予想順に並べた各馬の行（予想順位＋実着順）
 */
public record RaceEvaluation(
        boolean hasResults,
        Integer actualWinnerHorseNo,
        String actualWinnerName,
        boolean top1Hit,
        boolean top3Hit,
        List<Row> rows) {

    /**
     * 照合表示用の1行。
     *
     * @param predRank       予想順位（1=本命）
     * @param horseNo        馬番
     * @param horseName      馬名
     * @param winPercent     予想勝率の表示文字列
     * @param actualFinish   実着順（未確定は null）
     * @param isActualWinner 実際の1着か
     */
    public record Row(int predRank, int horseNo, String horseName, String winPercent,
                      Integer actualFinish, boolean isActualWinner) {
    }
}
