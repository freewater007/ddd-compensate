package com.freewater.ddd.demo.ddd.application.compensate.service.impl;

import com.freewater.ddd.demo.ddd.application.compensate.event.CompensateDealBillStateEvent;
import com.freewater.ddd.demo.ddd.application.compensate.event.CompensateDealBillStateModify;
import com.freewater.ddd.demo.ddd.application.compensate.service.CompensateBusinessService;
import com.freewater.ddd.demo.ddd.domain.compensate.aggregate.CompensateBillA;
import com.freewater.ddd.demo.ddd.domain.compensate.aggregate.CompensateDealBillA;
import com.freewater.ddd.demo.ddd.domain.compensate.factory.CompensateBillFactory;
import com.freewater.ddd.demo.ddd.domain.compensate.factory.CompensateDealBillFactory;
import com.freewater.ddd.demo.ddd.domain.compensate.repository.CompensateBillRepository;
import com.freewater.ddd.demo.ddd.domain.compensate.repository.CompensateDealBillRepository;
import com.freewater.ddd.demo.ddd.domain.compensate.service.CompensateBillDomainService;
import com.freewater.ddd.demo.ddd.domain.compensate.service.CompensateDealBillDomainService;
import com.freewater.ddd.demo.ddd.domain.compensate.vo.CompensateBillId;
import com.freewater.ddd.demo.ddd.domain.compensate.vo.OrderV;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.acl.CompensateBusinessFacade;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.acl.CompensateSelectFacade;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.assembler.CompensateAssembler;
import com.freewater.ddd.demo.ddd.infrastructure.compensate.enums.CheckTypeEnum;
import com.ddd.demo.ddd.userinterfaces.compensate.command.*;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.dealbill.DealBillCreateCommand;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.dto.CompensateDto;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.query.CompensateBillQuery;
import com.freewater.ddd.demo.feign.user.dto.UserResponse;
import com.freewater.ddd.demo.ddd.userinterfaces.compensate.command.*;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ????????????????????????????????????
 *
 * @auther FreeWater
 * @date 2021-8-30
 */
@Service
@AllArgsConstructor
public class CompensateBusinessServiceImpl implements CompensateBusinessService {

    private CompensateBillDomainService compensateBillDomainService;
    private CompensateDealBillDomainService compensateDealBillDomainService;
    private CompensateBillFactory compensateBillFactory;
    private CompensateDealBillFactory compensateDealBillFactory;
    private CompensateSelectFacade compensateSelectFacade;
    private CompensateBillRepository compensateBillRepository;
    private CompensateDealBillRepository compensateDealBillRepository;
    private CompensateBusinessFacade compensateBusinessFacade;
    private ApplicationContext applicationContext;
    private CompensateAssembler compensateAssembler;

    @Override
    public long save(CompensateApplyCommand compensateApplyCommand) {
        // ??????????????? ???????????????
        // ??????????????????-?????????????????????
        OrderV orderV = compensateSelectFacade.getOrderResponse(compensateApplyCommand.getCompensateBillCommand().getSubOid());
        // ??????????????????-?????????????????????
        UserResponse userResponse = compensateSelectFacade.getUserResponse(compensateApplyCommand.getCompensateBillCommand().getActuid());
        // ????????????????????????
        CompensateBillA compensateBillA = compensateBillFactory.createCompensateBillA(compensateApplyCommand, orderV, userResponse);
        // ??????????????????????????????????????????
        CompensateBillA compensateBillAdd = compensateBillA.process(compensateBillDomainService);
        // ????????????????????????
        compensateBillRepository.save(compensateBillAdd);
        // ??????????????????????????????????????????
        sendCreateMessage(compensateBillAdd);
        // ????????????????????????????????????
        long coid = compensateBillAdd.getCompensateBillId().getCoid();
        check(CheckTypeEnum.AUTO_CHECK, coid, compensateBillAdd.getActuid());
        return coid;
    }

    @Override
    public long saveOffline(CompensateOfflineCommand compensateOfflineCommand) {
        // ??????????????????-?????????????????????
        OrderV orderV = compensateSelectFacade.getOrderResponse(compensateOfflineCommand.getCompensateBillCommand().getSubOid());
        // ????????????????????????
        CompensateBillA compensateBillA = compensateBillFactory.createOfflineCompensateBillA(compensateOfflineCommand, orderV);
        // ??????????????????????????????????????????
        CompensateBillA compensateBillAdd = compensateBillA.process(compensateBillDomainService);
        // ????????????????????????
        compensateBillRepository.save(compensateBillAdd);
        // ??????????????????????????????????????????
        sendCreateMessage(compensateBillAdd);
        // ????????????????????????????????????
        long coid = compensateBillAdd.getCompensateBillId().getCoid();
        check(CheckTypeEnum.AUTO_CHECK, coid, compensateBillAdd.getActuid());
        return coid;
    }

