package com.museum.ticket.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class JwtService {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds:7200}") long expirationSeconds) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT 密钥长度不能少于 32 个字符");
        }
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(String visitorId) {
        long issuedAt = Instant.now().getEpochSecond();
        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encodeJson(Map.of("sub", visitorId, "iat", issuedAt, "exp", issuedAt + expirationSeconds));
        String content = header + "." + payload;
        return content + "." + sign(content);
    }

    public String parseVisitorId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("登录令牌格式不正确");
            }
            String content = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(content).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new UnauthorizedException("登录令牌签名无效");
            }
            JsonNode payload = objectMapper.readTree(BASE64_DECODER.decode(parts[1]));
            if (payload.path("exp").asLong() <= Instant.now().getEpochSecond()) {
                throw new UnauthorizedException("登录令牌已过期");
            }
            String visitorId = payload.path("sub").asText();
            if (visitorId.isBlank()) {
                throw new UnauthorizedException("登录令牌缺少游客信息");
            }
            return visitorId;
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("登录令牌无效");
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法生成登录令牌", exception);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return BASE64_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法签名登录令牌", exception);
        }
    }
}
