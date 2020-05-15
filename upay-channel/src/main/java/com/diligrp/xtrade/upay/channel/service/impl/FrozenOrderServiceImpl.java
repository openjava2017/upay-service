package com.diligrp.xtrade.upay.channel.service.impl;

import com.diligrp.xtrade.shared.sequence.IKeyGenerator;
import com.diligrp.xtrade.shared.sequence.KeyGeneratorManager;
import com.diligrp.xtrade.upay.channel.dao.IFrozenOrderDao;
import com.diligrp.xtrade.upay.channel.domain.*;
import com.diligrp.xtrade.upay.channel.exception.PaymentChannelException;
import com.diligrp.xtrade.upay.channel.model.FrozenOrder;
import com.diligrp.xtrade.upay.channel.service.IAccountChannelService;
import com.diligrp.xtrade.upay.channel.service.IFrozenOrderService;
import com.diligrp.xtrade.upay.channel.type.FrozenState;
import com.diligrp.xtrade.upay.channel.type.FrozenType;
import com.diligrp.xtrade.upay.core.ErrorCode;
import com.diligrp.xtrade.upay.core.dao.IFundAccountDao;
import com.diligrp.xtrade.upay.core.model.FundAccount;
import com.diligrp.xtrade.upay.core.type.SequenceKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Optional;

@Service("frozenOrderService")
public class FrozenOrderServiceImpl implements IFrozenOrderService {

    @Resource
    private IFrozenOrderDao frozenOrderDao;

    @Resource
    private IFundAccountDao fundAccountDao;

    @Resource
    private KeyGeneratorManager keyGeneratorManager;

    @Resource
    private IAccountChannelService accountChannelService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Long freeze(FreezeFundDto request) {
        Optional<FrozenType> frozenTypeOpt = FrozenType.getType(request.getType());
        frozenTypeOpt.orElseThrow(() -> new PaymentChannelException(ErrorCode.ILLEGAL_ARGUMENT_ERROR, "不支持此冻结类型"));
        Optional<FundAccount> accountOpt = fundAccountDao.findFundAccountById(request.getAccountId());
        FundAccount account = accountOpt.orElseThrow(() -> new PaymentChannelException(ErrorCode.ACCOUNT_NOT_FOUND, "资金账号不存在"));

        // 冻结资金
        LocalDateTime now = LocalDateTime.now();
        AccountChannel channel = AccountChannel.of(null, request.getAccountId());
        IFundTransaction transaction = channel.openTransaction(FrozenState.FROZEN.getCode(), now);
        transaction.freeze(request.getAmount());
        accountChannelService.submit(transaction);

        // 创建冻结资金订单
        IKeyGenerator keyGenerator = keyGeneratorManager.getKeyGenerator(SequenceKey.FROZEN_ID);
        long frozenId = keyGenerator.nextId();
        FrozenOrder frozenOrder = FrozenOrder.builder().frozenId(frozenId).paymentId(request.getPaymentId())
            .accountId(request.getAccountId()).name(account.getName()).type(request.getType())
            .amount(request.getAmount()).state(FrozenState.FROZEN.getCode()).userId(request.getUserId())
            .userName(request.getUserName()).description(request.getDescription()).version(0).createdTime(now).build();
        frozenOrderDao.insertFrozenOrder(frozenOrder);
        return frozenId;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void unfreeze(UnfreezeFundDto request) {
        Optional<FrozenOrder> orderOpt = frozenOrderDao.findFrozenOrderById(request.getFrozenId());
        FrozenOrder order = orderOpt.orElseThrow(() -> new PaymentChannelException(ErrorCode.OBJECT_NOT_FOUND, "冻结订单不存在"));
        if (order.getState() != FrozenState.FROZEN.getCode()) {
            throw new PaymentChannelException(ErrorCode.INVALID_OBJECT_STATE, "无效冻结状态，不能执行解冻操作");
        }

        LocalDateTime now = LocalDateTime.now();
        AccountChannel channel = AccountChannel.of(null, order.getAccountId());
        IFundTransaction transaction = channel.openTransaction(FrozenState.UNFROZEN.getCode(), now);
        transaction.unfreeze(order.getAmount());
        accountChannelService.submit(transaction);

        UpdateFrozenState updateState = UpdateFrozenState.of(request.getFrozenId(), FrozenState.UNFROZEN.getCode(),
                order.getVersion(), now);
        updateState.setUserId(request.getUserId());
        updateState.setUserName(request.getUserName());
        if (frozenOrderDao.compareAndSetState(updateState) <= 0) {
            throw new PaymentChannelException(ErrorCode.DATA_CONCURRENT_UPDATED, "系统忙，请稍后再试");
        }
    }
}
