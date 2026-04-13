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
     * 计算QC项目的平均分（排除回避专家的评分）
     * 
     * @param taskId 任务ID
     * @param proId 项目ID
     * @return 平均分（保留2位小数），如果无有效评分返回null
     */
    BigDecimal calculateAverageScore(String taskId, Integer proId);

    /**
     * 批量计算任务下所有项目的平均分（不落库）
     * 
     * @param taskId 任务ID
     * @return 计算成功的项目数量
     */
    int batchCalculateAverageScores(String taskId);

    /**
     * 计算并落库保存一次评分汇总结果（含明细）
     *
     * @param taskId 任务ID
     * @param proId 项目ID
     * @param phase 1=初评 2=复评
     * @param createdBy 触发计算的用户ID
     * @return 平均分（保留2位小数），无有效分返回null
     */
    BigDecimal calculateAndSave(String taskId, Integer proId, Integer phase, Integer createdBy);

    /**
     * 批量计算并落库保存任务下所有项目的评分汇总结果（含明细）
     *
     * @param taskId 任务ID
     * @param phase 1=初评 2=复评
     * @param createdBy 触发计算的用户ID
     * @return 成功写入结果的项目数量
     */
    int batchCalculateAndSave(String taskId, Integer phase, Integer createdBy);
}
