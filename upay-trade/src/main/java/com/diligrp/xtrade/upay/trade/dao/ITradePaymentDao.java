package com.diligrp.xtrade.upay.trade.dao;

import com.diligrp.xtrade.shared.mybatis.MybatisMapperSupport;
import com.diligrp.xtrade.upay.trade.model.TradePayment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("tradePaymentDao")
public interface ITradePaymentDao extends MybatisMapperSupport {
    void insertTradePayment(TradePayment payment);

    Optional<TradePayment> findTradePaymentById(String paymentId);
}