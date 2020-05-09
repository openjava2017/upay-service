package com.diligrp.xtrade.upay.channel.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AccountChannel {
    // 支付ID
    private String paymentId;
    // 资金账号ID
    private long accountId;
    // 业务类型
    private int type;
    // 发生时间
    private LocalDateTime when;
    // 资金流
    private List<ChannelFund> funds = new ArrayList<>();

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public LocalDateTime getWhen() {
        return when;
    }

    public void setWhen(LocalDateTime when) {
        this.when = when;
    }

    public void income(long amount, int type, String typeName) {
        funds.add(ChannelFund.of(amount, type, typeName));
    }

    public void outgo(long amount, int type, String typeName) {
        funds.add(ChannelFund.of(- amount, type, typeName));
    }

    public Stream<ChannelFund> fundStream() {
        return funds.stream();
    }

    public static class ChannelFund {
        // 金额 - 分
        private long amount;
        // 类型
        private int type;
        // 类型名称
        private String typeName;

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public static ChannelFund of(long amount, int type, String typeName) {
            ChannelFund fund = new ChannelFund();
            fund.setAmount(amount);
            fund.setType(type);
            fund.setTypeName(typeName);
            return fund;
        }
    }

    public static AccountChannel of(String paymentId, long accountId, int type, LocalDateTime when) {
        AccountChannel channel = new AccountChannel();
        channel.setPaymentId(paymentId);
        channel.setAccountId(accountId);
        channel.setType(type);
        channel.setWhen(when);
        return channel;
    }
}