package com.bootdo.cpe.utils;

import com.bootdo.cpe.domain.SurverProjectInfo;
import com.deepoove.poi.XWPFTemplate;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 勘察设计奖专家「主评意见」Word 导出。
 * 版式以 classpath:word/surver_main_review_opinion_template.docx 为准，占位符由 poi-tl 填充（保留模板样式）。
 */
public class PoiWordSurverMainReviewUtils {

    private static final Logger log = LoggerFactory.getLogger(PoiWordSurverMainReviewUtils.class);

    public static final String TEMPLATE_CLASSPATH = "word/surver_main_review_opinion_template.docx";

    public static final String TITLE_LINE1 = "2026 年度石油工程建设优秀勘察设计奖";
    public static final String TITLE_LINE2 = "评价意见表";

    private PoiWordSurverMainReviewUtils() {
    }

    /**
     * 按 classpath 模板生成单份评价意见表 docx。
     */
    public static byte[] renderOneDocx(SurverProjectInfo pro, String mainReviewText) throws IOException {
        Map<String, Object> data = buildRenderData(pro, mainReviewText);
        byte[] tplBytes = loadTemplateBytes();
        try {
            return renderWithPoiTl(tplBytes, data);
        } catch (Exception ex) {
            log.warn("poi-tl 渲染主评意见模板失败，回退 POI 替换: {}", ex.getMessage());
            return renderWithPoiReplace(tplBytes, data);
        }
    }

    /** 兼容旧调用 */
    public static byte[] renderOneDocx(String declareAccount, String proName, String mainReviewText) throws IOException {
        SurverProjectInfo pro = new SurverProjectInfo();
        pro.setDeclareAccount(declareAccount);
        pro.setProName(proName);
        return renderOneDocx(pro, mainReviewText);
    }

    private static byte[] renderWithPoiTl(byte[] tplBytes, Map<String, Object> data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(tplBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFTemplate template = XWPFTemplate.compile(in).render(data);
            try {
                template.write(out);
            } finally {
                template.close();
            }
            return out.toByteArray();
        }
    }

