package com.diligrp.xtrade.upay.boss.component;

import com.diligrp.xtrade.shared.domain.ServiceRequest;
import com.diligrp.xtrade.shared.sapi.CallableComponent;
import com.diligrp.xtrade.shared.util.AssertUtils;
import com.diligrp.xtrade.upay.boss.domain.FrozenId;
import com.diligrp.xtrade.upay.channel.domain.FreezeFundDto;
import com.diligrp.xtrade.upay.channel.service.IFrozenOrderService;
import com.diligrp.xtrade.upay.channel.type.FrozenType;

import javax.annotation.Resource;

@CallableComponent(id = "payment.fund.service")
public class FundServiceComponent {

    @Resource
    private IFrozenOrderService frozenOrderService;

    public FrozenId freeze(ServiceRequest<FreezeFundDto> request) {
        FreezeFundDto freezeFund = request.getData();
        AssertUtils.notNull(freezeFund.getAccountId(), "accountId missed");
        AssertUtils.notNull(freezeFund.getAmount(), "amount missed");
        freezeFund.setType(FrozenType.SYSTEM_FROZEN.getCode());
        Long id = frozenOrderService.freeze(freezeFund);
        return FrozenId.of(id);
    }

    public void unfreeze(ServiceRequest<FrozenId> request) {
        FrozenId frozenId = request.getData();
        AssertUtils.notNull(frozenId.getFrozenId(), "frozenId missed");
        frozenOrderService.unfreeze(frozenId.getFrozenId());
    }
}