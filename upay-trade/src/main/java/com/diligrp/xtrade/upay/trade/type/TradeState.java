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
public enum TradeState implements IEnumType {

    PENDING("待处理", 1),

    PROCESSING("处理中", 2),

    SUCCESS("交易成功", 3),

    FROZEN("交易冻结", 4),

    REFUND("交易退款", 5),

    CANCELED("交易取消", 6),

    CLOSED("交易关闭", 7);

    private String name;
    private int code;

    TradeState(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public static Optional<TradeState> getState(int code) {
        Stream<TradeState> STATES = Arrays.stream(TradeState.values());
        return STATES.filter(state -> state.getCode() == code).findFirst();
    }

    public static String getName(int code) {
        Stream<TradeState> STATES = Arrays.stream(TradeState.values());
        Optional<String> result = STATES.filter(state -> state.getCode() == code)
                .map(TradeState::getName).findFirst();
        return result.isPresent() ? result.get() : null;
    }

    public static List<TradeState> getStateList() {
        return Arrays.asList(TradeState.values());
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
