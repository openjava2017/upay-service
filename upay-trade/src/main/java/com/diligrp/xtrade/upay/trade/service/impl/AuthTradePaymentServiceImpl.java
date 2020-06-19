package com.diligrp.xtrade.upay.trade.service.impl;

import com.diligrp.xtrade.shared.exception.ServiceAccessException;
import com.diligrp.xtrade.shared.sequence.ISerialKeyGenerator;
import com.diligrp.xtrade.shared.sequence.KeyGeneratorManager;
import com.diligrp.xtrade.upay.channel.dao.IFrozenOrderDao;
import com.diligrp.xtrade.upay.channel.domain.AccountChannel;
import com.diligrp.xtrade.upay.channel.domain.FrozenStateDto;
import com.diligrp.xtrade.upay.channel.domain.IFundTransaction;
import com.diligrp.xtrade.upay.channel.model.FrozenOrder;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.type.ChannelType;
import com.diligrp.xtrade.upay.channel.type.FrozenState;
import com.diligrp.xtrade.upay.channel.type.FrozenType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IFundAccountDao;
import com.diligrp.xtrade.upay.core.dao.IMerchantDao;
import com.diligrp.xtrade.upay.core.model.AccountFund;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.type.SequenceKey;
import com.diligrp.xtrade.upay.trade.dao.IPaymentFeeDao;
import com.diligrp.xtrade.upay.trade.dao.IRefundPaymentDao;
import com.diligrp.xtrade.upay.trade.dao.ITradeOrderDao;
import com.diligrp.xtrade.upay.trade.dao.ITradePaymentDao;
import com.diligrp.xtrade.upay.trade.domain.Confirm;
import com.diligrp.xtrade.upay.trade.domain.Fee;
import com.diligrp.xtrade.upay.trade.domain.MerchantPermit;
import com.diligrp.xtrade.upay.trade.domain.Payment;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;
import com.diligrp.xtrade.upay.trade.domain.PaymentStateDto;
import com.diligrp.xtrade.upay.trade.domain.Refund;
import com.diligrp.xtrade.upay.trade.domain.TradeStateDto;
import com.diligrp.xtrade.upay.trade.exception.TradePaymentException;
import com.diligrp.xtrade.upay.trade.model.PaymentFee;
import com.diligrp.xtrade.upay.trade.model.RefundPayment;
import com.diligrp.xtrade.upay.trade.model.TradeOrder;
import com.diligrp.xtrade.upay.trade.model.TradePayment;
import com.diligrp.xtrade.upay.trade.service.IPaymentService;
import com.diligrp.xtrade.upay.trade.type.FundType;
import com.diligrp.xtrade.upay.trade.type.PaymentState;
import com.diligrp.xtrade.upay.trade.type.TradeState;
import com.diligrp.xtrade.upay.trade.type.TradeType;
import com.diligrp.xtrade.upay.trade.util.PaymentDatedIdStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("authTradePaymentService")
public class AuthTradePaymentServiceImpl extends TradePaymentServiceImpl implements IPaymentService {

    @Resource
    private IAccountChannelService accountChannelService;

    @Resource
    private ITradePaymentDao tradePaymentDao;

    @Resource
    private ITradeOrderDao tradeOrderDao;

    @Resource
    private IPaymentFeeDao paymentFeeDao;

    @Resource
    private IFrozenOrderDao frozenOrderDao;

    @Resource
    private IRefundPaymentDao refundPaymentDao;

    @Resource
    private IFundAccountDao fundAccountDao;

    @Resource
    private IMerchantDao merchantDao;

