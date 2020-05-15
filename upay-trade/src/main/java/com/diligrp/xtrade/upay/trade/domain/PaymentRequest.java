package com.diligrp.xtrade.upay.trade.domain;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PaymentRequest {
    // 交易ID
    private String tradeId;
    // 资金账号ID
    private Long accountId;
    // 支付渠道
    private Integer channelId;
    // 银行卡号-用于银行渠道进行圈存圈提
    private String cardNo;
    // 费用列表
    private List<Fee> fees;

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public List<Fee> getFees() {
        return fees == null ? Collections.EMPTY_LIST : fees;
    }

    public void setFees(List<Fee> fees) {
        this.fees = fees;
    }

    public Optional<List<Fee>> fees() {
        return Optional.ofNullable(fees);
    }
}
