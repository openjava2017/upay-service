package com.diligrp.xtrade.upay.core.model;

import com.diligrp.xtrade.shared.domain.BaseDo;

import java.time.LocalDateTime;

public class FundStatement extends BaseDo {
    // 支付ID
    private String paymentId;
    // 账号ID
    private Long accountId;
    // 子账号ID
    private Long childId;
    // 动作-收入 支出
    private Integer action;
    // 交易类型
    private Integer type;
    // 期初余额-分
    private Long balance;
    // 金额-分(正值 负值)
    private Long amount;
    // 资金类型
    private Integer fundType;
    // 备注
    private String description;

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getChildId() {
        return childId;
    }

    public void setChildId(Long childId) {
        this.childId = childId;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getFundType() {
        return fundType;
    }

    public void setFundType(Integer fundType) {
        this.fundType = fundType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Builder builder() {
        return new FundStatement().new Builder();
    }

    public class Builder {
        public Builder paymentId(String paymentId) {
            FundStatement.this.paymentId = paymentId;
            return this;
        }

        public Builder accountId(Long accountId) {
            FundStatement.this.accountId = accountId;
            return this;
        }

        public Builder childId(Long childId) {
            FundStatement.this.childId = childId;
            return this;
        }

        public Builder action(Integer action) {
            FundStatement.this.action = action;
            return this;
        }

        public Builder type(Integer type) {
            FundStatement.this.type = type;
            return this;
        }

        public Builder balance(Long balance) {
            FundStatement.this.balance = balance;
            return this;
        }

        public Builder amount(Long amount) {
            FundStatement.this.amount = amount;
            return this;
        }

        public Builder fundType(Integer fundType) {
            FundStatement.this.fundType = fundType;
            return this;
        }

        public Builder description(String description) {
            FundStatement.this.description = description;
            return this;
        }

        public Builder createdTime(LocalDateTime createdTime) {
            FundStatement.this.createdTime = createdTime;
            return this;
        }

        public FundStatement build() {
            return FundStatement.this;
        }
    }
}
