package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.*;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
import com.diligrp.xtrade.upay.trade.model.TradeOrder;
import com.diligrp.xtrade.upay.trade.model.TradePayment;
import com.diligrp.xtrade.upay.trade.service.IPaymentService;
import com.diligrp.xtrade.upay.trade.type.FundType;
import com.diligrp.xtrade.upay.trade.type.PaymentState;
import com.diligrp.xtrade.upay.trade.type.TradeState;
import com.diligrp.xtrade.upay.trade.type.TradeType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service("transferPaymentService")
public class TransferPaymentServiceImpl implements IPaymentService {
    @Resource
    private IAccountChannelService accountChannelService;

    @Resource
    private ITradePaymentDao tradePaymentDao;

    @Resource
    private ITradeOrderDao tradeOrderDao;

    @Override
    public PaymentResult commit(TradeOrder trade, Payment payment) {
        if (payment.getChannelId() != ChannelType.ACCOUNT.getCode()) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "转账交易不支持此渠道类型");
        }
        if (trade.getAccountId().equals(payment.getAccountId())) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "同一账号不能进行交易");
        }

        // 交易转出
        LocalDateTime now = LocalDateTime.now();
        String paymentId = trade.getTradeId();
        accountChannelService.checkTradePermission(payment.getAccountId(), payment.getPassword(), 5);
        AccountChannel fromChannel = AccountChannel.of(paymentId, payment.getAccountId());
        IFundTransaction fromTransaction = fromChannel.openTransaction(trade.getType(), now);
        fromTransaction.outgo(trade.getAmount(), FundType.FUND.getCode(), TradeType.getName(trade.getType()));
        accountChannelService.submit(fromTransaction);

        // 处理卖家收款
        AccountChannel toChannel = AccountChannel.of(paymentId, trade.getAccountId());
        IFundTransaction toTransaction = toChannel.openTransaction(trade.getType(), now);
        toTransaction.income(trade.getAmount(), FundType.FUND.getCode(), TradeType.getName(trade.getType()));
        accountChannelService.submit(toTransaction);

        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.SUCCESS.getCode(),
                trade.getVersion(), now);
        int result = tradeOrderDao.compareAndSetState(tradeState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        TradePayment paymentDo = TradePayment.builder().paymentId(paymentId).tradeId(trade.getTradeId())
                .channelId(payment.getChannelId()).accountId(trade.getAccountId()).name(trade.getName()).cardNo(null)
                .amount(payment.getAmount()).fee(0L).state(PaymentState.SUCCESS.getCode()).description(null)
                .version(0).createdTime(now).build();
        tradePaymentDao.insertTradePayment(paymentDo);

        return PaymentResult.of(paymentId, TradeState.SUCCESS.getCode());
    }

    @Override
    public TradeType supportType() {
        return TradeType.TRANSFER;
    }
}
