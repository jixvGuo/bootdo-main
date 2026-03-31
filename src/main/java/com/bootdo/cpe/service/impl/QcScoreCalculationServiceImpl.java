package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.QcGroupApplyInfoDao;
import com.bootdo.cpe.dao.QcResultInnovateScoreDao;
import com.bootdo.cpe.dao.QcResultSolveScoreDao;
import com.bootdo.cpe.domain.QcGroupApplyInfoDO;
import com.bootdo.cpe.domain.QcResultInnovateScoreDO;
import com.bootdo.cpe.domain.QcResultSolveScoreDO;
import com.bootdo.cpe.service.QcExpertAvoidanceService;
import com.bootdo.cpe.service.QcScoreCalculationService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public BigDecimal calculateAverageScore(String taskId, Integer proId) {
        if (StringUtils.isBlank(taskId) || proId == null) {
            return null;
        }

        // 获取该项目的所有回避专家ID列表
        List<Integer> avoidedExpertIds = avoidanceService.getAvoidedExpertIds(taskId, proId);

        // 获取项目信息，判断课题类型
        QcGroupApplyInfoDO projectInfo = qcGroupApplyInfoDao.get(proId);
        if (projectInfo == null) {
            logger.warn("项目不存在: proId={}", proId);
            return null;
        }

        String topicType = projectInfo.getTopicType();
        List<BigDecimal> validScores = new ArrayList<>();

        if ("solving".equals(topicType)) {
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
        } else if ("innovate".equals(topicType)) {
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
        if (validScores.isEmpty()) {
            logger.info("无有效评分: proId={}", proId);
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal score : validScores) {
            sum = sum.add(score);
        }

        BigDecimal average = sum.divide(new BigDecimal(validScores.size()), 2, RoundingMode.HALF_UP);
        
        // 四舍五入到整数
        BigDecimal roundedAverage = average.setScale(0, RoundingMode.HALF_UP);
        
        logger.info("计算平均分完成: proId={}, 有效评分数={}, 平均分={}, 四舍五入后={}", 
                   proId, validScores.size(), average, roundedAverage);
        
        return roundedAverage;
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
}
