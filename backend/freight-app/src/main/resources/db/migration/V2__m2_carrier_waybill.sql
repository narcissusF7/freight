-- M2 建表：承运准入 + 运单主线。沿用 V1 字段约定（org_id/create_by/update_by/audit 字段、
-- create_time/update_time 默认值、deleted 软删、金额 DECIMAL(18,2)）。表名按模块前缀分组。

CREATE TABLE cr_driver (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  org_id        BIGINT       NOT NULL,
  name          VARCHAR(64)  NOT NULL,
  phone         VARCHAR(32)  DEFAULT NULL,
  id_card_no    VARCHAR(64)  DEFAULT NULL COMMENT '身份证号，生产须加密存储 + 脱敏展示',
  driver_license_no VARCHAR(64) DEFAULT NULL COMMENT '驾驶证号',
  audit_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
  audit_remark  VARCHAR(256) DEFAULT NULL,
  create_by     VARCHAR(64)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     VARCHAR(64)  DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cr_driver_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机准入';

CREATE TABLE cr_vehicle (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  org_id        BIGINT       NOT NULL,
  plate_no      VARCHAR(32)  NOT NULL,
  vehicle_type  VARCHAR(64)  DEFAULT NULL,
  owner_name    VARCHAR(64)  DEFAULT NULL,
  capacity_ton  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '核定载重（吨）',
  axle_count    INT          DEFAULT NULL,
  driver_id     BIGINT       DEFAULT NULL,
  audit_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
  audit_remark  VARCHAR(256) DEFAULT NULL,
  create_by     VARCHAR(64)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     VARCHAR(64)  DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_cr_vehicle_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆准入';

CREATE TABLE wb_goods_source (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  org_id        BIGINT       NOT NULL,
  shipper_id    BIGINT       DEFAULT NULL,
  goods_name    VARCHAR(128) NOT NULL,
  weight        DECIMAL(12,2) DEFAULT NULL COMMENT '吨',
  volume        DECIMAL(12,2) DEFAULT NULL COMMENT '方',
  load_addr     VARCHAR(256) DEFAULT NULL,
  unload_addr   VARCHAR(256) DEFAULT NULL,
  load_time     DATETIME     DEFAULT NULL,
  freight_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '运费报价（元）',
  contact_name  VARCHAR(64)  DEFAULT NULL,
  contact_phone VARCHAR(32)  DEFAULT NULL,
  remark        VARCHAR(512) DEFAULT NULL,
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待承运 1已生成运单 2关闭',
  create_by     VARCHAR(64)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     VARCHAR(64)  DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_wb_gs_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货源发布';

CREATE TABLE wb_waybill (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  org_id          BIGINT       NOT NULL,
  waybill_no      VARCHAR(32)  NOT NULL,
  goods_source_id BIGINT       DEFAULT NULL,
  shipper_id      BIGINT       DEFAULT NULL,
  driver_id       BIGINT       DEFAULT NULL,
  vehicle_id      BIGINT       DEFAULT NULL,
  cargo_name      VARCHAR(128) DEFAULT NULL,
  weight          DECIMAL(12,2) DEFAULT NULL,
  load_addr       VARCHAR(256) DEFAULT NULL,
  unload_addr     VARCHAR(256) DEFAULT NULL,
  plan_load_time  DATETIME     DEFAULT NULL,
  status          VARCHAR(32)  NOT NULL COMMENT 'WaybillStatus 枚举名',
  report_status   TINYINT      NOT NULL DEFAULT 0 COMMENT '0未报 1已报 2失败',
  create_by       VARCHAR(64)  DEFAULT NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by       VARCHAR(64)  DEFAULT NULL,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wb_no (waybill_no),
  KEY idx_wb_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运单';

CREATE TABLE wb_node (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  waybill_id   BIGINT       NOT NULL,
  from_status  VARCHAR(32)  DEFAULT NULL,
  to_status    VARCHAR(32)  NOT NULL,
  operate_by   VARCHAR(64)  DEFAULT NULL,
  operate_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark       VARCHAR(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wb_node_wb (waybill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运单节点（只追加）';
