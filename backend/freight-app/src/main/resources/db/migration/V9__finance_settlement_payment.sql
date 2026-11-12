-- M4 财务域：计价结算 / 银企直联支付 / 钱包与资金流水 / 发票 / 四方对账。
--
-- 三条铁律（写在这里，免得后人绕开）：
-- 1. **金额一律 DECIMAL(18,2)**，禁止 DOUBLE；所有除法明确 HALF_UP，税额 = 含税 - 不含税（不是各自算再相减）。
-- 2. **资金写入（结算单状态 / 支付单 / 资金流水 / 钱包余额）必须在同一个本地事务内**，
--    这是选「模块化单体」而不是微服务的最主要收益；外部银行只通过 Outbox + 显式执行动作交付。
-- 3. **每条资金链路都有幂等键**：结算单 (waybill_id,biz_type)、支付单 request_id、
--    资金流水 (biz_type,biz_no)、发票 request_id、对账 (biz_date,scene)。

-- 运单约定运价：结算的计价基准。原来只有货源报价，运单没继承，导致结算只能拍脑袋，
-- 这里补上并在建单/接单时从货源带入（含税金额，元）。
ALTER TABLE wb_waybill
  ADD COLUMN freight_amount DECIMAL(18,2) DEFAULT NULL COMMENT '约定运费（含税，元），建单/接单时从货源继承';

-- ------------------------------------------------------------------ 结算单

