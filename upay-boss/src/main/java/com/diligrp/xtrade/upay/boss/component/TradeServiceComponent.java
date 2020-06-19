package com.diligrp.xtrade.upay.boss.component;

import com.diligrp.xtrade.shared.domain.ServiceRequest;
import com.diligrp.xtrade.shared.sapi.CallableComponent;
import com.diligrp.xtrade.shared.util.AssertUtils;
import com.diligrp.xtrade.upay.boss.domain.FundBalance;
import com.diligrp.xtrade.upay.boss.domain.TradeId;
import com.diligrp.xtrade.upay.trade.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.trade.domain.ConfirmRequest;
import com.diligrp.xtrade.upay.trade.domain.PaymentRequest;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;
import com.diligrp.xtrade.upay.trade.domain.RefundRequest;
import com.diligrp.xtrade.upay.trade.domain.TradeRequest;
import com.diligrp.xtrade.upay.trade.service.IPaymentPlatformService;
import com.diligrp.xtrade.upay.trade.service.IAuthPaymentService;

import javax.annotation.Resource;

@CallableComponent(id = "payment.trade.service")
public class TradeServiceComponent {

    @Resource
    private IPaymentPlatformService paymentPlatformService;

    @Resource
    private IAuthPaymentService authPaymentService;

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
        AssertUtils.isTrue(trade.getAmount() > 0, "Invalid amount");

        ApplicationPermit application = request.getContext().getObject(ApplicationPermit.class.getName(), ApplicationPermit.class);
        String tradeId = paymentPlatformService.createTrade(application, trade);
        return TradeId.of(tradeId);
    }

    /**
     * 交易订单提交支付，适用于所有交易业务
     * 预授权交易提交支付时只是冻结资金，后续需要进一步调用confirm/cancel进行资金操作
     * @see com.diligrp.xtrade.upay.trade.type.TradeType
     */
    public FundBalance commit(ServiceRequest<PaymentRequest> request) {
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
        PaymentResult result = paymentPlatformService.commit(application, payment);
        // 如有余额信息则返回余额信息
        return result.fund().map(fund ->
            FundBalance.of(fund.getAccountId(), fund.getBalance(), fund.getFrozenAmount())).orElse(null);
    }

    /**
     * 取消交易，适用于普通交易和预授权交易类型
     * 预付支付交易需经历 prepare->commit->confirm/cancel三个阶段
     * cancel阶段完成资金解冻，不进行任何消费
     */
    public FundBalance cancel(ServiceRequest<RefundRequest> request) {
        RefundRequest cancelRequest = request.getData();
        AssertUtils.notEmpty(cancelRequest.getTradeId(), "tradeId missed");

        ApplicationPermit application = request.getContext().getObject(ApplicationPermit.class.getName(), ApplicationPermit.class);
        PaymentResult result = paymentPlatformService.cancel(application, cancelRequest);
        // 如有余额信息则返回余额信息
        return result.fund().map(fund ->
            FundBalance.of(fund.getAccountId(), fund.getBalance(), fund.getFrozenAmount())).orElse(null);
    }

    /**
     * 确认交易，只适用于预授权缴费业务类型
     * 预授权业务需经历 prepare->commit->confirm/cancel三个阶段
     * confirm阶段完成资金解冻与消费，消费金额可以大于冻结金额（原订单金额）
     */
    public FundBalance confirm(ServiceRequest<ConfirmRequest> request) {
        ConfirmRequest confirm = request.getData();
        AssertUtils.notEmpty(confirm.getTradeId(), "tradeId missed");
        AssertUtils.notEmpty(confirm.getPassword(), "password missed");
        AssertUtils.notNull(confirm.getAmount(), "amount missed");
        AssertUtils.isTrue(confirm.getAmount() > 0, "Invalid amount");
        // 费用参数校验
        confirm.fees().ifPresent(fees -> fees.stream().forEach(fee -> {
            AssertUtils.notNull(fee.getType(), "fee type missed");
            AssertUtils.notNull(fee.getTypeName(), "fee name missed");
            AssertUtils.notNull(fee.getAmount(), "fee amount missed");
            AssertUtils.isTrue(fee.getAmount() > 0, "Invalid fee amount");
        }));

        ApplicationPermit application = request.getContext().getObject(ApplicationPermit.class.getName(), ApplicationPermit.class);
        PaymentResult result = paymentPlatformService.confirm(application, confirm);
        // 如有余额信息则返回余额信息
        return result.fund().map(fund ->
            FundBalance.of(fund.getAccountId(), fund.getBalance(), fund.getFrozenAmount())).orElse(null);
    }
}