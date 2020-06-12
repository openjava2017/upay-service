package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.shared.exception.ServiceAccessException;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IMerchantDao;
import com.diligrp.xtrade.upay.core.model.Application;
import com.diligrp.xtrade.upay.trade.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.trade.domain.MerchantPermit;
import com.diligrp.xtrade.upay.trade.service.IAccessPermitService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

@Service("accessPermitService")
public class AccessPermitServiceImpl implements IAccessPermitService {

    @Resource
    private IMerchantDao merchantDao;

    @Override
    public ApplicationPermit loadApplicationPermit(Long appId) {
        // TODO: load from cache or database
        Optional<Application> application = merchantDao.findApplicationById(appId);
        return application.map(app -> {
            MerchantPermit merchantPermit = merchantDao.findMerchantById(app.getMchId()).map(mer -> MerchantPermit.of(
                mer.getMchId(), mer.getProfitAccount(), mer.getVouchAccount(), mer.getPledgeAccount()))
                .orElseThrow(() -> new ServiceAccessException(ErrorCode.UNAUTHORIZED_ACCESS_ERROR, "商户信息未注册"));
            return ApplicationPermit.of(app.getAppId(), app.getAccessToken(), app.getSecretKey(), merchantPermit);
        }).orElseThrow(() -> new ServiceAccessException(ErrorCode.UNAUTHORIZED_ACCESS_ERROR, "应用信息未注册"));
    }

}
