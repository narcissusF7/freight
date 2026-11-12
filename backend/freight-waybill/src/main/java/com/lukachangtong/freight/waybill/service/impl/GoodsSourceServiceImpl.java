package com.lukachangtong.freight.waybill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lukachangtong.freight.waybill.dto.GoodsSourceSaveRequest;
import com.lukachangtong.freight.waybill.entity.WbGoodsSource;
import com.lukachangtong.freight.waybill.mapper.GoodsSourceMapper;
import com.lukachangtong.freight.waybill.service.GoodsSourceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GoodsSourceServiceImpl implements GoodsSourceService {

    @Resource
    private GoodsSourceMapper goodsSourceMapper;

    @Override
    public List<WbGoodsSource> list(Long orgId) {
        QueryWrapper<WbGoodsSource> qw = new QueryWrapper<>();
        if (orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("id");
        return goodsSourceMapper.selectList(qw);
    }

    @Override
    public Long create(GoodsSourceSaveRequest request, String operator) {
        WbGoodsSource gs = new WbGoodsSource();
        gs.setOrgId(request.getOrgId());
        gs.setShipperId(request.getShipperId());
        gs.setGoodsName(request.getGoodsName());
        gs.setWeight(request.getWeight());
        gs.setVolume(request.getVolume());
        gs.setLoadAddr(request.getLoadAddr());
        gs.setLoadLat(request.getLoadLat());
        gs.setLoadLng(request.getLoadLng());
        gs.setUnloadAddr(request.getUnloadAddr());
        gs.setUnloadLat(request.getUnloadLat());
        gs.setUnloadLng(request.getUnloadLng());
        gs.setLoadTime(request.getLoadTime());
        gs.setFreightPrice(request.getFreightPrice());
        gs.setContactName(request.getContactName());
        gs.setContactPhone(request.getContactPhone());
        gs.setRemark(request.getRemark());
        gs.setStatus(0); // 待承运
        gs.setCreateBy(operator);
        gs.setCreateTime(LocalDateTime.now());
        gs.setUpdateTime(LocalDateTime.now());
        goodsSourceMapper.insert(gs);
        return gs.getId();
    }

    @Override
    public void close(Long id, String operator) {
        WbGoodsSource upd = new WbGoodsSource();
        upd.setId(id);
        upd.setStatus(2); // 关闭
        upd.setUpdateBy(operator);
        upd.setUpdateTime(LocalDateTime.now());
        goodsSourceMapper.updateById(upd);
    }
}
