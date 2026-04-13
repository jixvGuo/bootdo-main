package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.QcScoreSubmitDao;
import com.bootdo.cpe.domain.QcScoreSubmitDO;
import com.bootdo.cpe.service.QcScoreSubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * {@link QcScoreSubmitService} 的默认实现
 * 委托 {@link QcScoreSubmitDao} 完成对 ass_qc_score_submit 的幂等快照写入及查询
 */
@Service
public class QcScoreSubmitServiceImpl implements QcScoreSubmitService {

    @Autowired
    private QcScoreSubmitDao qcScoreSubmitDao;

    @Override
    public QcScoreSubmitDO get(Integer id) {
        return qcScoreSubmitDao.get(id);
    }

    @Override
    public List<QcScoreSubmitDO> list(Map<String, Object> map) {
        return qcScoreSubmitDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return qcScoreSubmitDao.count(map);
    }

    @Override
    public int saveIgnore(QcScoreSubmitDO submit) {
        return qcScoreSubmitDao.saveIgnore(submit);
    }

    @Override
    public int remove(Integer id) {
        return qcScoreSubmitDao.remove(id);
    }
}
