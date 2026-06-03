package com.bootdo.cpe.utils;

import com.bootdo.common.utils.StringUtils;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 勘察奖专家「下载打分结果」：按 {@code excel/surver_expert_scoring_export_template.xlsx} 四 Sheet 导出本人评分明细 + 电子签章。
 */
public final class SurverExpertScoringExportUtils {

    private static final String TEMPLATE_PATH = "excel/surver_expert_scoring_export_template.xlsx";

    private static final String[] SUB_TYPES = {"contribution", "design", "software", "standard"};

    private static final SheetLayout[] LAYOUTS = {
            new SheetLayout(0, 3, 34, 37, 8, 4,
                    new String[]{"technicalLevel", "technicalDifficulty", "technicalInnovation", "economicBenefit", "materialQuality"},
                    9, 10, 11),
            new SheetLayout(1, 3, 33, 36, 11, 4,
                    new String[]{"overallTechnicalLevel", "difficultyInnovation", "digitalDesignLevel", "environmentSafety",
                            "designQuality", "energySaving", "greenConstruction", "materialQuality"},
                    12, 13, 14),
            new SheetLayout(2, 3, 33, 36, 9, 4,
                    new String[]{"technicalLevel", "technicalDifficulty", "technicalInnovation", "promotability", "economicBenefit", "materialQuality"},
                    10, 11, 12),
            new SheetLayout(3, 3, 33, 36, 9, 4,
                    new String[]{"technicalLevel", "technicalDifficulty", "technicalInnovation", "promotability", "economicBenefit", "materialQuality"},
                    10, 11, 12),
    };

    private SurverExpertScoringExportUtils() {
    }

    public static class ExportRow {
        public String declareAccount;
        public String proCode;
        public String topicName;
        public boolean avoided;
        public Map<String, Integer> scores = new LinkedHashMap<>();
        public Integer totalScore;
        public String opinionGrade;
        public String opinionText;
    }

