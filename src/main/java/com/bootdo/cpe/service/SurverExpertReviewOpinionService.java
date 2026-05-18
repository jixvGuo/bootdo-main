package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.SurverExpertReviewOpinionDO;

import java.util.List;

public interface SurverExpertReviewOpinionService {

    List<SurverExpertReviewOpinionDO> listByTaskAndExpert(String taskId, Long expertUid);

    int countExpertProAssigned(String taskId, Long expertUid, Integer proId);

    /**
     * 保存审核意见（空串表示清空为未选）
     */
    void saveAudit(String taskId, Long expertUid, Integer proId, String proSubType, String auditOpinion);

    /**
     * 保存主评：submitMain=true 时要求正文非空并置已提交；false 时仅更新正文（需已存在记录且已提交过）
     */
    void saveMain(String taskId, Long expertUid, Integer proId, String proSubType, String mainReviewText, boolean submitMain);
}
