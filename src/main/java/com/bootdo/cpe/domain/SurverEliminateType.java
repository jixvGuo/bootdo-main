package com.bootdo.cpe.domain;

import org.apache.commons.lang3.StringUtils;

/**
 * 勘察奖项目淘汰类型（与 4 张申报子表 eliminate_type 字段一致）。
 */
public final class SurverEliminateType {

    public static final String RATING = "rating";
    public static final String SCORE = "score";

    private SurverEliminateType() {
    }

    public static boolean isRating(String eliminateType) {
        return RATING.equals(normalize(eliminateType));
    }

    public static boolean isScore(String eliminateType) {
        return SCORE.equals(normalize(eliminateType));
    }

    public static boolean isValidType(String eliminateType) {
        String t = normalize(eliminateType);
        return RATING.equals(t) || SCORE.equals(t);
    }

    /**
     * 展示用：eliminated=1 且 type 为空时视为历史评级淘汰。
     */
    public static String resolveDisplayType(Integer eliminated, String eliminateType) {
        if (eliminated == null || eliminated != 1) {
            return null;
        }
        String t = normalize(eliminateType);
        if (StringUtils.isBlank(t)) {
            return RATING;
        }
        return t;
    }

    public static String displayLabel(Integer eliminated, String eliminateType) {
        if (eliminated == null || eliminated != 1) {
            return "未淘汰";
        }
        return isScore(resolveDisplayType(eliminated, eliminateType)) ? "打分淘汰" : "评级淘汰";
    }

    private static String normalize(String eliminateType) {
        return eliminateType == null ? null : eliminateType.trim().toLowerCase();
    }
}
