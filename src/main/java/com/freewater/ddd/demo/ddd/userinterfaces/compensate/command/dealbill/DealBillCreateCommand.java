package com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.dealbill;

import lombok.Data;

import java.io.Serializable;

/**
 * 售后履约单申请处理命令
 *
 * @auther FreeWater
 * @date 2021-8-26
 */
@Data
public class DealBillCreateCommand implements Serializable {

    /**
     * 履约单号
     */
    private Long coid;
    /**
     * 操作人编码
     */
    private Long actuid;
}
