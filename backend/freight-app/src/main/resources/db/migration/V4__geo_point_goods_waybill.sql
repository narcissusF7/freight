-- M2+ 地图选点：装货地/卸货地经纬度（GCJ-02，腾讯/高德通用火星坐标）。
-- 存 DECIMAL(10,6)：纬度精度约 0.11 米，足够装货点定位；不用 float，避免调度距离计算误差。
-- 坐标只随货源/运单存业务地点，不涉及人员位置采集。

ALTER TABLE wb_goods_source
  ADD COLUMN load_lat    DECIMAL(10,6) DEFAULT NULL COMMENT '装货地纬度 GCJ-02' AFTER load_addr,
  ADD COLUMN load_lng    DECIMAL(10,6) DEFAULT NULL COMMENT '装货地经度 GCJ-02' AFTER load_lat,
  ADD COLUMN unload_lat  DECIMAL(10,6) DEFAULT NULL COMMENT '卸货地纬度 GCJ-02' AFTER unload_addr,
  ADD COLUMN unload_lng  DECIMAL(10,6) DEFAULT NULL COMMENT '卸货地经度 GCJ-02' AFTER unload_lat;

ALTER TABLE wb_waybill
  ADD COLUMN load_lat    DECIMAL(10,6) DEFAULT NULL COMMENT '装货地纬度 GCJ-02' AFTER load_addr,
  ADD COLUMN load_lng    DECIMAL(10,6) DEFAULT NULL COMMENT '装货地经度 GCJ-02' AFTER load_lat,
  ADD COLUMN unload_lat  DECIMAL(10,6) DEFAULT NULL COMMENT '卸货地纬度 GCJ-02' AFTER unload_addr,
  ADD COLUMN unload_lng  DECIMAL(10,6) DEFAULT NULL COMMENT '卸货地经度 GCJ-02' AFTER unload_lat;
