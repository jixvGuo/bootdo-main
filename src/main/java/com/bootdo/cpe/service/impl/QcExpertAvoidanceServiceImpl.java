package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.QcExpertAvoidanceDao;
import com.bootdo.cpe.dao.QcGroupApplyInfoDao;
import com.bootdo.cpe.domain.QcExpertAvoidanceDO;
import com.bootdo.cpe.domain.QcGroupApplyInfoDO;
import com.bootdo.cpe.service.QcExpertAvoidanceService;
import com.bootdo.system.dao.DeptDao;
import com.bootdo.system.dao.UserDao;
import com.bootdo.system.domain.DeptDO;
import com.bootdo.system.domain.UserDO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QC专家回避服务实现
 * 
 * @author system
 * @date 2026-03-27
 */
@Service
public class QcExpertAvoidanceServiceImpl implements QcExpertAvoidanceService {

    @Autowired
    private QcExpertAvoidanceDao avoidanceDao;

    @Autowired
    private QcGroupApplyInfoDao qcGroupApplyInfoDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private DeptDao deptDao;

    @Override
    public QcExpertAvoidanceDO get(Integer id) {
        return avoidanceDao.get(id);
    }

    @Override
    public List<QcExpertAvoidanceDO> list(Map<String, Object> map) {
        return avoidanceDao.list(map);
    }

    @Override
    public int count(Map<String, Object> map) {
        return avoidanceDao.count(map);
    }

    @Override
    public int save(QcExpertAvoidanceDO avoidance) {
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

    @Override
    @Transactional
    public int autoAvoidByCompany(String taskId, Integer expertUserId, String expertCompany) {
        if (StringUtils.isBlank(taskId) || expertUserId == null || StringUtils.isBlank(expertCompany)) {
            return 0;
        }

        // 标准化专家单位名称（去空格、统一大小写等）
        String normalizedExpertCompany = normalizeCompanyName(expertCompany);

        // 查询该任务下所有QC项目
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        List<QcGroupApplyInfoDO> projects = qcGroupApplyInfoDao.list(params);

        if (projects == null || projects.isEmpty()) {
            return 0;
        }

        List<QcExpertAvoidanceDO> avoidanceList = new ArrayList<>();

        for (QcGroupApplyInfoDO project : projects) {
            boolean shouldAvoid = false;
            StringBuilder reason = new StringBuilder("单位重叠：");

            // 检查1：与小组单位（完成单位）匹配
            if (StringUtils.isNotBlank(project.getUnitName())) {
                String normalizedProjectUnit = normalizeCompanyName(project.getUnitName());
                if (isSameCompany(normalizedExpertCompany, normalizedProjectUnit)) {
                    shouldAvoid = true;
                    reason.append("完成单位[").append(project.getUnitName()).append("]");
                }
            }

            // 检查2：与申报人单位匹配
            if (project.getOptUid() != null) {
                UserDO applicant = userDao.get(Long.valueOf(project.getOptUid()));
                if (applicant != null && applicant.getDeptId() != null) {
                    DeptDO dept = deptDao.get(applicant.getDeptId());
                    if (dept != null && StringUtils.isNotBlank(dept.getName())) {
                        String normalizedApplicantDept = normalizeCompanyName(dept.getName());
                        if (isSameCompany(normalizedExpertCompany, normalizedApplicantDept)) {
                            if (shouldAvoid) {
                                reason.append("、");
                            }
                            shouldAvoid = true;
                            reason.append("申报单位[").append(dept.getName()).append("]");
                        }
                    }
                }
            }

            // 如果需要回避，插入回避记录
            if (shouldAvoid) {
                // 检查是否已存在回避记录
                int existing = avoidanceDao.checkAvoidance(taskId, project.getProId(), expertUserId);
                if (existing == 0) {
                    QcExpertAvoidanceDO avoidance = new QcExpertAvoidanceDO();
                    avoidance.setTaskId(taskId);
                    avoidance.setProId(project.getProId());
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
            avoidanceDao.batchSave(avoidanceList);
        }

        return avoidanceList.size();
    }

    @Override
    @Transactional
    public boolean manualAvoid(String taskId, Integer proId, Integer expertUserId, Integer createdBy, String reason) {
        if (StringUtils.isBlank(taskId) || proId == null || expertUserId == null) {
            return false;
        }

        // 检查是否已存在回避记录
        int existing = avoidanceDao.checkAvoidance(taskId, proId, expertUserId);
        if (existing > 0) {
            return false; // 已存在，不重复插入
        }

        QcExpertAvoidanceDO avoidance = new QcExpertAvoidanceDO();
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

        List<QcExpertAvoidanceDO> list = avoidanceDao.list(params);
        if (list != null && !list.isEmpty()) {
            for (QcExpertAvoidanceDO avoidance : list) {
                avoidanceDao.remove(avoidance.getId());
            }
            return true;
        }

        return false;
    }

    /**
     * 标准化公司名称：去空格、转小写、去常见后缀
     */
    private String normalizeCompanyName(String companyName) {
        if (StringUtils.isBlank(companyName)) {
            return "";
        }

        String normalized = companyName.trim().toLowerCase();

        // 去除常见的公司后缀
        String[] suffixes = {"有限公司", "有限责任公司", "股份有限公司", "集团", 
                            "公司", "Ltd", "Co", "Inc", "Corporation", "Corp"};
        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix.toLowerCase())) {
                normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
            }
        }

        return normalized;
    }

    /**
     * 判断两个公司名称是否相同（支持部分匹配）
     */
    private boolean isSameCompany(String company1, String company2) {
        if (StringUtils.isBlank(company1) || StringUtils.isBlank(company2)) {
            return false;
        }

        // 完全相同
        if (company1.equals(company2)) {
            return true;
        }

        // 较长的名称包含较短的名称（防止误判）
        int minLength = 3; // 最短匹配长度
        if (company1.length() >= minLength && company2.length() >= minLength) {
            return company1.contains(company2) || company2.contains(company1);
        }

        return false;
    }
}
