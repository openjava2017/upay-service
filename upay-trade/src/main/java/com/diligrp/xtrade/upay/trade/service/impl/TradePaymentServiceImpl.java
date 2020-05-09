package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.trade.dao.ITradeFeeDao;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.*;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
import com.diligrp.xtrade.upay.trade.model.PaymentFee;
import com.diligrp.xtrade.upay.trade.model.TradeFee;
import com.diligrp.xtrade.upay.trade.model.TradeOrder;
import com.diligrp.xtrade.upay.trade.model.TradePayment;
import com.diligrp.xtrade.upay.trade.service.IPaymentService;
import com.diligrp.xtrade.upay.trade.type.FundType;
import com.diligrp.xtrade.upay.trade.type.PaymentState;
import com.diligrp.xtrade.upay.trade.type.TradeState;
import com.diligrp.xtrade.upay.trade.type.TradeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("tradePaymentService")
public class TradePaymentServiceImpl implements IPaymentService {

    @Resource
    private IAccountChannelService accountChannelService;

    @Resource
    private ITradePaymentDao tradePaymentDao;

    @Resource
    private ITradeOrderDao tradeOrderDao;

    @Resource
    private ITradeFeeDao tradeFeeDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PaymentResult commit(TradeOrder trade, Payment payment) {
        if (payment.getChannelId() != ChannelType.ACCOUNT.getCode()) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "不支持该渠道进行即时交易");
        }
        if (trade.getAccountId().equals(payment.getAccountId())) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "同一账号不能进行交易");
        }

        List<TradeFee> tradeFees = Collections.emptyList();
        if (trade.getFee() > 0) {
            tradeFees = tradeFeeDao.findTradeFees(trade.getTradeId());
        }
        Optional<List<Fee>> paymentFeeOpt = payment.getObjects(Fee.class.getName());
        List<Fee> paymentFees = paymentFeeOpt.orElse(Collections.emptyList());

        // 处理买家付款
        LocalDateTime now = LocalDateTime.now();
        String paymentId = trade.getTradeId();
        AccountChannel sellerChannel = AccountChannel.of(paymentId, payment.getAccountId(), trade.getType(), now);
        sellerChannel.outgo(trade.getAmount(), FundType.FUND.getCode(), TradeType.getName(trade.getType()));
        paymentFees.forEach(fee -> {
            sellerChannel.outgo(fee.getAmount(), fee.getType(), FundType.getName(fee.getType()));
        });
        accountChannelService.submit(sellerChannel);

        // 处理卖家收款
        AccountChannel buyerChannel = AccountChannel.of(paymentId, trade.getAccountId(), trade.getType(), now);
        buyerChannel.income(trade.getAmount(), FundType.FUND.getCode(), TradeType.getName(trade.getType()));
        tradeFees.forEach(fee -> {
            buyerChannel.outgo(fee.getAmount(), fee.getType(), FundType.getName(fee.getType()));
        });
        accountChannelService.submit(buyerChannel);

        // 处理商户收益
        if (!tradeFees.isEmpty() || !paymentFees.isEmpty()) {
            Merchant merchant = payment.getObject(Merchant.class.getName(), Merchant.class);
            AccountChannel merChannel = AccountChannel.of(paymentId, merchant.getProfitAccount(), trade.getType(), now);
            tradeFees.forEach(fee ->
                merChannel.income(fee.getAmount(), fee.getType(), FundType.getName(fee.getType()))
            );
            paymentFees.forEach(fee ->
                merChannel.income(fee.getAmount(), fee.getType(), FundType.getName(fee.getType()))
            );
            accountChannelService.submit(merChannel);
        }

        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.SUCCESS.getCode(),
            now, trade.getVersion());
        int result = tradeOrderDao.compareAndSetState(tradeState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        long totalFees = paymentFees.stream().mapToLong(Fee::getAmount).sum();
        TradePayment paymentDo = TradePayment.builder().paymentId(paymentId).tradeId(trade.getTradeId())
            .channelId(payment.getChannelId()).accountId(trade.getAccountId()).name(trade.getName()).cardNo(null)
            .amount(payment.getAmount()).fee(totalFees).state(PaymentState.SUCCESS.getCode()).description(null)
            .version(0).createdTime(now).build();
        tradePaymentDao.insertTradePayment(paymentDo);
        if (!paymentFees.isEmpty()) {
            List<PaymentFee> paymentFeeDos = paymentFees.stream().map(fee ->
                    PaymentFee.of(paymentId, fee.getAmount(), fee.getType(), now)
            ).collect(Collectors.toList());
            tradeFeeDao.insertPaymentFees(paymentFeeDos);
        }

        return PaymentResult.of(paymentId, TradeState.SUCCESS.getCode());
    }

    @Override
    public TradeType supportType() {
        return TradeType.DIRECT_TRADE;
    }
}
