package com.diligrp.xtrade.upay.core.service.impl;

import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IAccountFundDao;
import com.diligrp.xtrade.upay.core.dao.IFundStatementDao;
import com.diligrp.xtrade.upay.core.domain.FrozenTransaction;
import com.diligrp.xtrade.upay.core.domain.FundActivity;
import com.diligrp.xtrade.upay.core.domain.FundTransaction;
import com.diligrp.xtrade.upay.core.exception.FundAccountException;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.model.FundStatement;
import com.diligrp.xtrade.upay.core.service.IFundAccountService;
import com.diligrp.xtrade.upay.core.service.IFundStreamEngine;
import com.diligrp.xtrade.upay.core.type.ActionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service("fundStreamEngine")
public class DefaultFundStreamEngine implements IFundStreamEngine {

    private static final int RETRIES = 3;

    @Resource
    private IFundAccountService fundAccountService;

    @Resource
    private IAccountFundDao accountFundDao;

    @Resource
    private IFundStatementDao fundStatementDao;

    /**
     * 操作资金余额和余额变动后添加资金流水，引入数据库乐观锁进行资金数据修改并在发生数据并发修改时引入重试机制；
     * 由于新启事务（新数据库连接）查询账户资金及数据版本（避免数据库隔离级别及Mybatis缓存造成无法读取到新版本数据），
     * 因此不能多次调用此类方法对同一账号进行两次资金事务操作；否则会造成第二次数据修改始终会失败
     * （第二次修改将不满足version=oldVersion），此业务场景包括：解冻并扣款；为了解决此类业务场景，
     * 请使用submitOnce方法进行，该方法不提供重试机制，在当前事务中进行账户资金及数据版本查询。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AccountFund submit(FundTransaction transaction) {
        boolean success = true;
        AccountFund accountFund = null;
        AtomicLong balance = new AtomicLong(0);
        long totalAmount = Arrays.stream(transaction.getActivities()).mapToLong(FundActivity::getAmount).sum();
        for (int retry = 0; retry < RETRIES; retry ++) {
            // 新启事务查询账户资金及数据版本，避免数据库隔离级别和Mybatis缓存造成乐观锁重试机制无法生效
            Optional<AccountFund> fundOpt = fundAccountService.findAccountFundById(transaction.getAccountId());
            accountFund = fundOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "账号资金不存在"));
            long availableAmount = accountFund.getBalance() - accountFund.getFrozenAmount();
            if (availableAmount + totalAmount < 0) {
                throw new FundAccountException(ErrorCode.INSUFFICIENT_ACCOUNT_FUND, "账户余额不足");
            }

            balance.set(accountFund.getBalance());
            accountFund.setBalance(accountFund.getBalance() + totalAmount);
            accountFund.setModifiedTime(transaction.getWhen());
            success = compareAndSetVersion(accountFund);
            if (success) break;
        }

        if (!success) {
            throw new FundAccountException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        List<FundStatement> statements = Arrays.stream(transaction.getActivities())
            .filter(activity -> activity.getAmount() != 0)
            .map(activity -> FundStatement.builder().paymentId(transaction.getPaymentId())
                .accountId(transaction.getAccountId()).childId(0L).tradeType(transaction.getType())
                .action(ActionType.getByAmount(activity.getAmount()).getCode())
                .balance(balance.getAndAdd(activity.getAmount())).amount(activity.getAmount())
                .type(activity.getType()).typeName(activity.getTypeName()).description(null)
                .createdTime(transaction.getWhen()).build())
            .collect(Collectors.toList());
        fundStatementDao.insertFundStatements(statements);
        return accountFund;
    }

    /**
     * 操作资金余额和余额变动后添加资金流水，引入数据库乐观锁进行资金数据修改并在数据并发修改时直接抛出异常
     * 不进行重试；此方法支持多次调用对同一账号进行两次资金事务操作；此业务场景包括：解冻并扣款；
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AccountFund submitOnce(FundTransaction transaction) {
        long totalAmount = Arrays.stream(transaction.getActivities()).mapToLong(FundActivity::getAmount).sum();
        Optional<AccountFund> fundOpt = accountFundDao.findAccountFundById(transaction.getAccountId());
        AccountFund accountFund = fundOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "账号资金不存在"));
        AtomicLong balance = new AtomicLong(accountFund.getBalance());
        long availableAmount = accountFund.getBalance() - accountFund.getFrozenAmount();
        if (availableAmount + totalAmount < 0) {
            throw new FundAccountException(ErrorCode.INSUFFICIENT_ACCOUNT_FUND, "账户余额不足");
        }

        accountFund.setBalance(accountFund.getBalance() + totalAmount);
        accountFund.setModifiedTime(transaction.getWhen());
        boolean success = compareAndSetVersion(accountFund);
        if (!success) {
            throw new FundAccountException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        List<FundStatement> statements = Arrays.stream(transaction.getActivities())
            .filter(activity -> activity.getAmount() != 0)
            .map(activity -> FundStatement.builder().paymentId(transaction.getPaymentId())
                .accountId(transaction.getAccountId()).childId(0L).tradeType(transaction.getType())
                .action(ActionType.getByAmount(activity.getAmount()).getCode())
                .balance(balance.getAndAdd(activity.getAmount())).amount(activity.getAmount())
                .type(activity.getType()).typeName(activity.getTypeName()).description(null)
                .createdTime(transaction.getWhen()).build())
            .collect(Collectors.toList());
        fundStatementDao.insertFundStatements(statements);
        return accountFund;
    }

    /**
     * 操作冻结资金，引入数据库乐观锁进行资金数据修改并在发生数据并发修改时引入重试机制；由于新启事务（新数据库连接）
     * 查询账户资金及数据版本（避免数据库隔离级别及Mybatis缓存造成无法读取到新版本数据），因此不能多次调用此类方法对
     * 同一账号进行两次资金事务操作；否则会造成第二次数据修改始终会失败（第二次修改将不满足version=oldVersion），
     * 此业务场景包括：解冻并扣款；为了解决此类业务场景，请使用submitOnce方法进行，该方法不提供重试机制，
     * 在当前事务中进行账户资金及数据版本查询。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AccountFund submit(FrozenTransaction transaction) {
        boolean success = true;
        AccountFund accountFund = null;
        for (int retry = 0; retry < RETRIES; retry ++) {
            // 新启事务查询账户资金及数据版本，避免数据库隔离级别和Mybatis缓存造成乐观锁重试机制无法生效
            Optional<AccountFund> fundOpt = fundAccountService.findAccountFundById(transaction.getAccountId());
            accountFund = fundOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "账号资金不存在"));
            long availableAmount = accountFund.getBalance() - accountFund.getFrozenAmount();
            if (availableAmount - transaction.getAmount() < 0) {
                throw new FundAccountException(ErrorCode.INSUFFICIENT_ACCOUNT_FUND, "账户余额不足");
            }
            if (accountFund.getFrozenAmount() + transaction.getAmount() < 0) {
                throw new FundAccountException(ErrorCode.INSUFFICIENT_ACCOUNT_FUND, "账户冻结余额不足");
            }

            accountFund.setFrozenAmount(accountFund.getFrozenAmount() + transaction.getAmount());
            accountFund.setModifiedTime(transaction.getWhen());
            success = compareAndSetVersion(accountFund);
            if (success) break;
        }

        if (!success) {
            throw new FundAccountException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        return accountFund;
    }

    /**
     * 操作冻结资金，引入数据库乐观锁进行资金数据修改并在发生数据并发修改时直接抛出异常不进行重试；
     * 此类方法支持多次调用对同一账号进行两次资金事务操作；此业务场景包括：解冻并扣款。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AccountFund submitOnce(FrozenTransaction transaction) {
        Optional<AccountFund> fundOpt = accountFundDao.findAccountFundById(transaction.getAccountId());
        AccountFund accountFund = fundOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "账号资金不存在"));
        long availableAmount = accountFund.getBalance() - accountFund.getFrozenAmount();
        if (availableAmount - transaction.getAmount() < 0) {
            throw new FundAccountException(ErrorCode.INSUFFICIENT_ACCOUNT_FUND, "账户余额不足");
        }
        if (accountFund.getFrozenAmount() + transaction.getAmount() < 0) {
            throw new FundAccountException(ErrorCode.INSUFFICIENT_ACCOUNT_FUND, "账户冻结余额不足");
        }

        accountFund.setFrozenAmount(accountFund.getFrozenAmount() + transaction.getAmount());
        accountFund.setModifiedTime(transaction.getWhen());
        boolean success = compareAndSetVersion(accountFund);
        if (!success) {
            throw new FundAccountException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        return accountFund;
    }

    private boolean compareAndSetVersion(AccountFund accountFund) {
        return accountFundDao.compareAndSetVersion(accountFund) > 0;
    }
}
