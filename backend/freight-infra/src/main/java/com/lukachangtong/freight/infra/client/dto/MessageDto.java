package com.lukachangtong.freight.infra.client.dto;

/**
 * 消息通道 DTO（短信 / 微信模板消息 / APP 推送）。
 *
 * 三类通道的返回结构一致（成功标志 + 服务商消息号 + 描述），因此共用一个 MessageResult：
 * 业务侧只关心「发没发出去、服务商给了什么号」，差异留在各 Client 实现里。
 */
public final class MessageDto {

    private MessageDto() {
    }

    public static class SmsRequest {
        private String phone;
        private String content;
        /** 幂等键，网关侧按此去重（重试不会重复下发） */
        private String requestId;

        public String getPhone() { return phone; }
        public void setPhone(String v) { this.phone = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
    }

    public static class WechatRequest {
        private String openId;
        /** 模板编号（公众号/小程序侧申请的模板 ID） */
        private String templateCode;
        private String content;
        private String page;
        private String requestId;

        public String getOpenId() { return openId; }
        public void setOpenId(String v) { this.openId = v; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String v) { this.templateCode = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getPage() { return page; }
        public void setPage(String v) { this.page = v; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
    }

    public static class PushRequest {
        /** 设备 token 或用户标识（厂商通道二选一） */
        private String target;
        private String title;
        private String content;
        private String requestId;

        public String getTarget() { return target; }
        public void setTarget(String v) { this.target = v; }
        public String getTitle() { return title; }
        public void setTitle(String v) { this.title = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String v) { this.requestId = v; }
    }

    public static class MessageResult {
        private boolean success;
        private String providerMsgId;
        private String message;

        public static MessageResult ok(String providerMsgId) {
            MessageResult r = new MessageResult();
            r.success = true;
            r.providerMsgId = providerMsgId;
            r.message = "mock sent";
            return r;
        }

        public static MessageResult fail(String message) {
            MessageResult r = new MessageResult();
            r.success = false;
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getProviderMsgId() { return providerMsgId; }
        public void setProviderMsgId(String v) { this.providerMsgId = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }
}
