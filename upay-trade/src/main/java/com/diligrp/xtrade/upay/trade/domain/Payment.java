package com.diligrp.xtrade.upay.trade.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Payment extends HashMap<String, Object> {
    // 交易ID
    private String tradeId;
    // 资金账号ID
    private Long accountId;
    // 支付金额
    private Long amount;
    // 支付渠道
    private Integer channelId;

    public static Payment of(String tradeId, Long accountId, Long amount, Integer channelId) {
        Payment transaction = new Payment();
        transaction.setTradeId(tradeId);
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setChannelId(channelId);
        return transaction;
    }

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

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getString(String param) {
        return (String)get(param);
    }

    public Long getLong(String param) {
        Object value = get(param);
        if (value != null) {
            return value instanceof Long ? (Long)value : Long.parseLong(value.toString());
        }
        return null;
    }

    public Integer getInteger(String param) {
        Object value = get(param);
        if (value != null) {
            return value instanceof Integer ? (Integer)value : Integer.parseInt(value.toString());
        }
        return null;
    }

    public <T> T getObject(String param, Class<T> type) {
        Object value = get(param);
        return value == null ? null : type.cast(value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getObject(String param) {
        Object value = get(param);
        return Optional.ofNullable ((T) value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<List<T>> getObjects(String param) {
        Object value = get(param);
        return Optional.ofNullable ((List<T>) value);
    }
}
