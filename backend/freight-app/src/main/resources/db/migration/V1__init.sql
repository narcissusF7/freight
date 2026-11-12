-- M1 骨架建表脚本（MySQL 8 / utf8mb4）
-- 金额一律 DECIMAL(18,2)；所有业务表带 org_id、审计字段、乐观锁 version、deleted。
-- 表名按模块前缀分组，便于未来整体迁出拆库。

CREATE TABLE outbox_event (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  event_key       VARCHAR(64)  NOT NULL COMMENT '投递幂等键',
  event_type      VARCHAR(64)  NOT NULL COMMENT '事件类型：REPORT/MESSAGE/...',
  payload         TEXT         NOT NULL,
  status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  retry_count     INT          NOT NULL DEFAULT 0,
  next_retry_time DATETIME     NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_event_key (event_key),
  KEY idx_outbox_status_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表：与业务数据同事务写入';

CREATE TABLE sys_org (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT      NOT NULL DEFAULT 0,
  org_name    VARCHAR(128) NOT NULL,
  org_code    VARCHAR(64) NOT NULL,
  status      TINYINT     NOT NULL DEFAULT 1,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_org_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构';

CREATE TABLE sys_user (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  org_id         BIGINT      NOT NULL,
  username       VARCHAR(64) NOT NULL,
  password_hash  VARCHAR(128) NOT NULL,
  real_name      VARCHAR(64) DEFAULT NULL,
  phone_encrypted VARCHAR(256) DEFAULT NULL COMMENT '手机号加密列，展示时脱敏',
  last_login_time DATETIME    DEFAULT NULL COMMENT '最近登录时间',
  status         TINYINT     NOT NULL DEFAULT 1,
  version        INT         NOT NULL DEFAULT 0,
  create_by      BIGINT      DEFAULT NULL,
  create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by      BIGINT      DEFAULT NULL,
  update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工账号';

CREATE TABLE sys_role (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  org_id      BIGINT      NOT NULL,
  role_code   VARCHAR(64) NOT NULL,
  role_name   VARCHAR(64) NOT NULL,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (org_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作角色';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE sys_menu (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT      NOT NULL DEFAULT 0,
  menu_name   VARCHAR(64) NOT NULL,
  menu_type   VARCHAR(16) NOT NULL DEFAULT 'M' COMMENT 'M=目录 C=菜单 F=按钮',
  path        VARCHAR(128) DEFAULT NULL,
  component   VARCHAR(128) DEFAULT NULL,
  permission  VARCHAR(64)  DEFAULT NULL COMMENT '权限标识，如 org:add',
  sort        INT         NOT NULL DEFAULT 0,
  status      TINYINT     NOT NULL DEFAULT 1,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_sys_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限';

CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

CREATE TABLE sys_operation_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      DEFAULT NULL,
  org_id      BIGINT      DEFAULT NULL,
  module      VARCHAR(64) DEFAULT NULL,
  action      VARCHAR(64) DEFAULT NULL,
  request_id  VARCHAR(64) DEFAULT NULL,
  trace_id    VARCHAR(64) DEFAULT NULL,
  detail      TEXT        DEFAULT NULL,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_log_user_time (user_id, create_time),
  KEY idx_log_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户操作日志（只追加）';

CREATE TABLE bd_shipper (
  id                    BIGINT       NOT NULL AUTO_INCREMENT,
  org_id                BIGINT       NOT NULL,
  name                  VARCHAR(128) NOT NULL,
  credit_code           VARCHAR(64)  DEFAULT NULL COMMENT '统一社会信用代码',
  legal_person_encrypted VARCHAR(256) DEFAULT NULL,
  audit_status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
  version               INT          NOT NULL DEFAULT 0,
  create_by             BIGINT       DEFAULT NULL,
  create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by             BIGINT       DEFAULT NULL,
  update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted               TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bd_shipper_credit (credit_code),
  KEY idx_bd_shipper_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='托运人（货主）';

CREATE TABLE bd_goods (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  org_id      BIGINT       NOT NULL,
  goods_name  VARCHAR(128) NOT NULL,
  goods_type  VARCHAR(64)  DEFAULT NULL,
  spec        VARCHAR(128) DEFAULT NULL,
  unit        VARCHAR(32)  DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_bd_goods_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常用货物';

CREATE TABLE bd_address (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  org_id       BIGINT       NOT NULL,
  address_name VARCHAR(128) NOT NULL,
  province     VARCHAR(32)  DEFAULT NULL,
  city         VARCHAR(32)  DEFAULT NULL,
  district     VARCHAR(32)  DEFAULT NULL,
  detail       VARCHAR(256) DEFAULT NULL,
  longitude    DECIMAL(10,6) DEFAULT NULL,
  latitude     DECIMAL(10,6) DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_bd_address_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常用装卸货地址';

CREATE TABLE bd_price_rule (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  org_id        BIGINT       NOT NULL,
  rule_name     VARCHAR(128) NOT NULL,
  formula       VARCHAR(512) NOT NULL COMMENT '按公里数/重量配置的价格公式',
  base_price    DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  status        TINYINT      NOT NULL DEFAULT 1,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格公式';

CREATE TABLE file_record (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  org_id       BIGINT       NOT NULL,
  biz_type     VARCHAR(32)  NOT NULL COMMENT 'CERT/RECEIPT/CONTRACT/INVOICE',
  biz_id       VARCHAR(64)  DEFAULT NULL,
  obs_key      VARCHAR(512) NOT NULL COMMENT '只存对象存储 key，不存公网直链',
  file_name    VARCHAR(256) DEFAULT NULL,
  content_type VARCHAR(128) DEFAULT NULL,
  size_bytes   BIGINT       DEFAULT NULL,
  create_by    BIGINT       DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_file_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证照/回单/合同/发票文件索引';
