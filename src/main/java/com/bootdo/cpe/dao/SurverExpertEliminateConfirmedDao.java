package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.SurverExpertEliminateConfirmedDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖-专家淘汰评级 快照表 DAO（提交后只读）
 */
@Mapper
public interface SurverExpertEliminateConfirmedDao {

    SurverExpertEliminateConfirmedDO get(Long id);

    List<SurverExpertEliminateConfirmedDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(SurverExpertEliminateConfirmedDO d);

    int remove(Long id);

    /**
     * 一次性把活动表里某专家在某任务下的全部有效记录复制到快照表
     * 实现细节看 mapper xml
     */
    int copyFromActiveByExpertAndTask(@Param("expertUid") Long expertUid,
                                      @Param("taskId") String taskId);

    /**
     * 删除某专家在某任务下的所有快照（撤销提交时使用，目前流程不开放给前端）
     */
    int deleteByExpertAndTask(@Param("expertUid") Long expertUid,
                              @Param("taskId") String taskId);
}
