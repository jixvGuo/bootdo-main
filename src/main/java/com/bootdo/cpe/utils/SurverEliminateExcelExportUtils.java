package com.bootdo.cpe.utils;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 按 {@code excel/surver_eliminate_template.xlsx} 原样导出：保留模板合并与黄底表头，仅替换标题/数据/统计。
 */
public final class SurverEliminateExcelExportUtils {

    private static final String TEMPLATE_PATH = "excel/surver_eliminate_template.xlsx";
    /** 响应头携带此版本号，便于确认服务已加载最新编译的导出类 */
    public static final String EXPORT_BUILD = "20260526-eliminate-avg-score";

    private static final int BLOCK_ROW_COUNT = 16;
    private static final int DATA_START_IN_BLOCK = 3;
    private static final int LEGEND_START_IN_BLOCK = 7;
    private static final int STAT_FIRST_ROW_IN_BLOCK = 8;
    private static final int TEMPLATE_DATA_SLOTS = LEGEND_START_IN_BLOCK - DATA_START_IN_BLOCK;
    private static final int TEMPLATE_EXPERT_COLS = 3;
    private static final int MAX_EXPERT_COLS_BEFORE_STAT_SHIFT = 5;

    private static final int COL_GRADE_A = 8;
    private static final int COL_GRADE_B = 9;
    private static final int COL_GRADE_C = 10;
    private static final int COL_GRADE_D = 11;
    private static final int COL_GRADE_AVOID = 12;
    private static final int COL_TOTAL = 13;
    private static final int COL_ELIM = 14;
    private static final int COL_EXPERT_START = 15;
    private static final int COL_STAT_LABEL_BASE = 20;

    private static final String[] STAT_LABELS = {"A", "B", "C", "D", "回避"};
    private static final int STAT_LABEL_ROWS = STAT_LABELS.length;

    private static final Map<String, String> SUB_TYPE_LABEL = new LinkedHashMap<>();

    static {
        SUB_TYPE_LABEL.put("design", "优秀设计奖");
        SUB_TYPE_LABEL.put("software", "计算机软件奖");
        SUB_TYPE_LABEL.put("standard", "标准设计奖");
        SUB_TYPE_LABEL.put("contribution", "优秀勘察奖");
        SUB_TYPE_LABEL.put("consulting", "优秀咨询奖");
    }

    private SurverEliminateExcelExportUtils() {
    }

