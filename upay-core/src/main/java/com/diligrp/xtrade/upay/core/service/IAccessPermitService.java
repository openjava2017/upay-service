package com.diligrp.xtrade.upay.core.service;

import com.diligrp.xtrade.upay.core.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.core.domain.MerchantPermit;
import com.diligrp.xtrade.upay.core.domain.RegisterApplication;
import com.diligrp.xtrade.upay.core.domain.RegisterMerchant;

public interface IAccessPermitService {
    ApplicationPermit loadApplicationPermit(Long appId);

    MerchantPermit registerMerchant(RegisterMerchant request);

    ApplicationPermit registerApplication(RegisterApplication request);
}
