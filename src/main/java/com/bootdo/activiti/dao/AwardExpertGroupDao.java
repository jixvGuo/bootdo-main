package com.bootdo.activiti.dao;

import com.bootdo.activiti.domain.AwardExpertGroupDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 专家分组 DAO（独立于 QcGroupDao/分组管理）
 */
public interface AwardExpertGroupDao {
    AwardExpertGroupDO get(@Param("taskId") String taskId, @Param("groupid") Integer groupid);

    List<AwardExpertGroupDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(AwardExpertGroupDO group);

    int update(AwardExpertGroupDO group);

    int remove(@Param("taskId") String taskId, @Param("groupid") Integer groupid);

    int batchRemove(@Param("taskId") String taskId, @Param("groupids") Integer[] groupids);

    List<AwardExpertGroupDO> getGroupsByTaskId(@Param("taskId") String taskId);

    int isGroupNameExist(@Param("taskId") String taskId, @Param("groupName") String groupName);

    AwardExpertGroupDO isGroupNameExistExcludeGroupId(@Param("taskId") String taskId,
                                                     @Param("groupName") String groupName,
                                                     @Param("groupId") Integer groupId);

    /**
     * 课题与专家分组关联（ass_award_pro_expert_group）
     */
    int upsertProGroup(@Param("taskId") String taskId,
                       @Param("proId") Integer proId,
                       @Param("groupId") Integer groupId);

    int removeProGroupByGroup(@Param("taskId") String taskId, @Param("groupId") Integer groupId);

    int removeProGroupByGroups(@Param("taskId") String taskId, @Param("groupIds") Integer[] groupIds);

    int countProByGroupId(@Param("taskId") String taskId, @Param("groupId") Integer groupId);

    Integer getProGroupId(@Param("taskId") String taskId, @Param("proId") Integer proId);

    /**
     * 一次性查询某任务下所有课题的专家分组归属（避免 N+1）
     * 返回每条：{ proid, groupid, name }
     */
    List<Map<String, Object>> listProAssignments(@Param("taskId") String taskId);
}
