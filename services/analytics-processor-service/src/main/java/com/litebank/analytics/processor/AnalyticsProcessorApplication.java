package com.litebank.analytics.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class AnalyticsProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsProcessorApplication.class, args);
    }
}
