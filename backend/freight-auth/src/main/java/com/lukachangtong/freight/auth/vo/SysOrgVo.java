package com.lukachangtong.freight.auth.vo;

import java.util.ArrayList;
import java.util.List;

/** 机构树节点视图。 */
public class SysOrgVo {

    private Long id;
    private Long parentId;
    private String orgName;
    private String orgCode;
    private Integer status;
    private List<SysOrgVo> children = new ArrayList<SysOrgVo>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public String getOrgCode() { return orgCode; }
    public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<SysOrgVo> getChildren() { return children; }
    public void setChildren(List<SysOrgVo> children) { this.children = children; }
}
