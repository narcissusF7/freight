package com.lukachangtong.freight.track.vo;

import java.io.Serializable;

/** 分区元信息（来自 information_schema.PARTITIONS）。 */
public class PartitionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String partitionName;
    /** LESS THAN 的边界值，pmax 为 MAXVALUE */
    private String partitionDescription;
    /** InnoDB 估算行数，仅供运维粗看，精确计数走 countBefore */
    private Long tableRows;

    public String getPartitionName() { return partitionName; }
    public void setPartitionName(String v) { this.partitionName = v; }
    public String getPartitionDescription() { return partitionDescription; }
    public void setPartitionDescription(String v) { this.partitionDescription = v; }
    public Long getTableRows() { return tableRows; }
    public void setTableRows(Long v) { this.tableRows = v; }
}
