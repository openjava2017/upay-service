package com.diligrp.xtrade.upay.core.domain;

import java.time.LocalDateTime;

public class FrozenTransaction {
    // 资金账号ID
    private long accountId;
    // 冻结金额
    private long amount;
    // 发生时间
    private LocalDateTime when;

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public LocalDateTime getWhen() {
        return when;
    }

    public void setWhen(LocalDateTime when) {
        this.when = when;
    }

    public static FrozenTransaction of(long accountId, long amount, LocalDateTime when) {
        FrozenTransaction transaction = new FrozenTransaction();
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setWhen(when);
        return transaction;
    }
}
