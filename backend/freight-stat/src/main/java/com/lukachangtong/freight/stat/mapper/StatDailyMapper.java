package com.lukachangtong.freight.stat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.stat.entity.StatDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatDailyMapper extends BaseMapper<StatDaily> {

    /**
     * 按 `(biz_date, org_id)` 覆盖写。
     *
     * 用 `ON DUPLICATE KEY UPDATE` 而不是「先删后插」：重算任务和查询可能同时跑，
     * 先删会让查询在瞬间看到空数据（用户以为昨天没有单）；upsert 是原子替换。
     */
    @Insert("INSERT INTO stat_daily (biz_date, org_id, waybill_created, waybill_amount, waybill_signed, "
            + "signed_amount, waybill_exception, settle_count, settle_amount, pay_count, pay_amount, "
            + "complaint_count, complaint_overdue, rating_count, rating_sum, alarm_count, calc_time) VALUES ("
            + "#{d.bizDate}, #{d.orgId}, #{d.waybillCreated}, #{d.waybillAmount}, #{d.waybillSigned}, "
            + "#{d.signedAmount}, #{d.waybillException}, #{d.settleCount}, #{d.settleAmount}, #{d.payCount}, "
            + "#{d.payAmount}, #{d.complaintCount}, #{d.complaintOverdue}, #{d.ratingCount}, #{d.ratingSum}, "
            + "#{d.alarmCount}, NOW()) "
            + "ON DUPLICATE KEY UPDATE waybill_created = VALUES(waybill_created), "
            + "waybill_amount = VALUES(waybill_amount), waybill_signed = VALUES(waybill_signed), "
            + "signed_amount = VALUES(signed_amount), waybill_exception = VALUES(waybill_exception), "
            + "settle_count = VALUES(settle_count), settle_amount = VALUES(settle_amount), "
            + "pay_count = VALUES(pay_count), pay_amount = VALUES(pay_amount), "
            + "complaint_count = VALUES(complaint_count), complaint_overdue = VALUES(complaint_overdue), "
            + "rating_count = VALUES(rating_count), rating_sum = VALUES(rating_sum), "
            + "alarm_count = VALUES(alarm_count), calc_time = NOW()")
    int upsert(@Param("d") StatDaily daily);
}
