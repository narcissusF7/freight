-- M5 客服与统计：投诉（24h 时效）/ 双向星级 / 信用评分 / 统计预聚合。
--
-- 三条设计约束：
-- 1. **投诉的 24 小时是监管硬指标**，不是运营口号：建单即起算，deadline_time 落库，
--    逾期由扫描任务打标并升级，考核看的是「逾期率」而不是「处理了多少」。
-- 2. **评价与被评价方唯一**：一张运单每个方向只能有一条评价（uk_rating_waybill_direction），
--    否则刷好评就是改一次评分再提交一次。
-- 3. **信用分是流水算出来的，不是随便改的字段**：每次变动落 cs_credit（含前后分值与等级），
--    当前分存 cs_credit_subject 便于查询；由 `(subject, biz_type, biz_no)` 唯一保证同一事件不重复扣分。

-- ------------------------------------------------------------------ 运单补时间戳

-- 统计要按「建单日 / 签收日」归集，但原来只有 create_time 与状态字符串：
-- 状态字段只存当前值，历史某天签收了多少单，事后根本还原不出来（节点表能查但聚合很贵）。
-- 这两个字段在状态跃迁时写入，属于「业务时间的事实表」，比事后推算可靠。
ALTER TABLE wb_waybill
  ADD COLUMN accept_time DATETIME DEFAULT NULL COMMENT '接单时间（首次进入 ACCEPTED）',
  ADD COLUMN sign_time   DATETIME DEFAULT NULL COMMENT '签收时间（首次进入 SIGNED）';

-- 回填历史数据：已签收的运单从节点表取最早一条 SIGNED 的时间，否则历史统计全是 0
UPDATE wb_waybill w
  JOIN (SELECT waybill_id, MIN(operate_time) AS t FROM wb_node WHERE to_status = 'SIGNED' GROUP BY waybill_id) n
    ON n.waybill_id = w.id
   SET w.sign_time = n.t
 WHERE w.sign_time IS NULL;

UPDATE wb_waybill w
  JOIN (SELECT waybill_id, MIN(operate_time) AS t FROM wb_node WHERE to_status = 'ACCEPTED' GROUP BY waybill_id) n
    ON n.waybill_id = w.id
   SET w.accept_time = n.t
 WHERE w.accept_time IS NULL;

CREATE INDEX idx_wb_create_time ON wb_waybill (create_time);
CREATE INDEX idx_wb_sign_time ON wb_waybill (sign_time);

-- ------------------------------------------------------------------ 投诉

