package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.Application;
import com.diligrp.xtrade.upay.trade.domain.Merchant;

import java.util.Optional;

public interface IMerchantService {
    Optional<Application> findApplicationById(Long appId);

    Optional<Merchant> findMerchantById(Long mchId);
}
