package com.diligrp.xtrade.upay.trade.domain;

import java.util.Collections;
import java.util.List;

public class Trade {
    // 交易类型
    private Integer type;
    // 账号ID
    private Long accountId;
    // 金额-分
    private Long amount;
    // 外部流水号
    private String serialNo;
    // 账务周期号
    private String cycleNo;
    // 备注
    private String description;
    // 费用列表
    private List<Fee> fees;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getCycleNo() {
        return cycleNo;
    }

    public void setCycleNo(String cycleNo) {
        this.cycleNo = cycleNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Fee> getFees() {
        return fees == null ? Collections.EMPTY_LIST : fees;
    }

    public void setFees(List<Fee> fees) {
        this.fees = fees;
    }
}
