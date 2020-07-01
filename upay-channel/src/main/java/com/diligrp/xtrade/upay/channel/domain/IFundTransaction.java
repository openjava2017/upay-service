package com.diligrp.xtrade.upay.channel.domain;

import com.diligrp.xtrade.upay.core.domain.FundTransaction;

import java.util.Optional;

public interface IFundTransaction {
    void income(long amount, int type, String typeName);

    void outgo(long amount, int type, String typeName);

    void freeze(long amount);

    void unfreeze(long amount);

    Optional<FundTransaction> fundTransaction();
}
