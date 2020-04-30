package com.diligrp.xtrade.upay;

import com.diligrp.xtrade.upay.boss.BossConfiguration;
import com.diligrp.xtrade.upay.core.CoreConfiguration;
import com.diligrp.xtrade.upay.trade.TradeConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
//@SpringBootApplication
@ComponentScan(basePackageClasses = {BossConfiguration.class, TradeConfiguration.class, CoreConfiguration.class})
public class UpayServiceBoostrap {
    public static void main(String[] args) {
        SpringApplication.run(UpayServiceBoostrap.class, args);
    }
}