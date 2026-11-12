-- V9 的 st_invoice.invoice_no 定为 VARCHAR(32)，但税控返回的模拟票号
-- 「MOCK-INV-<结算单号>:INV」有 35 位，插入时被截断报 Data truncation。
--
-- 真实税控票号是 8~20 位（全电发票 20 位），32 位本身够用；
-- 这里放宽到 64 位是为了容纳服务商回的「票号+批次」组合串，避免为了一个字段长度，
-- 在开票链路里做截断（截断发票号 = 发票查不到，比报错更糟）。
--
-- 迁移脚本一旦执行就不改（V9 已应用），修正一律走新版本，这是 Flyway 的基本纪律。
ALTER TABLE st_invoice
  MODIFY COLUMN invoice_no VARCHAR(64) DEFAULT NULL COMMENT '发票号码（税控返回）';
