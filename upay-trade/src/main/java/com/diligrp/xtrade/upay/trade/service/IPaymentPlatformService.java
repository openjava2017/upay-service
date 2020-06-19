package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.trade.domain.ConfirmRequest;
import com.diligrp.xtrade.upay.trade.domain.PaymentRequest;
import com.diligrp.xtrade.upay.trade.domain.PaymentResult;
import com.diligrp.xtrade.upay.trade.domain.RefundRequest;
import com.diligrp.xtrade.upay.trade.domain.TradeRequest;

public interface IPaymentPlatformService {
    String createTrade(ApplicationPermit application, TradeRequest trade);

    PaymentResult commit(ApplicationPermit application, PaymentRequest request);

    PaymentResult cancel(ApplicationPermit application, RefundRequest request);

    PaymentResult confirm(ApplicationPermit application, ConfirmRequest request);
}