    public static void exportByTemplate(HttpServletResponse response,
                                        List<Map<String, Object>> flatRows) throws Exception {
        try (InputStream tplIs = SurverEliminateExcelExportUtils.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_PATH)) {
            if (tplIs == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("模板文件不存在: " + TEMPLATE_PATH);
                return;
            }
            XSSFWorkbook wb = new XSSFWorkbook(tplIs);
            XSSFSheet sheet = wb.getSheetAt(0);

            List<GroupExportData> groups = buildGroupData(flatRows);
            int maxExperts = 1;
            for (GroupExportData g : groups) {
                maxExperts = Math.max(maxExperts, g.expertOrder.size());
            }

            trimSheetToSingleBlock(sheet);
            expandExpertColumnsIfNeeded(sheet, maxExperts);

            int colCount = captureColCount(maxExperts);
            List<RowSnapshot> blockTemplate = captureBlock(sheet, 0, BLOCK_ROW_COUNT, colCount);
            List<CellRangeAddress> blockMerges = captureBlockMerges(sheet, 0, BLOCK_ROW_COUNT - 1);

            if (groups.isEmpty()) {
                setTitleValue(sheet, 0, "（暂无数据）");
                writeResponse(response, wb, "确认淘汰名单.xlsx");
                return;
            }

            int writeBase = 0;
            for (GroupExportData group : groups) {
                pasteBlock(sheet, writeBase, blockTemplate, blockMerges);
                writeBase += fillGroupBlock(sheet, writeBase, group, maxExperts, colCount);
            }

            writeResponse(response, wb, "确认淘汰名单.xlsx");
        }
    }

    private static int fillGroupBlock(XSSFSheet sheet, int baseRow, GroupExportData group,
                                      int maxExperts, int colCount) {
        List<ProjectExportData> projects = new ArrayList<>(group.projects.values());
        projects.sort(Comparator
                .comparing((ProjectExportData p) -> SUB_TYPE_LABEL.getOrDefault(p.proSubType, p.proSubType))
                .thenComparing(p -> p.proCode));

        List<Long> expertUids = new ArrayList<>(group.expertOrder.keySet());
        int dataStyleRow = baseRow + DATA_START_IN_BLOCK;

        int extraRows = Math.max(0, projects.size() - TEMPLATE_DATA_SLOTS);
        if (extraRows > 0) {
            int insertAt = baseRow + LEGEND_START_IN_BLOCK;
            int last = sheet.getLastRowNum();
            if (insertAt <= last) {
                sheet.shiftRows(insertAt, last, extraRows, true, true);
            }
            for (int i = 0; i < extraRows; i++) {
                cloneRowStyles(sheet, dataStyleRow, insertAt + i, colCount);
            }
        }

        setTitleValue(sheet, baseRow, group.groupName);
        fillExpertHeaderNames(sheet, baseRow, expertUids, group.expertOrder, maxExperts);

        for (int i = 0; i < projects.size(); i++) {
            writeProjectRow(sheet, baseRow + DATA_START_IN_BLOCK + i, dataStyleRow, colCount,
                    i + 1, projects.get(i), expertUids, maxExperts);
        }

        int totalDataSlots = TEMPLATE_DATA_SLOTS + extraRows;
        for (int i = projects.size(); i < totalDataSlots; i++) {
            clearRowValues(sheet, baseRow + DATA_START_IN_BLOCK + i, dataStyleRow, colCount, maxExperts);
        }

        writeExpertStatRows(sheet, baseRow, extraRows, expertUids, group, maxExperts, colCount);

        return BLOCK_ROW_COUNT + extraRows;
    }

    /** 只改标题格文字，不拆合并区（与模板图一致：白底标题行 + 黄底表头行） */
    private static void setTitleValue(XSSFSheet sheet, int baseRow, String groupName) {
        XSSFRow row = getOrCreateRow(sheet, baseRow);
        XSSFCell cell = row.getCell(0);
        if (cell == null) {
            cell = row.createCell(0);
            XSSFRow tpl = sheet.getRow(0);
            if (tpl != null && tpl.getCell(0) != null) {
                cell.setCellStyle(tpl.getCell(0).getCellStyle());
            }
        }
        cell.setCellValue(formatGroupTitle(groupName));
    }

    private static String formatGroupTitle(String groupName) {
        if (groupName == null) {
            return "（未命名专家组）";
        }
        String name = groupName.trim();
        if (name.startsWith("专家组名称")) {
            name = name.substring("专家组名称".length()).trim();
        }
        if (name.endsWith("XXXXX")) {
            name = name.substring(0, name.length() - 5).trim();
        }
        return name.isEmpty() ? "（未命名专家组）" : name;
    }

    /** 仅写入本组实际专家姓名；模板多出的 KC 列清空，不显示占位符 */
    private static void fillExpertHeaderNames(XSSFSheet sheet, int baseRow, List<Long> expertUids,
                                              Map<Long, String> expertNames, int maxExperts) {
        int headerRow = baseRow + 1;
        XSSFRow row = getOrCreateRow(sheet, headerRow);
        XSSFCell headerStyleRef = row.getCell(COL_EXPERT_START);
        for (int i = 0; i < expertUids.size(); i++) {
            int col = expertCol(i);
            XSSFCell cell = row.getCell(col);
            if (cell == null) {
                cell = row.createCell(col);
                if (headerStyleRef != null) {
                    cell.setCellStyle(headerStyleRef.getCellStyle());
                }
            }
            String name = expertNames.get(expertUids.get(i));
            if (name == null || name.isEmpty()) {
                name = String.valueOf(expertUids.get(i));
            }
            cell.setCellValue(name);
        }
        clearUnusedExpertColumns(sheet, headerRow, headerRow, expertUids.size(), maxExperts);
    }

    private static void writeProjectRow(XSSFSheet sheet, int rowIdx, int styleRow, int colCount, int seq,
                                        ProjectExportData p, List<Long> expertUids, int maxExperts) {
        int cntA = 0, cntB = 0, cntC = 0, cntD = 0, cntAvoid = 0;
        for (Long uid : expertUids) {
            String grade = normalizeGrade(p.gradeByExpert.get(uid));
            switch (grade) {
                case "A":
                    cntA++;
                    break;
                case "B":
                    cntB++;
                    break;
                case "C":
                    cntC++;
                    break;
                case "D":
                    cntD++;
                    break;
                case "回避":
                    cntAvoid++;
                    break;
                default:
                    break;
            }
        }

        putCellValue(sheet, rowIdx, 0, styleRow, String.valueOf(seq));
        putCellValue(sheet, rowIdx, 1, styleRow, String.valueOf(p.proId));
        putCellValue(sheet, rowIdx, 2, styleRow, SUB_TYPE_LABEL.getOrDefault(p.proSubType, p.proSubType));
        putCellValue(sheet, rowIdx, 3, styleRow, p.proCode);
        putCellValue(sheet, rowIdx, 4, styleRow, p.declareAccount);
        putCellValue(sheet, rowIdx, 5, styleRow, p.topicName);
        putCellValue(sheet, rowIdx, 6, styleRow, p.groupName);
        putCellValue(sheet, rowIdx, 7, styleRow, p.companyName);
        putCellValue(sheet, rowIdx, COL_GRADE_A, styleRow, cntA > 0 ? String.valueOf(cntA) : "");
        putCellValue(sheet, rowIdx, COL_GRADE_B, styleRow, cntB > 0 ? String.valueOf(cntB) : "");
        putCellValue(sheet, rowIdx, COL_GRADE_C, styleRow, cntC > 0 ? String.valueOf(cntC) : "");
        putCellValue(sheet, rowIdx, COL_GRADE_D, styleRow, cntD > 0 ? String.valueOf(cntD) : "");
        putCellValue(sheet, rowIdx, COL_GRADE_AVOID, styleRow, cntAvoid > 0 ? String.valueOf(cntAvoid) : "");
        putCellValue(sheet, rowIdx, COL_TOTAL, styleRow, formatAvgScore(calcEliminateAvgScore(expertUids, p)));

        int elim = p.eliminated != null && p.eliminated == 1 ? 1 : 0;
        putCellValue(sheet, rowIdx, COL_ELIM, styleRow, elim == 1 ? "已淘汰" : "未淘汰");

        for (int e = 0; e < expertUids.size(); e++) {
            putCellValue(sheet, rowIdx, expertCol(e), styleRow,
                    normalizeGrade(p.gradeByExpert.get(expertUids.get(e))));
        }
        clearUnusedExpertColumns(sheet, rowIdx, styleRow, expertUids.size(), maxExperts);
    }

    private static void writeExpertStatRows(XSSFSheet sheet, int baseRow, int extraRows,
                                            List<Long> expertUids, GroupExportData group,
                                            int maxExperts, int colCount) {
        int statStart = baseRow + STAT_FIRST_ROW_IN_BLOCK + extraRows;
        int labelCol = statLabelCol(maxExperts);

        Map<Long, int[]> perExpert = new LinkedHashMap<>();
        for (Long uid : expertUids) {
            perExpert.put(uid, new int[STAT_LABEL_ROWS]);
        }
        for (ProjectExportData p : group.projects.values()) {
            for (Long uid : expertUids) {
                int idx = statIndex(normalizeGrade(p.gradeByExpert.get(uid)));
                if (idx >= 0) {
                    perExpert.get(uid)[idx]++;
                }
            }
        }

        for (int i = 0; i < STAT_LABEL_ROWS; i++) {
            int rowIdx = statStart + i;
            int tplRow = baseRow + STAT_FIRST_ROW_IN_BLOCK + i;
            putCellValue(sheet, rowIdx, labelCol, tplRow, STAT_LABELS[i]);
            for (int e = 0; e < expertUids.size(); e++) {
                int cnt = perExpert.get(expertUids.get(e))[i];
                putCellValue(sheet, rowIdx, expertCol(e), tplRow, cnt > 0 ? String.valueOf(cnt) : "");
            }
            clearUnusedExpertColumns(sheet, rowIdx, tplRow, expertUids.size(), maxExperts);
        }
    }

    /** 清空本组未使用的专家列（含模板 KC 占位列） */
    private static void clearUnusedExpertColumns(XSSFSheet sheet, int rowIdx, int styleRowIdx,
                                                 int groupExpertCount, int maxExperts) {
        int clearEnd = expertColumnClearEnd(maxExperts);
        for (int c = COL_EXPERT_START + groupExpertCount; c <= clearEnd; c++) {
            putCellValue(sheet, rowIdx, c, styleRowIdx, "");
        }
    }

    private static void putCellValue(XSSFSheet sheet, int rowIdx, int col, int styleRowIdx, String value) {
        XSSFRow row = getOrCreateRow(sheet, rowIdx);
        XSSFCell cell = row.getCell(col);
        if (cell == null) {
            cell = row.createCell(col);
            applyDataCellStyle(sheet, cell, styleRowIdx, col);
        }
        cell.setCellValue(value == null ? "" : value);
    }

    /** 新建格用模板数据行白底样式（不用黄底表头） */
    private static void applyDataCellStyle(XSSFSheet sheet, XSSFCell cell, int styleRowIdx, int col) {
        XSSFRow styleRow = sheet.getRow(styleRowIdx);
        if (styleRow == null) {
            return;
        }
        XSSFCell ref = styleRow.getCell(col);
        if (ref != null) {
            cell.setCellStyle(ref.getCellStyle());
            return;
        }
        for (int c : new int[]{9, 13, 15, 6, 8, 0}) {
            ref = styleRow.getCell(c);
            if (ref != null) {
                cell.setCellStyle(ref.getCellStyle());
                return;
            }
        }
    }

    private static void clearRowValues(XSSFSheet sheet, int rowIdx, int styleRow, int colCount, int maxExperts) {
        for (int c = 0; c < colCount; c++) {
            if (c == statLabelCol(maxExperts)) {
                continue;
            }
            putCellValue(sheet, rowIdx, c, styleRow, "");
        }
    }

    private static void cloneRowStyles(XSSFSheet sheet, int srcRowIdx, int destRowIdx, int colCount) {
        XSSFRow src = sheet.getRow(srcRowIdx);
        XSSFRow dest = getOrCreateRow(sheet, destRowIdx);
        if (src != null) {
            dest.setHeight(src.getHeight());
        }
        for (int c = 0; c < colCount; c++) {
            XSSFCell dc = dest.getCell(c);
            if (dc == null) {
                dc = dest.createCell(c);
            }
            if (src != null && src.getCell(c) != null) {
                dc.setCellStyle(src.getCell(c).getCellStyle());
            } else {
                applyDataCellStyle(sheet, dc, srcRowIdx, c);
            }
        }
    }

    private static void pasteBlock(XSSFSheet sheet, int destBaseRow, List<RowSnapshot> blockTemplate,
                                   List<CellRangeAddress> blockMerges) {
        removeMergesInRowRange(sheet, destBaseRow, destBaseRow + BLOCK_ROW_COUNT - 1);
        for (int i = 0; i < blockTemplate.size(); i++) {
            RowSnapshot snap = blockTemplate.get(i);
            XSSFRow dest = getOrCreateRow(sheet, destBaseRow + i);
            if (snap.height > 0) {
                dest.setHeight(snap.height);
            }
            for (Map.Entry<Integer, CellSnapshot> e : snap.cells.entrySet()) {
                int col = e.getKey();
                CellSnapshot cs = e.getValue();
                XSSFCell destCell = dest.getCell(col);
                if (destCell == null) {
                    destCell = dest.createCell(col);
                }
                destCell.setCellStyle(cs.style);
                if (cs.value != null && !cs.value.isEmpty()) {
                    destCell.setCellValue(cs.value);
                }
            }
        }
        for (CellRangeAddress region : blockMerges) {
            sheet.addMergedRegion(new CellRangeAddress(
                    region.getFirstRow() + destBaseRow,
                    region.getLastRow() + destBaseRow,
                    region.getFirstColumn(),
                    region.getLastColumn()));
        }
    }

    /** 模板常带 16 行之后的示例行；从底部删除，避免 shiftRows 负偏移把行号移到 -1 */
    private static void trimSheetToSingleBlock(XSSFSheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < BLOCK_ROW_COUNT) {
            return;
        }
        removeMergesInRowRange(sheet, BLOCK_ROW_COUNT, lastRow);
        for (int r = lastRow; r >= BLOCK_ROW_COUNT; r--) {
            XSSFRow row = sheet.getRow(r);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    /** 专家多于模板 3 列时向右扩列；超过 5 列时顺带右移统计区 */
    private static void expandExpertColumnsIfNeeded(XSSFSheet sheet, int expertCount) {
        int srcCol = COL_EXPERT_START + TEMPLATE_EXPERT_COLS - 1;
        int gapCols = Math.min(expertCount, MAX_EXPERT_COLS_BEFORE_STAT_SHIFT) - TEMPLATE_EXPERT_COLS;
        for (int i = 0; i < gapCols; i++) {
            ensureExpertHeaderColumn(sheet, srcCol, COL_EXPERT_START + TEMPLATE_EXPERT_COLS + i);
        }
        int shiftExtra = Math.max(0, expertCount - MAX_EXPERT_COLS_BEFORE_STAT_SHIFT);
        if (shiftExtra <= 0) {
            return;
        }
        int insertAt = COL_STAT_LABEL_BASE;
        int lastCol = COL_STAT_LABEL_BASE + 5;
        sheet.shiftColumns(insertAt, lastCol, shiftExtra);
        for (int i = 0; i < shiftExtra; i++) {
            ensureExpertHeaderColumn(sheet, srcCol, insertAt + i);
        }
    }

    private static void ensureExpertHeaderColumn(XSSFSheet sheet, int styleSrcCol, int col) {
        if (sheet.getColumnWidth(col) <= 0 && sheet.getColumnWidth(styleSrcCol) > 0) {
            sheet.setColumnWidth(col, sheet.getColumnWidth(styleSrcCol));
        }
        for (int r = 0; r < BLOCK_ROW_COUNT; r++) {
            copyCellStyle(sheet, r, styleSrcCol, r, col);
        }
        if (!hasExpertHeaderMerge(sheet, col)) {
            sheet.addMergedRegion(new CellRangeAddress(1, 2, col, col));
        }
    }

    private static boolean hasExpertHeaderMerge(XSSFSheet sheet, int col) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.getFirstRow() == 1 && r.getLastRow() == 2
                    && r.getFirstColumn() == col && r.getLastColumn() == col) {
                return true;
            }
        }
        return false;
    }

    private static void copyCellStyle(XSSFSheet sheet, int srcRow, int srcCol, int destRow, int destCol) {
        XSSFRow sr = sheet.getRow(srcRow);
        if (sr == null) {
            return;
        }
        XSSFCell sc = sr.getCell(srcCol);
        if (sc == null) {
            return;
        }
        XSSFRow dr = getOrCreateRow(sheet, destRow);
        XSSFCell dc = dr.getCell(destCol);
        if (dc == null) {
            dc = dr.createCell(destCol);
        }
        dc.setCellStyle(sc.getCellStyle());
    }

    private static int statLabelCol(int maxExperts) {
        return COL_STAT_LABEL_BASE + Math.max(0, maxExperts - MAX_EXPERT_COLS_BEFORE_STAT_SHIFT);
    }

    private static int expertCol(int index) {
        return COL_EXPERT_START + index;
    }

    /** 本组需清空的未用专家列右边界（至少覆盖模板 3 列，避免残留 KC1-x） */
    private static int expertColumnClearEnd(int maxExperts) {
        int layoutEnd = COL_EXPERT_START + Math.max(maxExperts, TEMPLATE_EXPERT_COLS) - 1;
        return Math.min(statLabelCol(maxExperts) - 1, layoutEnd);
    }

    private static int captureColCount(int maxExperts) {
        return statLabelCol(maxExperts) + 1;
    }

    private static int statIndex(String grade) {
        if (grade == null) {
            return -1;
        }
        for (int i = 0; i < STAT_LABELS.length; i++) {
            if (STAT_LABELS[i].equals(grade)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeGrade(String grade) {
        if (grade == null) {
            return "";
        }
        String raw = grade.trim();
        if (raw.isEmpty()) {
            return "";
        }
        if (raw.contains("回避")) {
            return "回避";
        }
        String g = raw.toUpperCase(Locale.ROOT);
        if ("A".equals(g) || "B".equals(g) || "C".equals(g) || "D".equals(g)) {
            return g;
        }
        if ("AVOID".equals(g) || "HB".equals(g)) {
            return "回避";
        }
        return raw;
    }

    /** 淘汰等级 → 分数（与模板 I–L 行 10/8/6/4 一致） */
    private static BigDecimal gradeToScore(String grade) {
        switch (grade) {
            case "A":
                return BigDecimal.valueOf(10);
            case "B":
                return BigDecimal.valueOf(8);
            case "C":
                return BigDecimal.valueOf(6);
            case "D":
                return BigDecimal.valueOf(4);
            default:
                return null;
        }
    }

    /**
     * 计算项目淘汰评级平均分：排除回避，A/B/C/D→10/8/6/4；
     * ≥3 个有效分去最高最低再平均，1–2 个直接平均（与 QC 奖规则一致）。
     */
    private static BigDecimal calcEliminateAvgScore(List<Long> expertUids, ProjectExportData p) {
        if (expertUids == null || expertUids.isEmpty() || p == null) {
            return null;
        }
        List<BigDecimal> validScores = new ArrayList<>();
        for (Long uid : expertUids) {
            String grade = normalizeGrade(p.gradeByExpert.get(uid));
            if ("回避".equals(grade) || grade.isEmpty()) {
                continue;
            }
            BigDecimal score = gradeToScore(grade);
            if (score != null) {
                validScores.add(score);
            }
        }
        if (validScores.isEmpty()) {
            return null;
        }
        List<BigDecimal> toAvg = new ArrayList<>(validScores);
        if (toAvg.size() >= 3) {
            toAvg.sort(BigDecimal::compareTo);
            toAvg.remove(0);
            toAvg.remove(toAvg.size() - 1);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal s : toAvg) {
            sum = sum.add(s);
        }
        return sum.divide(BigDecimal.valueOf(toAvg.size()), 2, RoundingMode.HALF_UP);
    }

    private static String formatAvgScore(BigDecimal avg) {
        if (avg == null) {
            return "";
        }
        return avg.toPlainString();
    }

    private static List<GroupExportData> buildGroupData(List<Map<String, Object>> flatRows) {
        Map<String, GroupExportData> map = new LinkedHashMap<>();
        if (flatRows == null) {
            return Collections.emptyList();
        }
        for (Map<String, Object> row : flatRows) {
            String groupName = str(row.get("expertGroupName"));
            if (groupName.isEmpty()) {
                groupName = "（未命名专家组）";
            }
            GroupExportData g = map.computeIfAbsent(groupName, GroupExportData::new);
            Integer proId = toInt(row.get("proId"));
            if (proId == null) {
                continue;
            }
            ProjectExportData p = g.projects.computeIfAbsent(proId, id -> {
                ProjectExportData pd = new ProjectExportData();
                pd.proId = id;
                pd.proSubType = str(row.get("proSubType"));
                pd.proCode = str(row.get("proCode"));
                pd.declareAccount = str(row.get("declareAccount"));
                pd.topicName = str(row.get("topicName"));
                pd.groupName = str(row.get("groupName"));
                pd.companyName = str(row.get("companyName"));
                pd.eliminated = toInt(row.get("eliminated"));
                return pd;
            });
            if (p.proCode.isEmpty()) {
                p.proCode = str(row.get("proCode"));
            }
            if (p.proSubType.isEmpty()) {
                p.proSubType = str(row.get("proSubType"));
            }
            if (p.topicName.isEmpty()) {
                p.topicName = str(row.get("topicName"));
            }
            Long expertUid = toLong(row.get("expertUid"));
            if (expertUid != null) {
                g.expertOrder.putIfAbsent(expertUid, str(row.get("expertName")));
                p.gradeByExpert.put(expertUid, str(row.get("grade")));
            }
        }
        return new ArrayList<>(map.values());
    }

    private static List<RowSnapshot> captureBlock(XSSFSheet sheet, int baseRow, int rowCount, int colCount) {
        List<RowSnapshot> list = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            XSSFRow row = sheet.getRow(baseRow + i);
            RowSnapshot snap = new RowSnapshot();
            if (row != null) {
                snap.height = row.getHeight();
                for (int c = 0; c < colCount; c++) {
                    XSSFCell cell = row.getCell(c);
                    if (cell == null) {
                        continue;
                    }
                    snap.cells.put(c, new CellSnapshot(cellValueAsString(cell), cell.getCellStyle()));
                }
            }
            list.add(snap);
        }
        return list;
    }

    private static List<CellRangeAddress> captureBlockMerges(XSSFSheet sheet, int baseRow, int endRow) {
        List<CellRangeAddress> list = new ArrayList<>();
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() >= baseRow && region.getLastRow() <= endRow) {
                list.add(new CellRangeAddress(
                        region.getFirstRow() - baseRow,
                        region.getLastRow() - baseRow,
                        region.getFirstColumn(),
                        region.getLastColumn()));
            }
        }
        return list;
    }

    private static void removeMergesInRowRange(XSSFSheet sheet, int firstRow, int lastRow) {
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.getLastRow() >= firstRow && r.getFirstRow() <= lastRow) {
                sheet.removeMergedRegion(i);
            }
        }
    }

    private static String cellValueAsString(XSSFCell cell) {
        if (cell.getCellTypeEnum() == CellType.NUMERIC) {
            double n = cell.getNumericCellValue();
            if (n == Math.floor(n)) {
                return String.valueOf((long) n);
            }
            return String.valueOf(n);
        }
        if (cell.getCellTypeEnum() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        if (cell.getCellTypeEnum() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        return "";
    }

    private static void writeResponse(HttpServletResponse response, XSSFWorkbook wb, String fileName)
            throws Exception {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + encoded);
        response.setHeader("X-Surver-Eliminate-Export-Build", EXPORT_BUILD);
        response.flushBuffer();
        try (OutputStream out = response.getOutputStream()) {
            wb.write(out);
            out.flush();
        }
        wb.close();
    }

    private static XSSFRow getOrCreateRow(XSSFSheet sheet, int rowIdx) {
        XSSFRow row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        return row;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class GroupExportData {
        final String groupName;
        final Map<Integer, ProjectExportData> projects = new LinkedHashMap<>();
        final Map<Long, String> expertOrder = new LinkedHashMap<>();

        GroupExportData(String groupName) {
            this.groupName = groupName;
        }
    }

    private static final class ProjectExportData {
        int proId;
        String proSubType;
        String proCode;
        String declareAccount;
        String topicName;
        String groupName;
        String companyName;
        Integer eliminated;
        final Map<Long, String> gradeByExpert = new HashMap<>();
    }

    private static final class RowSnapshot {
        short height = 0;
        final Map<Integer, CellSnapshot> cells = new HashMap<>();
    }

    private static final class CellSnapshot {
        final String value;
        final CellStyle style;

        CellSnapshot(String value, CellStyle style) {
            this.value = value;
            this.style = style;
        }
    }
}
