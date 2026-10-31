package com.candronex.slice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.candronex")
@EnableScheduling
public class SliceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SliceApplication.class, args);
    }
}
