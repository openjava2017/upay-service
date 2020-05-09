package com.diligrp.xtrade.upay.trade.domain;

import java.time.LocalDateTime;

public class TradeStateDto {
    // 交易ID
    private String tradeId;
    // 状态
    private Integer state;
    // 数据版本
    private Integer version;
    // 修改时间
    private LocalDateTime modifiedTime;

    public String getTradeId() {
        return tradeId;
    }

    public void setTradetId(String tradeId) {
        this.tradeId = tradeId;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(LocalDateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public static TradeStateDto of(String tradeId, Integer state, LocalDateTime modifiedTime, Integer version) {
        TradeStateDto accountState = new TradeStateDto();
        accountState.setTradetId(tradeId);
        accountState.setState(state);
        accountState.setModifiedTime(modifiedTime);
        accountState.setVersion(version);
        return accountState;
    }
}
