package com.diligrp.xtrade.upay.trade.service;

import com.diligrp.xtrade.upay.trade.domain.ApplicationPermit;

public interface IAccessPermitService {
    ApplicationPermit loadApplicationPermit(Long appId);
}
