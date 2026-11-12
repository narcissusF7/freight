-- M3 消息域：模板 + 发送记录（四通道：短信 / 微信 / APP 推送 / 站内信）。
-- 设计要点：
-- 1. 模板按「业务节点 + 通道」一行，运维改配置即可增删通道，业务代码零改动；
-- 2. 记录表 dedup_key 唯一索引，保证同一业务事件对同一收件人同通道只发一次（重试只更新状态）；
-- 3. 发送走「先落库 PENDING，再由调度器投递」，避免在业务事务里同步调外部网关。

CREATE TABLE msg_template (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  org_id        BIGINT       NOT NULL DEFAULT 0 COMMENT '0 = 平台默认模板，机构可覆盖同 code 模板',
  template_code VARCHAR(64)  NOT NULL,
  template_name VARCHAR(64)  NOT NULL,
  biz_type      VARCHAR(32)  NOT NULL COMMENT 'TRACK_ALARM / WAYBILL_STATUS / CERT_EXPIRY / CONTRACT_SIGNED …',
  channel       VARCHAR(16)  NOT NULL COMMENT 'SMS / WECHAT / PUSH / INBOX',
  receiver_type VARCHAR(16)  NOT NULL DEFAULT 'DRIVER' COMMENT 'DRIVER / SHIPPER / EMPLOYEE / ORG',
  title_tpl     VARCHAR(128) DEFAULT NULL,
  content_tpl   VARCHAR(512) NOT NULL COMMENT '支持 ${var} 占位符',
  enabled       TINYINT      NOT NULL DEFAULT 1,
  remark        VARCHAR(255) DEFAULT NULL,
  create_by     VARCHAR(64)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     VARCHAR(64)  DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  version       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_msg_tpl (org_id, template_code),
  KEY idx_msg_tpl_biz (biz_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板（业务节点 + 通道）';

CREATE TABLE msg_record (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  org_id          BIGINT       DEFAULT NULL,
  biz_type        VARCHAR(32)  NOT NULL,
  biz_key         VARCHAR(64)  NOT NULL,
  template_code   VARCHAR(64)  NOT NULL,
  channel         VARCHAR(16)  NOT NULL,
  receiver_type   VARCHAR(16)  NOT NULL,
  receiver_id     BIGINT       DEFAULT NULL,
  receiver        VARCHAR(128) DEFAULT NULL COMMENT '手机号 / openId / 设备号；站内信可空',
  title           VARCHAR(128) DEFAULT NULL,
  content         VARCHAR(512) NOT NULL,
  status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SENT / FAILED / GIVEN_UP',
  retry_count     INT          NOT NULL DEFAULT 0,
  next_retry_time DATETIME     DEFAULT NULL,
  sent_time       DATETIME     DEFAULT NULL,
  fail_reason     VARCHAR(255) DEFAULT NULL,
  provider_msg_id VARCHAR(64)  DEFAULT NULL,
  read_flag       TINYINT      NOT NULL DEFAULT 0 COMMENT '站内信已读标志，其他通道恒为 0',
  dedup_key       VARCHAR(160) NOT NULL COMMENT 'bizType:bizKey:channel:receiverType:receiverId',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_msg_dedup (dedup_key),
  KEY idx_msg_status (status, next_retry_time),
  KEY idx_msg_biz (biz_type, biz_key),
  KEY idx_msg_inbox (channel, org_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录（四通道）';

-- 平台默认模板：机构未覆盖时生效。收件人类型决定「哪个通道发给谁」，
-- 改这里就能调整节点通知策略（例如把预警短信改成只发站内信），不需要发版。
INSERT INTO msg_template (org_id, template_code, template_name, biz_type, channel, receiver_type, title_tpl, content_tpl, enabled, remark) VALUES
(0, 'TRACK_ALARM_SMS',    '在途预警-短信',   'TRACK_ALARM', 'SMS',    'DRIVER', NULL, '【鹭卡畅通】${content}（运单 ${waybillId}）', 1, '预警默认短信通知司机'),
(0, 'TRACK_ALARM_WECHAT', '在途预警-微信',   'TRACK_ALARM', 'WECHAT', 'DRIVER', '运单预警：${alarmType}', '${content}', 1, '需司机关注公众号并授权'),
(0, 'TRACK_ALARM_PUSH',   '在途预警-APP推送', 'TRACK_ALARM', 'PUSH',   'DRIVER', '运单预警：${alarmType}', '${content}（运单 ${waybillId}）', 1, '司机端 APP 推送'),
(0, 'TRACK_ALARM_INBOX',  '在途预警-站内信', 'TRACK_ALARM', 'INBOX',  'ORG',    '运单预警：${alarmType}', '${content}（运单 ${waybillId}，${alarmTime}）', 1, '调度台站内信'),
(0, 'WAYBILL_STATUS_INBOX','运单状态-站内信','WAYBILL_STATUS','INBOX','ORG',    '运单 ${waybillNo} 状态变更', '运单 ${waybillNo} 已变更为 ${status}', 1, '状态流转通知调度台');
