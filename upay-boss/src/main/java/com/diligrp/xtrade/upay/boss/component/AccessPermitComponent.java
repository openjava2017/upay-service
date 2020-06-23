package com.diligrp.xtrade.upay.boss.component;

import com.diligrp.xtrade.shared.domain.ServiceRequest;
import com.diligrp.xtrade.shared.sapi.CallableComponent;
import com.diligrp.xtrade.shared.util.AssertUtils;
import com.diligrp.xtrade.upay.core.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.core.domain.MerchantPermit;
import com.diligrp.xtrade.upay.core.domain.RegisterApplication;
import com.diligrp.xtrade.upay.core.domain.RegisterMerchant;
import com.diligrp.xtrade.upay.core.service.IAccessPermitService;

import javax.annotation.Resource;

/**
 * 接入许可注册服务组件
 */
@CallableComponent(id = "payment.permit.register")
public class AccessPermitComponent {
    @Resource
    private IAccessPermitService accessPermitService;

    public MerchantPermit merchant(ServiceRequest<RegisterMerchant> request) {
        RegisterMerchant merchant = request.getData();
        AssertUtils.notNull(merchant.getMchId(), "mchId missed");
        AssertUtils.notEmpty(merchant.getCode(), "code missed");
        AssertUtils.notEmpty(merchant.getName(), "name missed");
        AssertUtils.notEmpty(merchant.getPassword(), "password missed");
        return accessPermitService.registerMerchant(merchant);
    }

    public ApplicationPermit application(ServiceRequest<RegisterApplication> request) {
        RegisterApplication application = request.getData();
        AssertUtils.notNull(application.getAppId(), "appId missed");
        AssertUtils.notNull(application.getMchId(), "mchId missed");
        AssertUtils.notEmpty(application.getName(), "name missed");
        AssertUtils.notEmpty(application.getAccessToken(), "accessToken missed");

        return accessPermitService.registerApplication(application);
    }
}
