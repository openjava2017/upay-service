package com.diligrp.xtrade.upay.channel.domain;

public class FreezeFundDto {
    // 支付ID - 交易冻结使用
    private String paymentId;
    // 资金账号ID
    private Long accountId;
    // 类型 - 系统冻结和交易冻结
    private Integer type;
    // 冻结金额 - 分
    private Long amount;
    // 操作人 - 系统冻结时记录操作人
    private Long userId;
    // 操作人姓名 - 系统冻结时记录操作人
    private String userName;
    // 描述
    private String description;

    public static FreezeFundDto of(String paymentId, Long accountId, Integer type, Long amount) {
        FreezeFundDto frozenOrder = new FreezeFundDto();
        frozenOrder.setPaymentId(paymentId);
        frozenOrder.setAccountId(accountId);
        frozenOrder.setType(type);
        frozenOrder.setAmount(amount);
        return frozenOrder;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
