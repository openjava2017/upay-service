package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.trade.dao.ITradeFeeDao;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.*;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
import com.diligrp.xtrade.upay.trade.model.PaymentFee;
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
        Optional<List<Fee>> feesOpt = payment.getObjects(Fee.class.getName());
        List<Fee> fees = feesOpt.orElse(Collections.emptyList());

        // 处理买家付款
        LocalDateTime now = LocalDateTime.now();
        String paymentId = trade.getTradeId();
        AccountChannel buyerChannel = AccountChannel.of(paymentId, payment.getAccountId());
        IFundTransaction buyerTransaction = buyerChannel.openTransaction(trade.getType(), now);
        buyerTransaction.outgo(trade.getAmount(), FundType.FUND.getCode(), TradeType.getName(trade.getType()));
        accountChannelService.submit(buyerTransaction);

        // 处理卖家收款
        AccountChannel sellerChannel = AccountChannel.of(paymentId, trade.getAccountId());
        IFundTransaction sellerTransaction = sellerChannel.openTransaction(trade.getType(), now);
        sellerTransaction.income(trade.getAmount(), FundType.FUND.getCode(), TradeType.getName(trade.getType()));
        fees.forEach(fee -> {
            sellerTransaction.outgo(fee.getAmount(), fee.getType(), FundType.getName(fee.getType()));
        });
        accountChannelService.submit(sellerTransaction);

        // 处理商户收益
        if (!fees.isEmpty()) {
            Merchant merchant = payment.getObject(Merchant.class.getName(), Merchant.class);
            AccountChannel merChannel = AccountChannel.of(paymentId, merchant.getProfitAccount());
            IFundTransaction merTransaction = merChannel.openTransaction(trade.getType(), now);
            fees.forEach(fee ->
                merTransaction.income(fee.getAmount(), fee.getType(), FundType.getName(fee.getType()))
            );
            accountChannelService.submit(merTransaction);
        }

        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.SUCCESS.getCode(),
            now, trade.getVersion());
        int result = tradeOrderDao.compareAndSetState(tradeState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        long totalFees = fees.stream().mapToLong(Fee::getAmount).sum();
        TradePayment paymentDo = TradePayment.builder().paymentId(paymentId).tradeId(trade.getTradeId())
            .channelId(payment.getChannelId()).accountId(trade.getAccountId()).name(trade.getName()).cardNo(null)
            .amount(payment.getAmount()).fee(totalFees).state(PaymentState.SUCCESS.getCode()).description(null)
            .version(0).createdTime(now).build();
        tradePaymentDao.insertTradePayment(paymentDo);
        if (!fees.isEmpty()) {
            List<PaymentFee> paymentFeeDos = fees.stream().map(fee ->
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
