package com.example.keiba.service;

/**
 * 画面表示用の予想結果（1頭分）。
 *
 * @param horseNo        馬番
 * @param horseName      馬名
 * @param jockeyName     騎手名
 * @param winProbability 勝率(0.0〜1.0)
 * @param rank           勝率順位（1=本命）
 */
public record HorsePrediction(
        int horseNo,
        String horseName,
        String jockeyName,
        double winProbability,
        int rank) {

    /** パーセント表記（小数1桁）。 */
    public String winPercentLabel() {
        return String.format("%.1f%%", winProbability * 100.0);
    }
}
