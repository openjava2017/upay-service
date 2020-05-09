package com.diligrp.xtrade.upay.trade.dao;

import com.diligrp.xtrade.shared.mybatis.MybatisMapperSupport;
import com.diligrp.xtrade.upay.trade.model.PaymentFee;
import com.diligrp.xtrade.upay.trade.model.TradeFee;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("tradeFeeDao")
public interface ITradeFeeDao extends MybatisMapperSupport {
    void insertTradeFees(List<TradeFee> fees);

    List<TradeFee> findTradeFees(String tradeId);

    void insertPaymentFees(List<PaymentFee> fees);

    List<PaymentFee> findPaymentFees(String paymentId);
}
