-- M6 监管上报：部/省监测平台 4 类单据（运单/资金流水/车辆/驾驶员）上报记录与补偿。
--
-- 设计约束（对齐大纲 1.2「硬性合规约束」）：
-- 1. **上报完整性**：业务完成后必须上报，落「上报状态 + 回执 + 重试次数」，
--    不靠界面按钮驱动，靠扫描任务兜底——漏写一次 outbox 事件也不能漏报。
-- 2. **上报状态是事实表**：rp_report_record 一条记录对应一次「单据 → 报文 → 回执」，
--    失败重试是新增记录（或更新同一记录的重试字段），可审计「某单到底上报了几次、为什么失败」。
-- 3. **报文号唯一**：message_reference_number 用 GUID 生成，落唯一索引，重复上报幂等挡回。
-- 4. **令牌与配置不入业务代码**：rp_config 存机构级上报参数（交换代码、接口地址、令牌及其过期时间），
--    令牌由「身份验证接口」换取，缓存 2 小时（规范口径），过期自动重取。

-- ------------------------------------------------------------------ 上报配置（机构级）

CREATE TABLE rp_config (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  org_id             BIGINT       NOT NULL COMMENT '机构',
  sender_code        VARCHAR(20)  NOT NULL COMMENT '发送方代码（省监测系统交换代码）',
  receiver_code      VARCHAR(20)  NOT NULL COMMENT '接收方代码',
  endpoint           VARCHAR(255) NOT NULL COMMENT '上报接口地址',
  auth_endpoint      VARCHAR(255) NULL COMMENT '身份验证接口地址（换取令牌）',
  public_key         TEXT         NULL COMMENT '国密 SM2 公钥（验签/加密用）',
  token              VARCHAR(200) NULL COMMENT '当前有效令牌',
  token_expire_time  DATETIME     NULL COMMENT '令牌过期时间（规范 2 小时）',
  enabled            TINYINT      NOT NULL DEFAULT 1,
  create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rp_config_org (org_id),
  KEY idx_rp_config_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上报配置：交换代码/接口地址/令牌（机构级）';

-- ------------------------------------------------------------------ 上报记录（事实表）

CREATE TABLE rp_report_record (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  org_id             BIGINT       NOT NULL,
  message_ref_no     VARCHAR(36)  NOT NULL COMMENT '报文参考号（GUID，唯一，幂等键）',
  doc_type           VARCHAR(32)  NOT NULL COMMENT '单据类型：WAYBILL/FUND_FLOW/VEHICLE/DRIVER',
  biz_no             VARCHAR(64)  NOT NULL COMMENT '业务单号（运单号/流水号/车牌/身份证）',
  biz_id             BIGINT       NULL COMMENT '业务主键 id',
  action_type        VARCHAR(64)  NULL COMMENT '业务接口代码：WLHY_YD1001 / LOGINK_CN_FREIGHTCHARGES / ...',
  payload            TEXT         NOT NULL COMMENT '报文原文（加密前），审计留痕用',
  status             VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
  receipt            VARCHAR(255) NULL COMMENT '回执（成功/失败原因）',
  retry_count        INT          NOT NULL DEFAULT 0,
  next_retry_time    DATETIME     NULL COMMENT '下次重试时间（退避）',
  send_time          DATETIME     NULL COMMENT '上报时间',
  create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rp_ref_no (message_ref_no),
  KEY idx_rp_biz (biz_no, doc_type),
  KEY idx_rp_status_retry (status, next_retry_time),
  KEY idx_rp_org_time (org_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上报记录：单据 → 报文 → 回执 全链路留痕';

-- ------------------------------------------------------------------ 补偿队列（失败重试）

CREATE TABLE rp_retry (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  record_id          BIGINT       NOT NULL COMMENT '关联上报记录',
  retry_count        INT          NOT NULL DEFAULT 0,
  max_retry          INT          NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  fail_reason        VARCHAR(512) NULL COMMENT '失败原因',
  next_retry_time    DATETIME     NULL COMMENT '下次重试时间',
  status             VARCHAR(16)  NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/DONE/ABANDONED',
  create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rp_retry_record (record_id),
  KEY idx_rp_retry_status_time (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上报补偿队列：失败退避重试';

-- ------------------------------------------------------------------ 默认上报配置（演示）

-- 本地演示用 mock 上报：交换代码与接口地址为占位，真实联调由机构在 rp_config 维护。
-- 令牌由身份验证接口换取，这里不预置（首次上报时自动换取）。
INSERT INTO rp_config (org_id, sender_code, receiver_code, endpoint, auth_endpoint, enabled)
VALUES (1, 'FREIGHT-DEMO-ORG', 'ZJWLHY0001',
        'https://wlhy.mot.gov.cn/upload',
        'https://wlhy.mot.gov.cn/auth', 1);
