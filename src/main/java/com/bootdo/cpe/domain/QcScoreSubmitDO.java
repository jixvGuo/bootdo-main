package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * QC专家打分提交快照
 * 专家提交（scoreOver=1）时写入，唯一键 (task_id, pro_id, expert_uid, phase)
 */
public class QcScoreSubmitDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String taskId;
    private Integer proId;
    private Integer expertUid;
    /** 课题类型: solve=问题解决型  innovate=创新型 */
    private String topicType;
    /** 提交时 appraiseSum 转换后的 DECIMAL 快照 */
    private BigDecimal totalScore;
    /** 1=初评  2=复评 */
    private Integer phase;
    /** 原始打分表主键 (solve_score.id 或 innovate_score.id) */
    private Integer sourceId;
    private Date submitTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Integer getProId() { return proId; }
    public void setProId(Integer proId) { this.proId = proId; }

    public Integer getExpertUid() { return expertUid; }
    public void setExpertUid(Integer expertUid) { this.expertUid = expertUid; }

    public String getTopicType() { return topicType; }
    public void setTopicType(String topicType) { this.topicType = topicType; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public Integer getPhase() { return phase; }
    public void setPhase(Integer phase) { this.phase = phase; }

    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }

    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }
}
