package com.bootdo.cpe.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.cpe.dao.QcExpertEliminateConfirmedDao;
import com.bootdo.cpe.domain.QcExpertEliminateConfirmedDO;
import com.bootdo.cpe.service.QcExpertEliminateConfirmedService;

@Service
public class QcExpertEliminateConfirmedServiceImpl implements QcExpertEliminateConfirmedService {
    @Autowired
    private QcExpertEliminateConfirmedDao qcExpertEliminateConfirmedDao;

    @Override
    public QcExpertEliminateConfirmedDO get(Integer id) {
        return qcExpertEliminateConfirmedDao.get(id);
    }

    @Override
    public List<QcExpertEliminateConfirmedDO> list(Map<String, Object> map) {
        return qcExpertEliminateConfirmedDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return qcExpertEliminateConfirmedDao.count(map);
    }

    @Override
    public int save(QcExpertEliminateConfirmedDO record) {
        return qcExpertEliminateConfirmedDao.save(record);
    }

    @Override
    public int batchSaveFromEliminate(Long expertUid, String taskId) {
        Map<String, Object> params = new HashMap<>();
        params.put("expertUid", expertUid);
        params.put("taskId", taskId);
        return qcExpertEliminateConfirmedDao.batchSaveFromEliminate(params);
    }
}
