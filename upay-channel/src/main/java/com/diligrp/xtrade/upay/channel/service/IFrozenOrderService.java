package com.diligrp.xtrade.upay.channel.service;

import com.diligrp.xtrade.upay.channel.domain.FreezeFundDto;
import com.diligrp.xtrade.upay.channel.domain.UnfreezeFundDto;

public interface IFrozenOrderService {
    Long freeze(FreezeFundDto request);

    void unfreeze(UnfreezeFundDto request);
}
