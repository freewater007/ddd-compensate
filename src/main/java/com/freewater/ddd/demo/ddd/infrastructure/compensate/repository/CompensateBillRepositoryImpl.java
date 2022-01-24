package com.freewater.ddd.demo.ddd.infrastructure.compensate.repository;

import com.freewater.ddd.demo.ddd.domain.compensate.aggregate.CompensateBillA;
import com.freewater.ddd.demo.ddd.domain.compensate.repository.CompensateBillRepository;
import com.freewater.ddd.demo.ddd.domain.compensate.vo.CompensateBillId;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.assembler.CompensateAssembler;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.database.CompensateDo;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.enums.CstateEnum;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.exception.CompensateException;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.exception.CompensateFailEnum;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.CompensateCancelCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.CompensateCheckCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.CompensateFileLinkCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.CompensateUpdateDutyCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.strategy.CompensateStateChangeCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.query.CompensateBillQuery;
import com.freewater.ddd.demo.mapper.compensate.CompensateBillMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 补偿商品聚合根-仓库层
 *
 * @auther FreeWater
 * @date 2021-8-26
 */
@Service
@AllArgsConstructor
public class CompensateBillRepositoryImpl implements CompensateBillRepository {

    private CompensateAssembler assembler;
    private CompensateBillMapper compensateMapper;
    protected ObjectMapper objectMapper;


    @Override
    public CompensateBillA find(CompensateBillId compensateBillId) {
        CompensateDo compensateDo = compensateMapper.getCompensate(compensateBillId.getCoid());
        if (Objects.isNull(compensateDo)) {
            throw new CompensateException(CompensateFailEnum.COMPENSATE_IS_NULL);
        }
        return assembler.dbToCompensateBillA(compensateDo);
    }

    @Override
    public CompensateBillA save(CompensateBillA compensateBillA) {
        CompensateDo compensateDo = assembler.entityToCompensate(compensateBillA);
        compensateDo.setCstate(CstateEnum.APPROVAL_PENDING.getValue());
        try {
            String json = objectMapper.writeValueAsString(compensateBillA.getCompensateStrategyE());
            compensateDo.setStrategyJson(json);
            compensateMapper.add(compensateDo);
        } catch (Exception e) {
            throw new CompensateException("对象转换为json发生异常");
        }
        return compensateBillA;
    }

    @Override
    public void updateBillDuty(CompensateUpdateDutyCommand compensateUpdateDutyCommand) {

    }

    @Override
    public void updateBillFileLink(CompensateFileLinkCommand compensateFileLinkCommand) {

    }

    @Override
    public void cancel(CompensateCancelCommand compensate) {
        compensateMapper.updateCompensateState(compensate.getCoid(), CstateEnum.COMPENSATE_CANCEL.getValue());
    }

    @Override
    public void changeState(CompensateStateChangeCommand compensateStateChangeCommand) {
        compensateMapper.updateCompensateState(compensateStateChangeCommand.getCoid(), compensateStateChangeCommand.getState());
    }

    @Override
    public void passCheck(CompensateCheckCommand checkCommand) {
        compensateMapper.updateCompensateState(checkCommand.getCoid(), CstateEnum.COMPENSATE_PENDING.getValue());
    }

    @Override
    public List<CompensateBillA> listCompensateBillA(CompensateBillQuery query) {
        List<CompensateDo> compensateDoList = compensateMapper.listCompensateDo(query);
        if (CollectionUtils.isEmpty(compensateDoList)) {
            return Collections.emptyList();
        }
        return compensateDoList.stream().map(
                compensateDo -> {
                    CompensateBillA compensateBillA = assembler.dbToCompensateBillA(compensateDo);
                    return compensateBillA;
                }
        ).collect(Collectors.toList());
    }
}




















