package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 勘察奖 - 专家淘汰评级 快照表 DO（对应 ass_surver_expert_eliminate_confirmed）
 * 专家点击"确认淘汰名单"后，活动表数据复制到此处冻结，后续不可修改。
 */
public class SurverExpertEliminateConfirmedDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskId;
    private Integer proId;
    private String proSubType;
    private String proCode;
    private String topicName;
    private String companyName;
    private String groupName;
    private Long expertUid;
    private String expertName;
    private String grade;
    private String remark;
    private Date confirmedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Integer getProId() { return proId; }
    public void setProId(Integer proId) { this.proId = proId; }
    public String getProSubType() { return proSubType; }
    public void setProSubType(String proSubType) { this.proSubType = proSubType; }
    public String getProCode() { return proCode; }
    public void setProCode(String proCode) { this.proCode = proCode; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Long getExpertUid() { return expertUid; }
    public void setExpertUid(Long expertUid) { this.expertUid = expertUid; }
    public String getExpertName() { return expertName; }
    public void setExpertName(String expertName) { this.expertName = expertName; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Date confirmedAt) { this.confirmedAt = confirmedAt; }
}
