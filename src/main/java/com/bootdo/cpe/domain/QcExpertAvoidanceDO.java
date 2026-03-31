package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * QC专家回避记录
 * 
 * @author system
 * @date 2026-03-27
 */
public class QcExpertAvoidanceDO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 主键ID
    private Integer id;
    // 任务ID
    private String taskId;
    // 项目ID
    private Integer proId;
    // 专家用户ID
    private Integer expertUserId;
    // 回避类型：auto-自动回避, manual-手动回避
    private String avoidanceType;
    // 回避原因
    private String avoidanceReason;
    // 创建时间
    private Date created;
    // 创建人
    private Integer createdBy;
    // 删除标记
    private Integer deleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Integer getProId() {
        return proId;
    }

    public void setProId(Integer proId) {
        this.proId = proId;
    }

    public Integer getExpertUserId() {
        return expertUserId;
    }

    public void setExpertUserId(Integer expertUserId) {
        this.expertUserId = expertUserId;
    }

    public String getAvoidanceType() {
        return avoidanceType;
    }

    public void setAvoidanceType(String avoidanceType) {
        this.avoidanceType = avoidanceType;
    }

    public String getAvoidanceReason() {
        return avoidanceReason;
    }

    public void setAvoidanceReason(String avoidanceReason) {
        this.avoidanceReason = avoidanceReason;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
