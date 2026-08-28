package com.museum.ticket.visitor;

import com.museum.ticket.auth.AuthResponse;
import com.museum.ticket.auth.CurrentVisitor;
import com.museum.ticket.common.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public class VisitorService {
    private final VisitorMapper visitorMapper;

    public VisitorService(VisitorMapper visitorMapper) {
        this.visitorMapper = visitorMapper;
    }

    public AuthResponse getCurrentVisitor() {
        Visitor visitor = visitorMapper.selectById(CurrentVisitor.requireVisitorId());
        if (visitor == null || !"正常".equals(visitor.getStatus())) {
            throw new UnauthorizedException("游客账号不存在或已被禁用");
        }
        return new AuthResponse(visitor.getVisitorId(), visitor.getMobile(), visitor.getStatus(),
                visitor.getRegisterTime(), visitor.getLastLoginTime());
    }
}
