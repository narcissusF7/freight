package com.lukachangtong.freight.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.contract.dto.ContractSaveRequest;
import com.lukachangtong.freight.contract.entity.CtContract;
import com.lukachangtong.freight.contract.mapper.CtContractMapper;
import com.lukachangtong.freight.contract.service.ContractService;
import com.lukachangtong.freight.infra.client.CaSignClient;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignRequest;
import com.lukachangtong.freight.infra.client.dto.IntegrationDto.SignResult;
import com.lukachangtong.freight.infra.mapper.OutboxEventMapper;
import com.lukachangtong.freight.infra.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ContractServiceImpl implements ContractService {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    @Resource
    private CtContractMapper contractMapper;
    @Resource
    private CaSignClient caSignClient;
    @Resource
    private OutboxEventMapper outboxEventMapper;

    @Override
    public List<CtContract> list(Long orgId) {
        QueryWrapper<CtContract> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("id");
        return contractMapper.selectList(qw);
    }

    @Override
    public CtContract detail(Long id) {
        return contractMapper.selectById(id);
    }

    @Override
    public Long create(ContractSaveRequest request) {
        String type = request.getContractType() == null ? CtContract.TYPE_TRANSPORT : request.getContractType();
        if (!CtContract.TYPE_FRAMEWORK.equals(type) && !CtContract.TYPE_TRANSPORT.equals(type)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "未知的合同类型: " + type);
        }
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            content = composeContent(type, request);
        }
        CtContract c = new CtContract();
        c.setOrgId(request.getOrgId());
        c.setContractNo(generateContractNo());
        c.setContractType(type);
        c.setWaybillId(request.getWaybillId());
        c.setPartyA(request.getPartyA());
        c.setPartyB(request.getPartyB());
        c.setContent(content);
        c.setContentHash(sha256Hex(content));
        c.setSignStatus(CtContract.STATUS_PENDING);
        c.setCreateBy(request.getOperateBy());
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        contractMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CtContract sign(Long id, String party, String operator) {
        CtContract c = contractMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "合同不存在");
        }
        if (c.getSignStatus() != null && c.getSignStatus() == CtContract.STATUS_BOTH_SIGNED) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "合同已双方签署完成");
        }
        String p = party == null ? "A" : party.trim().toUpperCase();
        if (!"A".equals(p) && !"B".equals(p)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "签署方只能是 A 或 B");
        }
        if ("A".equals(p) && c.getSignatureA() != null) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "甲方已签署，请勿重复提交");
        }
        if ("B".equals(p) && c.getSignatureB() != null) {
            throw new BizException(ErrorCode.IDEMPOTENT_CONFLICT, "乙方已签署，请勿重复提交");
        }

        SignRequest req = new SignRequest();
        req.setRequestId(c.getContractNo() + ":" + p);
        req.setContractNo(c.getContractNo());
        req.setContentHash(c.getContentHash());
        req.setSignerId("A".equals(p) ? c.getPartyA() : c.getPartyB());
        SignResult result = caSignClient.sign(req);
        if (result == null || !result.isSuccess()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR,
                    "CA 签章失败: " + (result == null ? "无响应" : result.getMessage()));
        }

        CtContract upd = new CtContract();
        upd.setId(id);
        if ("A".equals(p)) {
            upd.setSignatureA(result.getSignature());
        } else {
            upd.setSignatureB(result.getSignature());
        }
        upd.setCertSerialNo(result.getCertSerialNo());
        String otherSignature = "A".equals(p) ? c.getSignatureB() : c.getSignatureA();
        boolean bothSigned = otherSignature != null;
        upd.setSignStatus(bothSigned ? CtContract.STATUS_BOTH_SIGNED : CtContract.STATUS_PARTY_A_SIGNED);
        if (bothSigned) {
            upd.setSignTime(LocalDateTime.now());
        }
        upd.setUpdateBy(operator);
        upd.setUpdateTime(LocalDateTime.now());
        contractMapper.updateById(upd);

        if (bothSigned) {
            writeOutbox(c.getContractNo(), "CONTRACT_SIGNED",
                    "{\"contractNo\":\"" + c.getContractNo() + "\",\"type\":\"" + c.getContractType() + "\"}");
        }
        return contractMapper.selectById(id);
    }

    @Override
    public boolean verify(Long id, String contentOverride, String party) {
        CtContract c = contractMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "合同不存在");
        }
        String p = party == null || party.trim().isEmpty() ? "A" : party.trim().toUpperCase();
        String signature = "B".equals(p) ? c.getSignatureB() : c.getSignatureA();
        if (signature == null) {
            return false;
        }
        String content = (contentOverride == null || contentOverride.isEmpty()) ? c.getContent() : contentOverride;
        String hash = sha256Hex(content);
        return caSignClient.verify(hash, signature);
    }

    // ---------- 内部 ----------

    private String composeContent(String type, ContractSaveRequest request) {
        String title = CtContract.TYPE_FRAMEWORK.equals(type) ? "网络货运托运框架协议" : "网络货运司机运输合同";
        StringBuilder sb = new StringBuilder();
        sb.append("合同名称：").append(title).append('\n');
        sb.append("合同类型：").append(type).append('\n');
        if (request.getWaybillId() != null) {
            sb.append("关联运单ID：").append(request.getWaybillId()).append('\n');
        }
        sb.append("甲方：").append(request.getPartyA() == null ? "" : request.getPartyA()).append('\n');
        sb.append("乙方：").append(request.getPartyB() == null ? "" : request.getPartyB()).append('\n');
        sb.append("签订日期：").append(LocalDate.now()).append('\n');
        sb.append("特别约定：承运方须保证人、车、证照在运输全程有效，"
                + "运输轨迹与回单完整留存不少于三年，用于监管核验。");
        return sb.toString();
    }

    /** 合同号：CT + yyyyMMddHHmmssSSS + 3 位随机（唯一索引 uk_ct_no）。 */
    private String generateContractNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "CT" + ts + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private static String sha256Hex(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(HEX[(b >> 4) & 0x0f]).append(HEX[b & 0x0f]);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "合同摘要计算失败: " + e.getMessage());
        }
    }

    private void writeOutbox(String contractNo, String eventType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventKey(contractNo + ":" + eventType + ":" + System.currentTimeMillis());
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(OutboxEvent.STATUS_PENDING);
        event.setRetryCount(0);
        outboxEventMapper.insert(event);
    }
}
