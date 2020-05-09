package com.diligrp.xtrade.upay.channel.service;

import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.core.model.AccountFund;

public interface IAccountChannelService {
    AccountFund submit(AccountChannel channel);
}
