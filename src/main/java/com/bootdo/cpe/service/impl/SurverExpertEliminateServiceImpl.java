package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.SurverExpertEliminateDao;
import com.bootdo.cpe.domain.SurverExpertEliminateDO;
import com.bootdo.cpe.service.SurverExpertEliminateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SurverExpertEliminateServiceImpl implements SurverExpertEliminateService {

    @Autowired
    private SurverExpertEliminateDao dao;

    @Override
    public SurverExpertEliminateDO get(Long id) { return dao.get(id); }

    @Override
    public List<SurverExpertEliminateDO> list(Map<String, Object> map) { return dao.list(map); }

    @Override
    public int count(Map<String, Object> map) { return dao.count(map); }

    @Override
    public int save(SurverExpertEliminateDO d) { return dao.save(d); }

    @Override
    public int update(SurverExpertEliminateDO d) { return dao.update(d); }

    @Override
    public int remove(Long id) { return dao.remove(id); }

    @Override
    public SurverExpertEliminateDO getByUnique(String taskId, Integer proId, Long expertUid) {
        return dao.getByUnique(taskId, proId, expertUid);
    }

    @Override
    public Long saveOrUpdateGrade(SurverExpertEliminateDO d) {
        SurverExpertEliminateDO exist = dao.getByUnique(d.getTaskId(), d.getProId(), d.getExpertUid());
        if (exist != null) {
            d.setId(exist.getId());
            // 仅更新可变字段
            dao.update(d);
            return exist.getId();
        }
        dao.save(d);
        return d.getId();
    }

    @Override
    public int batchSoftDeleteByTaskId(String taskId) {
        return dao.batchSoftDeleteByTaskId(taskId);
    }

    // ============================================================
    // Phase B - 管理员淘汰管理
    // ============================================================

    @Override
    public List<Map<String, Object>> aggregateCandidates(String taskId, String proSubType) {
        return dao.aggregateCandidates(taskId, proSubType);
    }

    @Override
    public int updateEliminatedBySubType(String proSubType, Integer proId, Integer eliminated) {
        return dao.updateEliminatedBySubType(proSubType, proId, eliminated);
    }

    @Override
    public int insertMinimalIfNotExists(String proSubType, Integer proId, Integer eliminated) {
        return dao.insertMinimalIfNotExists(proSubType, proId, eliminated);
    }

    @Override
    public List<Map<String, Object>> listConfirmedEliminated(String taskId) {
        return dao.listConfirmedEliminated(taskId);
    }

    @Override
    public int countAssignedProjects(String taskId, Long uid) {
        return dao.countAssignedProjects(taskId, uid);
    }

    @Override
    public Map<String, Object> getProjectSnapshotInfo(Integer proId) {
        return dao.getProjectSnapshotInfo(proId);
    }

    @Override
    public Map<String, Object> findProInfoByProCode(String taskId, String proCode) {
        return dao.findProInfoByProCode(taskId, proCode);
    }
}
