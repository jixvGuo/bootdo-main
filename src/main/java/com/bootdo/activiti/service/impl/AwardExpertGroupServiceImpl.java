package com.bootdo.activiti.service.impl;

import com.bootdo.activiti.dao.AwardExpertGroupDao;
import com.bootdo.activiti.domain.AwardExpertGroupDO;
import com.bootdo.activiti.service.AwardExpertGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("awardExpertGroupService")
public class AwardExpertGroupServiceImpl implements AwardExpertGroupService {
    @Autowired
    private AwardExpertGroupDao awardExpertGroupDao;

    @Override
    public AwardExpertGroupDO get(String taskId, Integer groupid) {
        return awardExpertGroupDao.get(taskId, groupid);
    }

    @Override
    public List<AwardExpertGroupDO> list(Map<String, Object> map) {
        return awardExpertGroupDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return awardExpertGroupDao.count(map);
    }

    @Override
    public int save(AwardExpertGroupDO group) {
        return awardExpertGroupDao.save(group);
    }

    @Override
    public int update(AwardExpertGroupDO group) {
        return awardExpertGroupDao.update(group);
    }

    @Override
    public int remove(String taskId, Integer groupid) {
        // 先清理关联表，再删除分组
        awardExpertGroupDao.removeProGroupByGroup(taskId, groupid);
        return awardExpertGroupDao.remove(taskId, groupid);
    }

    @Override
    public int batchRemove(String taskId, Integer[] groupids) {
        if (groupids == null || groupids.length == 0) {
            return 0;
        }
        awardExpertGroupDao.removeProGroupByGroups(taskId, groupids);
        return awardExpertGroupDao.batchRemove(taskId, groupids);
    }

    @Override
    public List<AwardExpertGroupDO> getGroupsByTaskId(String taskId) {
        return awardExpertGroupDao.getGroupsByTaskId(taskId);
    }

    @Override
    public boolean isGroupNameExist(String taskId, String groupName) {
        return awardExpertGroupDao.isGroupNameExist(taskId, groupName) > 0;
    }

    @Override
    public AwardExpertGroupDO isGroupNameExistExcludeGroupId(String taskId, String groupName, Integer groupId) {
        return awardExpertGroupDao.isGroupNameExistExcludeGroupId(taskId, groupName, groupId);
    }

    @Override
    public int assignProToGroup(String taskId, Integer proId, Integer groupId) {
        return awardExpertGroupDao.upsertProGroup(taskId, proId, groupId);
    }

    @Override
    public int countProByGroupId(String taskId, Integer groupId) {
        return awardExpertGroupDao.countProByGroupId(taskId, groupId);
    }

    @Override
    public Integer getProGroupId(String taskId, Integer proId) {
        return awardExpertGroupDao.getProGroupId(taskId, proId);
    }

    @Override
    public List<Map<String, Object>> listProAssignments(String taskId) {
        return awardExpertGroupDao.listProAssignments(taskId);
    }
}
