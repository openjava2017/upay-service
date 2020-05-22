package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.shared.sequence.IKeyGenerator;
import com.diligrp.xtrade.shared.sequence.KeyGeneratorManager;
import com.diligrp.xtrade.upay.channel.dao.IFrozenOrderDao;
import com.diligrp.xtrade.upay.channel.domain.*;
import com.diligrp.xtrade.upay.channel.exception.PaymentChannelException;
import com.diligrp.xtrade.upay.channel.model.FrozenOrder;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.channel.type.FrozenState;
import com.diligrp.xtrade.upay.channel.type.FrozenType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IFundAccountDao;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.type.SequenceKey;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.*;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
import com.diligrp.xtrade.upay.trade.model.TradeOrder;
import com.diligrp.xtrade.upay.trade.model.TradePayment;
import com.diligrp.xtrade.upay.trade.service.IPreTradePaymentService;
import com.diligrp.xtrade.upay.trade.type.PaymentState;
import com.diligrp.xtrade.upay.trade.type.TradeState;
import com.diligrp.xtrade.upay.trade.type.TradeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Optional;

@Service("preTradePaymentService")
public class PreTradePaymentServiceImpl implements IPreTradePaymentService {

    @Resource
    private IAccountChannelService accountChannelService;

    @Resource
    private IFundAccountDao fundAccountDao;

    @Resource
    private KeyGeneratorManager keyGeneratorManager;

    @Resource
    private ITradePaymentDao tradePaymentDao;

    @Resource
    private ITradeOrderDao tradeOrderDao;

