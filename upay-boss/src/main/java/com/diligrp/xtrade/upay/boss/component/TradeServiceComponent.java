package com.diligrp.xtrade.upay.boss.component;

import com.diligrp.xtrade.shared.domain.ServiceRequest;
import com.diligrp.xtrade.shared.sapi.CallableComponent;
import com.diligrp.xtrade.shared.util.AssertUtils;
import com.diligrp.xtrade.upay.boss.domain.PaymentId;
import com.diligrp.xtrade.upay.boss.domain.TradeId;
import com.diligrp.xtrade.upay.trade.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.trade.domain.PaymentRequest;
import com.diligrp.xtrade.upay.trade.domain.FrozenTradeDto;
import com.diligrp.xtrade.upay.trade.domain.TradeRequest;
import com.diligrp.xtrade.upay.trade.service.IPaymentPlatformService;
import com.diligrp.xtrade.upay.trade.service.IFrozenTradePaymentService;

import javax.annotation.Resource;

@CallableComponent(id = "payment.trade.service")
public class TradeServiceComponent {

    @Resource
    private IPaymentPlatformService paymentPlatformService;

    @Resource
    private IFrozenTradePaymentService preTradePaymentService;

    /**
     * 创建交易订单，适用于所有交易业务
     * @see com.diligrp.xtrade.upay.trade.type.TradeType
     */
    public TradeId prepare(ServiceRequest<TradeRequest> request) {
        TradeRequest trade = request.getData();
        // 基本参数校验
        AssertUtils.notNull(trade.getType(), "type missed");
        AssertUtils.notNull(trade.getAccountId(), "accountId missed");
        AssertUtils.notNull(trade.getAmount(), "amount missed");
        // 缴费业务amount=0，否则amount>0
        AssertUtils.isTrue(trade.getAmount() > 0, "Invalid amount");

        ApplicationPermit application = request.getContext().getObject(ApplicationPermit.class.getName(), ApplicationPermit.class);
        String tradeId = paymentPlatformService.createTrade(application, trade);
        return TradeId.of(tradeId);
    }

    /**
     * 交易订单提交支付，适用于所有交易业务
     * 预支付交易提交支付时只是冻结资金，后续需要进一步调用confirm/cancel进行资金操作
     * @see com.diligrp.xtrade.upay.trade.type.TradeType
     */
    public PaymentId commit(ServiceRequest<PaymentRequest> request) {
        PaymentRequest payment = request.getData();
        // 基本参数校验
        AssertUtils.notNull(payment.getTradeId(), "tradeId missed");
        AssertUtils.notNull(payment.getAccountId(), "accountId missed");
        AssertUtils.notNull(payment.getChannelId(), "channelId missed");
        // 费用参数校验
        payment.fees().ifPresent(fees -> fees.stream().forEach(fee -> {
            AssertUtils.notNull(fee.getType(), "fee type missed");
            AssertUtils.notNull(fee.getTypeName(), "fee name missed");
            AssertUtils.notNull(fee.getAmount(), "fee amount missed");
            AssertUtils.isTrue(fee.getAmount() > 0, "Invalid fee amount");
        }));

        ApplicationPermit application = request.getContext().getObject(ApplicationPermit.class.getName(), ApplicationPermit.class);
        String paymentId = paymentPlatformService.commit(application, payment);
        return PaymentId.of(paymentId);
    }

    /**
     * 确认预支付交易，只适用于预支付交易类型
     * 预付支付交易需经历 prepare->commit->confirm/cancel三个阶段
     * confirm阶段完成资金解冻与消费，消费金额可以小于等于冻结金额（原订单金额）
     */
    public void confirm(ServiceRequest<FrozenTradeDto> request) {
        FrozenTradeDto preTrade = request.getData();
        AssertUtils.notEmpty(preTrade.getPaymentId(), "paymentId missed");
        AssertUtils.notNull(preTrade.getAmount(), "amount missed");
        AssertUtils.isTrue(preTrade.getAmount() > 0, "Invalid amount");
        AssertUtils.notEmpty(preTrade.getPassword(), "password missed");

        preTradePaymentService.confirm(preTrade);
    }

    /**
     * 取消预支付交易，只适用于预支付交易类型
     * 预付支付交易需经历 prepare->commit->confirm/cancel三个阶段
     * cancel阶段完成资金解冻，不进行任何消费
     */
    public void cancel(ServiceRequest<PaymentId> request) {
        PaymentId paymentId = request.getData();
        AssertUtils.notEmpty(paymentId.getPaymentId(), "paymentId missed");

        preTradePaymentService.cancel(paymentId.getPaymentId());
    }
}