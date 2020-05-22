package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.PreTradeDto;

public interface IPreTradePaymentService extends IPaymentService {
    void confirm(PreTradeDto request);

    void cancel(String paymentId);
}
