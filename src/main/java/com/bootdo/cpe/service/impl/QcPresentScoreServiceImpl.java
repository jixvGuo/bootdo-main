package com.bootdo.cpe.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.bootdo.cpe.dao.QcPresentScoreDao;
import com.bootdo.cpe.domain.QcPresentScoreDO;
import com.bootdo.cpe.service.QcPresentScoreService;

@Service
public class QcPresentScoreServiceImpl implements QcPresentScoreService {

    @Autowired
    private QcPresentScoreDao qcPresentScoreDao;

    @Override
    public QcPresentScoreDO get(Integer id) {
        return qcPresentScoreDao.get(id);
    }

    @Override
    public List<QcPresentScoreDO> list(Map<String, Object> map) {
        return qcPresentScoreDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return qcPresentScoreDao.count(map);
    }

    @Override
    public int save(QcPresentScoreDO qcPresentScore) {
        return qcPresentScoreDao.save(qcPresentScore);
    }

    @Override
    public int update(QcPresentScoreDO qcPresentScore) {
        return qcPresentScoreDao.update(qcPresentScore);
    }

    @Override
    public int remove(Integer id) {
        return qcPresentScoreDao.remove(id);
    }

    @Override
    public int batchRemove(Integer[] ids) {
        return qcPresentScoreDao.batchRemove(ids);
    }
}
