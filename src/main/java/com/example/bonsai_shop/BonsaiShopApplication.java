package com.example.bonsai_shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BonsaiShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BonsaiShopApplication.class, args);
    }
}
