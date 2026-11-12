-- M3 剩余：电子围栏 + 在途预警 + 轨迹归档审计。
-- 围栏形状同时支持圆形（园区/装卸点常用）与多边形（禁行区/城市边界常用）；
-- 顶点以 JSON 文本存储，避免引入 GIS 扩展（当前单库 MySQL 8，不装 PostGIS 类扩展）。

CREATE TABLE tr_fence (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  org_id        BIGINT       NOT NULL,
  fence_code    VARCHAR(32)  NOT NULL COMMENT '围栏编码（机构内唯一）',
  fence_name    VARCHAR(64)  NOT NULL,
  fence_type    VARCHAR(16)  NOT NULL DEFAULT 'CUSTOM' COMMENT 'LOAD 装货 / UNLOAD 卸货 / FORBIDDEN 禁行 / CUSTOM 自定义',
  shape         VARCHAR(8)   NOT NULL DEFAULT 'CIRCLE' COMMENT 'CIRCLE 圆形 / POLYGON 多边形',
  center_lat    DECIMAL(10,6) DEFAULT NULL COMMENT 'GCJ-02 圆心纬度',
  center_lng    DECIMAL(10,6) DEFAULT NULL COMMENT 'GCJ-02 圆心经度',
  radius_m      INT           DEFAULT NULL COMMENT '圆形半径（米）',
  vertices      TEXT          DEFAULT NULL COMMENT '多边形顶点 JSON：[[lat,lng],...]（至少 3 点）',
  trigger_type  VARCHAR(8)   NOT NULL DEFAULT 'BOTH' COMMENT 'IN 进入 / OUT 离开 / BOTH 双向',
  enabled       TINYINT      NOT NULL DEFAULT 1,
  remark        VARCHAR(255) DEFAULT NULL,
  create_by     VARCHAR(64)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     VARCHAR(64)  DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  version       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fence_code (org_id, fence_code),
  KEY idx_fence_org (org_id, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子围栏（圆形/多边形）';

-- 预警表不做分区：单量远小于轨迹点（按规则聚合后每运单个位数），
-- 且需要 update（处理状态），分区表复合主键会限制 updateById。
CREATE TABLE tr_track_alarm (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  org_id        BIGINT       DEFAULT NULL,
  waybill_id    BIGINT       NOT NULL,
  driver_id     BIGINT       DEFAULT NULL,
  vehicle_id    BIGINT       DEFAULT NULL,
  alarm_type    VARCHAR(16)  NOT NULL COMMENT 'OVERSPEED 超速 / DEVIATE 偏航 / LONG_STAY 异常停留 / FENCE_IN 入围栏 / FENCE_OUT 出围栏',
  alarm_level   VARCHAR(8)   NOT NULL DEFAULT 'MEDIUM' COMMENT 'HIGH / MEDIUM / LOW',
  alarm_time    DATETIME     NOT NULL COMMENT '触发点定位时间',
  lat           DECIMAL(10,6) DEFAULT NULL,
  lng           DECIMAL(10,6) DEFAULT NULL,
  fence_id      BIGINT       DEFAULT NULL,
  content       VARCHAR(255) NOT NULL COMMENT '人可读描述，直接进推送文案',
  dedup_key     VARCHAR(96)  NOT NULL COMMENT '幂等键：同一运单同一事件重复分析只留一条',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未处理 / 1 已处理 / 2 已忽略',
  handle_by     VARCHAR(64)  DEFAULT NULL,
  handle_time   DATETIME     DEFAULT NULL,
  handle_remark VARCHAR(255) DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_alarm_dedup (dedup_key),
  KEY idx_alarm_wb (waybill_id, alarm_time),
  KEY idx_alarm_status (status, alarm_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在途预警（超速/偏航/停留/围栏进出）';

CREATE TABLE tr_archive_log (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  partition_name VARCHAR(32) NOT NULL,
  period_start   DATE        DEFAULT NULL,
  period_end     DATE        DEFAULT NULL,
  row_count      BIGINT      NOT NULL DEFAULT 0,
  file_key       VARCHAR(128) DEFAULT NULL COMMENT '对象存储 key，不存公网直链',
  status         VARCHAR(16) NOT NULL COMMENT 'DRY_RUN / ARCHIVED / PURGED / FAILED',
  message        VARCHAR(255) DEFAULT NULL,
  create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_archive_partition (partition_name, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轨迹冷数据归档审计（保留 3 年）';
