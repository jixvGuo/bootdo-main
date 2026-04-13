package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * QC 平均分计算汇总结果（含初评/复评，保留历史）
 */
public class QcScoreCalcResultDO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Integer id;
    /** 任务 ID */
    private String taskId;
    /** 项目 ID */
    private Integer proId;
    /** 评分阶段：1=初评  2=复评 */
    private Integer phase;
    /** 去掉最高/最低分后的加权平均分，保留两位小数 */
    private BigDecimal avgScore;
    /** 有效参与人数（排除回避、缺评后） */
    private Integer validCount;
    /** 实际参与平均计算的人数（排除最高/最低后） */
    private Integer usedCount;
    /** 记录创建时间 */
    private Date created;
    /** 触发计算的操作人 UID */
    private Integer createdBy;
    /** 软删除标志：0=有效  1=已删除 */
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

    public Integer getPhase() {
        return phase;
    }

    public void setPhase(Integer phase) {
        this.phase = phase;
    }

    public BigDecimal getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore) {
        this.avgScore = avgScore;
    }

    public Integer getValidCount() {
        return validCount;
    }

    public void setValidCount(Integer validCount) {
        this.validCount = validCount;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
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
