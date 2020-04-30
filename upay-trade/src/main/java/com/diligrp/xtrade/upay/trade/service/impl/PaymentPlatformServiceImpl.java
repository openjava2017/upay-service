package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.shared.sequence.ISerialKeyGenerator;
import com.diligrp.xtrade.shared.sequence.KeyGeneratorManager;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IFundAccountDao;
import com.diligrp.xtrade.upay.core.exception.FundAccountException;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.type.SequenceKey;
import com.diligrp.xtrade.upay.trade.dao.ITradeFeeDao;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.Application;
import com.diligrp.xtrade.upay.trade.domain.Fee;
import com.diligrp.xtrade.upay.trade.domain.Payment;
import com.diligrp.xtrade.upay.trade.domain.Trade;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
import com.diligrp.xtrade.upay.trade.model.TradeFee;
import com.diligrp.xtrade.upay.trade.model.TradeOrder;
import com.diligrp.xtrade.upay.trade.model.TradePayment;
import com.diligrp.xtrade.upay.trade.service.IPaymentPlatformService;
import com.diligrp.xtrade.upay.trade.type.FundType;
import com.diligrp.xtrade.upay.trade.type.PaymentState;
import com.diligrp.xtrade.upay.trade.type.TradeState;
import com.diligrp.xtrade.upay.trade.type.TradeType;
import com.diligrp.xtrade.upay.trade.util.TypeDatedIdStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("paymentPlatformService")
public class PaymentPlatformServiceImpl implements IPaymentPlatformService {

    @Resource
    private IFundAccountDao fundAccountDao;

    @Resource
    private ITradeOrderDao tradeOrderDao;

    @Resource
    private ITradeFeeDao tradeFeeDao;

    @Resource
    private ITradePaymentDao tradePaymentDao;

    @Resource
    private KeyGeneratorManager keyGeneratorManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String createTrade(Application application, Trade trade) {
        LocalDateTime when = LocalDateTime.now();
        Optional<TradeType> tradeType = TradeType.getType(trade.getType());
        tradeType.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));
        Optional<FundAccount> accountOpt = fundAccountDao.findFundAccountById(trade.getAccountId());
        FundAccount account = accountOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "资金账号不存在"));
        ISerialKeyGenerator keyGenerator = keyGeneratorManager.getSerialKeyGenerator(SequenceKey.TRADE_ID);
        String tradeId = keyGenerator.nextSerialNo(new TypeDatedIdStrategy(trade.getType()));

        // 检查是否是系统支持的费用类型，然后计算总费用
        trade.getFees().stream().map(fee -> FundType.getFee(fee.getType())).forEach(feeOpt ->
                feeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "不支持的费用类型")));
        long fees = trade.getFees().stream().mapToLong(Fee::getAmount).sum();

        TradeOrder tradeOrder = TradeOrder.builder().mchId(application.getMchId()).appId(application.getAppId())
                .tradeId(tradeId).type(trade.getType()).serialNo(trade.getSerialNo()).cycleNo(trade.getCycleNo())
                .accountId(account.getAccountId()).name(account.getName()).amount(trade.getAmount())
                .maxAmount(trade.getAmount()).fee(fees).state(TradeState.PENDING.getCode())
                .description(trade.getDescription()).version(0).createdTime(when).build();
        List<TradeFee> tradeFees = trade.getFees().stream().map(
                fee -> TradeFee.of(tradeId, fee.getAmount(), fee.getType(), when)).collect(Collectors.toList());
        tradeOrderDao.insertTradeOrder(tradeOrder);
        tradeFeeDao.insertTradeFees(tradeFees);
        return tradeId;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String tradePay(Application application, Payment payment) {
        LocalDateTime when = LocalDateTime.now();
        Optional<ChannelType> channelType = ChannelType.getType(payment.getChannelId());
        channelType.orElseThrow(() -> new TradePaymentException(ErrorCode.CHANNEL_NOT_SUPPORTED, "不支持的支付渠道"));

        Optional<TradeOrder> tradeOrderOpt = tradeOrderDao.findTradeOrderById(payment.getTradeId());
        TradeOrder tradeOrder = tradeOrderOpt.orElseThrow(
                () -> new TradePaymentException(ErrorCode.TRADE_NOT_FOUND, "交易不存在"));
        if (tradeOrder.getState() == TradeState.SUCCESS.getCode()) {

        }
        return null;
    }
}
