package com.bootdo.cpe.utils;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 导出专家淘汰Word文档工具类
 * 生成"初审不合格成果表"
 */
public class PoiWordEliminateUtils {
    private static final Logger logger = LoggerFactory.getLogger(PoiWordEliminateUtils.class);

    /**
     * 生成淘汰Word并写入response
     *
     * @param response       HTTP响应
     * @param expertName     专家姓名
     * @param dataList       淘汰项目列表，每条包含 proCode/topicName/groupName/companyName/reason
     * @param year           年份（如 2026）
     * @param signImagePath  专家签章图片磁盘绝对路径，可为null
     */
    public static void exportEliminateWord(HttpServletResponse response, String expertName,
                                            List<Map<String, Object>> dataList, String year,
                                            String signImagePath) throws Exception {
        XWPFDocument document = new XWPFDocument();

        // ===== 设置页面为横向A4 =====
        CTDocument1 ctDoc = document.getDocument();
        CTBody ctBody = ctDoc.getBody();
        CTSectPr sectPr = ctBody.isSetSectPr() ? ctBody.getSectPr() : ctBody.addNewSectPr();
        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        // A4横向: width=16838, height=11906 (单位twips)
        pageSize.setW(BigInteger.valueOf(16838));
        pageSize.setH(BigInteger.valueOf(11906));
        pageSize.setOrient(STPageOrientation.LANDSCAPE);
        // 设置页边距
        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(1134));    // ~2cm
        pageMar.setBottom(BigInteger.valueOf(1134));
        pageMar.setLeft(BigInteger.valueOf(1418));   // ~2.5cm
        pageMar.setRight(BigInteger.valueOf(1418));

        // ===== 标题行1 =====
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(0);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(year + "年度石油工程建设优秀质量管理小组活动成果评价 资料评分前");
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setFontFamily("宋体");

        // ===== 标题行2 =====
        XWPFParagraph subTitlePara = document.createParagraph();
        subTitlePara.setAlignment(ParagraphAlignment.CENTER);
        subTitlePara.setSpacingAfter(200);
        XWPFRun subTitleRun = subTitlePara.createRun();
        subTitleRun.setText("初审不合格成果表");
        subTitleRun.setBold(true);
        subTitleRun.setFontSize(16);
        subTitleRun.setFontFamily("宋体");

        // ===== 表格 =====
        String[] headers = {"序号", "资料编号\n（申报账号）", "课题名称", "小组名称", "申报单位",
                "初审意见（简述不合格理由，如不符合准则\n指出具体内容）"};
        int[] colWidths = {800, 1800, 3500, 2000, 2500, 4200};

        int dataRows = Math.max(dataList.size(), 9); // 最少9行
        XWPFTable table = document.createTable(dataRows + 1, headers.length);
        table.setWidth("100%");

        // 设置表格边框
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        setBorder(borders.addNewTop());
        setBorder(borders.addNewBottom());
        setBorder(borders.addNewLeft());
        setBorder(borders.addNewRight());
        setBorder(borders.addNewInsideH());
        setBorder(borders.addNewInsideV());

