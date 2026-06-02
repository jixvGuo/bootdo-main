package com.bootdo.cpe.service.impl;

import com.bootdo.activiti.service.AwardEnterpriseProjectService;
import com.bootdo.cpe.dao.SurverExpertAvoidanceDao;
import com.bootdo.cpe.domain.SurverExpertAvoidanceDO;
import com.bootdo.cpe.service.SurverExpertAvoidanceService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Autowired
    private AwardEnterpriseProjectService awardEnterpriseProjectService;


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
        //Phase C: 遍历 4 张申报子表(excellent/design/soft/standard),
        //               按完成单位 + 申报人单位 与 expertCompany 比对, 命中则 batchSave。

        // 标准化专家单位名称
        String normalizedExpertCompany = normalizeCompanyName(expertCompany);

        // 查询该任务下所有勘察奖项目（4种子类型）
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);

        // 获取所有子类型的项目
        List<Map<String, Object>> projects = awardEnterpriseProjectService.listSurverProjects(params);

        if (projects == null || projects.isEmpty()) {
            return 0;
        }

        List<SurverExpertAvoidanceDO> avoidanceList = new ArrayList<>();

        for (Map<String, Object> project : projects) {
            boolean shouldAvoid = false;
            StringBuilder reason = new StringBuilder("单位重叠：");

            // 检查：与申报单位完全匹配（SQL返回的字段名是 companyName）
            String companyName = (String) project.get("companyName");
            if (StringUtils.isNotBlank(companyName)) {
                String normalizedUnit = normalizeCompanyName(companyName);
                if (isSameCompany(normalizedExpertCompany, normalizedUnit)) {
                    shouldAvoid = true;
                    reason.append("申报单位[").append(companyName).append("]");
                }
            }

            // 如果需要回避，插入回避记录
            if (shouldAvoid) {
                Integer proId = (Integer) project.get("proId");
                // 检查是否已存在回避记录
                int existing = avoidanceDao.checkAvoidance(taskId, proId, expertUserId);
                if (existing == 0) {
                    SurverExpertAvoidanceDO avoidance = new SurverExpertAvoidanceDO();
                    avoidance.setTaskId(taskId);
                    avoidance.setProId(proId);
                    avoidance.setExpertUserId(expertUserId);
                    avoidance.setAvoidanceType("auto");
                    avoidance.setAvoidanceReason(reason.toString());
                    avoidance.setCreatedBy(expertUserId);
                    avoidanceList.add(avoidance);
                }
            }
        }

        // 批量插入回避记录
        if (!avoidanceList.isEmpty()) {
            for (SurverExpertAvoidanceDO avoidance : avoidanceList) {
                avoidanceDao.save(avoidance);
            }
        }

        return avoidanceList.size();
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
        avoidance.setCreatedBy(expertUserId);
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


    /**
     * 标准化公司名称：去空格、转小写
     */
    private String normalizeCompanyName(String companyName) {
        if (StringUtils.isBlank(companyName)) {
            return "";
        }
        return companyName.trim().toLowerCase();
    }

    /**
     * 判断两个公司名称是否相同（完全匹配）
     */
    private boolean isSameCompany(String company1, String company2) {
        if (StringUtils.isBlank(company1) || StringUtils.isBlank(company2)) {
            return false;
        }

        // 完全相同（标准化后比较）
        return company1.equals(company2);
    }
}
