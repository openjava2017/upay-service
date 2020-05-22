package com.diligrp.xtrade.upay.channel.domain;

public class FreezeFundDto {
    // 资金账号ID
    private Long accountId;
    // 类型 - 系统冻结和交易冻结
    private Integer type;
    // 冻结金额 - 分
    private Long amount;
    // 描述
    private String description;

    public static FreezeFundDto of(Long accountId, Integer type, Long amount) {
        FreezeFundDto frozenOrder = new FreezeFundDto();
        frozenOrder.setAccountId(accountId);
        frozenOrder.setType(type);
        frozenOrder.setAmount(amount);
        return frozenOrder;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
