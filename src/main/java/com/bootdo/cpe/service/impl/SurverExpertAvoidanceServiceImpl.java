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
     * 自动回避（按专家单位 vs 申报单位）
     * 遍历任务下所有勘察奖项目，按完成单位与专家单位比对：
     * 1. 匹配则创建自动回避记录
     * 2. 不再匹配则删除之前的自动回避记录
     */
    @Override
    @Transactional
    public int autoAvoidByCompany(String taskId, Integer expertUserId, String expertCompany) {
        if (StringUtils.isBlank(taskId) || expertUserId == null || StringUtils.isBlank(expertCompany)) {
            return 0;
        }

        // 标准化专家单位名称
        String normalizedExpertCompany = normalizeCompanyName(expertCompany);
        System.out.println("[勘察奖自动回避] 开始检查：taskId=" + taskId + ", expertUserId=" + expertUserId + ", expertCompany=" + expertCompany + ", normalized=" + normalizedExpertCompany);

        // 查询该任务下所有勘察奖项目（4种子类型），使用专门的自动回避查询
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);

        // 获取所有子类型的项目
        List<Map<String, Object>> projects = awardEnterpriseProjectService.listAllSurverProjectsForAutoAvoid(params);

        if (projects == null || projects.isEmpty()) {
            System.out.println("[勘察奖自动回避] 未找到项目");
            return 0;
        }
        System.out.println("[勘察奖自动回避] 找到 " + projects.size() + " 个项目");

        // 第一步：获取当前专家的所有自动回避记录
        Map<String, Object> queryAutoAvoid = new HashMap<>();
        queryAutoAvoid.put("taskId", taskId);
        queryAutoAvoid.put("expertUserId", expertUserId);
        queryAutoAvoid.put("avoidanceType", "auto");
        List<SurverExpertAvoidanceDO> existingAutoAvoidances = avoidanceDao.list(queryAutoAvoid);
        System.out.println("[勘察奖自动回避] 已有自动回避记录 " + (existingAutoAvoidances != null ? existingAutoAvoidances.size() : 0) + " 条");

        // 构建项目ID到项目信息的映射
        Map<Integer, Map<String, Object>> projectMap = new HashMap<>();
        for (Map<String, Object> project : projects) {
            Integer proId = (Integer) project.get("proId");
            if (proId != null) {
                projectMap.put(proId, project);
            }
        }

        // 第二步：检查现有自动回避记录，删除不再符合条件的
        List<Integer> toRemoveIds = new ArrayList<>();
        if (existingAutoAvoidances != null) {
            for (SurverExpertAvoidanceDO existing : existingAutoAvoidances) {
                Integer proId = existing.getProId();
                Map<String, Object> project = projectMap.get(proId);
                if (project == null) {
                    // 项目不存在了，删除回避记录
                    toRemoveIds.add(existing.getId());
                    System.out.println("[勘察奖自动回避] 删除回避记录：项目不存在，proId=" + proId);
                    continue;
                }
                // 检查专家单位是否仍与项目单位匹配
                String companyName = (String) project.get("companyName");
                boolean stillMatch = false;
                if (StringUtils.isNotBlank(companyName)) {
                    String normalizedUnit = normalizeCompanyName(companyName);
                    stillMatch = isSameCompany(normalizedExpertCompany, normalizedUnit);
                }
                if (!stillMatch) {
                    toRemoveIds.add(existing.getId());
                    String proCode = (String) project.get("proCode");
                    System.out.println("[勘察奖自动回避] 删除回避记录：项目[" + proCode + "] 单位不再匹配");
                }
            }
        }

        // 批量删除不再符合条件的自动回避记录
        if (!toRemoveIds.isEmpty()) {
            for (Integer id : toRemoveIds) {
                avoidanceDao.remove(id);
            }
            System.out.println("[勘察奖自动回避] 已删除 " + toRemoveIds.size() + " 条不再符合条件的自动回避记录");
        }

        // 第三步：创建新的自动回避记录
        List<SurverExpertAvoidanceDO> avoidanceList = new ArrayList<>();

        for (Map<String, Object> project : projects) {
            boolean shouldAvoid = false;
            StringBuilder reason = new StringBuilder("单位重叠：");

            // 检查：与申报单位完全匹配（SQL返回的字段名是 companyName）
            String companyName = (String) project.get("companyName");
            Integer proId = (Integer) project.get("proId");
            String proCode = (String) project.get("proCode");
            if (StringUtils.isNotBlank(companyName)) {
                String normalizedUnit = normalizeCompanyName(companyName);
                if (isSameCompany(normalizedExpertCompany, normalizedUnit)) {
                    shouldAvoid = true;
                    reason.append("申报单位[").append(companyName).append("]");
                    System.out.println("[勘察奖自动回避] 匹配成功：项目[" + proCode + "] 申报单位[" + companyName + "]");
                }
            }

            // 如果需要回避，插入回避记录
            if (shouldAvoid) {
                // 检查是否已存在回避记录（包括手动回避）
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
                    System.out.println("[勘察奖自动回避] 新增回避记录：项目[" + proCode + "]");
                } else {
                    System.out.println("[勘察奖自动回避] 已存在回避记录：项目[" + proCode + "]");
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
