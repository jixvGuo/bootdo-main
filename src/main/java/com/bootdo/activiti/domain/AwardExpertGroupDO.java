package com.bootdo.activiti.domain;

import java.io.Serializable;

/**
 * 专家分组（与已有的 QcGroupDO/分组管理 不是同一个功能点）
 * 表：ass_award_expert_group
 */
public class AwardExpertGroupDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskid;
    private Integer groupid;
    private String name;

    public String getTaskid() {
        return taskid;
    }

    public void setTaskid(String taskid) {
        this.taskid = taskid;
    }

    public Integer getGroupid() {
        return groupid;
    }

    public void setGroupid(Integer groupid) {
        this.groupid = groupid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
