package com.bootdo.activiti.service;

import com.bootdo.activiti.domain.QcGroupDO;

import java.util.List;
import java.util.Map;

public interface QcGroupService {
    QcGroupDO get(Integer groupid);

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
    List<QcGroupDO> getGroupsByTaskId(String taskId);

    /**
     * 检查分组名是否已存在
     * @param taskId
     * @param groupName
     * @return
     */
    boolean isGroupNameExist(String taskId, String groupName);
}
