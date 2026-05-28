package com.bootdo.cpe.utils;

import com.bootdo.common.utils.R;
import com.bootdo.cpe.service.ProjectCommonService;
import com.bootdo.oa.service.NotifyService;
import org.apache.commons.lang3.StringUtils;

/**
 * 勘察奖形审保存：自动保存更新草稿；正式提交且形审结果/评语变更时保留历史卡片。
 */
public final class SurverReviewSaveHelper {

    private SurverReviewSaveHelper() {
    }

    public interface RecordOps<T> {
        Integer getId(T entity);

        void setId(T entity, Integer id);

        void setOptUid(T entity, int uid);

        String getReviewResult(T entity);

        void setReviewResult(T entity, String value);

        String getRemarks(T entity);

        void setRemarks(T entity, String value);

        String getProName(T entity);

        T load(Integer id);

        int insert(T entity);

        int update(T entity);
    }

    public static <T> R save(T entity,
                               String formalSubmit,
                               String originReviewResult,
                               String originRemarks,
                               Integer proId,
                               Long operatorUid,
                               RecordOps<T> ops,
                               NotifyService notifyService,
                               ProjectCommonService projectCommonService) {
        ops.setOptUid(entity, operatorUid.intValue());
        Integer id = ops.getId(entity);
        boolean formal = SurverReviewNotifyHelper.isFormalSubmit(formalSubmit);

        if (formal && id != null && id > 0 && formalContentChanged(originReviewResult, originRemarks, entity, ops)) {
            T previous = ops.load(id);
            if (previous != null) {
                ops.setReviewResult(previous, StringUtils.trimToEmpty(originReviewResult));
                ops.setRemarks(previous, originRemarks);
                ops.update(previous);
            }
            ops.setId(entity, null);
            if (ops.insert(entity) <= 0) {
                return R.error();
            }
            int newId = ops.getId(entity);
            SurverReviewNotifyHelper.sendFormalReviewNotify(
                    notifyService, projectCommonService, proId,
                    ops.getProName(entity),
                    ops.getReviewResult(entity),
                    ops.getRemarks(entity),
                    operatorUid, newId);
            R r = R.ok();
            r.put("id", newId);
            r.put("historyCreated", true);
            return r;
        }

        if (id != null && id > 0) {
            ops.update(entity);
            if (formal) {
                SurverReviewNotifyHelper.sendFormalReviewNotify(
                        notifyService, projectCommonService, proId,
                        ops.getProName(entity),
                        ops.getReviewResult(entity),
                        ops.getRemarks(entity),
                        operatorUid, id);
            }
            R r = R.ok();
            r.put("id", id);
            return r;
        }

        if (ops.insert(entity) <= 0) {
            return R.error();
        }
        int newId = ops.getId(entity);
        if (formal) {
            SurverReviewNotifyHelper.sendFormalReviewNotify(
                    notifyService, projectCommonService, proId,
                    ops.getProName(entity),
                    ops.getReviewResult(entity),
                    ops.getRemarks(entity),
                    operatorUid, newId);
        }
        R r = R.ok();
        r.put("id", newId);
        return r;
    }

    private static <T> boolean formalContentChanged(String originReviewResult,
                                                    String originRemarks,
                                                    T entity,
                                                    RecordOps<T> ops) {
        return !StringUtils.equals(normalize(originReviewResult), normalize(ops.getReviewResult(entity)))
                || !StringUtils.equals(StringUtils.defaultString(originRemarks),
                StringUtils.defaultString(ops.getRemarks(entity)));
    }

    private static String normalize(String value) {
        return StringUtils.trimToEmpty(value);
    }
}
