package com.diligrp.xtrade.upay.channel.service.impl;

import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.channel.exception.PaymentChannelException;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.domain.FrozenTransaction;
import com.diligrp.xtrade.upay.core.domain.FundTransaction;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.service.IFundStreamEngine;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

@Service("accountChannelService")
public class AccountChannelServiceImpl implements IAccountChannelService {

    @Resource
    private IFundStreamEngine fundStreamEngine;

    @Override
    public AccountFund submit(IFundTransaction transaction) {
        Optional<FrozenTransaction> frozenTransaction = transaction.frozenTransaction();
        Optional<FundTransaction> fundTransaction = transaction.fundTransaction();
        if (frozenTransaction.isPresent() && fundTransaction.isPresent()) {
            fundStreamEngine.submitOnce(frozenTransaction.get());
            return fundStreamEngine.submitOnce(fundTransaction.get());
        }

        if (frozenTransaction.isPresent()) {
            return fundStreamEngine.submit(frozenTransaction.get());
        }
        if (fundTransaction.isPresent()) {
            return fundStreamEngine.submit(fundTransaction.get());
        }

        throw new PaymentChannelException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "无效的资金事务");
    }
}
