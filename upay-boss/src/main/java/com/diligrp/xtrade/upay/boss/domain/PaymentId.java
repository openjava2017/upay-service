package com.diligrp.xtrade.upay.boss.domain;

public class PaymentId {
    // 支付ID
    private String paymentId;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public static PaymentId of(String paymentId) {
        PaymentId payment = new PaymentId();
        payment.setPaymentId(paymentId);
        return payment;
    }
}
