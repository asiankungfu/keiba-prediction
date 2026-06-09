package com.example.keiba.config;

import com.example.keiba.service.WinProbabilityCalculator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DI 構成。設定値から勝率算出器を組み立てて Bean 登録する。
 */
@Configuration
@EnableConfigurationProperties(ModelProperties.class)
public class AppConfig {

    @Bean
    public WinProbabilityCalculator winProbabilityCalculator(ModelProperties props) {
        return new WinProbabilityCalculator(props.toWeights());
    }
}
