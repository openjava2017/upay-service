package com.diligrp.xtrade.upay.core.dao;

import com.diligrp.xtrade.shared.mybatis.MybatisMapperSupport;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("accountFundDao")
public interface IAccountFundDao extends MybatisMapperSupport {
    void insertAccountFund(AccountFund fund);

    Optional<AccountFund> findAccountFundById(Long accountId);

    int compareAndSetVersion(AccountFund accountFund);
}
