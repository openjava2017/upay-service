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
public enum PaymentState implements IEnumType {

    PENDING("待处理", 1),

    PROCESSING("处理中", 2),

    SUCCESS("成功", 3),

    CANCELED("取消", 6);

    private String name;
    private int code;

    PaymentState(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public static Optional<PaymentState> getState(int code) {
        Stream<PaymentState> STATES = Arrays.stream(PaymentState.values());
        return STATES.filter(state -> state.getCode() == code).findFirst();
    }

    public static String getName(int code) {
        Stream<PaymentState> STATES = Arrays.stream(PaymentState.values());
        Optional<String> result = STATES.filter(state -> state.getCode() == code)
                .map(PaymentState::getName).findFirst();
        return result.isPresent() ? result.get() : null;
    }

    public static List<PaymentState> getStateList() {
        return Arrays.asList(PaymentState.values());
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
