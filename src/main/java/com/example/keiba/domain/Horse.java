package com.example.keiba.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 競走馬を表すエンティティ。
 *
 * <p>JRA-VAN の血統登録番号を業務キーとして保持する。</p>
 */
@Entity
@Table(name = "horse")
@Getter
@Setter
@NoArgsConstructor
public class Horse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 血統登録番号（JRA-VAN の一意キー）。 */
    @Column(nullable = false, unique = true)
    private String registrationNumber;

    /** 馬名。 */
    @Column(nullable = false)
    private String name;

    /** 性別（牡/牝/セ）。 */
    private String sex;

    /** 生年。 */
    private Integer birthYear;
}
