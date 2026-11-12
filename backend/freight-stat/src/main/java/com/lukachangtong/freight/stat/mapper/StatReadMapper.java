package com.lukachangtong.freight.stat.mapper;

import com.lukachangtong.freight.stat.vo.AggRow;
import com.lukachangtong.freight.stat.vo.StatDetailRow;
import com.lukachangtong.freight.stat.vo.WaybillRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计域的**只读**数据访问。
 *
 * 这是 CQRS 的读侧：统计要跨 6 个域聚合，如果为每个指标都定义端口 + 适配器，
 * 会得到 7 个接口、7 个实现，而它们全都只有一条 SQL。这里直接只读查询业务表，
 * 用三条纪律约束风险：
 * 1. **只 SELECT**，不写任何业务表——写路径仍然只走各域自己的 Service；
 * 2. 所有语句集中在这一个文件，schema 变更时改一处；
 * 3. 单表/单 join，不做复杂关联，避免统计查询把业务库拖慢。
 *
 * 若将来统计要独立部署或换 OLAP，把这个类的实现换掉即可，Service 层不用改。
 */
@Mapper
public interface StatReadMapper {

    // ------------------------------------------------------------------ 聚合

    @Select("SELECT COUNT(*) AS cnt, COALESCE(SUM(freight_amount), 0) AS amt FROM wb_waybill "
            + "WHERE deleted = 0 AND create_time >= #{from} AND create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggWaybillCreated(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("orgId") Long orgId);

    @Select("SELECT COUNT(*) AS cnt, COALESCE(SUM(freight_amount), 0) AS amt FROM wb_waybill "
            + "WHERE deleted = 0 AND sign_time >= #{from} AND sign_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggWaybillSigned(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                            @Param("orgId") Long orgId);

    @Select("SELECT COUNT(*) AS cnt FROM wb_exception e "
            + "WHERE e.create_time >= #{from} AND e.create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR EXISTS ("
            + "  SELECT 1 FROM wb_waybill w WHERE w.id = e.waybill_id "
            + "  AND w.org_id = #{orgId,jdbcType=BIGINT}))")
    AggRow aggException(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                        @Param("orgId") Long orgId);

    @Select("SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt FROM st_settlement "
            + "WHERE deleted = 0 AND biz_type = 'PAYABLE' "
            + "AND settled_date >= #{from} AND settled_date < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggSettlement(@Param("from") LocalDate from, @Param("to") LocalDate to,
                         @Param("orgId") Long orgId);

    @Select("SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt FROM st_payment_order "
            + "WHERE status = 'SUCCESS' AND execute_time >= #{from} AND execute_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggPayment(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                      @Param("orgId") Long orgId);

    @Select("SELECT COUNT(*) AS cnt FROM cs_complaint "
            + "WHERE deleted = 0 AND create_time >= #{from} AND create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggComplaint(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                        @Param("orgId") Long orgId);

    /**
     * 当日新增投诉中「已经超时」的数量。
     *
     * 判定要覆盖两种情形：已处理但超时（resolved_time > deadline），以及还没处理且已过期。
     * 只统计 resolved 的会漏掉「现在正逾期」的，而这个数恰恰是给运营压力的那个。
     */
    @Select("SELECT COUNT(*) AS cnt FROM cs_complaint "
            + "WHERE deleted = 0 AND create_time >= #{from} AND create_time < #{to} "
            + "AND ((resolved_time IS NOT NULL AND resolved_time > deadline_time) "
            + "  OR (resolved_time IS NULL AND deadline_time < NOW())) "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggComplaintOverdue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                               @Param("orgId") Long orgId);

    @Select("SELECT COUNT(*) AS cnt, COALESCE(SUM(score), 0) AS amt FROM cs_rating "
            + "WHERE status = 'VALID' AND create_time >= #{from} AND create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggRating(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                     @Param("orgId") Long orgId);

    @Select("SELECT COUNT(*) AS cnt FROM tr_track_alarm "
            + "WHERE alarm_time >= #{from} AND alarm_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR org_id = #{orgId,jdbcType=BIGINT})")
    AggRow aggAlarm(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                    @Param("orgId") Long orgId);

    // ------------------------------------------------------------------ 维度聚合用明细

    @Select("SELECT w.id AS id, w.org_id AS orgId, w.load_addr AS loadAddr, w.unload_addr AS unloadAddr, "
            + "w.cargo_name AS cargoName, w.driver_id AS driverId, d.name AS driverName, "
            + "w.freight_amount AS amount, w.create_time AS createTime "
            + "FROM wb_waybill w LEFT JOIN cr_driver d ON d.id = w.driver_id "
            + "WHERE w.deleted = 0 AND w.create_time >= #{from} AND w.create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR w.org_id = #{orgId,jdbcType=BIGINT})")
    List<WaybillRow> waybillsForDim(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                    @Param("orgId") Long orgId);

    // ------------------------------------------------------------------ 数据穿透

    @Select("SELECT 'WAYBILL' AS bizType, w.id AS bizId, w.waybill_no AS bizNo, w.org_id AS orgId, "
            + "d.name AS partner, w.freight_amount AS amount, w.status AS status, w.create_time AS bizTime, "
            + "CONCAT(w.load_addr, ' → ', w.unload_addr) AS extra, "
            + "w.load_addr AS loadAddr, w.unload_addr AS unloadAddr, w.cargo_name AS cargoName, "
            + "w.driver_id AS driverId, d.name AS driverName "
            + "FROM wb_waybill w LEFT JOIN cr_driver d ON d.id = w.driver_id "
            + "WHERE w.deleted = 0 AND w.create_time >= #{from} AND w.create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR w.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY w.create_time DESC, w.id DESC LIMIT #{limit}")
    List<StatDetailRow> drillWaybillCreated(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            @Param("orgId") Long orgId,
                                            @Param("limit") int limit);

    @Select("SELECT 'WAYBILL' AS bizType, w.id AS bizId, w.waybill_no AS bizNo, w.org_id AS orgId, "
            + "d.name AS partner, w.freight_amount AS amount, w.status AS status, w.sign_time AS bizTime, "
            + "CONCAT(w.load_addr, ' → ', w.unload_addr) AS extra, "
            + "w.load_addr AS loadAddr, w.unload_addr AS unloadAddr, w.cargo_name AS cargoName, "
            + "w.driver_id AS driverId, d.name AS driverName "
            + "FROM wb_waybill w LEFT JOIN cr_driver d ON d.id = w.driver_id "
            + "WHERE w.deleted = 0 AND w.sign_time >= #{from} AND w.sign_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR w.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY w.sign_time DESC, w.id DESC LIMIT #{limit}")
    List<StatDetailRow> drillWaybillSigned(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           @Param("orgId") Long orgId,
                                           @Param("limit") int limit);

    @Select("SELECT 'SETTLEMENT' AS bizType, s.id AS bizId, s.settlement_no AS bizNo, s.org_id AS orgId, "
            + "s.partner_name AS partner, s.amount AS amount, s.status AS status, s.create_time AS bizTime, "
            + "CONCAT('运单 ', IFNULL(s.waybill_no, '-')) AS extra "
            + "FROM st_settlement s "
            + "WHERE s.deleted = 0 AND s.biz_type = 'PAYABLE' "
            + "AND s.settled_date >= #{from} AND s.settled_date < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR s.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY s.id DESC LIMIT #{limit}")
    List<StatDetailRow> drillSettlement(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                        @Param("orgId") Long orgId, @Param("limit") int limit);

    @Select("SELECT 'PAYMENT' AS bizType, p.id AS bizId, p.pay_no AS bizNo, p.org_id AS orgId, "
            + "p.payee_name AS partner, p.amount AS amount, p.status AS status, p.execute_time AS bizTime, "
            + "CONCAT('业务单 ', IFNULL(p.biz_no, '-'), IFNULL(CONCAT(' / 银行流水 ', p.bank_serial_no), '')) AS extra "
            + "FROM st_payment_order p "
            + "WHERE p.status = 'SUCCESS' AND p.execute_time >= #{from} AND p.execute_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR p.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY p.id DESC LIMIT #{limit}")
    List<StatDetailRow> drillPayment(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                     @Param("orgId") Long orgId, @Param("limit") int limit);

    @Select("SELECT 'COMPLAINT' AS bizType, c.id AS bizId, c.complaint_no AS bizNo, c.org_id AS orgId, "
            + "c.target_name AS partner, NULL AS amount, c.status AS status, c.create_time AS bizTime, "
            + "CONCAT(c.complaint_type, ' · ', LEFT(c.content, 60)) AS extra "
            + "FROM cs_complaint c "
            + "WHERE c.deleted = 0 AND c.create_time >= #{from} AND c.create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR c.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY c.id DESC LIMIT #{limit}")
    List<StatDetailRow> drillComplaint(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                       @Param("orgId") Long orgId, @Param("limit") int limit);

    @Select("SELECT 'RATING' AS bizType, r.id AS bizId, r.rating_no AS bizNo, r.org_id AS orgId, "
            + "r.target_name AS partner, NULL AS amount, r.status AS status, r.create_time AS bizTime, "
            + "CONCAT(r.score, ' 星 · ', IFNULL(r.content, '未填写文字')) AS extra "
            + "FROM cs_rating r "
            + "WHERE r.status = 'VALID' AND r.create_time >= #{from} AND r.create_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR r.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY r.id DESC LIMIT #{limit}")
    List<StatDetailRow> drillRating(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                    @Param("orgId") Long orgId, @Param("limit") int limit);

    @Select("SELECT 'ALARM' AS bizType, a.id AS bizId, CONCAT('ALARM-', a.id) AS bizNo, a.org_id AS orgId, "
            + "d.name AS partner, NULL AS amount, CAST(a.status AS CHAR) AS status, a.alarm_time AS bizTime, "
            + "CONCAT(a.alarm_type, ' · ', LEFT(a.content, 60)) AS extra "
            + "FROM tr_track_alarm a LEFT JOIN cr_driver d ON d.id = a.driver_id "
            + "WHERE a.alarm_time >= #{from} AND a.alarm_time < #{to} "
            + "AND (#{orgId,jdbcType=BIGINT} IS NULL OR a.org_id = #{orgId,jdbcType=BIGINT}) "
            + "ORDER BY a.id DESC LIMIT #{limit}")
    List<StatDetailRow> drillAlarm(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                   @Param("orgId") Long orgId, @Param("limit") int limit);

    /** 单指标合计（对账/校验用）：直接返回 BigDecimal，避免调用方再判空。 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM st_settlement WHERE deleted = 0 "
            + "AND biz_type = 'PAYABLE' AND org_id = #{orgId}")
    BigDecimal sumSettlementByOrg(@Param("orgId") Long orgId);
}
