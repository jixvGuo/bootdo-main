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
        // 必须用 cr.id，否则 JOIN 查询里 ORDER BY id 会按 pro.id 排序，取不到最新形审记录
        q.put("sort", "cr.id");
        q.put("order", "desc");
        q.put("offset", 0);
        q.put("limit", 1);
        q.put("reviewRecordOnly", "1");
        List<T> list = listQuery.apply(q);
        if (list == null || list.isEmpty()) {
            return emptySupplier.get();
        }
        return list.get(0);
    }

    /**
     * 打开形审页：预填最新一条记录（含 id），后续自动保存/提交均走 save，由后端按 id 更新同一条。
     * 原：clearEntityId 清空 id，首次自动保存会 insert 产生重复历史卡片。
     */
    public static <T> T loadLatestForFormSession(Map<String, Object> params,
                                                 Function<Map<String, Object>, List<T>> listQuery,
                                                 Supplier<T> emptySupplier) {
        return loadLatest(params, listQuery, emptySupplier);
    }
}
