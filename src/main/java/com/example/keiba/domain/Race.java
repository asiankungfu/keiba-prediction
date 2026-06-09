package com.example.keiba.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1つのレース（競走）を表すエンティティ。
 */
@Entity
@Table(name = "race")
@Getter
@Setter
@NoArgsConstructor
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** レース名（例: 日本ダービー）。 */
    @Column(nullable = false)
    private String name;

    /** 開催日。 */
    @Column(nullable = false)
    private LocalDate raceDate;

    /** 競馬場名（例: 東京）。 */
    @Column(nullable = false)
    private String course;

    /** 距離（メートル）。 */
    @Column(nullable = false)
    private int distanceMeters;

    /** 馬場（芝 / ダート）。 */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Surface surface;

    /** グレード（G1, G2, OP, 未勝利 など）。 */
    private String grade;

    /** この時点で結果が確定済みか（過去レース=true, 次走の予想対象=false）。 */
    @Column(nullable = false)
    private boolean finished;

    @OneToMany(mappedBy = "race", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RaceEntry> entries = new ArrayList<>();

    public void addEntry(RaceEntry entry) {
        entry.setRace(this);
        this.entries.add(entry);
    }
}