    private void check(CheckTypeEnum checkTypeEnum, long coid, long acid) {
        CompensateCheckCommand checkCommand = new CompensateCheckCommand();
        checkCommand.setCoid(coid);
        checkCommand.setOperator(acid);
        // ???????????????????????????
        CompensateBillId compensateBillId = new CompensateBillId();
        compensateBillId.setCoid(coid);
        CompensateBillA compensateBillA = compensateBillRepository.find(compensateBillId);
        boolean check = compensateBillA.check(compensateBillDomainService, checkTypeEnum, checkCommand);
        if (check) {
            // ???????????????????????????
            compensateBillRepository.passCheck(checkCommand);
            // ???????????????????????????
            DealBillCreateCommand dealBillCreateCommand = new DealBillCreateCommand();
            dealBillCreateCommand.setCoid(coid);
            dealBillCreateCommand.setActuid(acid);
            this.deal(dealBillCreateCommand);
        }
    }

    @Override
    public void deal(DealBillCreateCommand dealBillCreateCommand) {
        CompensateDealBillA compensateDealBillA = compensateDealBillFactory.createCompensateBillA(dealBillCreateCommand);
        CompensateDealBillA compensateDealResult = compensateDealBillA.deal(compensateDealBillDomainService);
        // ???????????????????????????
        compensateDealBillRepository.save(compensateDealResult);
        // ????????????????????????????????????????????????????????????????????????????????????
        this.sendDealBillEvent(compensateDealResult);
    }

    private void sendDealBillEvent(CompensateDealBillA compensateDealBillA) {
        CompensateDealBillStateModify compensateDealBillStateModify = new CompensateDealBillStateModify();
        compensateDealBillStateModify.setCompensateBillId(compensateDealBillA.getCompensateBillId());
        compensateDealBillStateModify.setCompensateDealBillId(compensateDealBillA.getCompensateDealBillId());
        compensateDealBillStateModify.setCompensateDealBillStateEnum(compensateDealBillA.getCompensateDealBillStateEnum());
        applicationContext.publishEvent(new CompensateDealBillStateEvent(compensateDealBillStateModify));
    }

    private void sendCreateMessage(CompensateBillA compensateBillA) {
        int shopId = compensateBillA.getOrderV().getShopId();
        long coid = compensateBillA.getCompensateBillId().getCoid();
        //????????????????????????
        compensateBusinessFacade.sendCompensateCreateMns(coid, shopId);
        if (compensateSelectFacade.specialCompanyTag(shopId)) {
            // ???????????????????????????????????????
            compensateBusinessFacade.deductUserPoint(compensateBillA.getReasonid(),
                    compensateBillA.getOrderV().getAcId(), coid);
        }
    }

    @Override
    public void batchCheck(long[] coids) {
        for (long coid : coids) {
            check(CheckTypeEnum.MANUAL_CHECK, coid, 0);
        }
    }

    @Override
    public void batchCancel(long[] coids) {
        for (long coid : coids) {
            this.cancel(coid, 0);
        }
    }

    private void cancel(long coid, long acid) {
        // ???????????????????????????
        CompensateBillId compensateBillId = new CompensateBillId();
        compensateBillId.setCoid(coid);
        CompensateBillA compensateBillA = compensateBillRepository.find(compensateBillId);
        boolean cancel = compensateBillA.canCancel();
        if (cancel) {
            CompensateCancelCommand compensateCancelCommand = new CompensateCancelCommand();
            compensateCancelCommand.setCoid(coid);
            compensateCancelCommand.setOperator(acid);
            compensateBillRepository.cancel(compensateCancelCommand);
            if (compensateSelectFacade.specialCompanyTag(compensateBillA.getOrderV().getShopId())) {
                // ???????????????????????????????????????
                compensateBusinessFacade.recoveryUserPoint(compensateBillA.getReasonid(),
                        compensateBillA.getOrderV().getAcId(), coid);
            }
        }
    }

    @Override
    public void updateDuty(CompensateUpdateDutyCommand compensateUpdateDutyCommand) {
        // ???????????????????????????
        CompensateBillId compensateBillId = new CompensateBillId();
        compensateBillId.setCoid(compensateUpdateDutyCommand.getCoid());
        CompensateBillA compensateBillA = compensateBillRepository.find(compensateBillId);
        compensateBillA.canUpdateDuty(compensateUpdateDutyCommand);
        // ?????????????????????
        compensateBillRepository.updateBillDuty(compensateUpdateDutyCommand);
    }

    @Override
    public void saveFileLink(CompensateFileLinkCommand compensateFileLinkCommand) {
        // ???????????????????????????
        CompensateBillId compensateBillId = new CompensateBillId();
        compensateBillId.setCoid(compensateFileLinkCommand.getCoid());
        CompensateBillA compensateBillA = compensateBillRepository.find(compensateBillId);
        compensateBillA.canUpdateFileLink(compensateFileLinkCommand);
        // ?????????????????????
        compensateBillRepository.updateBillFileLink(compensateFileLinkCommand);
    }

    @Override
    public List<CompensateDto> listCompensateDto(CompensateBillQuery query) {
        List<CompensateBillA> compensateBillList = compensateBillRepository.listCompensateBillA(query);
        if (CollectionUtils.isEmpty(compensateBillList)) {
            return Collections.emptyList();
        }
        return compensateBillList.stream().map(
                compensateBillA -> {
                    CompensateDto compensateDto = compensateAssembler.entityToCompensateDto(compensateBillA);
                    return compensateDto;
                }
        ).collect(Collectors.toList());

    }
}
