package com.freewater.ddd.demo.ddd.userinterfaces.compensate.command;

import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.strategy.StrategyGoodsCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.strategy.StrategyNoGoodsCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.strategy.StrategySupplyAgainCommand;
import lombok.Data;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;

/**
 * 售后补偿策略对象
 *
 * @auther FreeWater
 * @date 2021-8-26
 */
@Data
public class CompensateStrategyCommand implements Serializable {
    /**
     * 补发策略对象
     */
    @Valid
    private List<StrategySupplyAgainCommand> strategySupplyAgainCommandList;
    /**
     * 商品属性-问题商品退款
     */
    @Valid
    private List<StrategyGoodsCommand> strategyGoodsCommandList;
    /**
     * 非商品属性-退款策略
     */
    @Valid
    private StrategyNoGoodsCommand strategyNoGoodsCommand;
}
