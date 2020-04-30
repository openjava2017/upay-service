package com.diligrp.xtrade.upay.core.service.impl;

import com.diligrp.xtrade.shared.security.PasswordUtils;
import com.diligrp.xtrade.shared.sequence.IKeyGenerator;
import com.diligrp.xtrade.shared.sequence.KeyGeneratorManager;
import com.diligrp.xtrade.shared.type.Gender;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IAccountFundDao;
import com.diligrp.xtrade.upay.core.dao.IFundAccountDao;
import com.diligrp.xtrade.upay.core.domain.AccountStateDto;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.domain.RegisterAccount;
import com.diligrp.xtrade.upay.core.exception.FundAccountException;
import com.diligrp.xtrade.upay.core.service.IFundAccountService;
import com.diligrp.xtrade.upay.core.type.AccountState;
import com.diligrp.xtrade.upay.core.type.AccountType;
import com.diligrp.xtrade.upay.core.type.SequenceKey;
import com.diligrp.xtrade.upay.core.util.AccountStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Optional;

@Service("fundAccountService")
public class FundAccountServiceImpl implements IFundAccountService {

    @Resource
    private IFundAccountDao fundAccountDao;

    @Resource
    private IAccountFundDao accountFundDao;

    @Resource
    private KeyGeneratorManager keyGeneratorManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public long createFundAccount(Long mchId, RegisterAccount account) {
        Optional<AccountType> accountTypeOpt = AccountType.getType(account.getType());
        accountTypeOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "无效的账号类型"));
        if (account.getGender() != null) {
            Optional<Gender> genderOpt = Gender.getGender(account.getGender());
            genderOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "无效的性别"));
        }
        if (account.getParentId() != null && account.getParentId() != 0) {
            Optional<FundAccount> masterAccount = fundAccountDao.findFundAccountById(account.getParentId());
            masterAccount.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "主资金账号不存在"));
            masterAccount.ifPresent(AccountStateMachine::createChildAccountCheck);
        }

        LocalDateTime when = LocalDateTime.now();
        IKeyGenerator keyGenerator = keyGeneratorManager.getKeyGenerator(SequenceKey.FUND_ACCOUNT);
        long accountId = keyGenerator.nextId();
        long parentId = account.getParentId() == null ? 0 : account.getParentId();
        String secretKey = PasswordUtils.generateSecretKey();
        String password = PasswordUtils.encrypt(account.getPassword(), secretKey);

        FundAccount fundAccount = FundAccount.builder().customerId(account.getCustomerId()).accountId(accountId)
                .parentId(parentId).type(account.getType()).code(account.getCode()).name(account.getName())
                .gender(account.getGender()).mobile(account.getMobile()).email(account.getEmail())
                .idCode(account.getIdCode()).address(account.getAddress()).password(password).secretKey(secretKey)
                .state(AccountState.NORMAL.getCode()).mchId(mchId).version(0).createdTime(when).build();
        fundAccountDao.insertFundAccount(fundAccount);
        if (fundAccount.getParentId() == 0) {
            AccountFund accountFund = AccountFund.builder().accountId(accountId).balance(0L).frozenAmount(0L).vouchAmount(0L)
                    .dailyAmount(0L).version(0).createdTime(when).build();
            accountFundDao.insertAccountFund(accountFund);
        }

        return accountId;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void unregisterFundAccount(Long accountId) {
        Optional<FundAccount> accountOpt = fundAccountDao.findFundAccountById(accountId);
        accountOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "资金账号不存在"));
        accountOpt.ifPresent(AccountStateMachine::unregisterAccountCheck);
        Optional<AccountFund> fundOpt = accountFundDao.findAccountFundById(accountId);
        fundOpt.ifPresent(AccountStateMachine::unregisterFundCheck);
        AccountStateDto accountState = AccountStateDto.of(accountId, AccountState.VOID.getCode(),
                LocalDateTime.now(), accountOpt.get().getVersion());
        Integer result = fundAccountDao.compareAndSetState(accountState);
        if (result == 0) {
            throw new FundAccountException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }
    }

    /**
     * {@inheritDoc}
     *
     * 乐观锁实现需Spring事务传播属性使用REQUIRES_NEW，数据库事务隔离级别READ_COMMITTED
     * 为了防止业务层事务的数据隔离级别和Mybatis的查询缓存干扰导致数据的重复读（无法读取到最新的数据记录），
     * 因此新启一个Spring事务（一个新数据库连接）并将数据隔离级别设置成READ_COMMITTED;
     * Mysql默认隔离级别为REPEATABLE_READ
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Optional<AccountFund> findAccountFundById(Long accountId) {
        return accountFundDao.findAccountFundById(accountId);
    }
}