package com.diligrp.xtrade.upay.channel.service.impl;

import com.diligrp.xtrade.shared.security.PasswordUtils;
import com.diligrp.xtrade.shared.util.ObjectUtils;
import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.channel.exception.PaymentChannelException;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IFundAccountDao;
import com.diligrp.xtrade.upay.core.domain.FrozenTransaction;
import com.diligrp.xtrade.upay.core.domain.FundTransaction;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.service.IFundStreamEngine;
import com.diligrp.xtrade.upay.core.type.AccountState;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

@Service("accountChannelService")
public class AccountChannelServiceImpl implements IAccountChannelService {

    @Resource
    private IFundAccountDao fundAccountDao;

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

    @Override
    public void checkTradePermission(long accountId, String password, int maxPwdErrors) {
        Optional<FundAccount> accountOpt = fundAccountDao.findFundAccountById(accountId);
        FundAccount account = accountOpt.orElseThrow(() -> new PaymentChannelException(ErrorCode.ACCOUNT_NOT_FOUND, "资金账号不存在"));
        if (account.getState() != AccountState.NORMAL.getCode()) {
            throw new PaymentChannelException(ErrorCode.INVALID_ACCOUNT_STATE, "资金账户状态异常");
        }

        String encodedPwd = PasswordUtils.encrypt(password, account.getSecretKey());
        if (!ObjectUtils.equals(encodedPwd, account.getPassword())) {
            throw new PaymentChannelException(ErrorCode.INVALID_ACCOUNT_PASSWORD, "资金账户密码错误");
        }
    }
}
