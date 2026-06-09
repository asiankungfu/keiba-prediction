package com.example.keiba.repository;

import com.example.keiba.domain.Horse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 競走馬の永続化を担うリポジトリ。
 */
public interface HorseRepository extends JpaRepository<Horse, Long> {

    Optional<Horse> findByRegistrationNumber(String registrationNumber);
}
