package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * QC 平均分计算明细（谁参与/回避/剔除最高最低）
 */
public class QcScoreCalcDetailDO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Integer id;
    /** 关联的汇总结果 ID（ass_qc_score_calc_result.id） */
    private Integer resultId;
    /** 任务 ID */
    private String taskId;
    /** 项目 ID */
    private Integer proId;
    /** 评分阶段：1=初评  2=复评 */
    private Integer phase;
    /** 参与打分的专家用户 ID */
    private Integer expertUid;
    /** 关联 ass_qc_score_submit.id */
    private Integer submitId;
    /** 专家原始分（来自提交快照） */
    private BigDecimal rawScore;
    /** 是否回避：1=是  0=否 */
    private Integer isAvoided;
    /** 是否参与计算：1=参与  0=排除（回避或缺评） */
    private Integer isValid;
    /** 是否被剔除最低分：1=是  0=否 */
    private Integer isRemovedLow;
    /** 是否被剔除最高分：1=是  0=否 */
    private Integer isRemovedHigh;
    /** 记录创建时间 */
    private Date created;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getResultId() {
        return resultId;
    }

    public void setResultId(Integer resultId) {
        this.resultId = resultId;
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

    public Integer getExpertUid() {
        return expertUid;
    }

    public void setExpertUid(Integer expertUid) {
        this.expertUid = expertUid;
    }

    public Integer getSubmitId() {
        return submitId;
    }

    public void setSubmitId(Integer submitId) {
        this.submitId = submitId;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public void setRawScore(BigDecimal rawScore) {
        this.rawScore = rawScore;
    }

    public Integer getIsAvoided() {
        return isAvoided;
    }

    public void setIsAvoided(Integer isAvoided) {
        this.isAvoided = isAvoided;
    }

    public Integer getIsValid() {
        return isValid;
    }

    public void setIsValid(Integer isValid) {
        this.isValid = isValid;
    }

    public Integer getIsRemovedLow() {
        return isRemovedLow;
    }

    public void setIsRemovedLow(Integer isRemovedLow) {
        this.isRemovedLow = isRemovedLow;
    }

    public Integer getIsRemovedHigh() {
        return isRemovedHigh;
    }

    public void setIsRemovedHigh(Integer isRemovedHigh) {
        this.isRemovedHigh = isRemovedHigh;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
