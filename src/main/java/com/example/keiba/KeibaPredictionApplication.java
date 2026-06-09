package com.example.keiba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * アプリケーションのエントリポイント。
 *
 * <p>JRA-VAN Data Lab. から取り込んだレースデータをもとに、
 * 次走の各出走馬の勝率を算出して表示するWebアプリ。</p>
 */
@SpringBootApplication
public class KeibaPredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeibaPredictionApplication.class, args);
    }
}