    public static void exportByTemplate(HttpServletResponse response,
                                        String groupName,
                                        Map<String, List<ExportRow>> rowsBySubType,
                                        String signDiskPath,
                                        String expertDisplayName) throws Exception {
        try (InputStream tplIs = SurverExpertScoringExportUtils.class.getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (tplIs == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("模板文件不存在: " + TEMPLATE_PATH);
                return;
            }
            XSSFWorkbook wb = new XSSFWorkbook(tplIs);
            int pictureIdx = loadSignPicture(wb, signDiskPath);
            String dateStr = new SimpleDateFormat("yyyy.M.d").format(new Date());
            String titleGroup = StringUtils.isNotBlank(groupName) ? groupName.trim() : "专业组";

            for (int i = 0; i < SUB_TYPES.length; i++) {
                String subType = SUB_TYPES[i];
                List<ExportRow> rows = rowsBySubType != null ? rowsBySubType.get(subType) : null;
                if (rows == null) {
                    rows = new ArrayList<>();
                }
                fillSheet(wb.getSheetAt(i), LAYOUTS[i], rows, titleGroup, pictureIdx, dateStr);
            }

            String fileName = URLEncoder.encode(
                    (StringUtils.isNotBlank(expertDisplayName) ? expertDisplayName : "专家") + "_打分结果.xlsx",
                    StandardCharsets.UTF_8.name()).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"score\"; filename*=UTF-8''" + fileName);
            try (OutputStream out = response.getOutputStream()) {
                wb.write(out);
                out.flush();
            }
            wb.close();
        }
    }

    private static int loadSignPicture(XSSFWorkbook wb, String signDiskPath) {
        if (StringUtils.isBlank(signDiskPath)) {
            return -1;
        }
        File sf = new File(signDiskPath);
        if (!sf.isFile()) {
            return -1;
        }
        try {
            byte[] bytes = Files.readAllBytes(sf.toPath());
            String lower = signDiskPath.toLowerCase();
            int type = XSSFWorkbook.PICTURE_TYPE_PNG;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                type = XSSFWorkbook.PICTURE_TYPE_JPEG;
            } else if (lower.endsWith(".gif")) {
                type = XSSFWorkbook.PICTURE_TYPE_GIF;
            }
            return wb.addPicture(bytes, type);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static void fillSheet(XSSFSheet sheet, SheetLayout layout, List<ExportRow> rows,
                                  String groupName, int pictureIdx, String dateStr) {
        setCellString(sheet, 0, 0, "专业组评价打分表（" + groupName + "）");

        int dataStart = layout.dataStartRow;
        int sigRow = layout.sigRow;
        int extra = Math.max(0, rows.size() - layout.templateDataRows);
        if (extra > 0) {
            int last = sheet.getLastRowNum();
            if (sigRow <= last) {
                sheet.shiftRows(sigRow, last, extra, true, true);
            }
            XSSFRow styleRef = sheet.getRow(dataStart);
            for (int i = 0; i < extra; i++) {
                cloneRowFromTemplate(sheet, styleRef, sigRow - extra + i, layout.lastCol);
            }
        }
        int finalSigRow = sigRow + extra;

        XSSFRow styleRow = sheet.getRow(dataStart);
        for (int i = 0; i < rows.size(); i++) {
            writeDataRow(sheet, dataStart + i, styleRow, layout, i + 1, rows.get(i));
        }
        int totalSlots = layout.templateDataRows + extra;
        for (int i = rows.size(); i < totalSlots; i++) {
            clearDataRow(sheet, dataStart + i, styleRow, layout);
        }

        XSSFRow sigLabelRow = getOrCreateRow(sheet, finalSigRow);
        XSSFCell sigCell = getOrCreateCell(sigLabelRow, layout.sigCol);
        if (sigCell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK || StringUtils.isBlank(sigCell.getStringCellValue())) {
            sigCell.setCellValue("专家签字");
        }

        XSSFRow dateRow = getOrCreateRow(sheet, finalSigRow + 1);
        XSSFCell dateCell = getOrCreateCell(dateRow, layout.sigCol);
        dateCell.setCellValue("日期：" + dateStr);
        if (dateCell.getCellStyle() == null && sigCell.getCellStyle() != null) {
            dateCell.setCellStyle(sigCell.getCellStyle());
        }

        if (pictureIdx >= 0) {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            int picCol1 = Math.min(layout.sigCol + 1, layout.lastCol);
            int picCol2 = Math.min(layout.sigCol + 3, layout.lastCol + 1);
            XSSFClientAnchor anchor = new XSSFClientAnchor(600000, 80000, 0, 0,
                    picCol1, finalSigRow, picCol2, finalSigRow + 1);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIdx);
        }
    }

    private static void writeDataRow(XSSFSheet sheet, int rowIdx, XSSFRow styleRow, SheetLayout layout,
                                     int seq, ExportRow data) {
        XSSFRow row = getOrCreateRow(sheet, rowIdx);
        if (styleRow != null && row.getHeight() < 1) {
            row.setHeight(styleRow.getHeight());
        }
        setCellNumber(row, 0, seq, styleRow);
        setCellString(row, 1, data.declareAccount, styleRow);
        setCellString(row, 2, data.proCode, styleRow);
        setCellString(row, 3, data.topicName, styleRow);

        for (int s = 0; s < layout.scoreFields.length; s++) {
            Integer val = null;
            if (!data.avoided && data.scores != null) {
                val = data.scores.get(layout.scoreFields[s]);
            }
            setCellNumber(row, layout.scoreStartCol + s, val, styleRow);
        }
        setCellNumber(row, layout.totalCol, data.avoided ? null : data.totalScore, styleRow);
        setCellString(row, layout.opinionCol, formatOpinion(data), styleRow);
        setCellString(row, layout.remarkCol, data.avoided ? "已回避" : "", styleRow);
    }

    private static void clearDataRow(XSSFSheet sheet, int rowIdx, XSSFRow styleRow, SheetLayout layout) {
        XSSFRow row = getOrCreateRow(sheet, rowIdx);
        for (int c = 0; c <= layout.lastCol; c++) {
            XSSFCell cell = getOrCreateCell(row, c);
            cell.setBlank();
            if (styleRow != null && styleRow.getCell(c) != null) {
                cell.setCellStyle(styleRow.getCell(c).getCellStyle());
            }
        }
    }

    private static String formatOpinion(ExportRow data) {
        if (data == null || data.avoided) {
            return "";
        }
        String g = data.opinionGrade;
        String t = data.opinionText;
        if (StringUtils.isNotBlank(g) && StringUtils.isNotBlank(t)) {
            return g + "：" + t;
        }
        if (StringUtils.isNotBlank(g)) {
            return g;
        }
        return t != null ? t : "";
    }

    private static void cloneRowFromTemplate(XSSFSheet sheet, XSSFRow styleRow, int targetRowIdx, int lastCol) {
        if (styleRow == null) {
            return;
        }
        XSSFRow target = getOrCreateRow(sheet, targetRowIdx);
        target.setHeight(styleRow.getHeight());
        for (int c = 0; c <= lastCol; c++) {
            XSSFCell src = styleRow.getCell(c);
            if (src != null) {
                XSSFCell dst = getOrCreateCell(target, c);
                dst.setCellStyle(src.getCellStyle());
            }
        }
    }

    private static XSSFRow getOrCreateRow(XSSFSheet sheet, int rowIdx) {
        XSSFRow row = sheet.getRow(rowIdx);
        return row != null ? row : sheet.createRow(rowIdx);
    }

    private static XSSFCell getOrCreateCell(XSSFRow row, int col) {
        XSSFCell cell = row.getCell(col);
        return cell != null ? cell : row.createCell(col);
    }

    private static void setCellString(XSSFSheet sheet, int rowIdx, int col, String value) {
        XSSFRow row = getOrCreateRow(sheet, rowIdx);
        XSSFCell cell = getOrCreateCell(row, col);
        cell.setCellValue(value != null ? value : "");
    }

    private static void setCellString(XSSFRow row, int col, String value, XSSFRow styleRow) {
        XSSFCell cell = getOrCreateCell(row, col);
        cell.setCellValue(value != null ? value : "");
        applyStyle(cell, styleRow, col);
    }

    private static void setCellNumber(XSSFRow row, int col, Integer value, XSSFRow styleRow) {
        XSSFCell cell = getOrCreateCell(row, col);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
        applyStyle(cell, styleRow, col);
    }

    private static void applyStyle(XSSFCell cell, XSSFRow styleRow, int col) {
        if (styleRow != null && styleRow.getCell(col) != null && styleRow.getCell(col).getCellStyle() != null) {
            cell.setCellStyle(styleRow.getCell(col).getCellStyle());
        }
    }

    private static final class SheetLayout {
        final int sheetIndex;
        final int dataStartRow;
        final int templateDataRows;
        final int sigRow;
        final int sigCol;
        final int scoreStartCol;
        final String[] scoreFields;
        final int totalCol;
        final int opinionCol;
        final int remarkCol;
        final int lastCol;

        SheetLayout(int sheetIndex, int dataStartRow, int templateDataRows, int sigRow, int sigCol,
                    int scoreStartCol, String[] scoreFields, int totalCol, int opinionCol, int remarkCol) {
            this.sheetIndex = sheetIndex;
            this.dataStartRow = dataStartRow;
            this.templateDataRows = templateDataRows;
            this.sigRow = sigRow;
            this.sigCol = sigCol;
            this.scoreStartCol = scoreStartCol;
            this.scoreFields = scoreFields;
            this.totalCol = totalCol;
            this.opinionCol = opinionCol;
            this.remarkCol = remarkCol;
            this.lastCol = remarkCol;
        }
    }
}
