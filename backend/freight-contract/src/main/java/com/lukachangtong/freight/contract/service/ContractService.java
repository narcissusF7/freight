package com.lukachangtong.freight.contract.service;

import com.lukachangtong.freight.contract.dto.ContractSaveRequest;
import com.lukachangtong.freight.contract.entity.CtContract;

import java.util.List;

public interface ContractService {

    List<CtContract> list(Long orgId);

    CtContract detail(Long id);

    /** 创建合同：生成合同号 + SHA-256 摘要，状态待签署。 */
    Long create(ContractSaveRequest request);

    /**
     * 签署：party = A / B。调 CaSignClient 对内容摘要签章，
     * 双方签完置状态 2 并在同一事务写 Outbox（驱动合同归档/监管报送）。
     */
    CtContract sign(Long id, String party, String operator);

    /**
     * 验签：重算摘要后校验签名。
     *
     * @param contentOverride 非空时用该内容重算摘要（用于演示"内容被篡改则验签失败"）
     * @param party           A / B，空则取先签的一方
     */
    boolean verify(Long id, String contentOverride, String party);
}
