package com.diligrp.xtrade.upay.trade.domain;

import com.diligrp.xtrade.upay.core.model.AccountFund;

public class PaymentResult {
    // 支付ID
    private String paymentId;
    // 支付状态
    private Integer state;
    // 账户资金
    private AccountFund fund;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public AccountFund getFund() {
        return fund;
    }

    public void setFund(AccountFund fund) {
        this.fund = fund;
    }

    public static PaymentResult of(String paymentId, Integer state) {
        PaymentResult paymentState = new PaymentResult();
        paymentState.setPaymentId(paymentId);
        paymentState.setState(state);
        return paymentState;
    }

    public static PaymentResult of(String paymentId, Integer state, AccountFund fund) {
        PaymentResult paymentState = new PaymentResult();
        paymentState.setPaymentId(paymentId);
        paymentState.setState(state);
        paymentState.setFund(fund);
        return paymentState;
    }
}
