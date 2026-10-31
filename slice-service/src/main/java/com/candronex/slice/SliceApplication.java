package com.candronex.slice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.candronex")
public class SliceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SliceApplication.class, args);
    }
}
