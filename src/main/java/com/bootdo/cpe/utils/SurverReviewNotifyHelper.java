package com.bootdo.cpe.utils;

import com.bootdo.cpe.domain.EnumAwardType;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.service.ProjectCommonService;
import com.bootdo.oa.domain.NotifyDO;
import com.bootdo.oa.service.NotifyService;
import org.apache.commons.lang3.StringUtils;

/**
 * 勘察奖形审「正式提交」系统通知（自动保存草稿不发通知）。
 */
public final class SurverReviewNotifyHelper {

    private SurverReviewNotifyHelper() {
    }

    public static boolean isFormalSubmit(String formalSubmit) {
        return "1".equals(formalSubmit) || "true".equalsIgnoreCase(StringUtils.trimToEmpty(formalSubmit));
    }

    public static void sendFormalReviewNotify(NotifyService notifyService,
                                              ProjectCommonService projectCommonService,
                                              Integer proId,
                                              String proName,
                                              String reviewResult,
                                              String remarks,
                                              Long operatorUserId,
                                              int reviewRecordId) {
        long proCreateUid = projectCommonService.getProCreateUid(proId);
        Long[] uidArr = {proCreateUid};
        NotifyDO notifyDO = new NotifyDO();
        notifyDO.setType(EnumAwardType.SURVER.getAwrdType() + "");
        notifyDO.setUserIds(uidArr);
        notifyDO.setCreateBy(operatorUserId);
        String applyAwardName = "【勘察奖】" + proName;
        String title = (applyAwardName == null ? "" : applyAwardName) + "形式审查结果";
        String content = "形式审查结果:" + reviewResult + ";";
        if (StringUtils.isNotBlank(remarks)) {
            content += remarks;
        }
        notifyDO.setContent(content);
        notifyDO.setTitle(title);
        notifyService.save(notifyDO);
        notifyService.saveProReviewNotifyShip(notifyDO.getId(), proId, reviewRecordId,
                EnumProjectType.SURVER_PRO.getProType());
    }
}
