package com.chainauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChainAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChainAuthApplication.class, args);
    }
}
