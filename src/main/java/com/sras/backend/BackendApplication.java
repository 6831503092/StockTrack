package com.sras.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Bangkok"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Bangkok"));
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner printUrl() {
        return args -> {
            System.out.println("\n----------------------------------------------------------");
            System.out.println(" Web App:    http://localhost:8080");
            System.out.println(" H2 Console: http://localhost:8080/h2-console");
            System.out.println(" TimeZone:   " + TimeZone.getDefault().getID());
            System.out.println("----------------------------------------------------------\n");
        };
    }
}