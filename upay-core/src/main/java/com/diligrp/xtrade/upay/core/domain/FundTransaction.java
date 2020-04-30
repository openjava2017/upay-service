package com.diligrp.xtrade.upay.core.domain;

import java.time.LocalDateTime;

public class FundTransaction {
    // 支付ID
    private String paymentId;
    // 资金账号ID
    private long accountId;
    // 业务类型
    private int type;
    // 资金明细
    private FundActivity[] activities;
    // 发生时间
    private LocalDateTime when;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public FundActivity[] getActivities() {
        return activities;
    }

    public void setActivities(FundActivity[] activities) {
        this.activities = activities;
    }

    public LocalDateTime getWhen() {
        return when;
    }

    public void setWhen(LocalDateTime when) {
        this.when = when;
    }

    public static FundTransaction of(String paymentId, Long accountId, Integer type,
                                     FundActivity[] activities, LocalDateTime when) {
        FundTransaction transaction = new FundTransaction();
        transaction.setPaymentId(paymentId);
        transaction.setAccountId(accountId);
        transaction.setType(type);
        transaction.setActivities(activities);
        transaction.setWhen(when);
        return transaction;
    }
}
