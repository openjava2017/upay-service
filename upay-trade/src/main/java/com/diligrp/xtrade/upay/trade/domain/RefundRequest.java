package com.diligrp.xtrade.upay.trade.domain;

/**
 * 交易退款申请，包括：交易撤销、交易退款和交易冲正
 */
public class RefundRequest {
    // 原交易ID
    private String tradeId;
    // 处理金额
    private Long amount;
    // 支付密码
    private String password;

    public static RefundRequest of(String tradeId, Long amount, String password) {
        RefundRequest refund = new RefundRequest();
        refund.setTradeId(tradeId);
        refund.setAmount(amount);
        refund.setPassword(password);
        return refund;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
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
