-- M3 司机端 APP：账号能力 + 节点确认留痕。
--
-- 设计要点：
-- 1. 司机登录不新建用户体系，直接在 cr_driver 上加登录凭据——司机准入表本身就是承运方的
--    责任人档案，再造一张 sys_user 会出现「两处审核状态不一致」的老问题；
-- 2. wb_node 补定位与凭证列：APP 节点确认必须留「谁、在哪、几点、什么照片」，
--    这是网络货运监管核查真实性（是否真的到厂装卸）的核心证据链；
-- 3. online_status 只表示「司机是否愿意接单」，与轨迹上报的在线与否无关，别混用。

ALTER TABLE cr_driver
  ADD COLUMN password_hash    VARCHAR(100) DEFAULT NULL COMMENT '司机端登录密码 BCrypt，空表示未激活',
  ADD COLUMN push_client_id   VARCHAR(128) DEFAULT NULL COMMENT 'APP 推送 clientId（uni-push / 厂商通道）',
  ADD COLUMN online_status    TINYINT      NOT NULL DEFAULT 0 COMMENT '接单在线状态 0 离线 1 在线',
  ADD COLUMN last_login_time  DATETIME     DEFAULT NULL,
  ADD COLUMN login_fail_count INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  ADD COLUMN lock_until       DATETIME     DEFAULT NULL COMMENT '锁定截止时间（连错 5 次锁 10 分钟）';

-- 登录按手机号查（不建唯一索引：历史数据里存在同号多条，先由业务层校验并给出明确提示）
CREATE INDEX idx_cr_driver_phone ON cr_driver (phone);

ALTER TABLE wb_node
  ADD COLUMN lat           DECIMAL(10,6) DEFAULT NULL COMMENT '确认位置纬度（GCJ-02）',
  ADD COLUMN lng           DECIMAL(10,6) DEFAULT NULL COMMENT '确认位置经度（GCJ-02）',
  ADD COLUMN address       VARCHAR(255)  DEFAULT NULL COMMENT '确认位置逆地址',
  ADD COLUMN distance_m    INT           DEFAULT NULL COMMENT '距参考点（装/卸货地）距离，米',
  ADD COLUMN evidence_urls VARCHAR(1000) DEFAULT NULL COMMENT '凭证图片，逗号分隔',
  ADD COLUMN source        VARCHAR(24)   NOT NULL DEFAULT 'WEB' COMMENT 'WEB / DRIVER_APP / SYSTEM',
  ADD COLUMN client_ip     VARCHAR(64)   DEFAULT NULL COMMENT '提交端 IP，用于事后核查';

-- 司机端按司机查运单是高频路径（我的运单列表）
CREATE INDEX idx_wb_waybill_driver ON wb_waybill (driver_id, status);

-- 推送登记失败重试时按 clientId 反查司机
CREATE INDEX idx_cr_driver_push ON cr_driver (push_client_id);

-- 司机端消息模板：接单成功回执进 APP 消息中心（INBOX 通道，收件人类型 DRIVER）。
-- 不复用 V7 的 WAYBILL_STATUS_INBOX：那条的收件人是调度台（ORG），
-- 司机端只应看到发给自己的信，混在机构站内信里会被全员看到。
INSERT INTO msg_template (org_id, template_code, template_name, biz_type, channel, receiver_type,
                          title_tpl, content_tpl, enabled, remark) VALUES
(0, 'WAYBILL_ACCEPTED_INBOX', '接单成功-司机站内信', 'WAYBILL_ACCEPTED', 'INBOX', 'DRIVER',
 '接单成功', '运单 ${waybillNo} 已接单：${loadAddr} → ${unloadAddr}，计划装货 ${loadTime}，请按时到场。',
 1, '司机接单后回执，APP 消息中心可见');
