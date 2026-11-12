package com.lukachangtong.freight.track.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import com.lukachangtong.freight.track.dto.FenceSaveRequest;
import com.lukachangtong.freight.track.entity.TrFence;
import com.lukachangtong.freight.track.mapper.TrFenceMapper;
import com.lukachangtong.freight.track.service.FenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FenceServiceImpl implements FenceService {

    private static final List<String> FENCE_TYPES = Arrays.asList("LOAD", "UNLOAD", "FORBIDDEN", "CUSTOM");
    private static final List<String> TRIGGERS = Arrays.asList(
            TrFence.TRIGGER_IN, TrFence.TRIGGER_OUT, TrFence.TRIGGER_BOTH);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private TrFenceMapper fenceMapper;

    @Override
    public List<TrFence> list(Long orgId, String fenceType, Integer enabled) {
        QueryWrapper<TrFence> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        if (fenceType != null && !fenceType.trim().isEmpty()) {
            qw.eq("fence_type", fenceType.trim());
        }
        if (enabled != null) {
            qw.eq("enabled", enabled);
        }
        qw.orderByDesc("id");
        return fenceMapper.selectList(qw);
    }

    @Override
    public TrFence get(Long id) {
        TrFence fence = fenceMapper.selectById(id);
        if (fence == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "围栏不存在：" + id);
        }
        return fence;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FenceSaveRequest request) {
        TrFence fence = new TrFence();
        apply(request, fence, true);
        fence.setCreateBy(request.getOperateBy());
        fence.setCreateTime(LocalDateTime.now());
        fence.setUpdateTime(LocalDateTime.now());
        fenceMapper.insert(fence);
        return fence.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FenceSaveRequest request) {
        TrFence fence = get(id);
        apply(request, fence, false);
        fence.setUpdateBy(request.getOperateBy());
        fence.setUpdateTime(LocalDateTime.now());
        fenceMapper.updateById(fence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        get(id);
        fenceMapper.deleteById(id);
    }

    @Override
    public List<TrFence> listEnabled(Long orgId) {
        if (orgId == null) {
            return new ArrayList<>();
        }
        QueryWrapper<TrFence> qw = new QueryWrapper<>();
        qw.eq("org_id", orgId).eq("enabled", 1);
        return fenceMapper.selectList(qw);
    }

    private void apply(FenceSaveRequest req, TrFence fence, boolean isCreate) {
        if (req.getOrgId() == null && isCreate) {
            throw new BizException(ErrorCode.PARAM_INVALID, "orgId 不能为空");
        }
        if (req.getFenceName() == null || req.getFenceName().trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "围栏名称不能为空");
        }
        String shape = req.getShape() == null ? TrFence.SHAPE_CIRCLE : req.getShape().trim().toUpperCase();
        if (!TrFence.SHAPE_CIRCLE.equals(shape) && !TrFence.SHAPE_POLYGON.equals(shape)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "shape 仅支持 CIRCLE / POLYGON");
        }
        String type = req.getFenceType() == null ? "CUSTOM" : req.getFenceType().trim().toUpperCase();
        if (!FENCE_TYPES.contains(type)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "fenceType 仅支持 LOAD/UNLOAD/FORBIDDEN/CUSTOM");
        }
        String trigger = req.getTriggerType() == null ? TrFence.TRIGGER_BOTH : req.getTriggerType().trim().toUpperCase();
        if (!TRIGGERS.contains(trigger)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "triggerType 仅支持 IN/OUT/BOTH");
        }

        if (TrFence.SHAPE_CIRCLE.equals(shape)) {
            if (req.getCenterLat() == null || req.getCenterLng() == null
                    || req.getRadiusM() == null || req.getRadiusM() <= 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "圆形围栏必须提供中心点与正数半径（米）");
            }
            fence.setCenterLat(req.getCenterLat());
            fence.setCenterLng(req.getCenterLng());
            fence.setRadiusM(req.getRadiusM());
            fence.setVertices(null);
        } else {
            List<List<Double>> vs = req.getVertices();
            if (vs == null || vs.size() < 3) {
                throw new BizException(ErrorCode.PARAM_INVALID, "多边形围栏至少需要 3 个顶点");
            }
            for (List<Double> v : vs) {
                if (v == null || v.size() < 2 || v.get(0) == null || v.get(1) == null) {
                    throw new BizException(ErrorCode.PARAM_INVALID, "顶点格式应为 [lat,lng]");
                }
            }
            try {
                fence.setVertices(objectMapper.writeValueAsString(vs));
            } catch (Exception e) {
                throw new BizException(ErrorCode.PARAM_INVALID, "顶点序列化失败");
            }
            fence.setCenterLat(null);
            fence.setCenterLng(null);
            fence.setRadiusM(null);
        }

        if (isCreate) {
            if (req.getFenceCode() == null || req.getFenceCode().trim().isEmpty()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "围栏编码不能为空");
            }
            fence.setOrgId(req.getOrgId());
            fence.setFenceCode(req.getFenceCode().trim());
        }
        fence.setFenceName(req.getFenceName().trim());
        fence.setFenceType(type);
        fence.setShape(shape);
        fence.setTriggerType(trigger);
        fence.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        fence.setRemark(req.getRemark());
    }

    /** 供预警分析复用：把 vertices JSON 解析成坐标数组。 */
    public static List<double[]> parseVertices(ObjectMapper mapper, String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<List<Double>> raw = mapper.readValue(json, new TypeReference<List<List<Double>>>() { });
            List<double[]> out = new ArrayList<>(raw.size());
            for (List<Double> v : raw) {
                out.add(new double[]{v.get(0), v.get(1)});
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
