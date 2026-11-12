package com.lukachangtong.freight.customer.support;

import com.lukachangtong.freight.api.message.MessageNotifyPort;
import com.lukachangtong.freight.api.message.NotifyReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * 客服域通知（投诉受理/逾期/办结、差评预警）。
 *
 * 统一发调度台站内信，而不是直接给投诉人发短信：
 * 1. 货主侧还没有地址解析实现（`ReceiverAddressPort` 只解析司机），硬配短信只会刷出 GIVEN_UP；
 * 2. 投诉的「反馈」在业务上要求留痕，站内信可被查询与追责，短信发出去就查不到了。
 * 等货主手机号接入后再补 SMS 模板，业务代码不用改。
 */
@Component
public class CustomerNotifier {

    private static final Logger log = LoggerFactory.getLogger(CustomerNotifier.class);

    /** 消息端口可能不存在（只跑客服模块单测时），用 Provider 避免启动即失败 */
    @Resource
    private ObjectProvider<MessageNotifyPort> notifyProvider;

    public void notifyOrg(Long orgId, String bizType, String bizKey, Map<String, Object> vars) {
        if (orgId == null) {
            return;
        }
        MessageNotifyPort port = notifyProvider.getIfAvailable();
        if (port == null) {
            return;
        }
        try {
            port.notify(orgId, bizType, bizKey, vars,
                    Collections.singletonList(new NotifyReceiver(NotifyReceiver.TYPE_ORG, orgId)));
        } catch (Exception e) {
            // 通知是旁路，不能影响投诉主流程；失败只记日志
            log.warn("客服通知失败（不影响主流程）bizType={} bizKey={} err={}", bizType, bizKey, e.getMessage());
        }
    }
}
