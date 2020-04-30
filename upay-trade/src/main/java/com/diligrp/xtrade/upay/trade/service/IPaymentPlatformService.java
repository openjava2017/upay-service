package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.Application;
import com.diligrp.xtrade.upay.trade.domain.Payment;
import com.diligrp.xtrade.upay.trade.domain.Trade;

public interface IPaymentPlatformService {
    String createTrade(Application application, Trade trade);

    String tradePay(Application application, Payment payment);
}
