package com.lukachangtong.freight.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.infra.outbox.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表 Mapper。各业务模块在事务内写入 OutboxEvent，由 XXL-Job 异步投递。
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {
}
