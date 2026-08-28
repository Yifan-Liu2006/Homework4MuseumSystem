package com.museum.ticket.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.museum.ticket.common.BusinessException;
import com.museum.ticket.visitor.Visitor;
import com.museum.ticket.visitor.VisitorMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    private final VisitorMapper visitorMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(VisitorMapper visitorMapper, JwtService jwtService) {
        this.visitorMapper = visitorMapper;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (findByMobile(request.mobile()) != null) {
            throw new BusinessException("该手机号已注册");
        }

        Visitor visitor = new Visitor();
        visitor.setVisitorId(generateVisitorId());
        visitor.setMobile(request.mobile());
        visitor.setPasswordHash(passwordEncoder.encode(request.password()));
        visitor.setStatus("正常");
        visitor.setRegisterTime(LocalDateTime.now());
        visitorMapper.insert(visitor);
        return toResponse(visitor);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Visitor visitor = findByMobile(request.mobile());
        if (visitor == null || !passwordEncoder.matches(request.password(), visitor.getPasswordHash())) {
            throw new BusinessException("手机号或密码错误");
        }
        if (!"正常".equals(visitor.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        visitor.setLastLoginTime(LocalDateTime.now());
        visitorMapper.updateById(visitor);
        String token = jwtService.createToken(visitor.getVisitorId());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), toResponse(visitor));
    }

    private Visitor findByMobile(String mobile) {
        return visitorMapper.selectOne(new LambdaQueryWrapper<Visitor>().eq(Visitor::getMobile, mobile));
    }

    private String generateVisitorId() {
        String timestamp = LocalDateTime.now().format(ID_TIME_FORMAT);
        int randomNumber = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "V" + timestamp + randomNumber;
    }

    private AuthResponse toResponse(Visitor visitor) {
        return new AuthResponse(visitor.getVisitorId(), visitor.getMobile(), visitor.getStatus(),
                visitor.getRegisterTime(), visitor.getLastLoginTime());
    }
}
