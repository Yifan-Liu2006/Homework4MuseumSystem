package com.museum.ticket.person;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.museum.ticket.auth.CurrentVisitor;
import com.museum.ticket.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RealPersonService {
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    private final RealPersonMapper realPersonMapper;
    private final IdentityProtector identityProtector;

    public RealPersonService(RealPersonMapper realPersonMapper, IdentityProtector identityProtector) {
        this.realPersonMapper = realPersonMapper;
        this.identityProtector = identityProtector;
    }

    public List<RealPersonResponse> list() {
        return realPersonMapper.selectList(new LambdaQueryWrapper<RealPerson>()
                        .eq(RealPerson::getVisitorId, CurrentVisitor.requireVisitorId())
                        .orderByDesc(RealPerson::getCreatedAt))
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public RealPersonResponse create(RealPersonRequest request) {
        String visitorId = CurrentVisitor.requireVisitorId();
        String idHash = identityProtector.hash(request.idType(), request.idNumber());
        ensureIdentityAvailable(idHash, request.idType(), null);
        ensureSingleSelf(visitorId, request.isSelf(), null);

        RealPerson person = new RealPerson();
        person.setPersonId(generatePersonId());
        person.setVisitorId(visitorId);
        applyRequest(person, request, idHash);
        person.setCreatedAt(LocalDateTime.now());
        realPersonMapper.insert(person);
        return toResponse(person);
    }

    @Transactional
    public RealPersonResponse update(String personId, RealPersonRequest request) {
        RealPerson person = requireOwnedPerson(personId);
        String idHash = identityProtector.hash(request.idType(), request.idNumber());
        ensureIdentityAvailable(idHash, request.idType(), personId);
        ensureSingleSelf(person.getVisitorId(), request.isSelf(), personId);
        applyRequest(person, request, idHash);
        realPersonMapper.updateById(person);
        return toResponse(person);
    }

    @Transactional
    public void delete(String personId) {
        RealPerson person = requireOwnedPerson(personId);
        realPersonMapper.deleteById(person.getPersonId());
    }

    private RealPerson requireOwnedPerson(String personId) {
        RealPerson person = realPersonMapper.selectOne(new LambdaQueryWrapper<RealPerson>()
                .eq(RealPerson::getPersonId, personId)
                .eq(RealPerson::getVisitorId, CurrentVisitor.requireVisitorId()));
        if (person == null) {
            throw new BusinessException("实名人员不存在或无权访问");
        }
        return person;
    }

    private void ensureIdentityAvailable(String idHash, String idType, String excludedPersonId) {
        LambdaQueryWrapper<RealPerson> query = new LambdaQueryWrapper<RealPerson>()
                .eq(RealPerson::getIdType, idType).eq(RealPerson::getIdHash, idHash);
        if (excludedPersonId != null) {
            query.ne(RealPerson::getPersonId, excludedPersonId);
        }
        if (realPersonMapper.selectCount(query) > 0) {
            throw new BusinessException("该证件已绑定实名人员");
        }
    }

    private void ensureSingleSelf(String visitorId, boolean isSelf, String excludedPersonId) {
        if (!isSelf) {
            return;
        }
        LambdaQueryWrapper<RealPerson> query = new LambdaQueryWrapper<RealPerson>()
                .eq(RealPerson::getVisitorId, visitorId).eq(RealPerson::getIsSelf, true);
        if (excludedPersonId != null) {
            query.ne(RealPerson::getPersonId, excludedPersonId);
        }
        if (realPersonMapper.selectCount(query) > 0) {
            throw new BusinessException("每位游客只能设置一个本人实名信息");
        }
    }

    private void applyRequest(RealPerson person, RealPersonRequest request, String idHash) {
        person.setName(request.name().trim());
        person.setIdType(request.idType());
        person.setIdHash(idHash);
        person.setIdMasked(identityProtector.mask(request.idNumber()));
        person.setIsSelf(request.isSelf());
    }

    private String generatePersonId() {
        return "P" + LocalDateTime.now().format(ID_TIME_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private RealPersonResponse toResponse(RealPerson person) {
        return new RealPersonResponse(person.getPersonId(), person.getName(), person.getIdType(),
                person.getIdMasked(), Boolean.TRUE.equals(person.getIsSelf()), person.getCreatedAt());
    }
}
