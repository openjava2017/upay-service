package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.Payment;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;
import com.diligrp.xtrade.upay.trade.domain.TradeStateDto;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
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

@Service("depositPaymentService")
public class PredepositPaymentServiceImpl implements IPaymentService {

    @Resource
    private IAccountChannelService accountChannelService;

    @Resource
    private ITradePaymentDao tradePaymentDao;

    @Resource
    private ITradeOrderDao tradeOrderDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PaymentResult commit(TradeOrder trade, Payment payment) {
        if (!ChannelType.forDeposit(payment.getChannelId())) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "不支持该渠道进行预充值业务");
        }
        if (!trade.getAccountId().equals(payment.getAccountId())) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "充值账号不一致");
        }

        // 处理个人充值
        String paymentId = trade.getTradeId();
        LocalDateTime now = LocalDateTime.now();
        AccountChannel channel = AccountChannel.of(paymentId, trade.getAccountId());
        IFundTransaction transaction = channel.openTransaction(trade.getType(), now);
        transaction.income(trade.getAmount(), FundType.FUND.getCode(), FundType.FUND.getName());
        AccountFund fund = accountChannelService.submit(transaction);

        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.SUCCESS.getCode(), trade.getVersion(), now);
        int result = tradeOrderDao.compareAndSetState(tradeState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }
        TradePayment paymentDo = TradePayment.builder().paymentId(paymentId).tradeId(trade.getTradeId())
            .channelId(payment.getChannelId()).accountId(trade.getAccountId()).name(trade.getName()).cardNo(null)
            .amount(payment.getAmount()).fee(0L).state(PaymentState.SUCCESS.getCode())
            .description(tradeName(payment.getChannelId())).version(0).createdTime(now).build();
        tradePaymentDao.insertTradePayment(paymentDo);

        return PaymentResult.of(PaymentResult.CODE_SUCCESS, paymentId, fund);
    }

    private String tradeName(int channelType) {
        if (channelType == ChannelType.CASH.getCode()) {
            return "现金预存款";
        } else if (channelType == ChannelType.POS.getCode()) {
            return "POS预存款";
        }
        return null;
    }

    @Override
    public TradeType supportType() {
        return TradeType.PRE_DEPOSIT;
    }
}
