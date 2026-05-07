package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.SurverExpertAvoidanceDao;
import com.bootdo.cpe.domain.SurverExpertAvoidanceDO;
import com.bootdo.cpe.service.SurverExpertAvoidanceService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 勘察奖-专家回避服务实现
 *
 * 大部分方法 1:1 镜像 {@link com.bootdo.cpe.service.impl.QcExpertAvoidanceServiceImpl}。
 * 区别：autoAvoidByCompany 暂为 stub，将在 Phase C（专家打分页）落地时按 4 张申报子表
 * 各自的"完成单位 / 申报单位"字段补全。
 */
@Service
public class SurverExpertAvoidanceServiceImpl implements SurverExpertAvoidanceService {

    @Autowired
    private SurverExpertAvoidanceDao avoidanceDao;

    @Override
    public SurverExpertAvoidanceDO get(Integer id) {
        return avoidanceDao.get(id);
    }

    @Override
    public List<SurverExpertAvoidanceDO> list(Map<String, Object> map) {
        return avoidanceDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return avoidanceDao.count(map);
    }

    @Override
    public int save(SurverExpertAvoidanceDO avoidance) {
        return avoidanceDao.save(avoidance);
    }

    @Override
    public int remove(Integer id) {
        return avoidanceDao.remove(id);
    }

    @Override
    public int batchRemove(Integer[] ids) {
        return avoidanceDao.batchRemove(ids);
    }

    @Override
    public boolean checkAvoidance(String taskId, Integer proId, Integer expertUserId) {
        return avoidanceDao.checkAvoidance(taskId, proId, expertUserId) > 0;
    }

    @Override
    public List<Integer> getAvoidedProIds(String taskId, Integer expertUserId) {
        return avoidanceDao.getAvoidedProIds(taskId, expertUserId);
    }

    @Override
    public List<Integer> getAvoidedExpertIds(String taskId, Integer proId) {
        return avoidanceDao.getAvoidedExpertIds(taskId, proId);
    }

    /**
     * Phase A: stub — 仅记录入参合法性校验，实际"按单位匹配 4 张子表"逻辑在 Phase C 落地
     * 落地参考: QcExpertAvoidanceServiceImpl#autoAvoidByCompany
     */
    @Override
    @Transactional
    public int autoAvoidByCompany(String taskId, Integer expertUserId, String expertCompany) {
        if (StringUtils.isBlank(taskId) || expertUserId == null || StringUtils.isBlank(expertCompany)) {
            return 0;
        }
        // TODO Phase C: 遍历 4 张申报子表(excellent/design/soft/standard),
        //               按完成单位 + 申报人单位 与 expertCompany 比对, 命中则 batchSave。
        return 0;
    }

    @Override
    @Transactional
    public boolean manualAvoid(String taskId, Integer proId, Integer expertUserId, Integer createdBy, String reason) {
        if (StringUtils.isBlank(taskId) || proId == null || expertUserId == null) {
            return false;
        }
        int existing = avoidanceDao.checkAvoidance(taskId, proId, expertUserId);
        if (existing > 0) {
            return false;
        }
        SurverExpertAvoidanceDO avoidance = new SurverExpertAvoidanceDO();
        avoidance.setTaskId(taskId);
        avoidance.setProId(proId);
        avoidance.setExpertUserId(expertUserId);
        avoidance.setAvoidanceType("manual");
        avoidance.setAvoidanceReason(StringUtils.isBlank(reason) ? "手动回避" : reason);
        avoidance.setCreatedBy(createdBy);
        return avoidanceDao.save(avoidance) > 0;
    }

    @Override
    @Transactional
    public boolean cancelAvoidance(String taskId, Integer proId, Integer expertUserId) {
        if (StringUtils.isBlank(taskId) || proId == null || expertUserId == null) {
            return false;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("proId", proId);
        params.put("expertUserId", expertUserId);
        List<SurverExpertAvoidanceDO> list = avoidanceDao.list(params);
        if (list != null && !list.isEmpty()) {
            for (SurverExpertAvoidanceDO avoidance : list) {
                avoidanceDao.remove(avoidance.getId());
            }
            return true;
        }
        return false;
    }
}
