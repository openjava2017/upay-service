package com.diligrp.xtrade.upay.core.service;

import com.diligrp.xtrade.upay.core.domain.FrozenTransaction;
import com.diligrp.xtrade.upay.core.domain.FundTransaction;
import com.diligrp.xtrade.upay.core.model.AccountFund;

public interface IFundStreamEngine {
    AccountFund submit(FundTransaction transaction);

    AccountFund submitOnce(FundTransaction transaction);

    AccountFund submit(FrozenTransaction transaction);

    AccountFund submitOnce(FrozenTransaction transaction);
}