        // 表头
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            setCellWidth(cell, colWidths[i]);
            setCellText(cell, headers[i], true, 11);
            setCellVAlign(cell);
        }

        // 数据行
        for (int i = 0; i < dataRows; i++) {
            XWPFTableRow row = table.getRow(i + 1);
            // 设置最小行高
            CTTrPr trPr = row.getCtRow().isSetTrPr() ? row.getCtRow().getTrPr() : row.getCtRow().addNewTrPr();
            CTHeight ht = trPr.addNewTrHeight();
            ht.setVal(BigInteger.valueOf(567)); // ~1cm min height

            if (i < dataList.size()) {
                Map<String, Object> item = dataList.get(i);
                String[] values = {
                        String.valueOf(i + 1),
                        getStr(item, "proCode"),
                        getStr(item, "topicName"),
                        getStr(item, "groupName"),
                        getStr(item, "companyName"),
                        getStr(item, "reason")
                };
                for (int j = 0; j < values.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    setCellWidth(cell, colWidths[j]);
                    setCellText(cell, values[j], false, 11);
                    setCellVAlign(cell);
                }
            } else {
                // 空行，只填序号
                XWPFTableCell numCell = row.getCell(0);
                setCellWidth(numCell, colWidths[0]);
                setCellText(numCell, String.valueOf(i + 1), false, 11);
                setCellVAlign(numCell);
                for (int j = 1; j < headers.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    setCellWidth(cell, colWidths[j]);
                    setCellVAlign(cell);
                }
            }
        }

        // ===== 空行 =====
        XWPFParagraph emptyPara = document.createParagraph();
        emptyPara.setSpacingAfter(0);
        emptyPara.createRun().setText("");

        // ===== 签字行：用无边框2列表格实现左右对齐 =====
        XWPFTable signTable = document.createTable(1, 2);
        signTable.setWidth("100%");
        // 去掉边框
        CTTblPr signTblPr = signTable.getCTTbl().getTblPr();
        CTTblBorders signBorders = signTblPr.isSetTblBorders() ? signTblPr.getTblBorders() : signTblPr.addNewTblBorders();
        setNoBorder(signBorders.addNewTop());
        setNoBorder(signBorders.addNewBottom());
        setNoBorder(signBorders.addNewLeft());
        setNoBorder(signBorders.addNewRight());
        setNoBorder(signBorders.addNewInsideH());
        setNoBorder(signBorders.addNewInsideV());

        // 左侧：专家签字： + 签章图片
        XWPFTableCell signCell = signTable.getRow(0).getCell(0);
        signCell.removeParagraph(0);
        XWPFParagraph signPara = signCell.addParagraph();
        signPara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun signRun = signPara.createRun();
        signRun.setText("专家签字：");
        signRun.setFontSize(12);
        signRun.setFontFamily("宋体");
        // 嵌入签章图片
        if (signImagePath != null) {
            File signFile = new File(signImagePath);
            if (signFile.exists()) {
                try {
                    InputStream signIs = new FileInputStream(signFile);
                    String ext = signImagePath.substring(signImagePath.lastIndexOf(".") + 1).toLowerCase();
                    int picType = Document.PICTURE_TYPE_PNG;
                    if ("jpg".equals(ext) || "jpeg".equals(ext)) {
                        picType = Document.PICTURE_TYPE_JPEG;
                    } else if ("gif".equals(ext)) {
                        picType = Document.PICTURE_TYPE_GIF;
                    } else if ("bmp".equals(ext)) {
                        picType = Document.PICTURE_TYPE_BMP;
                    }
                    // 签章图片宽75pt 高40pt
                    XWPFRun imgRun = signPara.createRun();
                    imgRun.addPicture(signIs, picType, signFile.getName(),
                            Units.toEMU(75), Units.toEMU(40));
                    signIs.close();
                } catch (Exception e) {
                    logger.error("嵌入签章图片失败: {}", e.getMessage());
                }
            }
        }

        // 右侧：年 月 日（使用实际导出时间）
        XWPFTableCell dateCell = signTable.getRow(0).getCell(1);
        dateCell.removeParagraph(0);
        XWPFParagraph datePara = dateCell.addParagraph();
        datePara.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun dateRun = datePara.createRun();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        String currentDate = sdf.format(new Date());
        dateRun.setText(currentDate);
        dateRun.setFontSize(12);
        dateRun.setFontFamily("宋体");

        // ===== 写入响应 =====
        String fileName = "初审不合格成果表_" + expertName + ".docx";
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

        OutputStream out = response.getOutputStream();
        document.write(out);
        out.flush();
        out.close();
        document.close();
    }

    private static void setBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setColor("000000");
        border.setSpace(BigInteger.valueOf(0));
    }

    private static void setNoBorder(CTBorder border) {
        border.setVal(STBorder.NONE);
        border.setSz(BigInteger.valueOf(0));
        border.setSpace(BigInteger.valueOf(0));
    }

    private static void setCellWidth(XWPFTableCell cell, int width) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth tw = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tw.setW(BigInteger.valueOf(width));
        tw.setType(STTblWidth.DXA);
    }

    private static void setCellVAlign(XWPFTableCell cell) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTVerticalJc vJc = tcPr.isSetVAlign() ? tcPr.getVAlign() : tcPr.addNewVAlign();
        vJc.setVal(STVerticalJc.CENTER);
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean bold, int fontSize) {
        // 清空默认段落
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingAfter(0);
        para.setSpacingBefore(0);

        if (text != null && text.contains("\n")) {
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                XWPFRun run = para.createRun();
                run.setText(lines[i]);
                run.setBold(bold);
                run.setFontSize(fontSize);
                run.setFontFamily("宋体");
                if (i < lines.length - 1) {
                    run.addBreak();
                }
            }
        } else {
            XWPFRun run = para.createRun();
            run.setText(text != null ? text : "");
            run.setBold(bold);
            run.setFontSize(fontSize);
            run.setFontFamily("宋体");
        }
    }

    private static String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
