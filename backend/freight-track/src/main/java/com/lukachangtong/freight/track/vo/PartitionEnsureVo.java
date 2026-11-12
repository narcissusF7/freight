package com.lukachangtong.freight.track.vo;

import java.io.Serializable;

/** 分区预建结果，运维接口直接返回给调用方看。 */
public class PartitionEnsureVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestedMonth;
    private String partitionName;
    private String boundary;
    /** EXISTS 已存在（幂等跳过） / CREATED 已创建 */
    private String action;
    private String message;

    public String getRequestedMonth() { return requestedMonth; }
    public void setRequestedMonth(String v) { this.requestedMonth = v; }
    public String getPartitionName() { return partitionName; }
    public void setPartitionName(String v) { this.partitionName = v; }
    public String getBoundary() { return boundary; }
    public void setBoundary(String v) { this.boundary = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
}
