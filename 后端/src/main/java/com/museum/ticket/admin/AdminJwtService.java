package com.museum.ticket.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.ticket.common.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class AdminJwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public AdminJwtService(ObjectMapper objectMapper,
                           @Value("${app.admin.jwt-secret}") String secret,
                           @Value("${app.admin.jwt-expiration-seconds:14400}") long expirationSeconds) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("管理员 JWT 密钥长度不能少于 32 个字符");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String create(AdminCurrent.AdminPrincipal principal) {
        try {
            long now = Instant.now().getEpochSecond();
            String header = encode(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encode(Map.of("sub", principal.workerId(), "roleId", principal.roleId(),
                    "roleName", principal.roleName(), "type", "admin", "iat", now,
                    "exp", now + expirationSeconds));
            String content = header + "." + payload;
            return content + "." + sign(content);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成管理员令牌", exception);
        }
    }

    public AdminCurrent.AdminPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("管理员令牌格式不正确");
            }
            String content = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(content).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new UnauthorizedException("管理员令牌签名无效");
            }
            JsonNode payload = objectMapper.readTree(DECODER.decode(parts[1]));
            if (!"admin".equals(payload.path("type").asText())
                    || payload.path("exp").asLong() <= Instant.now().getEpochSecond()) {
                throw new UnauthorizedException("管理员令牌无效或已过期");
            }
            return new AdminCurrent.AdminPrincipal(payload.path("sub").asText(),
                    payload.path("roleId").asText(), payload.path("roleName").asText());
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("管理员令牌无效");
        }
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private String encode(Map<String, Object> value) throws Exception {
        return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }
}
