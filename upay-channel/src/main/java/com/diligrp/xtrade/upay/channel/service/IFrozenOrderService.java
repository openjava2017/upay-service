package com.diligrp.xtrade.upay.channel.service;

import com.diligrp.xtrade.upay.channel.domain.FreezeFundDto;

public interface IFrozenOrderService {
    long freeze(FreezeFundDto request);

    void unfreeze(Long frozenId);
}
