package com.museum.ticket.person;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("real_person")
public class RealPerson {
    @TableId("personID")
    private String personId;
    private String visitorId;
    private String name;
    private String idType;
    private String idHash;
    private String idMasked;
    private Boolean isSelf;
    private LocalDateTime createdAt;

    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }
    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }
    public String getIdHash() { return idHash; }
    public void setIdHash(String idHash) { this.idHash = idHash; }
    public String getIdMasked() { return idMasked; }
    public void setIdMasked(String idMasked) { this.idMasked = idMasked; }
    public Boolean getIsSelf() { return isSelf; }
    public void setIsSelf(Boolean self) { isSelf = self; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
