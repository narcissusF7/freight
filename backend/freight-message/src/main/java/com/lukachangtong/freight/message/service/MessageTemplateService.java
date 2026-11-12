package com.lukachangtong.freight.message.service;

import com.lukachangtong.freight.message.dto.TemplateSaveRequest;
import com.lukachangtong.freight.message.entity.MsgTemplate;

import java.util.List;

/** 消息模板维护（节点通道配置化）。 */
public interface MessageTemplateService {

    List<MsgTemplate> list(Long orgId, String bizType, String channel);

    MsgTemplate get(Long id);

    Long create(TemplateSaveRequest request);

    void update(Long id, TemplateSaveRequest request);

    void delete(Long id);
}
