package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.shared.sequence.ISerialKeyGenerator;
import com.diligrp.xtrade.shared.sequence.KeyGeneratorManager;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IFundAccountDao;
import com.diligrp.xtrade.upay.core.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.core.domain.MerchantPermit;
import com.diligrp.xtrade.upay.core.exception.FundAccountException;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.type.SequenceKey;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.domain.Confirm;
import com.diligrp.xtrade.upay.trade.domain.ConfirmRequest;
import com.diligrp.xtrade.upay.trade.domain.Fee;
import com.diligrp.xtrade.upay.trade.domain.Payment;
import com.diligrp.xtrade.upay.trade.domain.PaymentRequest;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;
import com.diligrp.xtrade.upay.trade.domain.Refund;
import com.diligrp.xtrade.upay.trade.domain.RefundRequest;
import com.diligrp.xtrade.upay.trade.domain.TradeRequest;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
import com.diligrp.xtrade.upay.trade.model.TradeOrder;
import com.diligrp.xtrade.upay.trade.service.IPaymentPlatformService;
import com.diligrp.xtrade.upay.trade.service.IPaymentService;
import com.diligrp.xtrade.upay.trade.type.TradeState;
import com.diligrp.xtrade.upay.trade.type.TradeType;
import com.diligrp.xtrade.upay.trade.util.TradeDatedIdStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service("paymentPlatformService")
public class PaymentPlatformServiceImpl implements IPaymentPlatformService, BeanPostProcessor {

    @Resource
    private IFundAccountDao fundAccountDao;

    @Resource
    private ITradeOrderDao tradeOrderDao;

    @Resource
    private KeyGeneratorManager keyGeneratorManager;

    private Map<TradeType, IPaymentService> services = new HashMap<>();

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String createTrade(ApplicationPermit application, TradeRequest trade) {
        LocalDateTime when = LocalDateTime.now();
        Optional<TradeType> tradeType = TradeType.getType(trade.getType());
        tradeType.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));
        Optional<FundAccount> accountOpt = fundAccountDao.findFundAccountById(trade.getAccountId());
        FundAccount account = accountOpt.orElseThrow(() -> new FundAccountException(ErrorCode.ACCOUNT_NOT_FOUND, "资金账号不存在"));

        ISerialKeyGenerator keyGenerator = keyGeneratorManager.getSerialKeyGenerator(SequenceKey.TRADE_ID);
        String tradeId = keyGenerator.nextSerialNo(new TradeDatedIdStrategy(trade.getType()));
        TradeOrder tradeOrder = TradeOrder.builder().mchId(application.getMerchant().getMchId()).appId(application.getAppId())
            .tradeId(tradeId).type(trade.getType()).serialNo(trade.getSerialNo()).cycleNo(trade.getCycleNo())
            .accountId(account.getAccountId()).name(account.getName()).amount(trade.getAmount())
            .maxAmount(trade.getAmount()).fee(0L).state(TradeState.PENDING.getCode())
            .description(trade.getDescription()).version(0).createdTime(when).build();
        tradeOrderDao.insertTradeOrder(tradeOrder);
        return tradeId;
    }

    @Override
    public PaymentResult commit(ApplicationPermit application, PaymentRequest request) {
        Optional<ChannelType> channelType = ChannelType.getType(request.getChannelId());
        channelType.orElseThrow(() -> new TradePaymentException(ErrorCode.CHANNEL_NOT_SUPPORTED, "不支持的支付渠道"));

        Optional<TradeOrder> tradeOpt = tradeOrderDao.findTradeOrderById(request.getTradeId());
        TradeOrder trade = tradeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_FOUND, "交易不存在"));
        if (trade.getState() != TradeState.PENDING.getCode()) {
            throw new TradePaymentException(ErrorCode.INVALID_TRADE_STATE, "无效的交易状态");
        }
        Optional<TradeType> typeOpt = TradeType.getType(trade.getType());
        TradeType tradeType = typeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));
        Optional<IPaymentService> serviceOpt = tradeService(tradeType);
        IPaymentService service = serviceOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));
        if (!trade.getMchId().equals(application.getMerchant().getMchId())) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "商户信息错误");
        }

        // 检查是否是系统支持的费用类型 - 支付系统只负责记录费用类型，暂不校验费用类型，原因是业务系统费用过多且可动态配置
//        List<Fee> feeList = request.fees().orElseGet(Collections::emptyList);
//        feeList.stream().map(fee -> FundType.getFee(fee.getType())).forEach(feeOpt ->
//            feeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "不支持的费用类型")));

        Payment payment = Payment.of(request.getAccountId(), trade.getAmount(), request.getChannelId(), request.getPassword());
        payment.put(MerchantPermit.class.getName(), application.getMerchant());
        request.fees().ifPresent(fees -> payment.put(Fee.class.getName(), fees));

        return service.commit(trade, payment);
    }

    @Override
    public PaymentResult confirm(ApplicationPermit application, ConfirmRequest request) {
        Optional<TradeOrder> tradeOpt = tradeOrderDao.findTradeOrderById(request.getTradeId());
        TradeOrder trade = tradeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_FOUND, "交易不存在"));
        if (!TradeState.forConfirm(trade.getState())) {
            throw new TradePaymentException(ErrorCode.INVALID_TRADE_STATE, "无效的交易状态，不能确认消费");
        }
        Optional<TradeType> typeOpt = TradeType.getType(trade.getType());
        TradeType tradeType = typeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));
        Optional<IPaymentService> serviceOpt = tradeService(tradeType);
        IPaymentService service = serviceOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));

        Confirm confirm = Confirm.of(request.getAccountId(), request.getAmount(), request.getPassword());
        confirm.put(MerchantPermit.class.getName(), application.getMerchant());
        request.fees().ifPresent(fees -> confirm.put(Fee.class.getName(), fees));
        return service.confirm(trade, confirm);
    }

    @Override
    public PaymentResult cancel(ApplicationPermit application, RefundRequest request) {
        Optional<TradeOrder> tradeOpt = tradeOrderDao.findTradeOrderById(request.getTradeId());
        TradeOrder trade = tradeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_FOUND, "交易不存在"));
        if (!TradeState.forCancel(trade.getState())) {
            throw new TradePaymentException(ErrorCode.INVALID_TRADE_STATE, "无效的交易状态，不能撤销交易");
        }
        Optional<TradeType> typeOpt = TradeType.getType(trade.getType());
        TradeType tradeType = typeOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));
        Optional<IPaymentService> serviceOpt = tradeService(tradeType);
        IPaymentService service = serviceOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.TRADE_NOT_SUPPORTED, "不支持的交易类型"));

        Refund cancel = Refund.of(request.getAccountId(), trade.getAmount(), request.getPassword());
        cancel.put(MerchantPermit.class.getName(), application.getMerchant());
        return service.cancel(trade, cancel);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof IPaymentService) {
            IPaymentService tradeService = (IPaymentService) bean;
            services.put(tradeService.supportType(), tradeService);
        }
        return bean;
    }

    private Optional<IPaymentService> tradeService(TradeType tradeType) {
        return Optional.ofNullable(services.get(tradeType));
    }
}
