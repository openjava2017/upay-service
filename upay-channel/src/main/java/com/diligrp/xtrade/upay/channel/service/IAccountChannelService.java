package com.diligrp.xtrade.upay.channel.service;

import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.core.domain.TransactionStatus;
import com.diligrp.xtrade.upay.core.model.FundAccount;

public interface IAccountChannelService {
    TransactionStatus submit(IFundTransaction transaction);

    FundAccount checkTradePermission(long accountId, String password, int maxPwdErrors);

    FundAccount checkTradePermission(long accountId);
}