    @Resource
    private KeyGeneratorManager keyGeneratorManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PaymentResult commit(TradeOrder trade, Payment payment) {
        if (payment.getChannelId() != ChannelType.ACCOUNT.getCode()) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "不支持该渠道进行即时交易业务");
        }
        if (trade.getAccountId().equals(payment.getAccountId())) {
            throw new TradePaymentException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "同一账号不能进行交易");
        }
        Optional<FundAccount> accountOpt = fundAccountDao.findFundAccountById(payment.getAccountId());
        FundAccount account = accountOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.ACCOUNT_NOT_FOUND, "资金账号不存在"));

        // 冻结资金
        LocalDateTime now = LocalDateTime.now();
        accountChannelService.checkTradePermission(payment.getAccountId(), payment.getPassword(), 5);
        ISerialKeyGenerator keyGenerator = keyGeneratorManager.getSerialKeyGenerator(SequenceKey.PAYMENT_ID);
        String paymentId = keyGenerator.nextSerialNo(new PaymentDatedIdStrategy(trade.getType()));
        AccountChannel channel = AccountChannel.of(paymentId, payment.getAccountId());
        IFundTransaction transaction = channel.openTransaction(FrozenState.FROZEN.getCode(), now);
        transaction.freeze(trade.getAmount());
        accountChannelService.submit(transaction);

        // 创建冻结资金订单
        long frozenId = keyGeneratorManager.getKeyGenerator(SequenceKey.FROZEN_ID).nextId();
        FrozenOrder frozenOrder = FrozenOrder.builder().frozenId(frozenId).paymentId(paymentId)
            .accountId(payment.getAccountId()).name(account.getName()).type(FrozenType.TRADE_FROZEN.getCode())
            .amount(trade.getAmount()).state(FrozenState.FROZEN.getCode()).description(null)
            .version(0).createdTime(now).build();
        frozenOrderDao.insertFrozenOrder(frozenOrder);

        // 冻结交易订单
        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.FROZEN.getCode(), trade.getVersion(), now);
        int result = tradeOrderDao.compareAndSetState(tradeState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }
        // 生成"处理中"支付的支付记录
        TradePayment paymentDo = TradePayment.builder().paymentId(paymentId).tradeId(trade.getTradeId())
            .channelId(payment.getChannelId()).accountId(trade.getAccountId()).name(trade.getName()).cardNo(null)
            .amount(payment.getAmount()).fee(0L).state(PaymentState.PROCESSING.getCode())
            .description(TradeType.AUTH_TRADE.getName()).version(0).createdTime(now).build();
        tradePaymentDao.insertTradePayment(paymentDo);

        return PaymentResult.of(PaymentResult.CODE_SUCCESS, paymentId);
    }

    /**
     * "预授权缴费"业务确认预授权消费(交易冻结后确认实际缴费金额)，当前业务场景允许实际缴费金额大于冻结金额
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PaymentResult confirm(TradeOrder trade, Confirm confirm) {
        Optional<List<Fee>> feesOpt = confirm.getObjects(Fee.class.getName());
        List<Fee> fees = feesOpt.orElseGet(Collections::emptyList);

        // "预授权交易"不存在组合支付的情况，因此一个交易订单只对应一条支付记录
        Optional<TradePayment> paymentOpt = tradePaymentDao.findOneTradePayment(trade.getTradeId());
        TradePayment payment = paymentOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "支付记录不存在"));
        // 查询冻结订单
        Optional<FrozenOrder> orderOpt = frozenOrderDao.findFrozenOrderByPaymentId(payment.getPaymentId());
        FrozenOrder frozenOrder = orderOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "冻结订单不存在"));
        if (frozenOrder.getState() != FrozenState.FROZEN.getCode()) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "无预授权资金记录");
        }

        LocalDateTime now = LocalDateTime.now();
        accountChannelService.checkTradePermission(payment.getAccountId(), confirm.getPassword(), 5);
        // 获取商户收益账号信息
        MerchantPermit merchant = merchantDao.findMerchantById(trade.getMchId()).map(mer -> MerchantPermit.of(
            mer.getMchId(), mer.getProfitAccount(), mer.getVouchAccount(), mer.getPledgeAccount()))
            .orElseThrow(() -> new ServiceAccessException(ErrorCode.OBJECT_NOT_FOUND, "商户信息未注册"));

        // 处理买家付款和买家佣金
        AccountChannel fromChannel = AccountChannel.of(payment.getPaymentId(), payment.getAccountId());
        IFundTransaction fromTransaction = fromChannel.openTransaction(trade.getType(), now);
        fromTransaction.unfreeze(frozenOrder.getAmount());
        fromTransaction.outgo(confirm.getAmount(), FundType.FUND.getCode(), FundType.FUND.getName());
        fees.stream().filter(Fee::forBuyer).forEach(fee -> {
            fromTransaction.outgo(fee.getAmount(), fee.getType(), fee.getTypeName());
        });
        AccountFund fund = accountChannelService.submit(fromTransaction);

        // 处理卖家收款和卖家佣金
        AccountChannel toChannel = AccountChannel.of(payment.getPaymentId(), trade.getAccountId());
        IFundTransaction toTransaction = toChannel.openTransaction(trade.getType(), now);
        toTransaction.income(confirm.getAmount(), FundType.FUND.getCode(), FundType.FUND.getName());
        fees.stream().filter(Fee::forSeller).forEach(fee -> {
            toTransaction.outgo(fee.getAmount(), fee.getType(), fee.getTypeName());
        });
        accountChannelService.submit(toTransaction);

        // 处理商户收益
        if (!fees.isEmpty()) {
            AccountChannel merChannel = AccountChannel.of(payment.getPaymentId(), merchant.getProfitAccount());
            IFundTransaction merTransaction = merChannel.openTransaction(trade.getType(), now);
            fees.forEach(fee ->
                merTransaction.income(fee.getAmount(), fee.getType(), fee.getTypeName())
            );
            accountChannelService.submit(merTransaction);
        }

        // 修改冻结订单"已解冻"状态
        FrozenStateDto frozenState = FrozenStateDto.of(frozenOrder.getFrozenId(), FrozenState.UNFROZEN.getCode(),
            frozenOrder.getVersion(), now);
        if (frozenOrderDao.compareAndSetState(frozenState) == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        // 卖家佣金存储在TradeOrder订单模型中
        long toFee = fees.stream().filter(Fee::forSeller).mapToLong(Fee::getAmount).sum();
        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), confirm.getAmount(), toFee,
            TradeState.SUCCESS.getCode(), trade.getVersion(), now);
        int result = tradeOrderDao.compareAndSetState(tradeState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }
        // 买家佣金存储在TradePayment支付模型中
        long fromFee = fees.stream().filter(Fee::forBuyer).mapToLong(Fee::getAmount).sum();
        PaymentStateDto paymentState = PaymentStateDto.of(payment.getPaymentId(), confirm.getAmount(), fromFee,
            PaymentState.SUCCESS.getCode(), payment.getVersion(), now);
        result = tradePaymentDao.compareAndSetState(paymentState);
        if (result == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统正忙，请稍后重试");
        }
        if (!fees.isEmpty()) {
            List<PaymentFee> paymentFeeDos = fees.stream().map(fee ->
                PaymentFee.of(payment.getPaymentId(), fee.getUseFor(), fee.getAmount(), fee.getType(), fee.getTypeName(), now)
            ).collect(Collectors.toList());
            paymentFeeDao.insertPaymentFees(paymentFeeDos);
        }

        return PaymentResult.of(PaymentResult.CODE_SUCCESS, payment.getPaymentId(), fund);
    }

    /**
     * 撤销交易-退交易资金和佣金，交易撤销需要修改交易订单状态
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PaymentResult cancel(TradeOrder trade, Refund cancel) {
        if (trade.getState() == TradeState.SUCCESS.getCode()) {
            return super.cancel(trade, cancel);
        }

        // "预授权交易"不存在组合支付的情况，因此一个交易订单只对应一条支付记录
        Optional<TradePayment> paymentOpt = tradePaymentDao.findOneTradePayment(trade.getTradeId());
        TradePayment payment = paymentOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "支付记录不存在"));
        // 查询冻结订单
        LocalDateTime when = LocalDateTime.now();
        Optional<FrozenOrder> orderOpt = frozenOrderDao.findFrozenOrderByPaymentId(payment.getPaymentId());
        FrozenOrder order = orderOpt.orElseThrow(() -> new TradePaymentException(ErrorCode.OBJECT_NOT_FOUND, "冻结订单不存在"));
        if (order.getState() != FrozenState.FROZEN.getCode()) {
            throw new TradePaymentException(ErrorCode.OPERATION_NOT_ALLOWED, "无效交易状态，不能执行该操作");
        }

        // 解冻冻结资金
        AccountChannel channel = AccountChannel.of(payment.getPaymentId(), payment.getAccountId());
        IFundTransaction transaction = channel.openTransaction(trade.getType(), when);
        transaction.unfreeze(trade.getAmount());
        AccountFund fund = accountChannelService.submit(transaction);
        // 修改冻结订单状态
        FrozenStateDto frozenState = FrozenStateDto.of(order.getFrozenId(), FrozenState.UNFROZEN.getCode(),
            trade.getVersion(), when);
        if (frozenOrderDao.compareAndSetState(frozenState) == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        // 撤销支付记录
        PaymentStateDto paymentState = PaymentStateDto.of(payment.getPaymentId(), PaymentState.CANCELED.getCode(),
            payment.getVersion(), when);
        if (tradePaymentDao.compareAndSetState(paymentState) == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        // 撤销交易订单
        TradeStateDto tradeState = TradeStateDto.of(trade.getTradeId(), TradeState.CANCELED.getCode(), trade.getVersion(), when);
        if (tradeOrderDao.compareAndSetState(tradeState) == 0) {
            throw new TradePaymentException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
        return PaymentResult.of(PaymentResult.CODE_SUCCESS, payment.getPaymentId(), fund);
    }

    @Override
    public TradeType supportType() {
        return TradeType.AUTH_TRADE;
    }
}
