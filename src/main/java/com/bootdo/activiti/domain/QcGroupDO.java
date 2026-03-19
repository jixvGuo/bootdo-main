package com.bootdo.activiti.domain;

import java.io.Serializable;

public class QcGroupDO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskid;
    private int groupid;
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
