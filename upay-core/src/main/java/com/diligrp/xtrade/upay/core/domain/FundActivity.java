package com.diligrp.xtrade.upay.core.domain;

public class FundActivity {
    // 金额 - 分（正数或负数）
    private long amount;
    // 资金类型
    private int fundType;
    // 资金描述
    private String description;

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public int getFundType() {
        return fundType;
    }

    public void setFundType(int fundType) {
        this.fundType = fundType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static FundActivity of(long amount, int fundType, String description) {
        FundActivity activity = new FundActivity();
        activity.setAmount(amount);
        activity.setFundType(fundType);
        activity.setDescription(description);
        return activity;
    }
}
