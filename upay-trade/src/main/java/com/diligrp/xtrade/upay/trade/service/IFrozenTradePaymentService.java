package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.FrozenTradeDto;

public interface IFrozenTradePaymentService extends IPaymentService {
    void confirm(FrozenTradeDto request);

    void cancel(String paymentId);
}