package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.SurverExpertScoringDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖专家打分DAO
 */
@Mapper
public interface SurverExpertScoringDao {

    SurverExpertScoringDO get(Long id);

    SurverExpertScoringDO getByTaskProExpert(@Param("taskId") String taskId,
                                              @Param("proId") Integer proId,
                                              @Param("expertUid") Long expertUid);

    List<SurverExpertScoringDO> list(Map<String, Object> params);

    int count(Map<String, Object> params);

    int save(SurverExpertScoringDO scoring);

    int update(SurverExpertScoringDO scoring);

    int remove(Long id);

    int batchRemove(Long[] ids);

    /**
     * 批量确认打分结果
     */
    int confirmScoring(@Param("taskId") String taskId,
                       @Param("expertUid") Long expertUid);

    /**
     * 检查是否已确认
     */
    int checkConfirmed(@Param("taskId") String taskId,
                       @Param("expertUid") Long expertUid);

    /**
     * 重置打分确认状态（管理员驳回专家的打分确认）
     */
    int resetConfirmStatus(@Param("taskId") String taskId,
                           @Param("expertUid") Long expertUid);

    /**
     * 导出分数查询：获取项目+专家打分+专家分组信息
     */
    List<Map<String, Object>> listForExport(Map<String, Object> params);
}