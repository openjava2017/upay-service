package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.Application;
import com.diligrp.xtrade.upay.trade.domain.PaymentRequest;
import com.diligrp.xtrade.upay.trade.domain.TradeRequest;

public interface IPaymentPlatformService {
    String createTrade(Application application, TradeRequest trade);

    String commit(Application application, PaymentRequest request);
}
