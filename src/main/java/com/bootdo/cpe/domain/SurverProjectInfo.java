package com.bootdo.cpe.domain;

import com.bootdo.cpe.dto.QcBaseProjectInfoDO;
import com.bootdo.cpe.utils.AwardSurverSubTypeEnum;

/**
 * 勘察奖项目信息
 *
 * @author houzb
 * @version 1.0
 * @date 2022-03-28 22:32
 */
public class SurverProjectInfo extends QcBaseProjectInfoDO {
    private int id;
    private String proCode;
    /**
     * 项目id
     */
    private int proId;
    /**
     * 项目编号
     */
    private String proNum;
    /**
     * 项目类型
     */
    private String proType;
    /**
     * 项目子类型
     */
    private String proSubType;
    /**
     * 子类型名称
     */
    private String proSubTypeStr;
    /**
     * 项目名称
     */
    private String proName;
    /**
     * 申请单位
     */
    private String applyCompany;
    /**
     * 专业
     */
    private String major;

    /**
     * 申报账号（项目独立字段）
     */
    private String declareAccount;

    /**
     * 人员名单（贡献主要人员逗号拼接）
     */
    private String memberList;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProCode() {
        return proCode;
    }

    public void setProCode(String proCode) {
        this.proCode = proCode;
    }

    public int getProId() {
        return proId;
    }

    public void setProId(int proId) {
        this.proId = proId;
    }

    public String getProNum() {
        return proNum;
    }

    public void setProNum(String proNum) {
        this.proNum = proNum;
    }

    public String getProType() {
        return proType;
    }

    public void setProType(String proType) {
        this.proType = proType;
    }

    public String getProSubType() {
        return proSubType;
    }

    public void setProSubType(String proSubType) {
        this.proSubType = proSubType;
        AwardSurverSubTypeEnum subTypeEnum = AwardSurverSubTypeEnum.getSubTypeEnum(this.proSubType);
        this.proSubTypeStr = subTypeEnum != null ? subTypeEnum.getDesc() : "暂无";
    }

    public String getProSubTypeStr() {
        return proSubTypeStr;
    }

    public void setProSubTypeStr(String proSubTypeStr) {
        this.proSubTypeStr = proSubTypeStr;
    }

    public String getProName() {
        return proName;
    }

    public void setProName(String proName) {
        this.proName = proName;
    }

    public String getApplyCompany() {
        return applyCompany;
    }

    public void setApplyCompany(String applyCompany) {
        this.applyCompany = applyCompany;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getDeclareAccount() {
        return declareAccount;
    }

    public void setDeclareAccount(String declareAccount) {
        this.declareAccount = declareAccount;
    }

    public String getMemberList() {
        return memberList;
    }

    public void setMemberList(String memberList) {
        this.memberList = memberList;
    }

}
