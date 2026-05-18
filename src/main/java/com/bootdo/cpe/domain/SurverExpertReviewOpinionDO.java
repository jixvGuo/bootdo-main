package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 勘察奖专家打分页：审核意见 + 主评意见（表 ass_surver_expert_review_opinion）
 */
public class SurverExpertReviewOpinionDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskId;
    private Integer proId;
    private String proSubType;
    private Long expertUid;
    /** agree | disagree */
    private String auditOpinion;
    private String mainReviewText;
    private Integer mainReviewSubmitted;
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
    public Long getExpertUid() { return expertUid; }
    public void setExpertUid(Long expertUid) { this.expertUid = expertUid; }
    public String getAuditOpinion() { return auditOpinion; }
    public void setAuditOpinion(String auditOpinion) { this.auditOpinion = auditOpinion; }
    public String getMainReviewText() { return mainReviewText; }
    public void setMainReviewText(String mainReviewText) { this.mainReviewText = mainReviewText; }
    public Integer getMainReviewSubmitted() { return mainReviewSubmitted; }
    public void setMainReviewSubmitted(Integer mainReviewSubmitted) { this.mainReviewSubmitted = mainReviewSubmitted; }
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
    public Date getUpdated() { return updated; }
    public void setUpdated(Date updated) { this.updated = updated; }
}