CREATE TABLE st_settlement (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  settlement_no   VARCHAR(32)   NOT NULL,
  org_id          BIGINT        NOT NULL,
  biz_type        VARCHAR(16)   NOT NULL DEFAULT 'PAYABLE' COMMENT 'PAYABLE 应付司机 / RECEIVABLE 应收货主',
  waybill_id      BIGINT        DEFAULT NULL,
  waybill_no      VARCHAR(32)   DEFAULT NULL,
  partner_type    VARCHAR(16)   DEFAULT NULL COMMENT 'DRIVER / SHIPPER / CARRIER',
  partner_id      BIGINT        DEFAULT NULL,
  partner_name    VARCHAR(64)   DEFAULT NULL,
  base_amount     DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '基础运费（含税）',
  load_fee        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '装货费',
  unload_fee      DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '卸货费',
  wait_fee        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '等待/压车费',
  oil_subsidy     DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '油补',
  deduct_amount   DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '扣款（货损、迟到等），正数参与扣减',
  tax_rate        DECIMAL(6,4)  NOT NULL DEFAULT 0.0900 COMMENT '运输服务增值税率 9%',
  amount          DECIMAL(18,2) NOT NULL COMMENT '结算总额（含税）',
  net_amount      DECIMAL(18,2) NOT NULL COMMENT '不含税金额',
  tax_amount      DECIMAL(18,2) NOT NULL COMMENT '税额',
  status          VARCHAR(16)   NOT NULL COMMENT 'DRAFT/CONFIRMED/PAYING/PAID/INVOICED/CLOSED/VOID',
  settled_date    DATE          DEFAULT NULL COMMENT '结算业务日期，日终对账按此归集',
  paid_time       DATETIME      DEFAULT NULL,
  remark          VARCHAR(255)  DEFAULT NULL,
  create_by       VARCHAR(64)   DEFAULT NULL,
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by       VARCHAR(64)   DEFAULT NULL,
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT       NOT NULL DEFAULT 0,
  version         INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_st_no (settlement_no),
  -- 幂等：一张运单在同一方向上只能有一张有效结算单，重复点击不会重复结算
  UNIQUE KEY uk_st_waybill (waybill_id, biz_type),
  KEY idx_st_status (org_id, status),
  KEY idx_st_date (settled_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单（应付/应收）';

-- ------------------------------------------------------------------ 支付单（归 payment 模块，表名沿用 st_ 前缀便于财务域统一对账）

CREATE TABLE st_payment_order (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  pay_no          VARCHAR(32)   NOT NULL COMMENT '支付单号，同时作为银行幂等 requestId',
  org_id          BIGINT        NOT NULL,
  pay_type        VARCHAR(16)   NOT NULL DEFAULT 'SETTLE' COMMENT 'SETTLE 结算付款 / WITHDRAW 提现',
  biz_id          BIGINT        DEFAULT NULL COMMENT 'SETTLE→结算单ID；WITHDRAW→提现单ID',
  biz_no          VARCHAR(32)   DEFAULT NULL,
  payee_type      VARCHAR(16)   DEFAULT NULL,
  payee_id        BIGINT        DEFAULT NULL,
  payee_name      VARCHAR(64)   DEFAULT NULL,
  payee_account   VARCHAR(64)   DEFAULT NULL,
  bank_code       VARCHAR(32)   DEFAULT NULL,
  amount          DECIMAL(18,2) NOT NULL,
  status          VARCHAR(16)   NOT NULL COMMENT 'PENDING/PAYING/SUCCESS/FAILED/CLOSED',
  bank_serial_no  VARCHAR(64)   DEFAULT NULL,
  tls_protocol    VARCHAR(16)   DEFAULT NULL,
  fail_reason     VARCHAR(255)  DEFAULT NULL,
  retry_count     INT           NOT NULL DEFAULT 0,
  execute_time    DATETIME      DEFAULT NULL,
  callback_time   DATETIME      DEFAULT NULL,
  create_by       VARCHAR(64)   DEFAULT NULL,
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version         INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_pay_status (org_id, status),
  KEY idx_pay_biz (pay_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银企直联支付单';

-- ------------------------------------------------------------------ 钱包与资金流水

CREATE TABLE st_wallet (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  org_id       BIGINT        NOT NULL,
  owner_type   VARCHAR(16)   NOT NULL COMMENT 'DRIVER / SHIPPER / CARRIER',
  owner_id     BIGINT        NOT NULL,
  owner_name   VARCHAR(64)   DEFAULT NULL,
  balance      DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总余额',
  frozen       DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '冻结金额（提现处理中）',
  total_in     DECIMAL(18,2) NOT NULL DEFAULT 0,
  total_out    DECIMAL(18,2) NOT NULL DEFAULT 0,
  status       VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version      INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallet_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金账户（钱包）';

CREATE TABLE st_fund_flow (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  flow_no        VARCHAR(32)   NOT NULL,
  org_id         BIGINT        NOT NULL,
  wallet_id      BIGINT        NOT NULL,
  owner_type     VARCHAR(16)   NOT NULL,
  owner_id       BIGINT        NOT NULL,
  biz_type       VARCHAR(24)   NOT NULL COMMENT 'SETTLE_IN 结算入账 / WITHDRAW_OUT 提现出金 / ADJUST 人工调账',
  biz_no         VARCHAR(32)   NOT NULL COMMENT '业务单号（结算单号/支付单号）',
  direction      VARCHAR(8)    NOT NULL COMMENT 'IN / OUT',
  amount         DECIMAL(18,2) NOT NULL,
  balance_before DECIMAL(18,2) NOT NULL COMMENT '记账前余额，留证用',
  balance_after  DECIMAL(18,2) NOT NULL COMMENT '记账后余额，钱包余额可由流水重算校验',
  remark         VARCHAR(255)  DEFAULT NULL,
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_flow_no (flow_no),
  -- 幂等：同一业务单同一动作只入账一次，重复回调不会重复加钱
  UNIQUE KEY uk_flow_biz (biz_type, biz_no),
  KEY idx_flow_wallet (wallet_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水（只追加，余额的唯一凭据）';

CREATE TABLE st_withdraw (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  withdraw_no   VARCHAR(32)   NOT NULL,
  org_id        BIGINT        NOT NULL,
  owner_type    VARCHAR(16)   NOT NULL DEFAULT 'DRIVER',
  owner_id      BIGINT        NOT NULL,
  owner_name    VARCHAR(64)   DEFAULT NULL,
  amount        DECIMAL(18,2) NOT NULL COMMENT '申请提现金额',
  fee           DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '手续费',
  actual_amount DECIMAL(18,2) NOT NULL COMMENT '实际到账 = amount - fee',
  bank_account  VARCHAR(64)   DEFAULT NULL,
  bank_name     VARCHAR(64)   DEFAULT NULL,
  status        VARCHAR(16)   NOT NULL COMMENT 'APPLIED/APPROVED/REJECTED/PAID',
  audit_by      VARCHAR(64)   DEFAULT NULL,
  audit_time    DATETIME      DEFAULT NULL,
  audit_remark  VARCHAR(255)  DEFAULT NULL,
  pay_no        VARCHAR(32)   DEFAULT NULL,
  create_by     VARCHAR(64)   DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_withdraw_no (withdraw_no),
  KEY idx_withdraw_owner (owner_type, owner_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请';

-- ------------------------------------------------------------------ 发票

CREATE TABLE st_invoice (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  invoice_no     VARCHAR(32)   DEFAULT NULL COMMENT '发票号码（税控返回）',
  invoice_code   VARCHAR(32)   DEFAULT NULL,
  org_id         BIGINT        NOT NULL,
  biz_type       VARCHAR(16)   NOT NULL DEFAULT 'PAYABLE' COMMENT 'PAYABLE 司机运费票 / RECEIVABLE 货主票',
  settlement_id  BIGINT        NOT NULL,
  settlement_no  VARCHAR(32)   NOT NULL,
  waybill_no     VARCHAR(32)   DEFAULT NULL,
  title          VARCHAR(128)  NOT NULL COMMENT '发票抬头',
  tax_no         VARCHAR(32)   DEFAULT NULL COMMENT '纳税人识别号',
  amount         DECIMAL(18,2) NOT NULL,
  net_amount     DECIMAL(18,2) NOT NULL,
  tax_amount     DECIMAL(18,2) NOT NULL,
  tax_rate       DECIMAL(6,4)  NOT NULL DEFAULT 0.0900,
  status         VARCHAR(16)   NOT NULL COMMENT 'PENDING/ISSUED/VOID/FAILED',
  pdf_url        VARCHAR(255)  DEFAULT NULL,
  request_id     VARCHAR(64)   NOT NULL COMMENT '开票幂等键',
  issued_time    DATETIME      DEFAULT NULL,
  void_time      DATETIME      DEFAULT NULL,
  void_reason    VARCHAR(255)  DEFAULT NULL,
  create_by      VARCHAR(64)   DEFAULT NULL,
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invoice_req (request_id),
  KEY idx_invoice_settlement (settlement_id),
  KEY idx_invoice_status (org_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子发票';

-- ------------------------------------------------------------------ 对账

CREATE TABLE st_reconciliation (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  biz_date         DATE          NOT NULL,
  scene            VARCHAR(16)   NOT NULL DEFAULT 'DAILY',
  settlement_count INT           NOT NULL DEFAULT 0,
  settlement_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  payment_count    INT           NOT NULL DEFAULT 0,
  payment_amount   DECIMAL(18,2) NOT NULL DEFAULT 0,
  flow_count       INT           NOT NULL DEFAULT 0,
  flow_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
  wallet_delta     DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '当日钱包净变动（IN-OUT）',
  bank_count       INT           NOT NULL DEFAULT 0,
  bank_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
  diff_count       INT           NOT NULL DEFAULT 0,
  diff_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
  status           VARCHAR(16)   NOT NULL COMMENT 'MATCH 平 / DIFF 有差异',
  run_by           VARCHAR(64)   DEFAULT NULL,
  run_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark           VARCHAR(255)  DEFAULT NULL,
  PRIMARY KEY (id),
  -- 同日同一场景只保留一份结果，重跑覆盖（对账是幂等的快照，不是流水）
  UNIQUE KEY uk_recon (biz_date, scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='四方对账结果';

CREATE TABLE st_recon_diff (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  recon_id      BIGINT        NOT NULL,
  diff_type     VARCHAR(32)   NOT NULL COMMENT 'SETTLE_VS_PAY 结算对支付 / PAY_VS_FLOW 支付对流水 / WALLET_BALANCE 余额校验 / BANK_FILE 银行文件差异',
  biz_no        VARCHAR(32)   DEFAULT NULL,
  expect_amount DECIMAL(18,2) DEFAULT NULL,
  actual_amount DECIMAL(18,2) DEFAULT NULL,
  diff_amount   DECIMAL(18,2) DEFAULT NULL,
  remark        VARCHAR(255)  DEFAULT NULL,
  handled       TINYINT       NOT NULL DEFAULT 0,
  handle_by     VARCHAR(64)   DEFAULT NULL,
  handle_remark VARCHAR(255)  DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_diff_recon (recon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账差异明细';

CREATE TABLE st_summary (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  biz_date      DATE          NOT NULL,
  org_id        BIGINT        NOT NULL,
  biz_type      VARCHAR(16)   NOT NULL,
  settle_count  INT           NOT NULL DEFAULT 0,
  settle_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  paid_count    INT           NOT NULL DEFAULT 0,
  paid_amount   DECIMAL(18,2) NOT NULL DEFAULT 0,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_summary (biz_date, org_id, biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算汇总（日终任务写入，看板用）';
