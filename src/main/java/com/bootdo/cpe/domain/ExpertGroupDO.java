package com.bootdo.cpe.domain;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;


/***
 * 专家组管理
 * 连接数据库表 add_special_info 与 Java 业务逻辑
 * 贯穿了专家从“被分派”到“完成打分/淘汰确认”的全生命周期管理。
 *
  */
public class ExpertGroupDO implements Serializable {
    private static final long serialVersionUID = 1L;

    //
    private Integer id;
    // 提交ID
    private String proId;
    // 任务ID
    private String taskId;
    //
    private  String userId;
    //类型
    private  Integer  isGroupLeader;

    // 专业分组
    private  String groupName;
    /**
     * 工作单位
      */
    private String company;
    /**
     * 银行账号
     */
    private String bankAccount;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 登录的账号
     */
    private String loginAccount;
    /**
     * 专家名称
     */
    private String expertName;
    /**
     * 专家签名
     */
    private String  expertSignUrl;

    /**
     * 项目类型
     */
    private String proType;

    /**
     * 淘汰确认提交状态：0-未提交，1-已提交
     */
    private Integer eliminateOver;

    /**
     * 打分确认提交状态：0-未提交，1-已提交
     *
     * qc专家提交最终打分结果后，后端设置为1，禁用"提交最终打分结果"按钮
     * 连带 在提交后，评分输入框 也会被锁定为只读
     */
    private Integer scoreOver;

    /**
     * 新增：已分派的项目ID列表（用于页面回显，不存数据库）
     */
    private List<Integer> assignedProjectIds;

    public List<Integer> getAssignedProjectIds() {
        return assignedProjectIds;
    }

    public void setAssignedProjectIds(List<Integer> assignedProjectIds) {
        this.assignedProjectIds = assignedProjectIds;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        if(StringUtils.isNotBlank(groupName)) {
            groupName = groupName.trim();
        }
        this.groupName = groupName;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProId() {
        return proId;
    }

    public void setProId(String proId) {
        this.proId = proId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getIsGroupLeader() {
        return isGroupLeader;
    }

    public void setIsGroupLeader(Integer isGroupLeader) {
        this.isGroupLeader = isGroupLeader;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        if(StringUtils.isNotBlank(company)) {
            company = company.trim();
        }
        this.company = company;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        if(StringUtils.isNotBlank(bankAccount)) {
            bankAccount = bankAccount.trim();
        }
        this.bankAccount = bankAccount;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if(StringUtils.isNotBlank(phone)) {
            phone = phone.trim();
        }
        this.phone = phone;
    }

    public String getLoginAccount() {
        return loginAccount;
    }

    public void setLoginAccount(String loginAccount) {
        if(StringUtils.isNotBlank(loginAccount)) {
            loginAccount = loginAccount.trim();
        }
        this.loginAccount = loginAccount;
    }

    public String getExpertName() {
        return expertName;
    }

    public void setExpertName(String expertName) {
        if(StringUtils.isNotBlank(expertName)) {
            expertName = expertName.trim();
        }
        this.expertName = expertName;
    }

    public String getExpertSignUrl() {
        return expertSignUrl;
    }

    public void setExpertSignUrl(String expertSignUrl) {
        if(StringUtils.isNotBlank(expertSignUrl)) {
            expertSignUrl = expertSignUrl.trim();
        }
        this.expertSignUrl = expertSignUrl;
    }

    public Integer getEliminateOver() {
        return eliminateOver;
    }

    public void setEliminateOver(Integer eliminateOver) {
        this.eliminateOver = eliminateOver;
    }

    public Integer getScoreOver() {
        return scoreOver;
    }

    public void setScoreOver(Integer scoreOver) {
        this.scoreOver = scoreOver;
    }

    public String getProType() {
        return proType;
    }

    public void setProType(String proType) {
        this.proType = proType;
    }
}
