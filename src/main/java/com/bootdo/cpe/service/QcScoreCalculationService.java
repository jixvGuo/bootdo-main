package com.bootdo.cpe.service;

import java.math.BigDecimal;

/**
 * QC项目评分计算服务
 * 负责计算QC项目的平均分，排除回避专家的评分
 * 
 * @author system
 * @date 2026-03-27
 */
public interface QcScoreCalculationService {

    /**
     * 计算QC项目的平均分（排除回避专家的评分，四舍五入）
     * 
     * @param taskId 任务ID
     * @param proId 项目ID
     * @return 平均分（四舍五入到整数），如果无有效评分返回null
     */
    BigDecimal calculateAverageScore(String taskId, Integer proId);

    /**
     * 批量计算任务下所有项目的平均分
     * 
     * @param taskId 任务ID
     * @return 计算成功的项目数量
     */
    int batchCalculateAverageScores(String taskId);
}
