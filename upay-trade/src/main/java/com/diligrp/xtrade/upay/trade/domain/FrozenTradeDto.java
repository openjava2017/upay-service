package com.diligrp.xtrade.upay.trade.domain;

public class FrozenTradeDto {
    // 支付ID
    private String paymentId;
    // 实际支付金额
    private Long amount;
    // 支付密码
    private String password;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
