package com.lukachangtong.freight.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lukachangtong.freight.customer.entity.CsCreditSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CsCreditSubjectMapper extends BaseMapper<CsCreditSubject> {

    @Select("SELECT * FROM cs_credit_subject WHERE subject_type = #{subjectType} AND subject_id = #{subjectId}")
    CsCreditSubject selectBySubject(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId);

    /**
     * 原子加减分与计数。
     *
     * 不在 Java 里「读出来 + 加上去」再写回：那样两个并发事件会丢一次扣分，
     * 而丢分恰恰是信用系统最不能接受的错误（该扣的没扣）。
     * 等级也在 SQL 里按新的分数算，避免 Java 与 SQL 两处阈值不一致。
     */
    @Update("UPDATE cs_credit_subject SET "
            // 注意顺序：MySQL 的 SET 子句是**从左到右求值且后面的表达式看到的是已更新的值**。
            // 先算 level 再改 score，level 里的 `score + #{delta}` 才是「变动前的分 + 增量」；
            // 反过来写会变成 (旧分+增量)+增量 再判等级——差评扣完 10 分被算成 80，等级从 A 掉到 B。
            + "level = CASE "
            + "  WHEN LEAST(100, GREATEST(0, score + #{delta})) >= 90 THEN 'A' "
            + "  WHEN LEAST(100, GREATEST(0, score + #{delta})) >= 75 THEN 'B' "
            + "  WHEN LEAST(100, GREATEST(0, score + #{delta})) >= 60 THEN 'C' "
            + "  ELSE 'D' END, "
            + "score = LEAST(100, GREATEST(0, score + #{delta})), "
            + "good_count = good_count + #{goodInc}, "
            + "neutral_count = neutral_count + #{neutralInc}, "
            + "bad_count = bad_count + #{badInc}, "
            + "complaint_count = complaint_count + #{complaintInc}, "
            + "total_count = total_count + #{totalInc}, "
            + "last_change_time = NOW(), update_time = NOW(), version = version + 1 "
            + "WHERE id = #{id}")
    int applyDelta(@Param("id") Long id,
                   @Param("delta") int delta,
                   @Param("goodInc") int goodInc,
                   @Param("neutralInc") int neutralInc,
                   @Param("badInc") int badInc,
                   @Param("complaintInc") int complaintInc,
                   @Param("totalInc") int totalInc);
}
