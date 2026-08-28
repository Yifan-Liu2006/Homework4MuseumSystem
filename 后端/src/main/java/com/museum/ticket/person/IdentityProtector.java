package com.museum.ticket.person;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class IdentityProtector {
    public String normalize(String idNumber) {
        return idNumber.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    public String hash(String idType, String idNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = (idType + ":" + normalize(idNumber)).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持 SHA-256", exception);
        }
    }

    public String mask(String idNumber) {
        String normalized = normalize(idNumber);
        if (normalized.length() <= 8) {
            return normalized.substring(0, 2) + "*".repeat(normalized.length() - 4)
                    + normalized.substring(normalized.length() - 2);
        }
        return normalized.substring(0, 3) + "*".repeat(normalized.length() - 7)
                + normalized.substring(normalized.length() - 4);
    }
}
