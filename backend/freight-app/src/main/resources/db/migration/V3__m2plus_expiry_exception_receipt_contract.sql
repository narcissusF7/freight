-- M2+ 迁移：证件有效期 / 异常件 / 回单 / 电子合同。
-- 只做向前兼容变更（ADD COLUMN / CREATE TABLE），不改已有列语义。

-- 1) 承运准入：证件有效期（用于到期预警）
ALTER TABLE cr_driver
  ADD COLUMN license_expire_date DATE DEFAULT NULL COMMENT '驾驶证有效期至' AFTER driver_license_no,
  ADD COLUMN id_card_expire_date DATE DEFAULT NULL COMMENT '身份证有效期至' AFTER license_expire_date;

ALTER TABLE cr_vehicle
  ADD COLUMN vehicle_license_expire_date DATE DEFAULT NULL COMMENT '道路运输证有效期至' AFTER vehicle_type,
  ADD COLUMN insurance_expire_date DATE DEFAULT NULL COMMENT '保险有效期至' AFTER vehicle_license_expire_date;

-- 提前筛出临期证件，避免全表扫描
ALTER TABLE cr_driver  ADD KEY idx_cr_driver_lic_exp (license_expire_date);
ALTER TABLE cr_vehicle ADD KEY idx_cr_vehicle_lic_exp (vehicle_license_expire_date);

-- 2) 异常件（只追加；处理结果回写同一行）
CREATE TABLE wb_exception (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  waybill_id     BIGINT       NOT NULL,
  exception_type VARCHAR(32)  NOT NULL COMMENT 'CARGO_DAMAGE 货损 / DELAY 延误 / ACCIDENT 事故 / ADDRESS 地址异常 / OTHER',
  description    VARCHAR(512) DEFAULT NULL,
  image_urls     VARCHAR(1024) DEFAULT NULL COMMENT '逗号分隔的图片 URL',
  report_by      VARCHAR(64)  DEFAULT NULL,
  report_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待处理 1 已处理',
  handle_remark  VARCHAR(512) DEFAULT NULL,
  handle_by      VARCHAR(64)  DEFAULT NULL,
  handle_time    DATETIME     DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_wb_exc_wb (waybill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运单异常件';

-- 3) 回单
CREATE TABLE wb_receipt (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  waybill_id  BIGINT       NOT NULL,
  file_key    VARCHAR(256) NOT NULL COMMENT '对象存储 key',
  file_name   VARCHAR(256) DEFAULT NULL,
  file_url    VARCHAR(512) DEFAULT NULL,
  upload_by   VARCHAR(64)  DEFAULT NULL,
  upload_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verified    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待核验 1 已核验',
  remark      VARCHAR(256) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_wb_receipt_wb (waybill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运单回单';

-- 4) 电子合同（托运框架协议 / 司机运输合同）
CREATE TABLE ct_contract (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  org_id         BIGINT       DEFAULT NULL,
  contract_no    VARCHAR(64)  NOT NULL,
  contract_type  VARCHAR(32)  NOT NULL COMMENT 'FRAMEWORK 托运框架协议 / TRANSPORT 司机运输合同',
  waybill_id     BIGINT       DEFAULT NULL,
  party_a        VARCHAR(128) DEFAULT NULL,
  party_b        VARCHAR(128) DEFAULT NULL,
  content        TEXT         COMMENT '合同正文（要素拼接结果）',
  content_hash   VARCHAR(128) NOT NULL COMMENT 'SHA-256 摘要，签章只签摘要不传明文',
  sign_status    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待签 1 甲方已签 2 双方已签 3 已作废',
  signature_a    VARCHAR(1024) DEFAULT NULL,
  signature_b    VARCHAR(1024) DEFAULT NULL,
  cert_serial_no VARCHAR(64)  DEFAULT NULL,
  sign_time      DATETIME     DEFAULT NULL,
  create_by      VARCHAR(64)  DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by      VARCHAR(64)  DEFAULT NULL,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ct_no (contract_no),
  KEY idx_ct_wb (waybill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子合同';
