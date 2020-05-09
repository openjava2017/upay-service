package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.Payment;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;
import com.diligrp.xtrade.upay.trade.model.TradeOrder;
import com.diligrp.xtrade.upay.trade.type.TradeType;

public interface IPaymentService {
    PaymentResult commit(TradeOrder trade, Payment payment);

    TradeType supportType();
}