package com.diligrp.xtrade.upay.boss.domain;

public class TradeId {
    // 交易ID
    private String tradeId;

    // 交易状态
    private Integer state;

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public static TradeId of(String tradeId) {
        return TradeId.of(tradeId, null);
    }

    public static TradeId of(String tradeId, Integer state) {
        TradeId trade = new TradeId();
        trade.setTradeId(tradeId);
        trade.setState(state);
        return trade;
    }
}
