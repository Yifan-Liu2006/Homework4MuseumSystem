package com.museum.ticket.admin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class AdminIdGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");

    private AdminIdGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + LocalDateTime.now().format(FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
