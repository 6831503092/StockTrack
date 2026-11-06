package com.sras.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner printUrl() {
        return args -> {
            System.out.println("\n----------------------------------------------------------");
            System.out.println(" Web App:    http://localhost:8080");
            System.out.println(" H2 Console: http://localhost:8080/h2-console");
            System.out.println("----------------------------------------------------------\n");
        };
    }
}