package com.diligrp.xtrade.upay.channel.service;

import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.core.model.AccountFund;

public interface IAccountChannelService {
    AccountFund submit(IFundTransaction transaction);

    void checkTradePermission(long accountId, String password, int maxPwdErrors);
}
