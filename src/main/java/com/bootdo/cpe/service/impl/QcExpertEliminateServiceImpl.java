package com.bootdo.cpe.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.cpe.dao.QcExpertEliminateDao;
import com.bootdo.cpe.domain.QcExpertEliminateDO;
import com.bootdo.cpe.service.QcExpertEliminateService;

@Service
public class QcExpertEliminateServiceImpl implements QcExpertEliminateService {
    @Autowired
    private QcExpertEliminateDao qcExpertEliminateDao;

    @Override
    public QcExpertEliminateDO get(Integer id) {
        return qcExpertEliminateDao.get(id);
    }

    @Override
    public List<QcExpertEliminateDO> list(Map<String, Object> map) {
        return qcExpertEliminateDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return qcExpertEliminateDao.count(map);
    }

    @Override
    public int save(QcExpertEliminateDO qcExpertEliminate) {
        return qcExpertEliminateDao.save(qcExpertEliminate);
    }

    @Override
    public int update(QcExpertEliminateDO qcExpertEliminate) {
        return qcExpertEliminateDao.update(qcExpertEliminate);
    }

    @Override
    public int remove(Integer id) {
        return qcExpertEliminateDao.remove(id);
    }

    @Override
    public int batchSoftDeleteByTaskId(String taskId) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        return qcExpertEliminateDao.batchSoftDeleteByTaskId(params);
    }
}
