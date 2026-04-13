package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.QcScoreCalcDetailDao;
import com.bootdo.cpe.domain.QcScoreCalcDetailDO;
import com.bootdo.cpe.service.QcScoreCalcDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * {@link QcScoreCalcDetailService} 的默认实现
 * 委托 {@link QcScoreCalcDetailDao} 完成对 ass_qc_score_calc_detail 的增删改查
 */
@Service
public class QcScoreCalcDetailServiceImpl implements QcScoreCalcDetailService {

    @Autowired
    private QcScoreCalcDetailDao qcScoreCalcDetailDao;

    @Override
    public QcScoreCalcDetailDO get(Integer id) {
        return qcScoreCalcDetailDao.get(id);
    }

    @Override
    public List<QcScoreCalcDetailDO> list(Map<String, Object> map) {
        return qcScoreCalcDetailDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return qcScoreCalcDetailDao.count(map);
    }

    @Override
    public int save(QcScoreCalcDetailDO detail) {
        return qcScoreCalcDetailDao.save(detail);
    }

    @Override
    public int batchSave(List<QcScoreCalcDetailDO> list) {
        return qcScoreCalcDetailDao.batchSave(list);
    }

    @Override
    public int update(QcScoreCalcDetailDO detail) {
        return qcScoreCalcDetailDao.update(detail);
    }

    @Override
    public int remove(Integer id) {
        return qcScoreCalcDetailDao.remove(id);
    }

    @Override
    public int batchRemove(Integer[] ids) {
        return qcScoreCalcDetailDao.batchRemove(ids);
    }

    @Override
    public int deleteByResultId(Integer resultId) {
        return qcScoreCalcDetailDao.deleteByResultId(resultId);
    }
}
