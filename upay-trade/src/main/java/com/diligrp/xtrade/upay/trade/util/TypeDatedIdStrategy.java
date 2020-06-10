package com.diligrp.xtrade.upay.trade.util;

import com.diligrp.xtrade.shared.sequence.ISerialKeyGenerator;
import com.diligrp.xtrade.shared.util.DateUtils;
import com.diligrp.xtrade.shared.util.RandomUtils;

import java.time.LocalDate;

/**
 * 交易号生成策略，生成规则：年月日+两位类型码+两位随机码+至少四位顺序数
 */
public class TypeDatedIdStrategy implements ISerialKeyGenerator.IDatedIdStrategy {

    private Integer type;

    public TypeDatedIdStrategy(Integer type) {
        this.type = type;
    }

    @Override
    public String id(LocalDate date, long sequence) {
        StringBuilder builder = new StringBuilder();
        String dateFormat = DateUtils.formatDate(date, DateUtils.YYYYMMDD);
        builder.append(dateFormat).append(type).append(RandomUtils.randomNumber(2));
        if (sequence < 10) {
            builder.append("000").append(sequence);
        } else if (sequence < 100) {
            builder.append("00").append(sequence);
        } else if (sequence < 1000) {
            builder.append("0").append(sequence);
        } else {
            builder.append(sequence);
        }

        return builder.toString();
    }
}
