package com.diligrp.xtrade.upay.core.dao;

import com.diligrp.xtrade.shared.mybatis.MybatisMapperSupport;
import com.diligrp.xtrade.upay.core.model.FundStatement;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("fundStatementDao")
public interface IFundStatementDao extends MybatisMapperSupport {
    void insertFundStatements(List<FundStatement> statements);
}