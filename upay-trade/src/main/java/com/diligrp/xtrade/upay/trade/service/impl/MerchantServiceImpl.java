package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.upay.trade.dao.IMerchantDao;
import com.diligrp.xtrade.upay.trade.domain.Application;
import com.diligrp.xtrade.upay.trade.domain.Merchant;
import com.diligrp.xtrade.upay.trade.service.IMerchantService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

@Service("merchantService")
public class MerchantServiceImpl implements IMerchantService {

    @Resource
    private IMerchantDao merchantDao;

    @Override
    public Optional<Application> findApplicationById(Long appId) {
        return merchantDao.findApplicationById(appId);
        // TODO: cache it or not
    }

    @Override
    public Optional<Merchant> findMerchantById(Long mchId) {
        return merchantDao.findMerchantById(mchId);
        //TODO: cache it if no status checked
    }
}
