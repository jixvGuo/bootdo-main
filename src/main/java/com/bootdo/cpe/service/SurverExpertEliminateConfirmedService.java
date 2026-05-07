package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.SurverExpertEliminateConfirmedDO;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖-专家淘汰评级 快照表 Service（提交后只读）
 */
public interface SurverExpertEliminateConfirmedService {

    SurverExpertEliminateConfirmedDO get(Long id);

    List<SurverExpertEliminateConfirmedDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(SurverExpertEliminateConfirmedDO d);

    int remove(Long id);

    /**
     * 把活动表里 (expertUid, taskId, deleted=0) 全量复制到快照表。
     * 调用前应先确认本专家在该任务下尚未确认过（即 add_special_info.eliminate_over=0）。
     */
    int copyFromActiveByExpertAndTask(Long expertUid, String taskId);

    int deleteByExpertAndTask(Long expertUid, String taskId);
}
