package com.bootdo.activiti.service;

import com.bootdo.activiti.domain.AwardExpertGroupDO;

import java.util.List;
import java.util.Map;

/**
 * 专家分组服务（独立于 QcGroupService/分组管理）
 */
public interface AwardExpertGroupService {
    AwardExpertGroupDO get(String taskId, Integer groupid);

    List<AwardExpertGroupDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(AwardExpertGroupDO group);

    int update(AwardExpertGroupDO group);

    int remove(String taskId, Integer groupid);

    int batchRemove(String taskId, Integer[] groupids);

    List<AwardExpertGroupDO> getGroupsByTaskId(String taskId);

    boolean isGroupNameExist(String taskId, String groupName);

    AwardExpertGroupDO isGroupNameExistExcludeGroupId(String taskId, String groupName, Integer groupId);

    int assignProToGroup(String taskId, Integer proId, Integer groupId);

    int countProByGroupId(String taskId, Integer groupId);

    Integer getProGroupId(String taskId, Integer proId);

    List<Map<String, Object>> listProAssignments(String taskId);
}
