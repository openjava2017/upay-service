package com.diligrp.xtrade.upay.channel.service;

import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.model.FundAccount;

public interface IAccountChannelService {
    AccountFund submit(IFundTransaction transaction);

    FundAccount checkTradePermission(long accountId, String password, int maxPwdErrors);

    FundAccount checkTradePermission(long accountId);
}