CREATE TABLE cs_complaint (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  complaint_no      VARCHAR(32)  NOT NULL COMMENT '投诉单号 CP...',
  org_id            BIGINT       NOT NULL,
  source            VARCHAR(16)  NOT NULL DEFAULT 'WEB' COMMENT 'DRIVER_APP / SHIPPER_APP / WEB / PHONE',
  complainant_type  VARCHAR(16)  NOT NULL COMMENT 'DRIVER / SHIPPER / OTHER',
  complainant_id    BIGINT       DEFAULT NULL,
  complainant_name  VARCHAR(64)  DEFAULT NULL,
  complainant_phone VARCHAR(32)  DEFAULT NULL,
  target_type       VARCHAR(16)  NOT NULL DEFAULT 'DRIVER' COMMENT 'DRIVER / SHIPPER / PLATFORM',
  target_id         BIGINT       DEFAULT NULL COMMENT '被投诉对象 ID',
  target_name       VARCHAR(64)  DEFAULT NULL,
  waybill_id        BIGINT       DEFAULT NULL,
  waybill_no        VARCHAR(32)  DEFAULT NULL,
  complaint_type    VARCHAR(16)  NOT NULL COMMENT 'DELAY 迟到 / DAMAGE 货损 / ATTITUDE 态度 / FEE 费用 / LOST 丢货 / OTHER',
  level             VARCHAR(8)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / URGENT',
  content           VARCHAR(1000) NOT NULL,
  image_urls        VARCHAR(1000) DEFAULT NULL COMMENT '凭证图片，逗号分隔',
  status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED/REJECTED/CLOSED',
  accept_time       DATETIME     DEFAULT NULL COMMENT '受理时间（SLA 起算点）',
  deadline_time     DATETIME     NOT NULL COMMENT '处理截止 = 受理 + 24h（监管要求）',
  assign_to         VARCHAR(64)  DEFAULT NULL COMMENT '处理人',
  assign_time       DATETIME     DEFAULT NULL,
  resolved_time     DATETIME     DEFAULT NULL,
  resolve_result    VARCHAR(1000) DEFAULT NULL COMMENT '处理结论（也是给投诉人的反馈内容）',
  liable            TINYINT      NOT NULL DEFAULT 0 COMMENT '责任是否成立；只有成立才扣信用分，不成立只驳回',
  satisfaction      TINYINT      DEFAULT NULL COMMENT '投诉人满意度 1~5，办结时收集',
  close_time        DATETIME     DEFAULT NULL,
  close_remark      VARCHAR(255) DEFAULT NULL,
  overdue_flag      TINYINT      NOT NULL DEFAULT 0 COMMENT '1 = 已逾期（由扫描任务打标）',
  escalate_level    TINYINT      NOT NULL DEFAULT 0 COMMENT '升级次数，逾期一次 +1',
  create_by         VARCHAR(64)  DEFAULT NULL,
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by         VARCHAR(64)  DEFAULT NULL,
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted           TINYINT      NOT NULL DEFAULT 0,
  version           INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_complaint_no (complaint_no),
  KEY idx_cs_status (org_id, status),
  KEY idx_cs_deadline (status, deadline_time),
  KEY idx_cs_waybill (waybill_id),
  KEY idx_cs_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉单（24h 时效）';

CREATE TABLE cs_handle (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  complaint_id BIGINT      NOT NULL,
  handle_type VARCHAR(16)  NOT NULL COMMENT 'ACCEPT 受理 / ASSIGN 分派 / FOLLOW 跟进 / RESOLVE 处理 / REJECT 驳回 / CLOSE 办结 / ESCALATE 升级',
  content     VARCHAR(1000) NOT NULL COMMENT '处理说明；对投诉人可见的部分在 resolve 时进入 feedback',
  image_urls  VARCHAR(1000) DEFAULT NULL,
  handle_by   VARCHAR(64)  DEFAULT NULL,
  handle_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_handle_complaint (complaint_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉处理记录（只追加，投诉全流程留痕）';

-- ------------------------------------------------------------------ 评价

CREATE TABLE cs_rating (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  rating_no     VARCHAR(32)  NOT NULL,
  org_id        BIGINT       NOT NULL,
  waybill_id    BIGINT       NOT NULL,
  waybill_no    VARCHAR(32)  DEFAULT NULL,
  direction     VARCHAR(24)  NOT NULL COMMENT 'SHIPPER_TO_DRIVER 货主评司机 / DRIVER_TO_SHIPPER 司机评货主',
  rater_type    VARCHAR(16)  NOT NULL COMMENT 'DRIVER / SHIPPER',
  rater_id      BIGINT       NOT NULL,
  rater_name    VARCHAR(64)  DEFAULT NULL,
  target_type   VARCHAR(16)  NOT NULL,
  target_id     BIGINT       NOT NULL,
  target_name   VARCHAR(64)  DEFAULT NULL,
  score         TINYINT      NOT NULL COMMENT '总体星级 1~5',
  punctuality   TINYINT      DEFAULT NULL COMMENT '准时',
  service       TINYINT      DEFAULT NULL COMMENT '服务',
  cargo         TINYINT      DEFAULT NULL COMMENT '货物完好',
  attitude      TINYINT      DEFAULT NULL COMMENT '沟通态度',
  tags          VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
  content       VARCHAR(500) DEFAULT NULL,
  anonymous     TINYINT      NOT NULL DEFAULT 0,
  status        VARCHAR(16)  NOT NULL DEFAULT 'VALID' COMMENT 'VALID / HIDDEN（违规内容隐藏，不物理删除）',
  reply_content VARCHAR(500) DEFAULT NULL COMMENT '被评价方回复',
  reply_time    DATETIME     DEFAULT NULL,
  reply_by      VARCHAR(64)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rating_no (rating_no),
  -- 一张运单每个方向只能评一次：刷分的人最常用的手段就是反复提交
  UNIQUE KEY uk_rating_waybill_direction (waybill_id, direction),
  KEY idx_rating_target (target_type, target_id, id),
  KEY idx_rating_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='双向星级评价';

-- ------------------------------------------------------------------ 信用

CREATE TABLE cs_credit_subject (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  subject_type   VARCHAR(16)  NOT NULL COMMENT 'DRIVER / SHIPPER',
  subject_id     BIGINT       NOT NULL,
  subject_name   VARCHAR(64)  DEFAULT NULL,
  score          INT          NOT NULL DEFAULT 100 COMMENT '信用分，满分 100',
  level          VARCHAR(4)   NOT NULL DEFAULT 'A' COMMENT 'A≥90 / B≥75 / C≥60 / D<60',
  good_count     INT          NOT NULL DEFAULT 0 COMMENT '好评数（≥4 星）',
  neutral_count  INT          NOT NULL DEFAULT 0 COMMENT '中评（3 星）',
  bad_count      INT          NOT NULL DEFAULT 0 COMMENT '差评（≤2 星）',
  complaint_count INT         NOT NULL DEFAULT 0 COMMENT '被投诉且责任成立次数',
  total_count    INT          NOT NULL DEFAULT 0 COMMENT '累计评价数',
  last_change_time DATETIME   DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version        INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_subject (subject_type, subject_id),
  KEY idx_credit_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用主体当前分值（由流水重算可校验）';

CREATE TABLE cs_credit (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  subject_type VARCHAR(16)  NOT NULL,
  subject_id   BIGINT       NOT NULL,
  subject_name VARCHAR(64)  DEFAULT NULL,
  biz_type     VARCHAR(24)  NOT NULL COMMENT 'COMPLAINT_CONFIRMED / RATING_GOOD / RATING_NEUTRAL / RATING_BAD / ADJUST',
  biz_no       VARCHAR(64)  NOT NULL COMMENT '业务单号（投诉单号/评价单号/调整单号）',
  delta        INT          NOT NULL COMMENT '本次加减分，负数扣分',
  score_before INT          NOT NULL,
  score_after  INT          NOT NULL,
  level_before VARCHAR(4)   DEFAULT NULL,
  level_after  VARCHAR(4)   DEFAULT NULL,
  reason       VARCHAR(255) DEFAULT NULL,
  create_by    VARCHAR(64)  DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  -- 幂等：同一事件对同一主体只计一次分，重复办结/重复提交都不会把分扣两遍
  UNIQUE KEY uk_credit_biz (subject_type, subject_id, biz_type, biz_no),
  KEY idx_credit_subject (subject_type, subject_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用变动流水';

-- ------------------------------------------------------------------ 统计预聚合

-- 日汇总宽表：一次算好，查询不再打明细。指标口径与「统计中心」页面一一对应。
CREATE TABLE stat_daily (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  biz_date           DATE          NOT NULL,
  org_id             BIGINT        NOT NULL DEFAULT 0 COMMENT '0 = 全平台汇总；查询按 org 维度取',
  waybill_created    INT           NOT NULL DEFAULT 0,
  waybill_amount     DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '新建运单约定运费合计',
  waybill_signed     INT           NOT NULL DEFAULT 0,
  signed_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
  waybill_exception  INT           NOT NULL DEFAULT 0,
  settle_count       INT           NOT NULL DEFAULT 0,
  settle_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
  pay_count          INT           NOT NULL DEFAULT 0,
  pay_amount         DECIMAL(18,2) NOT NULL DEFAULT 0,
  complaint_count    INT           NOT NULL DEFAULT 0,
  complaint_overdue  INT           NOT NULL DEFAULT 0,
  rating_count       INT           NOT NULL DEFAULT 0,
  rating_sum         INT           NOT NULL DEFAULT 0 COMMENT '星级合计，均分 = rating_sum / rating_count',
  alarm_count        INT           NOT NULL DEFAULT 0,
  calc_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stat_daily (biz_date, org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计日汇总（可重算的快照）';

CREATE TABLE stat_dim (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  biz_date       DATE          NOT NULL,
  org_id         BIGINT        NOT NULL DEFAULT 0,
  dim_type       VARCHAR(24)   NOT NULL COMMENT 'LOAD_PROVINCE / UNLOAD_PROVINCE / CARGO / DRIVER',
  dim_key        VARCHAR(64)   NOT NULL COMMENT '维度值（省份名/货物名/司机ID）',
  dim_label      VARCHAR(128)  DEFAULT NULL COMMENT '展示名（司机姓名等）',
  waybill_count  INT           NOT NULL DEFAULT 0,
  amount         DECIMAL(18,2) NOT NULL DEFAULT 0,
  complaint_count INT          NOT NULL DEFAULT 0,
  alarm_count    INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stat_dim (biz_date, org_id, dim_type, dim_key),
  KEY idx_stat_dim_query (dim_type, biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计维度聚合（地区/货物/司机）';

-- ------------------------------------------------------------------ 客服消息模板

-- 投诉受理、逾期、办结、差评四条：统一进调度台站内信。
-- 没有给投诉人发短信：SHIPPER 目前没有地址解析实现（ReceiverAddressPort 只解析司机），
-- 硬配短信模板只会刷出一堆 GIVEN_UP，等接入货主手机号后再补。
INSERT INTO msg_template (org_id, template_code, template_name, biz_type, channel, receiver_type,
                          title_tpl, content_tpl, enabled, remark) VALUES
(0, 'COMPLAINT_NEW_INBOX',     '投诉受理-站内信', 'COMPLAINT_NEW',     'INBOX', 'ORG',
 '新投诉 ${complaintNo}', '${complainantName} 投诉 ${targetName}：${complaintType}（${level}），要求 24 小时内处理，截止 ${deadline}。',
 1, '投诉建单即通知调度台'),
(0, 'COMPLAINT_OVERDUE_INBOX', '投诉逾期-站内信', 'COMPLAINT_OVERDUE', 'INBOX', 'ORG',
 '投诉已逾期 ${complaintNo}', '投诉 ${complaintNo} 已超过 24 小时处理时限（截止 ${deadline}），请立即处理并反馈。',
 1, '逾期扫描任务触发'),
(0, 'COMPLAINT_RESOLVED_INBOX','投诉办结-站内信', 'COMPLAINT_RESOLVED','INBOX', 'ORG',
 '投诉已办结 ${complaintNo}', '投诉 ${complaintNo} 处理完成：${result}。满意度 ${satisfaction} 星。',
 1, '办结反馈闭环留痕'),
(0, 'RATING_BAD_INBOX',        '差评预警-站内信', 'RATING_BAD',        'INBOX', 'ORG',
 '收到差评 ${ratingNo}', '${targetName} 收到 ${score} 星评价：${content}（运单 ${waybillNo}）。',
 1, '差评联动信用扣分并通知调度台');
