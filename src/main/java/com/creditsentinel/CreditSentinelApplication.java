package com.creditsentinel;

import com.creditsentinel.config.CreditProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CreditProperties.class)
public class CreditSentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditSentinelApplication.class, args);
    }
}
