package com.diligrp.xtrade.upay.trade.dao;

import com.diligrp.xtrade.shared.mybatis.MybatisMapperSupport;
import com.diligrp.xtrade.upay.trade.domain.Application;
import com.diligrp.xtrade.upay.trade.domain.Merchant;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("merchantDao")
public interface IMerchantDao extends MybatisMapperSupport {
    Optional<Application> findApplicationById(Long appId);

    Optional<Merchant> findMerchantById(Long mchId);
}
