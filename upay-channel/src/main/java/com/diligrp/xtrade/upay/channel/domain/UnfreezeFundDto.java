package com.diligrp.xtrade.upay.channel.domain;

public class UnfreezeFundDto {
    // 冻结ID
    private Long frozenId;
    // 操作人 - 系统冻结时记录操作人
    private Long userId;
    // 操作人姓名 - 系统冻结时记录操作人
    private String userName;

    public Long getFrozenId() {
        return frozenId;
    }

    public void setFrozenId(Long frozenId) {
        this.frozenId = frozenId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
