package com.diligrp.xtrade.upay.trade.type;

import com.diligrp.xtrade.shared.type.IEnumType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author: brenthuang
 * @date: 2020/03/24
 */
public enum TradeType implements IEnumType {

    DEPOSIT("充值", 10),

    WITHDRAW("提现", 11),

    FEE("缴费", 12),

    PRE_DEPOSIT("预存款", 13),

    DIRECT_TRADE("即时交易", 20),

    FROZEN_TRADE("交易冻结", 21),

    TRANSFER("转账", 22),

    BANK_DEPOSIT("圈存", 30),

    BANK_WITHDRAW("圈提", 31),

    CANCEL("交易撤销", 40),

    REFUND("交易退款", 41),

    CORRECT("冲正", 42);

    private String name;
    private int code;

    TradeType(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public static Optional<TradeType> getType(int code) {
        Stream<TradeType> TYPES = Arrays.stream(TradeType.values());
        return TYPES.filter(type -> type.getCode() == code).findFirst();
    }

    public static String getName(int code) {
        Stream<TradeType> STATES = Arrays.stream(TradeType.values());
        Optional<String> result = STATES.filter(type -> type.getCode() == code)
                .map(TradeType::getName).findFirst();
        return result.isPresent() ? result.get() : null;
    }

    public static List<TradeType> getTypeList() {
        return Arrays.asList(TradeType.values());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return name;
    }
}
