package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.trade.dao.ITradeFeeDao;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.Merchant;
import com.diligrp.xtrade.upay.trade.domain.Payment;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;
import com.diligrp.xtrade.upay.trade.domain.TradeStateDto;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
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

@Service("withdrawPaymentService")
public class WithdrawPaymentServiceImpl implements IPaymentService {

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
        if (payment.getChannelId() != ChannelType.POS.getCode()) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "不支持该渠道进行账户提现");
        }
        if (!trade.getAccountId().equals(payment.getAccountId())) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "提现账号不一致");
        }

        List<TradeFee> tradeFees = Collections.emptyList();
        if (trade.getFee() > 0) {
            tradeFees = tradeFeeDao.findTradeFees(trade.getTradeId());
        }

        // 处理个人提现
        LocalDateTime now = LocalDateTime.now();
        String paymentId = trade.getTradeId();
        AccountChannel channel = AccountChannel.of(paymentId, trade.getAccountId(), trade.getType(), now);
        channel.outgo(trade.getAmount(), FundType.FUND.getCode(), TradeType.getName(trade.getType()));
        tradeFees.forEach(fee -> {
            channel.outgo(fee.getAmount(), fee.getType(), FundType.getName(fee.getType()));
        });
        AccountFund fund = accountChannelService.submit(channel);

        // 处理商户收益
        if (!tradeFees.isEmpty()) {
            Merchant merchant = payment.getObject(Merchant.class.getName(), Merchant.class);
            AccountChannel merChannel = AccountChannel.of(paymentId, merchant.getProfitAccount(), trade.getType(), now);
            tradeFees.forEach(fee ->
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
        TradePayment paymentDo = TradePayment.builder().paymentId(paymentId).tradeId(trade.getTradeId())
            .channelId(payment.getChannelId()).accountId(trade.getAccountId()).name(trade.getName()).cardNo(null)
            .amount(payment.getAmount()).fee(0L).state(PaymentState.SUCCESS.getCode()).description(null)
            .version(0).createdTime(now).build();
        tradePaymentDao.insertTradePayment(paymentDo);

        return PaymentResult.of(paymentId, TradeState.SUCCESS.getCode(), fund);
    }

    @Override
    public TradeType supportType() {
        return TradeType.WITHDRAW;
    }
}
