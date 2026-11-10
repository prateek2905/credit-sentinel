package com.creditsentinel.config;

import com.creditsentinel.detection.BreachDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DetectionConfig {

    @Bean
    BreachDetector breachDetector() {
        return new BreachDetector();
    }
}
