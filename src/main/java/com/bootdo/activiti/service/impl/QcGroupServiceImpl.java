package com.bootdo.activiti.service.impl;

import com.bootdo.activiti.dao.QcGroupDao;
import com.bootdo.activiti.domain.QcGroupDO;
import com.bootdo.activiti.service.QcGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("qcGroupService")
public class QcGroupServiceImpl implements QcGroupService {
    @Autowired
    private QcGroupDao qcGroupDao;

    @Override
    public QcGroupDO get(String taskId,Integer groupid) {
        return qcGroupDao.get(taskId, groupid);
    }

    @Override
    public List<QcGroupDO> list(Map<String, Object> map) {
        return qcGroupDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return qcGroupDao.count(map);
    }

    @Override
    public int save(QcGroupDO qcGroup) {
        return qcGroupDao.save(qcGroup);
    }

    @Override
    public int update(QcGroupDO qcGroup) {
        return qcGroupDao.update(qcGroup);
    }

    @Override
    public int updatepro(QcGroupDO qcGroup) {
        return qcGroupDao.updatepro(qcGroup);
    }

    @Override
    public int remove(Integer groupid) {
        return qcGroupDao.remove(groupid);
    }

    @Override
    public int batchRemove(Integer[] groupids) {
        return qcGroupDao.batchRemove(groupids);
    }

    @Override
    public List<QcGroupDO> getGroupsByTaskId(String taskId) {
        return qcGroupDao.getGroupsByTaskId(taskId);
    }

    @Override
    public boolean isGroupNameExist(String taskId, String groupName) {
        return qcGroupDao.isGroupNameExist(taskId, groupName) > 0;
    }
}
