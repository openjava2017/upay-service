package com.diligrp.xtrade.upay.core.util;

import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.exception.FundAccountException;
import com.diligrp.xtrade.upay.core.type.AccountState;

public final class AccountStateMachine {
    public static void createChildAccountCheck(FundAccount account) {
        if (account.getParentId() != 0) {
            throw new FundAccountException(ErrorCode.OPERATION_NOT_ALLOWED, "只有主账号才能创建子账号");
        }
        if (account.getState() != AccountState.NORMAL.getCode()) {
            throw new FundAccountException(ErrorCode.INVALID_ACCOUNT_STATE, "主账户状态不正常");
        }
    }

    public static void unregisterAccountCheck(FundAccount account) {
        // 删除资金状态暂时无限制, "*" -> "注销"
    }

    public static void unregisterFundCheck(AccountFund fund) {
        if (fund.getBalance() > 0) {
            throw new FundAccountException(ErrorCode.INVALID_FUND_STATE, "不能注销有余额的资金账户");
        }
    }
}
