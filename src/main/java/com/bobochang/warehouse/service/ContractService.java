package com.bobochang.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bobochang.warehouse.entity.Checkin;
import com.bobochang.warehouse.entity.Contract;
import com.bobochang.warehouse.entity.Result;
import com.bobochang.warehouse.mapper.ContractMapper;
import com.bobochang.warehouse.page.Page;

import java.util.List;

/**
 * @author bobochang
 * @Description
 * @Date 2023/9/19 - 10:54
 */
public interface ContractService extends IService<Contract> {
    // 根据合同 id 查询合同
    public Contract findContractById(Integer contractId);

    // 分页查询合同
    public Page queryContractPage(Page page,Contract contract);

    // 添加合同
    public Result saveContract(Contract contract);

    // 修改合同状态
    public int updateContractState(Contract contract);

    // 修改合同昵称
    public Result updateContractById(Contract contract);

    public int searchContractCount(Contract contract);

    Contract findContractById(int contractId);

    Result selectAllContract();

    int updateContractIfPurchase(Contract contract);
}
