package com.bootdo.cpe.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author houzb
 * @Description
 * @create 2020-09-29 1:46
 */
@Mapper
public interface ProjectCommonDao {

    public String getProjectStat(int proId);

    long getProCreateUid(int proId);

    /**
     * 更新项目成果编码
     * @param proId
     * @param resultCode
     * @return
     */
    public int updateProResultCode(@Param("proId") int proId, @Param("resultCode") String resultCode);

    /**
     * 更新项目申报账号
     */
    public int updateProDeclareAccount(@Param("proId") int proId, @Param("declareAccount") String declareAccount);

    /** 勘察奖：是否有查新，仅允许 是/否；value 为 null 表示清空 */
    int updateExtSurverNovelty(@Param("proId") int proId, @Param("value") String value);

    String getExtSurverNovelty(@Param("proId") int proId);

    /**
     * 获取项目申报账号
     */
    public String getDeclareAccount(@Param("proId") int proId);

    public int removePros(String taskId);
    public int removeTask(String taskId);

    public int updateProGroupName(@Param("proId") int proId, @Param("groupName") String groupName);

}
