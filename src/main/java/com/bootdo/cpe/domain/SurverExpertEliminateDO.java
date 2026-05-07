package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 勘察奖 - 专家淘汰评级 活动表 DO（对应 ass_surver_expert_eliminate）
 *
 * 与 QC 的 {@link QcExpertEliminateDO} 相比：
 *  - 用 grade(A/B/C/D) 替代 reason(text)
 *  - 多了 proSubType 标识 4 类申报子表
 *  - 没有 prev_pro_stat（QC 用于撤销时恢复状态，勘察奖暂不需要）
 */
public class SurverExpertEliminateDO implements Serializable {
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
    private Integer deleted;
    private Date created;
    private Date updated;

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
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
    public Date getUpdated() { return updated; }
    public void setUpdated(Date updated) { this.updated = updated; }
}
