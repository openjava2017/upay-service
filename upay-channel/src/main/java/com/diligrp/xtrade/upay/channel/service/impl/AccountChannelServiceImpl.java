package com.diligrp.xtrade.upay.channel.service.impl;

import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.channel.exception.PaymentChannelException;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.domain.FundActivity;
import com.diligrp.xtrade.upay.core.domain.FundTransaction;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.service.IFundStreamEngine;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("accountChannelService")
public class AccountChannelServiceImpl implements IAccountChannelService {

    @Resource
    private IFundStreamEngine fundStreamEngine;

    @Override
    public AccountFund submit(AccountChannel channel) {
        FundActivity[] activities = channel.fundStream().map(fund ->
                FundActivity.of(fund.getAmount(), fund.getType(), fund.getTypeName())).toArray(FundActivity[]::new);
        if (activities.length == 0) {
            throw new PaymentChannelException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "账户渠道参数错误");
        }

        FundTransaction transaction = FundTransaction.of(channel.getPaymentId(), channel.getAccountId(),
                channel.getType(), activities, channel.getWhen());
        return fundStreamEngine.submit(transaction);
    }
}
