package com.lukachangtong.freight.track.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 归档执行报告：dryRun 时也返回将要处理的清单，便于上线前先看影响面。 */
public class ArchiveReportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean dryRun;
    private String cutoff;
    private long totalRows;
    private List<Item> items = new ArrayList<>();

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean v) { this.dryRun = v; }
    public String getCutoff() { return cutoff; }
    public void setCutoff(String v) { this.cutoff = v; }
    public long getTotalRows() { return totalRows; }
    public void setTotalRows(long v) { this.totalRows = v; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> v) { this.items = v; }

    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        private String partitionName;
        private String periodStart;
        private String periodEnd;
        private long rowCount;
        private String fileKey;
        private String status;
        private String message;

        public String getPartitionName() { return partitionName; }
        public void setPartitionName(String v) { this.partitionName = v; }
        public String getPeriodStart() { return periodStart; }
        public void setPeriodStart(String v) { this.periodStart = v; }
        public String getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(String v) { this.periodEnd = v; }
        public long getRowCount() { return rowCount; }
        public void setRowCount(long v) { this.rowCount = v; }
        public String getFileKey() { return fileKey; }
        public void setFileKey(String v) { this.fileKey = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { this.message = v; }
    }
}
