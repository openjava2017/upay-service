package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.ApplicationPermit;
import com.diligrp.xtrade.upay.trade.domain.PaymentRequest;
import com.diligrp.xtrade.upay.trade.domain.TradeRequest;

public interface IPaymentPlatformService {
    String createTrade(ApplicationPermit application, TradeRequest trade);

    String commit(ApplicationPermit application, PaymentRequest request);
}
