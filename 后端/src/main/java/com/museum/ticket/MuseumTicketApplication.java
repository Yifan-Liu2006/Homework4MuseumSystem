package com.museum.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MuseumTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(MuseumTicketApplication.class, args);
    }
}
