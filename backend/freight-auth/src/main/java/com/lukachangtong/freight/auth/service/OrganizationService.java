package com.lukachangtong.freight.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.auth.entity.SysOrg;
import com.lukachangtong.freight.auth.mapper.SysOrgMapper;
import com.lukachangtong.freight.auth.vo.SysOrgVo;
import com.lukachangtong.freight.common.BizException;
import com.lukachangtong.freight.common.ErrorCode;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 机构管理（系统设置）。 */
@Service
public class OrganizationService {

    @Resource
    private SysOrgMapper orgMapper;

    public List<SysOrgVo> tree() {
        List<SysOrg> all = orgMapper.selectList(null);
        Map<Long, SysOrgVo> map = new HashMap<Long, SysOrgVo>();
        for (SysOrg o : all) {
            map.put(o.getId(), toVo(o));
        }
        List<SysOrgVo> roots = new ArrayList<SysOrgVo>();
        for (SysOrgVo vo : map.values()) {
            Long pid = vo.getParentId();
            if (pid == null || pid == 0L || !map.containsKey(pid)) {
                roots.add(vo);
            } else {
                map.get(pid).getChildren().add(vo);
            }
        }
        return roots;
    }

    public List<SysOrg> list(Long orgId) {
        QueryWrapper<SysOrg> qw = new QueryWrapper<SysOrg>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        return orgMapper.selectList(qw);
    }

    public Long create(SysOrg org) {
        if (orgMapper.selectCount(new QueryWrapper<SysOrg>().eq("org_code", org.getOrgCode())) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "机构编码已存在");
        }
        orgMapper.insert(org);
        return org.getId();
    }

    public void update(SysOrg org) {
        if (org.getId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "缺少主键");
        }
        orgMapper.updateById(org);
    }

    public void delete(Long id) {
        if (orgMapper.selectCount(new QueryWrapper<SysOrg>().eq("parent_id", id)) > 0) {
            throw new BizException(ErrorCode.ILLEGAL_STATE_TRANSFER, "存在下级机构，无法删除");
        }
        orgMapper.deleteById(id);
    }

    private SysOrgVo toVo(SysOrg o) {
        SysOrgVo vo = new SysOrgVo();
        vo.setId(o.getId());
        vo.setParentId(o.getParentId());
        vo.setOrgName(o.getOrgName());
        vo.setOrgCode(o.getOrgCode());
        vo.setStatus(o.getStatus());
        return vo;
    }
}
