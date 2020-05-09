package com.diligrp.xtrade.upay.trade.domain;

public class Fee {
    // 金额-元
    private Long amount;
    // 费用类型
    private Integer type;
    // 费用描述
    private String typeName;

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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}