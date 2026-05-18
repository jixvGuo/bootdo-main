package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.SurverExpertReviewOpinionDao;
import com.bootdo.cpe.domain.SurverExpertReviewOpinionDO;
import com.bootdo.cpe.service.SurverExpertReviewOpinionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SurverExpertReviewOpinionServiceImpl implements SurverExpertReviewOpinionService {

    @Autowired
    private SurverExpertReviewOpinionDao dao;

    @Override
    public List<SurverExpertReviewOpinionDO> listByTaskAndExpert(String taskId, Long expertUid) {
        return dao.listByTaskAndExpert(taskId, expertUid);
    }

    @Override
    public int countExpertProAssigned(String taskId, Long expertUid, Integer proId) {
        return dao.countExpertProAssigned(taskId, expertUid, proId);
    }

    @Override
    public void saveAudit(String taskId, Long expertUid, Integer proId, String proSubType, String auditOpinion) {
        assertAssigned(taskId, expertUid, proId);
        String normalized = normalizeAudit(auditOpinion);
        SurverExpertReviewOpinionDO exist = dao.getByUnique(taskId, proId, expertUid);
        if (exist == null) {
            SurverExpertReviewOpinionDO row = new SurverExpertReviewOpinionDO();
            row.setTaskId(taskId);
            row.setProId(proId);
            row.setProSubType(StringUtils.isNotBlank(proSubType) ? proSubType : "");
            row.setExpertUid(expertUid);
            row.setAuditOpinion(normalized);
            row.setMainReviewText(null);
            row.setMainReviewSubmitted(0);
            dao.insert(row);
            return;
        }
        dao.updateAudit(exist.getId(), normalized);
    }

    @Override
    public void saveMain(String taskId, Long expertUid, Integer proId, String proSubType, String mainReviewText, boolean submitMain) {
        assertAssigned(taskId, expertUid, proId);
        String text = mainReviewText == null ? "" : mainReviewText.trim();
        if (submitMain && StringUtils.isBlank(text)) {
            throw new IllegalArgumentException("主评意见不能为空");
        }
        SurverExpertReviewOpinionDO exist = dao.getByUnique(taskId, proId, expertUid);
        if (exist == null) {
            if (!submitMain) {
                throw new IllegalArgumentException("请先提交主评意见");
            }
            SurverExpertReviewOpinionDO row = new SurverExpertReviewOpinionDO();
            row.setTaskId(taskId);
            row.setProId(proId);
            row.setProSubType(StringUtils.isNotBlank(proSubType) ? proSubType : "");
            row.setExpertUid(expertUid);
            row.setAuditOpinion(null);
            row.setMainReviewText(text);
            row.setMainReviewSubmitted(1);
            dao.insert(row);
            return;
        }
        if (submitMain) {
            dao.updateMain(exist.getId(), text, 1);
        } else {
            if (exist.getMainReviewSubmitted() == null || exist.getMainReviewSubmitted() != 1) {
                throw new IllegalArgumentException("尚未提交主评，请使用提交操作");
            }
            dao.updateMain(exist.getId(), text, 1);
        }
    }

    private void assertAssigned(String taskId, Long expertUid, Integer proId) {
        if (dao.countExpertProAssigned(taskId, expertUid, proId) <= 0) {
            throw new IllegalArgumentException("无权操作该项目");
        }
    }

    /** null 表示清空；agree / disagree 合法 */
    private static String normalizeAudit(String auditOpinion) {
        if (auditOpinion == null || auditOpinion.trim().isEmpty()) {
            return null;
        }
        String v = auditOpinion.trim();
        if (!"agree".equals(v) && !"disagree".equals(v)) {
            throw new IllegalArgumentException("审核意见取值非法");
        }
        return v;
    }
}
