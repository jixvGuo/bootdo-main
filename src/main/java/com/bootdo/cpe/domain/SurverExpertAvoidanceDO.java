package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 勘察奖-专家回避记录 DO（对应 ass_surver_expert_avoidance）
 *
 * 1:1 镜像 {@link QcExpertAvoidanceDO}：字段、含义、取值（auto/manual）一致。
 * 通过 task_id 与 QC 数据天然隔离。
 */
public class SurverExpertAvoidanceDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String taskId;
    private Integer proId;
    private Integer expertUserId;
    private String avoidanceType;     // auto | manual
    private String avoidanceReason;
    private Date created;
    private Integer createdBy;
    private Integer deleted;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Integer getProId() { return proId; }
    public void setProId(Integer proId) { this.proId = proId; }
    public Integer getExpertUserId() { return expertUserId; }
    public void setExpertUserId(Integer expertUserId) { this.expertUserId = expertUserId; }
    public String getAvoidanceType() { return avoidanceType; }
    public void setAvoidanceType(String avoidanceType) { this.avoidanceType = avoidanceType; }
    public String getAvoidanceReason() { return avoidanceReason; }
    public void setAvoidanceReason(String avoidanceReason) { this.avoidanceReason = avoidanceReason; }
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
