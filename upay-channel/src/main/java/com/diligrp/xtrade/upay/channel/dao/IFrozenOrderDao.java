package com.diligrp.xtrade.upay.channel.dao;

import com.diligrp.xtrade.shared.mybatis.MybatisMapperSupport;
import com.diligrp.xtrade.upay.channel.domain.UpdateFrozenState;
import com.diligrp.xtrade.upay.channel.model.FrozenOrder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("frozenOrderDao")
public interface IFrozenOrderDao extends MybatisMapperSupport {
    void insertFrozenOrder(FrozenOrder frozenOrder);

    Optional<FrozenOrder> findFrozenOrderById(Long frozenId);

    int compareAndSetState(UpdateFrozenState frozenState);
}