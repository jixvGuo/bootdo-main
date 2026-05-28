package com.bootdo.cpe.utils;

import com.bootdo.common.utils.DateUtils;
import com.bootdo.cpe.domain.SurverReviewConsultResultDO;
import com.bootdo.system.domain.UserDO;
import com.bootdo.system.service.UserService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 勘察奖形审历史记录查询与展示（list/reviewRecords 弹窗，与 QC 奖语义对齐）。
 */
public final class SurverReviewRecordsHelper {

    private SurverReviewRecordsHelper() {
    }

    /** 按形审记录 id 倒序，且排除 LEFT JOIN 产生的空行 */
    public static Map<String, Object> historyQueryParams(Integer proId) {
        Map<String, Object> params = new HashMap<>();
        params.put("proId", proId);
        params.put("sort", "cr.id");
        params.put("order", "desc");
        params.put("offset", 0);
        params.put("limit", 200);
        params.put("reviewRecordOnly", "1");
        return params;
    }

    public static List<Map<String, Object>> toDisplayList(List<?> records, UserService userService) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object obj : records) {
            Map<String, Object> item = toDisplayItem(obj, userService);
            if (item != null) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparing(
                (Map<String, Object> m) -> (Integer) m.get("id"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private static Map<String, Object> toDisplayItem(Object obj, UserService userService) {
        Integer id = invokeInteger(obj, "getId");
        if (id == null || id <= 0) {
            return null;
        }
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("reviewResult", resolveReviewResult(obj));
        item.put("remarks", StringUtils.defaultString(invokeString(obj, "getRemarks")));
        Date updated = invokeDate(obj, "getUpdated");
        Date created = invokeDate(obj, "getCreated");
        Date showTime = updated != null ? updated : created;
        item.put("reviewTime", showTime != null
                ? DateUtils.format(showTime, DateUtils.DATE_TIME_PATTERN) : "");
        item.put("reviewerName", resolveReviewerName(obj, userService));
        return item;
    }

    private static String resolveReviewResult(Object obj) {
        if (obj instanceof SurverReviewConsultResultDO) {
            SurverReviewConsultResultDO consult = (SurverReviewConsultResultDO) obj;
            return coalesceText(consult.getReviewResult(), consult.getNeedIsVcd());
        }
        return StringUtils.defaultString(invokeString(obj, "getReviewResult"), "无");
    }

    private static String coalesceText(String primary, String fallback) {
        if (StringUtils.isNotBlank(primary)) {
            return primary.trim();
        }
        if (StringUtils.isNotBlank(fallback)) {
            return fallback.trim();
        }
        return "无";
    }

    private static String resolveReviewerName(Object obj, UserService userService) {
        Integer optUid = invokeInteger(obj, "getOptUid");
        if (optUid == null || optUid <= 0 || userService == null) {
            return "未知";
        }
        UserDO user = userService.get(optUid.longValue());
        if (user == null) {
            return String.valueOf(optUid);
        }
        if (StringUtils.isNotBlank(user.getName())) {
            return user.getName();
        }
        if (StringUtils.isNotBlank(user.getUsername())) {
            return user.getUsername();
        }
        return String.valueOf(optUid);
    }

    private static Integer invokeInteger(Object obj, String methodName) {
        Object value = invoke(obj, methodName);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private static String invokeString(Object obj, String methodName) {
        Object value = invoke(obj, methodName);
        return value != null ? value.toString() : null;
    }

    private static Date invokeDate(Object obj, String methodName) {
        Object value = invoke(obj, methodName);
        return value instanceof Date ? (Date) value : null;
    }

    private static Object invoke(Object obj, String methodName) {
        try {
            return obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
