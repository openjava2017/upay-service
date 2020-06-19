package com.diligrp.xtrade.upay.trade.domain;

import com.diligrp.xtrade.upay.core.model.AccountFund;

import java.util.HashMap;
import java.util.Optional;

public class PaymentResult extends HashMap<String, Object> {
    // 交易成功
    public static final int CODE_SUCCESS = 200;

    // 交易状态
    private int code;
    // 支付ID
    private String paymentId;
    // 账户资金
    private AccountFund fund;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public AccountFund getFund() {
        return fund;
    }

    public void setFund(AccountFund fund) {
        this.fund = fund;
    }

    public Optional<AccountFund> fund() {
        return Optional.ofNullable(fund);
    }

    public boolean isSuccess() {
        return code == CODE_SUCCESS;
    }

    public static PaymentResult of(int code, String paymentId) {
        return PaymentResult.of(code, paymentId, null);
    }

    public static PaymentResult of(int code, String paymentId, AccountFund fund) {
        PaymentResult paymentState = new PaymentResult();
        paymentState.setPaymentId(paymentId);
        paymentState.setCode(code);
        paymentState.setFund(fund);
        return paymentState;
    }
}
