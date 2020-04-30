package com.diligrp.xtrade.upay.trade;

import com.diligrp.xtrade.shared.mybatis.MybatisMapperSupport;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.diligrp.xtrade.upay.trade")
@MapperScan(basePackages =  {"com.diligrp.xtrade.upay.trade.dao"}, markerInterface = MybatisMapperSupport.class)
public class TradeConfiguration {
}