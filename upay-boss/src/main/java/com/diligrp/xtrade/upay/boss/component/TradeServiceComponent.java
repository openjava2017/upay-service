package com.diligrp.xtrade.upay.boss.component;

import com.diligrp.xtrade.shared.domain.ServiceRequest;
import com.diligrp.xtrade.shared.sapi.CallableComponent;
import com.diligrp.xtrade.shared.util.AssertUtils;
import com.diligrp.xtrade.upay.boss.domain.PaymentId;
import com.diligrp.xtrade.upay.boss.domain.TradeId;
import com.diligrp.xtrade.upay.trade.domain.Application;
import com.diligrp.xtrade.upay.trade.domain.PaymentRequest;
import com.diligrp.xtrade.upay.trade.domain.TradeRequest;
import com.diligrp.xtrade.upay.trade.service.IPaymentPlatformService;

import javax.annotation.Resource;

@CallableComponent(id = "payment.trade.service")
public class TradeServiceComponent {

    @Resource
    private IPaymentPlatformService paymentPlatformService;

    public TradeId prepare(ServiceRequest<TradeRequest> request) {
        TradeRequest trade = request.getData();
        // 基本参数校验
        AssertUtils.notNull(trade.getType(), "type missed");
        AssertUtils.notNull(trade.getAccountId(), "accountId missed");
        AssertUtils.notNull(trade.getAmount(), "amount missed");
        AssertUtils.isTrue(trade.getAmount() > 0, "Invalid amount");

        Application application = request.getContext().getObject(Application.class.getName(), Application.class);
        String tradeId = paymentPlatformService.createTrade(application, trade);
        return TradeId.of(tradeId);
    }

    public PaymentId commit(ServiceRequest<PaymentRequest> request) {
        PaymentRequest payment = request.getData();
        // 基本参数校验
        AssertUtils.notNull(payment.getTradeId(), "tradeId missed");
        AssertUtils.notNull(payment.getAccountId(), "accountId missed");
        AssertUtils.notNull(payment.getChannelId(), "channelId missed");
        // 费用参数校验
        payment.fees().ifPresent(fees -> fees.stream().forEach(fee -> {
            AssertUtils.notNull(fee.getAmount(), "fee amount missed");
            AssertUtils.isTrue(fee.getAmount() > 0, "Invalid fee amount");
        }));

        Application application = request.getContext().getObject(Application.class.getName(), Application.class);
        String paymentId = paymentPlatformService.commit(application, payment);
        return PaymentId.of(paymentId);
    }
}