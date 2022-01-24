package com.freewater.ddd.demo.mapper.compensate;

import com.freewater.ddd.demo.ddd.infrastructure.compensate.database.CompensateDealLogDo;
import com.freewater.ddd.demo.sqlprovider.compensate.CompensateDealBillSqlProvider;
import org.apache.ibatis.annotations.*;

/**
 * 售后补偿履约单数据层mapper接口
 *
 * @auther FreeWater
 * @date 2021-10-18
 */
@Mapper
public interface CompensateDealBillMapper {

    /**
     * 新增售后补偿履约单信息
     *
     * @param compensateDealLogDo 售后补偿履约单对象
     */
    @InsertProvider(type = CompensateDealBillSqlProvider.class, method = "addCompensateDealLogSql")
    @Options(useGeneratedKeys = true, keyProperty = "dealId", keyColumn = "deal_id")
    void addCompensateDealLog(CompensateDealLogDo compensateDealLogDo);

    /**
     * 获取补偿履约单信息
     *
     * @param dealId 补偿单号
     * @return 补偿单基础信息
     */
    @SelectProvider(type = CompensateDealBillSqlProvider.class, method = "getCompensateDealLogSql")
    CompensateDealLogDo getCompensateDealLog(@Param("dealId") int dealId);

    /**
     * 更改补偿单状态
     *
     * @param compensateDealLogDo 履约单数据表对象
     */
    @UpdateProvider(type = CompensateDealBillSqlProvider.class, method = "updateSql")
    void update(CompensateDealLogDo compensateDealLogDo);
}
