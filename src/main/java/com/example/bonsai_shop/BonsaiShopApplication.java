package com.example.bonsai_shop;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BonsaiShopApplication {

    /**
     * Đặt múi giờ mặc định cho toàn bộ JVM về Asia/Ho_Chi_Minh (UTC+7).
     * Cần thiết khi deploy trên môi trường Cloud (GCP, AWS...) chạy UTC+0.
     * Gọi trước khi bất kỳ bean nào được khởi tạo để đảm bảo nhất quán.
     */
    @PostConstruct
    void initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BonsaiShopApplication.class, args);
    }
}