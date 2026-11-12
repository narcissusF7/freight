package com.lukachangtong.freight.track.mapper;

import com.lukachangtong.freight.track.vo.PartitionVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分区运维 SQL。
 *
 * 这里必须用 ${} 拼 DDL（MySQL 不支持把标识符当参数绑定），
 * 因此**调用方必须先校验分区名符合 pYYYYMM 格式**（见 TrackMaintenanceServiceImpl），
 * 不能把外部输入直接传进来，否则存在标识符注入风险。
 */
@Mapper
public interface TrackMaintenanceMapper {

    @Select("SELECT PARTITION_NAME AS partitionName, PARTITION_DESCRIPTION AS partitionDescription, "
            + "TABLE_ROWS AS tableRows FROM information_schema.PARTITIONS "
            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tr_track_point' "
            + "AND PARTITION_NAME IS NOT NULL ORDER BY PARTITION_ORDINAL_POSITION")
    List<PartitionVo> listPartitions();

    /** 把 pmax 拆成「新月份分区 + 新 pmax」，这是按月扩容的标准做法，不重建表、不锁全表。 */
    @Update("ALTER TABLE tr_track_point REORGANIZE PARTITION pmax INTO ("
            + "PARTITION ${newPartition} VALUES LESS THAN ('${boundary}'), "
            + "PARTITION pmax VALUES LESS THAN (MAXVALUE))")
    int reorganizePmax(@Param("newPartition") String newPartition, @Param("boundary") String boundary);

    /** 冷数据清理：DROP PARTITION 是秒级元数据操作，不产生大事务与 binlog 膨胀。 */
    @Update("ALTER TABLE tr_track_point DROP PARTITION ${partition}")
    int dropPartition(@Param("partition") String partition);

    @Select("SELECT COUNT(*) FROM tr_track_point WHERE gps_time < #{end}")
    long countBefore(@Param("end") LocalDateTime end);

    /** 精确统计单个分区行数（information_schema.TABLE_ROWS 是估算值，归档审计不能用估算）。 */
    @Select("SELECT COUNT(*) FROM tr_track_point PARTITION (${partition})")
    long countInPartition(@Param("partition") String partition);
}
