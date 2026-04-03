package com.bootdo.cpe.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * QC专家淘汰记录表
 */
public class QcExpertEliminateDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Long expertUid;
    private Integer proId;
    private String taskId;
    private String reason;
    private String prevProStat;
    private Date created;
    private Integer deleted;

    // 以下为关联查询字段（非表字段）
    private String topicName;
    private String groupName;
    private String companyName;
    private String proCode;
    private String expertName;
    private String qcGroupName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getExpertUid() {
        return expertUid;
    }

    public void setExpertUid(Long expertUid) {
        this.expertUid = expertUid;
    }

    public Integer getProId() {
        return proId;
    }

    public void setProId(Integer proId) {
        this.proId = proId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPrevProStat() {
        return prevProStat;
    }

    public void setPrevProStat(String prevProStat) {
        this.prevProStat = prevProStat;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getProCode() {
        return proCode;
    }

    public void setProCode(String proCode) {
        this.proCode = proCode;
    }

    public String getExpertName() {
        return expertName;
    }

    public void setExpertName(String expertName) {
        this.expertName = expertName;
    }

    public String getQcGroupName() {
        return qcGroupName;
    }

    public void setQcGroupName(String qcGroupName) {
        this.qcGroupName = qcGroupName;
    }
}
