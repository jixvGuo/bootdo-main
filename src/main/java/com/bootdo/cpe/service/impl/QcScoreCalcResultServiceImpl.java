package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.QcScoreCalcResultDao;
import com.bootdo.cpe.domain.QcScoreCalcResultDO;
import com.bootdo.cpe.service.QcScoreCalcResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * {@link QcScoreCalcResultService} 的默认实现
 * 委托 {@link QcScoreCalcResultDao} 完成对 ass_qc_score_calc_result 的增删改查及幂等 upsert
 */
@Service
public class QcScoreCalcResultServiceImpl implements QcScoreCalcResultService {

    @Autowired
    private QcScoreCalcResultDao qcScoreCalcResultDao;

    @Override
    public QcScoreCalcResultDO get(Integer id) {
        return qcScoreCalcResultDao.get(id);
    }

    @Override
    public List<QcScoreCalcResultDO> list(Map<String, Object> map) {
        return qcScoreCalcResultDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return qcScoreCalcResultDao.count(map);
    }

    @Override
    public int save(QcScoreCalcResultDO result) {
        return qcScoreCalcResultDao.save(result);
    }

    @Override
    public int upsert(QcScoreCalcResultDO result) {
        return qcScoreCalcResultDao.upsert(result);
    }

    @Override
    public int update(QcScoreCalcResultDO result) {
        return qcScoreCalcResultDao.update(result);
    }

    @Override
    public int remove(Integer id) {
        return qcScoreCalcResultDao.remove(id);
    }

    @Override
    public int batchRemove(Integer[] ids) {
        return qcScoreCalcResultDao.batchRemove(ids);
    }
}
