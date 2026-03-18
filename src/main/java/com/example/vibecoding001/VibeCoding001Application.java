package com.example.vibecoding001;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"com.example.vibecoding001.mapper", "com.example.vibecoding001.payment.repository"})
public class VibeCoding001Application {

    public static void main(String[] args) {
        SpringApplication.run(VibeCoding001Application.class, args);
    }

}
