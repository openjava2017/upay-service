package com.diligrp.xtrade.upay.trade.domain;

public class Fee {
    // 金额-元
    private Long amount;
    // 费用类型
    private Integer type;

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
}