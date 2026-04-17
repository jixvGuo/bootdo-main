package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * QC奖发布分（第二次打分）评分表
 * 问题解决型与创新型共用同一套评分标准
 */
public class QcPresentScoreDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer optUid;
    private Integer proId;
    private String taskId;
    // 逻辑性（6分）
    private BigDecimal logicScore;
    // 专业性（6分）
    private BigDecimal professionalScore;
    // 发布形式（4分）
    private BigDecimal presentFormatScore;
    // 表达（4分）
    private BigDecimal expressionScore;
    // 回答问题（4分）
    private BigDecimal answerScore;
    // 成员/时间（4分）
    private BigDecimal memberTimeScore;
    // 课题类型（问题解决型 / 创新型）
    private String topicType;
    // 总分
    private String appraiseSum;
    // 是否已提交打分: 0=未提交 1=已提交
    private Integer scoreOver;
    private Date created;
    private Date updated;
    private Integer deleted;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOptUid() { return optUid; }
    public void setOptUid(Integer optUid) { this.optUid = optUid; }

    public Integer getProId() { return proId; }
    public void setProId(Integer proId) { this.proId = proId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public BigDecimal getLogicScore() { return logicScore; }
    public void setLogicScore(BigDecimal logicScore) { this.logicScore = logicScore; }

    public BigDecimal getProfessionalScore() { return professionalScore; }
    public void setProfessionalScore(BigDecimal professionalScore) { this.professionalScore = professionalScore; }

    public BigDecimal getPresentFormatScore() { return presentFormatScore; }
    public void setPresentFormatScore(BigDecimal presentFormatScore) { this.presentFormatScore = presentFormatScore; }

    public BigDecimal getExpressionScore() { return expressionScore; }
    public void setExpressionScore(BigDecimal expressionScore) { this.expressionScore = expressionScore; }

    public BigDecimal getAnswerScore() { return answerScore; }
    public void setAnswerScore(BigDecimal answerScore) { this.answerScore = answerScore; }

    public BigDecimal getMemberTimeScore() { return memberTimeScore; }
    public void setMemberTimeScore(BigDecimal memberTimeScore) { this.memberTimeScore = memberTimeScore; }

    public String getTopicType() { return topicType; }
    public void setTopicType(String topicType) { this.topicType = topicType; }

    public String getAppraiseSum() { return appraiseSum; }
    public void setAppraiseSum(String appraiseSum) { this.appraiseSum = appraiseSum; }

    public Integer getScoreOver() { return scoreOver; }
    public void setScoreOver(Integer scoreOver) { this.scoreOver = scoreOver; }

    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }

    public Date getUpdated() { return updated; }
    public void setUpdated(Date updated) { this.updated = updated; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
