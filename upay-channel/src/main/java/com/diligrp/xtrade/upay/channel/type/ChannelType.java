package com.diligrp.xtrade.upay.channel.type;

import com.diligrp.xtrade.shared.type.IEnumType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author: brenthuang
 * @date: 2020/03/24
 */
public enum ChannelType implements IEnumType {

    ACCOUNT("账户渠道", 1),

    CASH("现金渠道", 2),

    POS("POS渠道", 3);

    private String name;
    private int code;

    ChannelType(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public static Optional<ChannelType> getType(int code) {
        Stream<ChannelType> TYPES = Arrays.stream(ChannelType.values());
        return TYPES.filter(type -> type.getCode() == code).findFirst();
    }

    public static String getName(int code) {
        Stream<ChannelType> STATES = Arrays.stream(ChannelType.values());
        Optional<String> result = STATES.filter(type -> type.getCode() == code)
                .map(ChannelType::getName).findFirst();
        return result.isPresent() ? result.get() : null;
    }

    public static List<ChannelType> getTypeList() {
        return Arrays.asList(ChannelType.values());
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
