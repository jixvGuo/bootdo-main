package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.SurverExpertScoringDao;
import com.bootdo.cpe.domain.SurverExpertScoringDO;
import com.bootdo.cpe.service.SurverExpertScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖专家打分Service实现
 */
@Service
public class SurverExpertScoringServiceImpl implements SurverExpertScoringService {

    @Autowired
    private SurverExpertScoringDao scoringDao;

    @Override
    public SurverExpertScoringDO get(Long id) {
        return scoringDao.get(id);
    }

    @Override
    public SurverExpertScoringDO getByTaskProExpert(String taskId, Integer proId, Long expertUid) {
        return scoringDao.getByTaskProExpert(taskId, proId, expertUid);
    }

    @Override
    public List<SurverExpertScoringDO> list(Map<String, Object> params) {
        return scoringDao.list(params);
    }

    @Override
    public int count(Map<String, Object> params) {
        return scoringDao.count(params);
    }

    @Override
    public int save(SurverExpertScoringDO scoring) {
        return scoringDao.save(scoring);
    }

    @Override
    public int update(SurverExpertScoringDO scoring) {
        return scoringDao.update(scoring);
    }

    @Override
    public int remove(Long id) {
        return scoringDao.remove(id);
    }

    @Override
    public int batchRemove(Long[] ids) {
        return scoringDao.batchRemove(ids);
    }

    @Override
    public int confirmScoring(String taskId, Long expertUid) {
        return scoringDao.confirmScoring(taskId, expertUid);
    }

    @Override
    public boolean isConfirmed(String taskId, Long expertUid) {
        return scoringDao.checkConfirmed(taskId, expertUid) > 0;
    }

    @Override
    public int resetConfirmStatus(String taskId, Long expertUid) {
        return scoringDao.resetConfirmStatus(taskId, expertUid);
    }

    @Override
    public List<Map<String, Object>> listForExport(Map<String, Object> params) {
        return scoringDao.listForExport(params);
    }
}