    @Resource
    private IFrozenOrderDao frozenOrderDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PaymentResult commit(TradeOrder trade, Payment payment) {
        if (payment.getChannelId() != ChannelType.ACCOUNT.getCode()) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "预付交易不支持此渠道类型");
        }
        if (trade.getAccountId().equals(payment.getAccountId())) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "同一账号不能进行交易");
        }
        Optional<FundAccount> accountOpt = fundAccountDao.findFundAccountById(payment.getAccountId());
        FundAccount account = accountOpt.orElseThrow(() -> new PaymentChannelException(ErrorCode.ACCOUNT_NOT_FOUND, "资金账号不存在"));

        // 冻结资金
        LocalDateTime now = LocalDateTime.now();
        String paymentId = trade.getTradeId();
        accountChannelService.checkTradePermission(payment.getAccountId(), payment.getPassword(), 5);
        AccountChannel channel = AccountChannel.of(paymentId, payment.getAccountId());
        IFundTransaction transaction = channel.openTransaction(FrozenState.FROZEN.getCode(), now);
        transaction.freeze(trade.getAmount());
        accountChannelService.submit(transaction);

        // 创建冻结资金订单
        IKeyGenerator keyGenerator = keyGeneratorManager.getKeyGenerator(SequenceKey.FROZEN_ID);
        long frozenId = keyGenerator.nextId();
        FrozenOrder frozenOrder = FrozenOrder.builder().frozenId(frozenId).paymentId(paymentId)
                .accountId(payment.getAccountId()).name(account.getName()).type(FrozenType.TRADE_FROZEN.getCode())
                .amount(trade.getAmount()).state(FrozenState.FROZEN.getCode()).description(null)
                .version(0).createdTime(now).build();
        frozenOrderDao.insertFrozenOrder(frozenOrder);

        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.PROCESSING.getCode(),
                trade.getVersion(), now);
        int result = tradeOrderDao.compareAndSetState(tradeState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }

        TradePayment paymentDo = TradePayment.builder().paymentId(paymentId).tradeId(trade.getTradeId())
                .channelId(payment.getChannelId()).accountId(trade.getAccountId()).name(trade.getName()).cardNo(null)
                .amount(payment.getAmount()).fee(0L).state(PaymentState.PROCESSING.getCode()).description(null)
                .version(0).createdTime(now).build();
        tradePaymentDao.insertTradePayment(paymentDo);

        return PaymentResult.of(paymentId, TradeState.SUCCESS.getCode());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirm(PreTradeDto request) {
        Optional<TradePayment> paymentOpt = tradePaymentDao.findTradePaymentById(request.getPaymentId());
        TradePayment payment = paymentOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "交易不存在"));
        Optional<TradeOrder> tradeOpt = tradeOrderDao.findTradeOrderById(payment.getTradeId());
        TradeOrder trade = tradeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "交易不存在"));
        if (trade.getType() != TradeType.PRE_TRADE.getCode()) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "该交易不允许此类操作");
        }
        if (trade.getState() != TradeState.PROCESSING.getCode()) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "无效的交易状态");
        }
        if (request.getAmount() > trade.getAmount()) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "实际付款金额超过预付金额");
        }

        LocalDateTime when = LocalDateTime.now();
        Optional<FrozenOrder> orderOpt = frozenOrderDao.findFrozenOrderByPaymentId(payment.getPaymentId());
        FrozenOrder order = orderOpt.orElseThrow(() -> new PaymentChannelException(ErrorCode.OBJECT_NOT_FOUND, "冻结订单不存在"));
        if (order.getState() != FrozenState.FROZEN.getCode()) {
            throw new PaymentChannelException(ErrorCode.OPERATION_NOT_ALLOWED, "无效冻结状态，不能执行该操作");
        }

        accountChannelService.checkTradePermission(payment.getAccountId(), request.getPassword(), 5);
        AccountChannel channel = AccountChannel.of(payment.getPaymentId(), order.getAccountId());
        IFundTransaction transaction = channel.openTransaction(trade.getType(), when);
        transaction.unfreeze(order.getAmount());
        transaction.outgo(request.getAmount(), trade.getType(), TradeType.getName(trade.getType()));
        accountChannelService.submit(transaction);

        FrozenStateDto updateState = FrozenStateDto.of(order.getFrozenId(), FrozenState.UNFROZEN.getCode(),
                order.getVersion(), when);
        if (frozenOrderDao.compareAndSetState(updateState) == 0) {
            throw new PaymentChannelException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        PaymentStateDto paymentState = PaymentStateDto.of(payment.getPaymentId(), request.getAmount(),
                PaymentState.SUCCESS.getCode(), payment.getVersion(), when);
        if (tradePaymentDao.compareAndSetState(paymentState) == 0) {
            throw new PaymentChannelException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), request.getAmount(),
                TradeState.SUCCESS.getCode(), trade.getVersion(), when);
        if (tradeOrderDao.compareAndSetState(tradeState) == 0) {
            throw new PaymentChannelException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void cancel(String paymentId) {
        Optional<TradePayment> paymentOpt = tradePaymentDao.findTradePaymentById(paymentId);
        TradePayment payment = paymentOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "交易不存在"));
        Optional<TradeOrder> tradeOpt = tradeOrderDao.findTradeOrderById(payment.getTradeId());
        TradeOrder trade = tradeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "交易不存在"));
        if (trade.getType() != TradeType.PRE_TRADE.getCode()) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "该交易不允许此类操作");
        }
        if (trade.getState() != TradeState.PROCESSING.getCode()) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "无效的交易状态");
        }

        LocalDateTime when = LocalDateTime.now();
        Optional<FrozenOrder> orderOpt = frozenOrderDao.findFrozenOrderByPaymentId(payment.getPaymentId());
        FrozenOrder order = orderOpt.orElseThrow(() -> new PaymentChannelException(ErrorCode.OBJECT_NOT_FOUND, "冻结订单不存在"));
        if (order.getState() != FrozenState.FROZEN.getCode()) {
            throw new PaymentChannelException(ErrorCode.OPERATION_NOT_ALLOWED, "无效冻结状态，不能执行该操作");
        }

        AccountChannel channel = AccountChannel.of(payment.getPaymentId(), order.getAccountId());
        IFundTransaction transaction = channel.openTransaction(trade.getType(), when);
        transaction.unfreeze(order.getAmount());
        accountChannelService.submit(transaction);

        FrozenStateDto updateState = FrozenStateDto.of(order.getFrozenId(), FrozenState.UNFROZEN.getCode(),
                order.getVersion(), when);
        if (frozenOrderDao.compareAndSetState(updateState) == 0) {
            throw new PaymentChannelException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        PaymentStateDto paymentState = PaymentStateDto.of(payment.getPaymentId(), PaymentState.CANCELED.getCode(),
                payment.getVersion(), when);
        if (tradePaymentDao.compareAndSetState(paymentState) == 0) {
            throw new PaymentChannelException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.CANCELED.getCode(),
                trade.getVersion(), when);
        if (tradeOrderDao.compareAndSetState(tradeState) == 0) {
            throw new PaymentChannelException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
    }

    @Override
    public TradeType supportType() {
        return TradeType.PRE_TRADE;
    }
}
