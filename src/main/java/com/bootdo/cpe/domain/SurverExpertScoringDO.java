package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 勘察奖专家打分明细表
 */
public class SurverExpertScoringDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskId;
    private Integer proId;
    private String proSubType;
    private Long expertUid;

    // 勘察项目、标准设计项目、软件项目共用字段
    private Integer technicalLevel;
    private Integer technicalDifficulty;
    private Integer technicalInnovation;
    private Integer economicBenefit;
    private Integer materialQuality;

    // 设计项目专用字段
    private Integer overallTechnicalLevel;
    private Integer difficultyInnovation;
    private Integer digitalDesignLevel;
    private Integer environmentSafety;
    private Integer designQuality;
    private Integer energySaving;
    private Integer greenConstruction;

    // 标准设计和软件项目专用字段
    private Integer promotability;

    // 总分
    private Integer totalScore;

    // 主评意见
    private String opinionGrade;
    private String opinionText;

    // 状态
    private Integer confirmed;
    private Date confirmedAt;

    // 审计字段
    private Date created;
    private Date updated;
    private Integer deleted;

    // Getters and Setters
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

    public Integer getTechnicalLevel() { return technicalLevel; }
    public void setTechnicalLevel(Integer technicalLevel) { this.technicalLevel = technicalLevel; }

    public Integer getTechnicalDifficulty() { return technicalDifficulty; }
    public void setTechnicalDifficulty(Integer technicalDifficulty) { this.technicalDifficulty = technicalDifficulty; }

    public Integer getTechnicalInnovation() { return technicalInnovation; }
    public void setTechnicalInnovation(Integer technicalInnovation) { this.technicalInnovation = technicalInnovation; }

    public Integer getEconomicBenefit() { return economicBenefit; }
    public void setEconomicBenefit(Integer economicBenefit) { this.economicBenefit = economicBenefit; }

    public Integer getMaterialQuality() { return materialQuality; }
    public void setMaterialQuality(Integer materialQuality) { this.materialQuality = materialQuality; }

    public Integer getOverallTechnicalLevel() { return overallTechnicalLevel; }
    public void setOverallTechnicalLevel(Integer overallTechnicalLevel) { this.overallTechnicalLevel = overallTechnicalLevel; }

    public Integer getDifficultyInnovation() { return difficultyInnovation; }
    public void setDifficultyInnovation(Integer difficultyInnovation) { this.difficultyInnovation = difficultyInnovation; }

    public Integer getDigitalDesignLevel() { return digitalDesignLevel; }
    public void setDigitalDesignLevel(Integer digitalDesignLevel) { this.digitalDesignLevel = digitalDesignLevel; }

    public Integer getEnvironmentSafety() { return environmentSafety; }
    public void setEnvironmentSafety(Integer environmentSafety) { this.environmentSafety = environmentSafety; }

    public Integer getDesignQuality() { return designQuality; }
    public void setDesignQuality(Integer designQuality) { this.designQuality = designQuality; }

    public Integer getEnergySaving() { return energySaving; }
    public void setEnergySaving(Integer energySaving) { this.energySaving = energySaving; }

    public Integer getGreenConstruction() { return greenConstruction; }
    public void setGreenConstruction(Integer greenConstruction) { this.greenConstruction = greenConstruction; }

    public Integer getPromotability() { return promotability; }
    public void setPromotability(Integer promotability) { this.promotability = promotability; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public String getOpinionGrade() { return opinionGrade; }
    public void setOpinionGrade(String opinionGrade) { this.opinionGrade = opinionGrade; }

    public String getOpinionText() { return opinionText; }
    public void setOpinionText(String opinionText) { this.opinionText = opinionText; }

    public Integer getConfirmed() { return confirmed; }
    public void setConfirmed(Integer confirmed) { this.confirmed = confirmed; }

    public Date getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Date confirmedAt) { this.confirmedAt = confirmedAt; }

    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }

    public Date getUpdated() { return updated; }
    public void setUpdated(Date updated) { this.updated = updated; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}