package com.diligrp.xtrade.upay.trade.model;

import com.diligrp.xtrade.shared.domain.BaseDo;

import java.time.LocalDateTime;

public class PaymentFee extends BaseDo {
    // 支付ID
    private String paymentId;
    // 金额-分
    private Long amount;
    // 费用类型
    private Integer type;

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public static PaymentFee of(String paymentId, Long amount, Integer type, LocalDateTime when) {
        PaymentFee fee = new PaymentFee();
        fee.setPaymentId(paymentId);
        fee.setAmount(amount);
        fee.setType(type);
        fee.setCreatedTime(when);
        return fee;
    }
}
