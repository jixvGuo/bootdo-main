package com.bootdo.cpe.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 形审表单页加载：取该项目最新一条形审记录（与 list/reviewRecords、列表 latestReview* 一致）。
 */
public final class SurverReviewFormLoadHelper {

    private SurverReviewFormLoadHelper() {
    }

    public static <T> T loadLatest(Map<String, Object> params,
                                   Function<Map<String, Object>, List<T>> listQuery,
                                   Supplier<T> emptySupplier) {
        Map<String, Object> q = new HashMap<>(params);
        q.put("sort", "id");
        q.put("order", "desc");
        q.put("offset", 0);
        q.put("limit", 1);
        List<T> list = listQuery.apply(q);
        if (list == null || list.isEmpty()) {
            return emptySupplier.get();
        }
        return list.get(0);
    }
}
