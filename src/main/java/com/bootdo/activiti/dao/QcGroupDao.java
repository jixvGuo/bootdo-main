package com.bootdo.activiti.dao;

import com.bootdo.activiti.domain.QcGroupDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface QcGroupDao {
    QcGroupDO get(@Param("taskId") String taskId, @Param("groupid") Integer groupid);

    List<QcGroupDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(QcGroupDO qcGroup);

    int update(QcGroupDO qcGroup);

    int updatepro(QcGroupDO qcGroup);

    int remove(Integer groupid);

    int batchRemove(Integer[] groupids);

    /**
     * 根据任务 id 获取分组列表
     * @param taskId
     * @return
     */
    List<QcGroupDO> getGroupsByTaskId(@Param("taskId") String taskId);

    /**
     * 检查分组名是否已存在
     * @param taskId
     * @param groupName
     * @return
     */
    int isGroupNameExist(@Param("taskId") String taskId, @Param("groupName") String groupName);

    /**
     * 检查分组名是否已存在（排除当前分组）
     * @param taskId
     * @param groupName
     * @param groupId
     * @return
     */
    QcGroupDO isGroupNameExistExcludeGroupId(@Param("taskId") String taskId, @Param("groupName") String groupName, @Param("groupId") Integer groupId);


}
