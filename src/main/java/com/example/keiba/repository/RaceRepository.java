package com.example.keiba.repository;

import com.example.keiba.domain.Race;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * レースの永続化を担うリポジトリ。
 */
public interface RaceRepository extends JpaRepository<Race, Long> {

    /** これから行われる（結果未確定の）レースを開催日昇順で取得。 */
    List<Race> findByFinishedFalseOrderByRaceDateAsc();

    /** 出走表(entries)・馬(horse)を一括フェッチして N+1 を避ける。 */
    @EntityGraph(attributePaths = {"entries", "entries.horse"})
    Race findWithEntriesById(Long id);
}
