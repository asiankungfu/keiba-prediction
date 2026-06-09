package com.example.keiba.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * あるレースへの1頭の出走（出走表の1行）を表すエンティティ。
 *
 * <p>勝率算出に用いる特徴量（過去成績のスナップショット）を併せて保持する。
 * これらは JRA-VAN の蓄積データを集計して取り込み時に算出する。</p>
 */
@Entity
@Table(name = "race_entry")
@Getter
@Setter
@NoArgsConstructor
public class RaceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "race_id")
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horse_id")
    private Horse horse;

    /** 枠番。 */
    private int frameNo;

    /** 馬番。 */
    @Column(nullable = false)
    private int horseNo;

    /** 騎手名。 */
    private String jockeyName;

    /** 斤量（kg）。 */
    private double weightKg;

    // --- 勝率算出に用いる特徴量 ---

    /** 通算出走回数。 */
    private int careerStarts;

    /** 通算勝率（0.0〜1.0）。 */
    private double careerWinRate;

    /** 騎手の直近勝率（0.0〜1.0）。 */
    private double jockeyWinRate;

    /** 当該条件（距離・馬場）への適性（0.0〜1.0）。 */
    private double conditionAptitude;

    // --- 結果（確定後のみ） ---

    /** 着順（1着=1）。未確定は null。 */
    private Integer finishPosition;

    /** 単勝オッズ（任意・参考値）。 */
    private Double odds;
}
