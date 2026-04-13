package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.QcGroupApplyInfoDao;
import com.bootdo.cpe.dao.QcResultInnovateScoreDao;
import com.bootdo.cpe.dao.QcResultSolveScoreDao;
import com.bootdo.cpe.dao.QcScoreSubmitDao;
import com.bootdo.cpe.domain.QcGroupApplyInfoDO;
import com.bootdo.cpe.domain.QcResultInnovateScoreDO;
import com.bootdo.cpe.domain.QcResultSolveScoreDO;
import com.bootdo.cpe.domain.QcScoreCalcDetailDO;
import com.bootdo.cpe.domain.QcScoreCalcResultDO;
import com.bootdo.cpe.domain.QcScoreSubmitDO;
import com.bootdo.cpe.service.QcScoreCalcDetailService;
import com.bootdo.cpe.service.QcScoreCalcResultService;
import com.bootdo.cpe.service.QcExpertAvoidanceService;
import com.bootdo.cpe.service.QcScoreCalculationService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QC项目评分计算服务实现
 * 
 * @author system
 * @date 2026-03-27
 */
@Service
public class QcScoreCalculationServiceImpl implements QcScoreCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(QcScoreCalculationServiceImpl.class);

    @Autowired
    private QcResultSolveScoreDao qcResultSolveScoreDao;

    @Autowired
    private QcResultInnovateScoreDao qcResultInnovateScoreDao;

    @Autowired
    private QcExpertAvoidanceService avoidanceService;

    @Autowired
    private QcGroupApplyInfoDao qcGroupApplyInfoDao;

    @Autowired
    private QcScoreCalcResultService qcScoreCalcResultService;

    @Autowired
    private QcScoreCalcDetailService qcScoreCalcDetailService;

    @Autowired
    private QcScoreSubmitDao qcScoreSubmitDao;

    @Override
    public BigDecimal calculateAverageScore(String taskId, Integer proId) {
        if (StringUtils.isBlank(taskId) || proId == null) {
            return null;
        }

        // 获取该项目的所有回避专家ID列表
        List<Integer> avoidedExpertIds = avoidanceService.getAvoidedExpertIds(taskId, proId);

        // 获取项目信息，判断课题类型（用list+proId查询，因为get按主键id查）
        Map<String, Object> proQuery = new HashMap<>();
        proQuery.put("proId", String.valueOf(proId));
        List<QcGroupApplyInfoDO> proList = qcGroupApplyInfoDao.list(proQuery);
        QcGroupApplyInfoDO projectInfo = (proList != null && !proList.isEmpty()) ? proList.get(0) : null;
        if (projectInfo == null) {
            logger.warn("项目不存在: proId={}", proId);
            return null;
        }

        String topicType = projectInfo.getTopicType();
        List<BigDecimal> validScores = new ArrayList<>();

        if ("问题解决型".equals(topicType)) {
            // 问题解决型
            Map<String, Object> params = new HashMap<>();
            params.put("proId", proId);
            params.put("taskId", taskId);
            params.put("deleted", 0);
            
            List<QcResultSolveScoreDO> scoreList = qcResultSolveScoreDao.list(params);
            for (QcResultSolveScoreDO score : scoreList) {
                // 排除回避专家的评分
                if (avoidedExpertIds.contains(score.getOptUid())) {
                    logger.info("排除回避专家评分: proId={}, expertId={}", proId, score.getOptUid());
                    continue;
                }
                
                // 收集有效评分
                if (StringUtils.isNotBlank(score.getAppraiseSum())) {
                    try {
                        BigDecimal scoreValue = new BigDecimal(score.getAppraiseSum());
                        validScores.add(scoreValue);
                    } catch (NumberFormatException e) {
                        logger.warn("无效评分格式: proId={}, expertId={}, score={}", 
                                   proId, score.getOptUid(), score.getAppraiseSum());
                    }
                }
            }
        } else if ("创新型".equals(topicType)) {
            // 创新型
            Map<String, Object> params = new HashMap<>();
            params.put("proId", proId);
            params.put("taskId", taskId);
            params.put("deleted", 0);
            
            List<QcResultInnovateScoreDO> scoreList = qcResultInnovateScoreDao.list(params);
            for (QcResultInnovateScoreDO score : scoreList) {
                // 排除回避专家的评分
                if (avoidedExpertIds.contains(score.getOptUid())) {
                    logger.info("排除回避专家评分: proId={}, expertId={}", proId, score.getOptUid());
                    continue;
                }
                
                // 收集有效评分
                if (StringUtils.isNotBlank(score.getAppraiseSum())) {
                    try {
                        BigDecimal scoreValue = new BigDecimal(score.getAppraiseSum());
                        validScores.add(scoreValue);
                    } catch (NumberFormatException e) {
                        logger.warn("无效评分格式: proId={}, expertId={}, score={}", 
                                   proId, score.getOptUid(), score.getAppraiseSum());
                    }
                }
            }
        } else {
            logger.warn("未知课题类型: proId={}, topicType={}", proId, topicType);
            return null;
        }

        // 计算平均分
        return calcAverage(validScores, proId);
    }

    private BigDecimal calcAverage(List<BigDecimal> validScores, Integer proId) {
        if (validScores == null || validScores.isEmpty()) {
            logger.info("无有效评分: proId={}", proId);
            return null;
        }

        int scoreCount = validScores.size();
        List<BigDecimal> scoresToAverage = new ArrayList<>(validScores);

        // 根据有效分数数量决定计算方式
        if (scoreCount >= 3) {
            // ≥3个：去掉一个最高分，去掉一个最低分，求平均
            scoresToAverage.sort(BigDecimal::compareTo);
            scoresToAverage.remove(0); // 去最低
            scoresToAverage.remove(scoresToAverage.size() - 1); // 去最高
            logger.info("去除最高最低分: proId={}, 原始分数数={}, 去除后={}", proId, scoreCount, scoresToAverage.size());
        } else {
            // 1-2个：直接求平均
            logger.info("有效分数≤2个，直接平均: proId={}, 分数数={}", proId, scoreCount);
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal score : scoresToAverage) {
            sum = sum.add(score);
        }

        BigDecimal average = sum.divide(new BigDecimal(scoresToAverage.size()), 2, RoundingMode.HALF_UP);
        logger.info("计算平均分完成: proId={}, 原始有效分数={}, 参与计算分数={}, 平均分={}",
                proId, scoreCount, scoresToAverage.size(), average);
        return average;
    }

    private BigDecimal calcAverageDirect(List<BigDecimal> scores, Integer proId) {
        if (scores == null || scores.isEmpty()) {
            logger.info("无参与计算的有效评分: proId={}", proId);
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal score : scores) {
            sum = sum.add(score);
        }
        BigDecimal average = sum.divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP);
        logger.info("计算平均分完成(直接平均): proId={}, 参与计算分数={}, 平均分={}", proId, scores.size(), average);
        return average;
    }

    @Override
    @Transactional
    public BigDecimal calculateAndSave(String taskId, Integer proId, Integer phase, Integer createdBy) {
        if (StringUtils.isBlank(taskId) || proId == null || phase == null) {
            return null;
        }

        // ★ 从提交快照表读取（类型安全 DECIMAL，无需 if-else 区分课题类型）
        Map<String, Object> submitQuery = new HashMap<>();
        submitQuery.put("taskId", taskId);
        submitQuery.put("proId", proId);
        submitQuery.put("phase", phase);
        List<QcScoreSubmitDO> submitList = qcScoreSubmitDao.list(submitQuery);

        if (submitList == null || submitList.isEmpty()) {
            logger.info("submit 表无记录，跳过计算: taskId={}, proId={}, phase={}", taskId, proId, phase);
            return null;
        }

        // 获取回避专家列表
        List<Integer> avoidedExpertIds = avoidanceService.getAvoidedExpertIds(taskId, proId);

        // 构建明细（不再需要 if-else 分支判断课题类型）
        List<QcScoreCalcDetailDO> detailList = new ArrayList<>();
        for (QcScoreSubmitDO s : submitList) {
            boolean avoided = avoidedExpertIds != null && avoidedExpertIds.contains(s.getExpertUid());
            QcScoreCalcDetailDO d = new QcScoreCalcDetailDO();
            d.setTaskId(taskId);
            d.setProId(proId);
            d.setPhase(phase);
            d.setExpertUid(s.getExpertUid());
            d.setSubmitId(s.getId());
            d.setRawScore(s.getTotalScore());        // 已是 DECIMAL，无需转换
            d.setIsAvoided(avoided ? 1 : 0);
            d.setIsValid((!avoided && s.getTotalScore() != null) ? 1 : 0);
            d.setIsRemovedLow(0);
            d.setIsRemovedHigh(0);
            detailList.add(d);
        }

        int validCount = (int) detailList.stream().filter(d -> Integer.valueOf(1).equals(d.getIsValid())).count();
        int usedCount = validCount;
        if (validCount >= 3) {
            usedCount = validCount - 2;
            markRemovedHighLow(detailList);
        }

        BigDecimal avg = calcAverageDirect(extractUsedScores(detailList), proId);

        // ★ UPSERT：唯一键 (task_id, pro_id, phase) 保证每次重算覆盖，不新增行
        QcScoreCalcResultDO result = new QcScoreCalcResultDO();
        result.setTaskId(taskId);
        result.setProId(proId);
        result.setPhase(phase);
        result.setAvgScore(avg);
        result.setValidCount(validCount);
        result.setUsedCount(Math.max(usedCount, 0));
        result.setCreatedBy(createdBy);
        result.setDeleted(0);
        qcScoreCalcResultService.upsert(result);

        // ★ 先删旧明细再插新（配合 UPSERT 保证明细同步）
        if (result.getId() != null) {
            qcScoreCalcDetailService.deleteByResultId(result.getId());
            for (QcScoreCalcDetailDO d : detailList) {
                d.setResultId(result.getId());
            }
            if (!detailList.isEmpty()) {
                qcScoreCalcDetailService.batchSave(detailList);
            }
        }

        return avg;
    }

    private QcScoreCalcDetailDO buildDetail(String taskId, Integer proId, Integer phase, Integer expertUid,
                                           String appraiseSum, List<Integer> avoidedExpertIds) {
        QcScoreCalcDetailDO d = new QcScoreCalcDetailDO();
        d.setTaskId(taskId);
        d.setProId(proId);
        d.setPhase(phase);
        d.setExpertUid(expertUid);
        d.setIsRemovedHigh(0);
        d.setIsRemovedLow(0);

        boolean avoided = expertUid != null && avoidedExpertIds != null && avoidedExpertIds.contains(expertUid);
        d.setIsAvoided(avoided ? 1 : 0);

        BigDecimal raw = null;
        if (StringUtils.isNotBlank(appraiseSum)) {
            try {
                raw = new BigDecimal(appraiseSum);
            } catch (Exception ignore) {
                raw = null;
            }
        }
        d.setRawScore(raw);
        // 有效分：未回避且 rawScore 可解析
        d.setIsValid((!avoided && raw != null) ? 1 : 0);
        return d;
    }

    private void markRemovedHighLow(List<QcScoreCalcDetailDO> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            return;
        }
        QcScoreCalcDetailDO low = null;
        QcScoreCalcDetailDO high = null;

        for (QcScoreCalcDetailDO d : detailList) {
            if (!Integer.valueOf(1).equals(d.getIsValid()) || d.getRawScore() == null) {
                continue;
            }
            if (low == null || d.getRawScore().compareTo(low.getRawScore()) < 0) {
                low = d;
            }
            if (high == null || d.getRawScore().compareTo(high.getRawScore()) > 0) {
                high = d;
            }
        }

        if (low != null) {
            low.setIsRemovedLow(1);
        }
        if (high != null) {
            if (low != null && low == high) {
                // 所有有效分相同时，high 与 low 指向同一对象，另找一个有效元素标记为最高
                for (QcScoreCalcDetailDO d : detailList) {
                    if (Integer.valueOf(1).equals(d.getIsValid()) && d.getRawScore() != null && d != low) {
                        high = d;
                        break;
                    }
                }
                if (high == low) {
                    return;
                }
            }
            high.setIsRemovedHigh(1);
        }
    }

    private List<BigDecimal> extractUsedScores(List<QcScoreCalcDetailDO> detailList) {
        List<BigDecimal> used = new ArrayList<>();
        if (detailList == null) {
            return used;
        }
        for (QcScoreCalcDetailDO d : detailList) {
            if (!Integer.valueOf(1).equals(d.getIsValid())) {
                continue;
            }
            if (Integer.valueOf(1).equals(d.getIsRemovedLow()) || Integer.valueOf(1).equals(d.getIsRemovedHigh())) {
                continue;
            }
            if (d.getRawScore() != null) {
                used.add(d.getRawScore());
            }
        }
        return used;
    }

    @Override
    public int batchCalculateAverageScores(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return 0;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        
        List<QcGroupApplyInfoDO> projects = qcGroupApplyInfoDao.list(params);
        int successCount = 0;

        for (QcGroupApplyInfoDO project : projects) {
            try {
                BigDecimal avgScore = calculateAverageScore(taskId, project.getProId());
                if (avgScore != null) {
                    // TODO: 将平均分保存到项目表或结果表
                    // 这里需要根据实际业务需求决定保存位置
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("计算项目平均分失败: proId={}", project.getProId(), e);
            }
        }

        logger.info("批量计算平均分完成: taskId={}, 成功数={}/{}", taskId, successCount, projects.size());
        return successCount;
    }

    @Override
    @Transactional
    public int batchCalculateAndSave(String taskId, Integer phase, Integer createdBy) {
        if (StringUtils.isBlank(taskId) || phase == null) {
            return 0;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        List<QcGroupApplyInfoDO> projects = qcGroupApplyInfoDao.list(params);
        int successCount = 0;
        if (projects == null || projects.isEmpty()) {
            return 0;
        }

        for (QcGroupApplyInfoDO project : projects) {
            try {
                BigDecimal avg = calculateAndSave(taskId, project.getProId(), phase, createdBy);
                if (avg != null) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("批量计算并保存失败: proId={}", project.getProId(), e);
            }
        }

        logger.info("批量计算并保存完成: taskId={}, phase={}, 成功数={}/{}", taskId, phase, successCount, projects.size());
        return successCount;
    }
}
