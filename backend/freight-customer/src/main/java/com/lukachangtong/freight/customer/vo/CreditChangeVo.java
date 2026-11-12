package com.lukachangtong.freight.customer.vo;

/**
 * 一次信用变动的结果。
 *
 * `changed=false` 表示该业务事件此前已计过分（幂等命中），前端据此提示「已计分，未重复扣分」。
 */
public class CreditChangeVo {

    private boolean changed = false;
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private String bizType;
    private String bizNo;
    private int delta = 0;
    private int scoreBefore = 0;
    private int scoreAfter = 0;
    private String levelBefore;
    private String levelAfter;

    public boolean isChanged() { return changed; }
    /** Jackson 走 getX 命名，保留一个别名让前端能拿到 changed 字段 */
    public boolean getChanged() { return changed; }
    public void setChanged(boolean changed) { this.changed = changed; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }
    public int getScoreBefore() { return scoreBefore; }
    public void setScoreBefore(int scoreBefore) { this.scoreBefore = scoreBefore; }
    public int getScoreAfter() { return scoreAfter; }
    public void setScoreAfter(int scoreAfter) { this.scoreAfter = scoreAfter; }
    public String getLevelBefore() { return levelBefore; }
    public void setLevelBefore(String levelBefore) { this.levelBefore = levelBefore; }
    public String getLevelAfter() { return levelAfter; }
    public void setLevelAfter(String levelAfter) { this.levelAfter = levelAfter; }
}