    private static byte[] renderWithPoiReplace(byte[] tplBytes, Map<String, Object> data) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(tplBytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            replacePlaceholders(doc, data);
            doc.write(out);
            return out.toByteArray();
        }
    }

    private static Map<String, Object> buildRenderData(SurverProjectInfo pro, String mainReviewText) {
        Map<String, Object> data = new HashMap<>();
        data.put("titleLine1", TITLE_LINE1);
        data.put("titleLine2", TITLE_LINE2);
        data.put("applyCompany", pro != null ? nullToEmpty(pro.getApplyCompany()) : "");
        data.put("completeCompany", pro != null ? nullToEmpty(resolveCompleteCompany(pro)) : "");
        data.put("declareAccount", pro != null ? nullToEmpty(pro.getDeclareAccount()) : "");
        data.put("proName", pro != null ? nullToEmpty(pro.getProName()) : "");
        data.put("groupName", pro != null ? nullToEmpty(resolveGroupName(pro)) : "");
        data.put("mainReviewText", nullToEmpty(mainReviewText));
        return data;
    }

    private static String resolveGroupName(SurverProjectInfo pro) {
        if (pro.getQcGroupName() != null && !pro.getQcGroupName().trim().isEmpty()) {
            return pro.getQcGroupName().trim();
        }
        return "";
    }

    private static String resolveCompleteCompany(SurverProjectInfo pro) {
        return "";
    }

    /**
     * 每次从 classpath 读取模板（不缓存），确保 resources 下 docx 修改后重新编译即可生效。
     * 原逻辑：isNewLayoutTemplate 校验失败会退回代码内置简易模板，导致与用户编辑的 docx 样式不一致。
     */
    public static byte[] loadTemplateBytes() throws IOException {
        byte[] fromClasspath = readClasspathTemplate();
        if (canOpenAsDocx(fromClasspath) && containsPlaceholders(fromClasspath)) {
            return fromClasspath;
        }
        log.warn("classpath 模板 {} 不可用，使用内置默认模板", TEMPLATE_CLASSPATH);
        return buildDefaultTemplateBytes();
    }

    private static boolean containsPlaceholders(byte[] bytes) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return collectAllText(doc).contains("{{");
        } catch (Exception e) {
            return false;
        }
    }

    private static String collectAllText(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : doc.getParagraphs()) {
            sb.append(p.getText());
        }
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    sb.append(cell.getText());
                }
            }
        }
        return sb.toString();
    }

    private static boolean canOpenAsDocx(byte[] bytes) {
        if (!isValidDocxZip(bytes)) {
            return false;
        }
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return doc.getParagraphs() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static byte[] readClasspathTemplate() throws IOException {
        try (InputStream in = PoiWordSurverMainReviewUtils.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_CLASSPATH)) {
            if (in == null) {
                return null;
            }
            return readAllBytes(in);
        }
    }

    private static boolean isValidDocxZip(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04;
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /** POI 回退替换（会损失部分样式，仅 poi-tl 失败时使用） */
    private static void replacePlaceholders(XWPFDocument doc, Map<String, Object> data) {
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            replaceInParagraph(paragraph, data);
        }
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceInParagraph(paragraph, data);
                    }
                }
            }
        }
    }

    private static void replaceInParagraph(XWPFParagraph paragraph, Map<String, Object> data) {
        String text = paragraph.getText();
        if (text == null || text.isEmpty() || !text.contains("{{")) {
            return;
        }
        String replaced = text;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (replaced.contains(placeholder)) {
                replaced = replaced.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        if (replaced.equals(text)) {
            return;
        }
        List<XWPFRun> runs = paragraph.getRuns();
        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        if (text.contains("{{mainReviewText}}")) {
            writeMultilineRun(paragraph, replaced, 12);
            return;
        }
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("宋体");
        run.setFontSize(12);
        run.setText(replaced);
    }

    private static void writeMultilineRun(XWPFParagraph paragraph, String text, int fontSize) {
        String[] lines = text.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                paragraph.createRun().addBreak();
            }
            XWPFRun run = paragraph.createRun();
            run.setFontFamily("宋体");
            run.setFontSize(fontSize);
            run.setText(lines[i]);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 开发用：用代码生成默认模板到 resources（与手工模板占位符一致）。
     */
    public static void main(String[] args) throws Exception {
        Path out = Paths.get("src/main/resources/word/surver_main_review_opinion_template.docx");
        Files.createDirectories(out.getParent());
        byte[] bytes = buildDefaultTemplateBytes();
        try (OutputStream os = Files.newOutputStream(out)) {
            os.write(bytes);
        }
        System.out.println("Wrote template: " + out.toAbsolutePath() + " (" + bytes.length + " bytes)");
    }

    static byte[] buildDefaultTemplateBytes() throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            XWPFParagraph title1 = doc.createParagraph();
            title1.setAlignment(ParagraphAlignment.CENTER);
            title1.setSpacingAfter(0);
            XWPFRun t1 = title1.createRun();
            t1.setBold(true);
            t1.setFontFamily("宋体");
            t1.setFontSize(16);
            t1.setText("{{titleLine1}}");

            XWPFParagraph title2 = doc.createParagraph();
            title2.setAlignment(ParagraphAlignment.CENTER);
            title2.setSpacingAfter(200);
            XWPFRun t2 = title2.createRun();
            t2.setBold(true);
            t2.setFontFamily("宋体");
            t2.setFontSize(16);
            t2.setText("{{titleLine2}}");

            XWPFTable table = doc.createTable(7, 1);
            table.setWidth("100%");

            setLabelValueRow(table.getRow(0), "申报单位：", "{{applyCompany}}");
            setLabelValueRow(table.getRow(1), "完成单位：", "{{completeCompany}}");
            setLabelValueRow(table.getRow(2), "资料编号（申报账号）：", "{{declareAccount}}");
            setLabelValueRow(table.getRow(3), "课题名称：", "{{proName}}");
            setLabelValueRow(table.getRow(4), "小组名称：", "{{groupName}}");

            XWPFTableRow opinionRow = table.getRow(5);
            setRowHeight(opinionRow, 5600);
            XWPFTableCell opinionCell = opinionRow.getCell(0);
            clearCell(opinionCell);
            XWPFParagraph opLabel = opinionCell.addParagraph();
            XWPFRun opLabelRun = opLabel.createRun();
            opLabelRun.setBold(true);
            opLabelRun.setFontFamily("宋体");
            opLabelRun.setFontSize(12);
            opLabelRun.setText("评价意见：");
            XWPFParagraph opBody = opinionCell.addParagraph();
            opBody.createRun().setText("{{mainReviewText}}");

            XWPFTableRow signRow = table.getRow(6);
            setRowHeight(signRow, 2400);
            XWPFTableCell signCell = signRow.getCell(0);
            clearCell(signCell);
            signCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTTOM);
            addCenteredParagraph(signCell, "主评专家：");
            addCenteredParagraph(signCell, "副评专家：");
            addCenteredParagraph(signCell, "2026 年    月    日");

            XWPFParagraph noteTitle = doc.createParagraph();
            noteTitle.setSpacingBefore(160);
            XWPFRun noteTitleRun = noteTitle.createRun();
            noteTitleRun.setFontFamily("宋体");
            noteTitleRun.setFontSize(10);
            noteTitleRun.setText("说明：");

            XWPFParagraph note1 = doc.createParagraph();
            XWPFRun n1 = note1.createRun();
            n1.setFontFamily("宋体");
            n1.setFontSize(10);
            n1.setText("1、评价意见包括：亮点、不足和改进措施等内容；");

            XWPFParagraph note2 = doc.createParagraph();
            XWPFRun n2 = note2.createRun();
            n2.setFontFamily("宋体");
            n2.setFontSize(10);
            n2.setText("2、评价意见由主评专家和副评专家签字。");

            doc.write(bos);
            return bos.toByteArray();
        }
    }

    private static void setLabelValueRow(XWPFTableRow row, String label, String valuePlaceholder) {
        XWPFTableCell cell = row.getCell(0);
        clearCell(cell);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun labelRun = p.createRun();
        labelRun.setBold(true);
        labelRun.setFontFamily("宋体");
        labelRun.setFontSize(12);
        labelRun.setText(label);
        XWPFRun valueRun = p.createRun();
        valueRun.setFontFamily("宋体");
        valueRun.setFontSize(12);
        valueRun.setText(valuePlaceholder);
        setRowHeight(row, 600);
    }

    private static void clearCell(XWPFTableCell cell) {
        int n = cell.getParagraphs().size();
        for (int i = n - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
    }

    private static void addCenteredParagraph(XWPFTableCell cell, String text) {
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(120);
        XWPFRun run = p.createRun();
        run.setFontFamily("宋体");
        run.setFontSize(12);
        run.setText(text);
    }

    private static void setRowHeight(XWPFTableRow row, int heightTwips) {
        row.setHeight(heightTwips);
        if (row.getCtRow().getTrPr() == null) {
            row.getCtRow().addNewTrPr();
        }
        if (row.getCtRow().getTrPr().sizeOfTrHeightArray() == 0) {
            row.getCtRow().getTrPr().addNewTrHeight();
        }
        row.getCtRow().getTrPr().getTrHeightArray(0).setVal(BigInteger.valueOf(heightTwips));
        row.getCtRow().getTrPr().getTrHeightArray(0).setHRule(STHeightRule.EXACT);
    }

}
