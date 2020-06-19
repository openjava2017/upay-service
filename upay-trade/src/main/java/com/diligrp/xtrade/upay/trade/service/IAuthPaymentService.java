package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.ConfirmRequest;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;

/**
 * 预授权支付接口
 * 预授权交易业务场景处理经历三个阶段: prepare(创建交易) -> commit(冻结资金) -> confirm/cancel(确认消费/撤销冻结)
 */
public interface IAuthPaymentService extends IPaymentService {
    /**
     * "预授权交易"确认预授权消费(交易冻结后确认实际缴费金额)，当前业务场景允许实际缴费金额大于冻结金额
     */
    PaymentResult confirm(ConfirmRequest request);
}