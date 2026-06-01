package com.bootdo.cpe.controller.surver;

import com.bootdo.activiti.domain.AwardScoreDetailInfo;
import com.bootdo.activiti.domain.EnterpriseDocUploadDo;
import com.bootdo.activiti.domain.EnterpriseProjectInfoDo;
import com.bootdo.activiti.service.AwardEnterpriseProjectService;
import com.bootdo.activiti.service.AwardPublishTaskService;
import com.bootdo.activiti.service.SpecialistService;
import com.bootdo.cpe.domain.EnumAwardType;
import com.bootdo.system.domain.UserDO;
import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.common.service.FileService;
import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.R;
import com.bootdo.common.utils.StringUtils;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.ExpertGroupDO;
import com.bootdo.cpe.domain.SurverExpertScoringDO;
import com.bootdo.cpe.service.ExpertGroupService;
import com.bootdo.cpe.service.SurverExpertScoringService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bootdo.common.config.Constant.ROLE_SPECIALIST_ID;
import static com.bootdo.common.config.Constant.ROLE_SURVER_SPECALIST_ID;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Controller
@RequestMapping("/surverScore")
public class SurverScoreController extends BaseSurverController {

    /** 勘察奖任务「评分标准」附件类型（协会在任务编辑页上传，专家打分页下载） */
    public static final String SURVER_SCORE_STANDARD_FILE_TYPE = "surver_score_standard_file";

    private String prefix = "cpe/survey";
    @Autowired
    private SpecialistService specialistService;
    @Autowired
    private ExpertGroupService expertGroupService;
    @Autowired
    private AwardEnterpriseProjectService awardEnterpriseProjectService;
    @Autowired
    private FileService fileService;
    @Autowired
    private BootdoConfig bootdoConfig;
    @Autowired
    private AwardPublishTaskService awardPublishTaskService;
    @Autowired
    private SurverExpertScoringService scoringService;

    /**
     * 勘察专家打分页 taskId：与 {@link SurverProController#getSurverProList} 一致（专家组绑定），
     * 不用父类「最新发布任务」。菜单若带了错误 taskId 也会覆盖。
     */
    @Override
    public void packageAwardTaskId(ModelMap map, Map<String, Object> params) {
        String resolved = resolveSurverScoreTaskId();
        if (StringUtils.isNotBlank(resolved)) {
            params.put("taskId", resolved);
        }
        super.packageAwardTaskId(map, params);
    }

    /**
     * 解析当前用户应使用的勘察任务 ID（协会上传评分标准的 taskId 须与此一致）
     */
    private String resolveSurverScoreTaskId() {
        UserDO user = getUser();
        if (user == null || user.getRoleIds() == null) {
            return null;
        }
        List<Long> roles = user.getRoleIds();
        long uid = getUserId();
        String awardType = EnumAwardType.SURVER.getAwrdType() + "";

        // 勘察评审专家(76)：与项目列表相同，取专家组绑定 taskId
        if (roles.contains(ROLE_SURVER_SPECALIST_ID)) {
            Map<String, Object> bindQuery = new HashMap<>();
            bindQuery.put("userId", String.valueOf(uid));
            bindQuery.put("proType", "surver_pro_group");
            List<ExpertGroupDO> expertBindings = expertGroupService.list(bindQuery);
            if (expertBindings != null) {
                for (ExpertGroupDO binding : expertBindings) {
                    String bindTaskId = binding.getTaskId();
                    if (StringUtils.isNotBlank(bindTaskId) && findScoreStandardDoc(bindTaskId) != null) {
                        return bindTaskId;
                    }
                }
                if (!expertBindings.isEmpty()) {
                    String bindTaskId = expertBindings.get(0).getTaskId();
                    if (StringUtils.isNotBlank(bindTaskId)) {
                        return bindTaskId;
                    }
                }
            }
        }

        // 科技奖专家(62)等：按分派项目 / 已上传评分标准任务推断
        if (roles.contains(ROLE_SPECIALIST_ID) || roles.contains(ROLE_SURVER_SPECALIST_ID)) {
            String withStandard = awardPublishTaskService.getExpertAssignTaskIdWithScoreStandard(
                    uid, awardType, SURVER_SCORE_STANDARD_FILE_TYPE);
            if (StringUtils.isNotBlank(withStandard)) {
                return withStandard;
            }
            return awardPublishTaskService.getLatestTaskIdForExpertAssign(uid, awardType);
        }
        return null;
    }

    /** 请求 taskId 无文件时，回退到 {@link #resolveSurverScoreTaskId()} */
    private String effectiveScoreStandardTaskId(String requestTaskId) {
        if (StringUtils.isNotBlank(requestTaskId) && findScoreStandardDoc(requestTaskId) != null) {
            return requestTaskId;
        }
        String resolved = resolveSurverScoreTaskId();
        return StringUtils.isNotBlank(resolved) ? resolved : requestTaskId;
    }

    @RequestMapping("/proList")
    @RequiresPermissions("surveraward:score:prolist")
    public String toSurverScorePro(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Object proSubType = params.get("proSubType");
        map.put("proSubType", proSubType);
        return prefix + "/specialist/score/score_pro_list";
    }

    /**
     * 勘察设计评级（专家打分页点击「评级」后在顶部菜单栏打开的独立 Tab）
     */
    @RequestMapping("/proRatingList")
    @RequiresPermissions("surveraward:score:prolist")
    public String toSurverScoreProRating(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Object proSubType = params.get("proSubType");
        map.put("proSubType", proSubType);
        map.put("surverExpertScorePage", true);
        return prefix + "/specialist/score/score_pro_rating_list";
    }

    /**
     * 勘察奖专家打分（菜单栏独立入口）
     */
    @RequestMapping("/proScoringList")
    @RequiresPermissions("surveraward:score:prolist")
    public String toSurverScoreProScoring(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Object proSubType = params.get("proSubType");
        map.put("proSubType", proSubType);
        map.put("surverExpertScoringPage", true);
        return prefix + "/specialist/score/score_pro_scoring_list";
    }

    /**
     * 勘察奖淘汰评语（从专家打分页点击按钮打开的新标签页）
     */
    @RequestMapping("/proEliminatedList")
    @RequiresPermissions("surveraward:score:prolist")
    public String toSurverEliminatedList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        return prefix + "/specialist/score/score_pro_eliminated_list";
    }

    /**
     * 管理员驳回专家的打分确认（重置打分确认状态，让专家可重新修改打分）
     */
    @RequestMapping("/rejectScoringConfirm")
    @ResponseBody
    @RequiresPermissions("surveraward:score:prolist")
    public R rejectScoringConfirm(@RequestParam String taskId, @RequestParam Long expertUid) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        if (expertUid == null) {
            return R.error("专家ID不能为空");
        }

        int count = scoringService.resetConfirmStatus(taskId, expertUid);
        if (count > 0) {
            return R.ok("已驳回打分确认，专家可重新修改打分");
        } else {
            return R.error("未找到该专家的打分记录或未确认状态");
        }
    }

    /**
     * 导出分数（导出专家打分结果）
     * 小组联络人：导出本组、未淘汰的分数
     * 管理员：分组显示，导出全部、未淘汰的分数
     * 导出excel分成4个sheet统计四个奖项，分数从高到低
     */
    @RequestMapping("/exportScore")
    public void exportScore(@RequestParam String taskId,
                            @RequestParam(required = false) String groupName,
                            @RequestParam(required = false, defaultValue = "false") Boolean showEliminated,
                            HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(taskId)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("任务ID不能为空");
            return;
        }

        try {
            // 获取当前用户信息
            UserDO user = getUser();
            if (user == null) {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("用户未登录");
                return;
            }

            // 判断是否为小组联络人（角色ID需要根据实际系统配置调整）
            // 小组联络人只能导出自己负责的专业组
            boolean isGroupContact = isSurverGroupContact(user);

            // 如果是小组联络人，需要确定其负责的专业组
            String exportGroupName = groupName;
            if (isGroupContact && StringUtils.isBlank(exportGroupName)) {
                // 小组联络人默认只导出自己绑定的专业组
                exportGroupName = getContactGroupName(user, taskId);
            }

            // 查询导出数据
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("taskId", taskId);
            if (StringUtils.isNotBlank(exportGroupName)) {
                queryParams.put("groupName", exportGroupName);
            }
            // showEliminated=true时导出已淘汰的，false时导出未淘汰的
            queryParams.put("eliminated", showEliminated);

            List<Map<String, Object>> dataList = scoringService.listForExport(queryParams);
            if (dataList == null || dataList.isEmpty()) {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("没有可导出的数据");
                return;
            }

            // 按proSubType分组（4个奖项）
            // contribution - 优秀勘察奖
            // design - 优秀设计奖
            // software - 优秀勘察设计计算机软件奖
            // standard - 优秀标准设计奖
            Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
            groupedData.put("contribution", new ArrayList<>());
            groupedData.put("design", new ArrayList<>());
            groupedData.put("software", new ArrayList<>());
            groupedData.put("standard", new ArrayList<>());

            for (Map<String, Object> item : dataList) {
                String proSubType = (String) item.get("proSubType");
                if (groupedData.containsKey(proSubType)) {
                    groupedData.get(proSubType).add(item);
                }
            }

            // 奖项名称映射
            Map<String, String> sheetNameMap = new LinkedHashMap<>();
            sheetNameMap.put("contribution", "优秀勘察奖");
            sheetNameMap.put("design", "优秀设计奖");
            sheetNameMap.put("software", "优秀勘察设计计算机软件奖");
            sheetNameMap.put("standard", "优秀标准设计奖");

            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 为每个奖项创建一个sheet
            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
                String proSubType = entry.getKey();
                List<Map<String, Object>> sheetData = entry.getValue();
                String sheetName = sheetNameMap.get(proSubType);

                if (sheetData.isEmpty()) {
                    // 如果没有数据，创建空sheet并提示
                    Sheet sheet = workbook.createSheet(sheetName);
                    Row emptyRow = sheet.createRow(0);
                    Cell emptyCell = emptyRow.createCell(0);
                    emptyCell.setCellValue("暂无数据");
                    continue;
                }

                // 按分数从高到低排序
                Collections.sort(sheetData, new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> a, Map<String, Object> b) {
                        Integer scoreA = (Integer) a.get("totalScore");
                        Integer scoreB = (Integer) b.get("totalScore");
                        if (scoreA == null) scoreA = 0;
                        if (scoreB == null) scoreB = 0;
                        return scoreB.compareTo(scoreA); // 降序
                    }
                });

                // 获取专业组名称
                String currentGroupName = StringUtils.isNotBlank(exportGroupName) ? exportGroupName : "全部";
                if (!sheetData.isEmpty()) {
                    Object firstGroupName = sheetData.get(0).get("groupName");
                    if (firstGroupName != null) {
                        currentGroupName = firstGroupName.toString();
                    }
                }

                createSheet(workbook, sheetName, sheetData, headerStyle, titleStyle, dataStyle, currentGroupName);
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = "勘察奖_专家打分结果.xlsx";
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"score\"; filename*=UTF-8''" + encoded);

            // 写入响应流
            OutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("导出失败：" + e.getMessage());
        }
    }

    /**
     * 创建Sheet页
     */
    private void createSheet(Workbook workbook, String sheetName, List<Map<String, Object>> data,
                            CellStyle headerStyle, CellStyle titleStyle, CellStyle dataStyle,
                            String groupName) {
        Sheet sheet = workbook.createSheet(sheetName);

        // 第1行：标题行（合并单元格）
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("专业组评价打分表（" + groupName + "）");
        titleCell.setCellStyle(titleStyle);
        // 合并A1到M1
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

        // 第2行：表头第一部分
        Row headerRow1 = sheet.createRow(1);
        Row headerRow2 = sheet.createRow(2);

        // 设置表头
        String[] headers = {"序号", "申报账号", "项目编号", "项目名称", "专家名称", "项目平均分（100）", "专家推荐等级", "形式审查意见", "备注"};
        int[] headerWidths = {8, 15, 15, 30, 10, 15, 15, 20, 20};

        int colIndex = 0;
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow1.createCell(colIndex);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);

            // 合并序号、申报账号、项目编号、项目名称的行（2-3行）
            if (i < 4) {
                sheet.addMergedRegion(new CellRangeAddress(1, 2, colIndex, colIndex));
            }
            colIndex++;

            // 在"项目名称"后插入5个专家评分列（KC1-1到KC1-5）
            if (i == 3) {
                // 先设置"专家名称"合并单元格
                Cell expertNameCell = headerRow1.createCell(colIndex);
                expertNameCell.setCellValue("专家名称");
                expertNameCell.setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, colIndex, colIndex + 4));

                // 设置KC1-1到KC1-5
                for (int j = 1; j <= 5; j++) {
                    Cell kcCell = headerRow2.createCell(colIndex + j - 1);
                    kcCell.setCellValue("KC1-" + j);
                    kcCell.setCellStyle(headerStyle);
                }
                colIndex += 5;
            }

            // 合并项目平均分、专家推荐等级、形式审查意见、备注的行（2-3行）
            if (i >= 5) {
                sheet.addMergedRegion(new CellRangeAddress(1, 2, colIndex - 1, colIndex - 1));
            }
        }

        // 第4行开始：数据行
        int rowNum = 3;
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> item = data.get(i);
            Row dataRow = sheet.createRow(rowNum++);

            int cellIdx = 0;
            // 序号
            Cell cell0 = dataRow.createCell(cellIdx++);
            cell0.setCellValue(i + 1);
            cell0.setCellStyle(dataStyle);

            // 申报账号
            Cell cell1 = dataRow.createCell(cellIdx++);
            cell1.setCellValue(getStringValue(item.get("declareAccount")));
            cell1.setCellStyle(dataStyle);

            // 项目编号
            Cell cell2 = dataRow.createCell(cellIdx++);
            cell2.setCellValue(getStringValue(item.get("proCode")));
            cell2.setCellStyle(dataStyle);

            // 项目名称
            Cell cell3 = dataRow.createCell(cellIdx++);
            cell3.setCellValue(getStringValue(item.get("topicName")));
            cell3.setCellStyle(dataStyle);

            // 专家名称
            Cell cell4 = dataRow.createCell(cellIdx++);
            cell4.setCellValue(getStringValue(item.get("expertName")));
            cell4.setCellStyle(dataStyle);

            // 专家评分（KC1-1到KC1-5，根据不同的奖项类型显示不同的评分项）
            List<Integer> scores = getScoresByType(item, data.get(0).get("proSubType").toString());
            for (int j = 0; j < 5; j++) {
                Cell scoreCell = dataRow.createCell(cellIdx++);
                if (j < scores.size() && scores.get(j) != null) {
                    scoreCell.setCellValue(scores.get(j));
                } else {
                    scoreCell.setCellValue("");
                }
                scoreCell.setCellStyle(dataStyle);
            }

            // 项目平均分
            Cell avgCell = dataRow.createCell(cellIdx++);
            Integer totalScore = (Integer) item.get("totalScore");
            if (totalScore != null) {
                avgCell.setCellValue(totalScore.doubleValue());
            }
            avgCell.setCellStyle(dataStyle);

            // 专家推荐等级
            Cell gradeCell = dataRow.createCell(cellIdx++);
            gradeCell.setCellValue(getStringValue(item.get("opinionGrade")));
            gradeCell.setCellStyle(dataStyle);

            // 形式审查意见
            Cell opinionCell = dataRow.createCell(cellIdx++);
            opinionCell.setCellValue(getStringValue(item.get("opinionText")));
            opinionCell.setCellStyle(dataStyle);

            // 备注
            Cell remarkCell = dataRow.createCell(cellIdx++);
            remarkCell.setCellValue("");
            remarkCell.setCellStyle(dataStyle);
        }

        // 设置列宽
        for (int i = 0; i < headerWidths.length; i++) {
            sheet.setColumnWidth(i, headerWidths[i] * 256);
        }
    }

    /**
     * 根据奖项类型获取评分项
     */
    private List<Integer> getScoresByType(Map<String, Object> item, String proSubType) {
        List<Integer> scores = new ArrayList<>();
        switch (proSubType) {
            case "contribution": // 优秀勘察奖
                scores.add((Integer) item.get("technicalLevel"));
                scores.add((Integer) item.get("technicalDifficulty"));
                scores.add((Integer) item.get("technicalInnovation"));
                scores.add((Integer) item.get("economicBenefit"));
                scores.add((Integer) item.get("materialQuality"));
                break;
            case "design": // 优秀设计奖
                scores.add((Integer) item.get("overallTechnicalLevel"));
                scores.add((Integer) item.get("difficultyInnovation"));
                scores.add((Integer) item.get("digitalDesignLevel"));
                scores.add((Integer) item.get("environmentSafety"));
                scores.add((Integer) item.get("designQuality"));
                break;
            case "software": // 优秀勘察设计计算机软件奖
            case "standard": // 优秀标准设计奖
                scores.add((Integer) item.get("technicalLevel"));
                scores.add((Integer) item.get("technicalDifficulty"));
                scores.add((Integer) item.get("technicalInnovation"));
                scores.add((Integer) item.get("promotability"));
                scores.add((Integer) item.get("economicBenefit"));
                break;
        }
        return scores;
    }

    /**
     * 创建标题样式
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    /**
     * 获取字符串值
     */
    private String getStringValue(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    /**
     * 判断当前用户是否为勘察奖小组联络人
     */
    private boolean isSurverGroupContact(UserDO user) {
        if (user == null || user.getRoleIds() == null) {
            return false;
        }
        // 勘察奖小组联络人角色ID，需要根据实际系统配置调整
        // 假设角色ID为80（需要确认实际值）
        return user.getRoleIds().contains(80L);
    }

    /**
     * 获取小组联络人负责的专业组名称
     */
    private String getContactGroupName(UserDO user, String taskId) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", user.getUserId());
        params.put("taskId", taskId);
        params.put("proType", "surver_view_scope");
        List<ExpertGroupDO> bindings = expertGroupService.list(params);
        if (bindings != null && !bindings.isEmpty()) {
            return bindings.get(0).getGroupName();
        }
        return null;
    }

    /**
     * 专家点击下载前校验：无文件时返回 JSON，前端 layer 弹窗提示（避免 location 跳转显示纯文本）
     */
    @ResponseBody
    @RequestMapping("/checkScoreStandardFile")
    @RequiresPermissions("surveraward:score:prolist")
    public R checkScoreStandardFile(@RequestParam(required = false) String taskId) {
        taskId = effectiveScoreStandardTaskId(taskId);
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        EnterpriseDocUploadDo doc = findScoreStandardDoc(taskId);
        if (doc == null) {
            return R.error("暂未上传评分标准文件");
        }
        String uploadPath = bootdoConfig.getUploadPath();
        String relative = doc.getUrl().replaceFirst("^/files/", "");
        File file = new File(uploadPath, relative);
        if (!file.isFile()) {
            return R.error("评分标准文件不存在或已被删除");
        }
        return R.ok();
    }

    /**
     * 专家下载当前任务已上传的「评分标准」文件（协会在任务编辑页上传）
     */
    @RequestMapping("/downloadScoreStandardFile")
    @RequiresPermissions("surveraward:score:prolist")
    public void downloadScoreStandardFile(HttpServletResponse response, @RequestParam(required = false) String taskId) throws IOException {
        taskId = effectiveScoreStandardTaskId(taskId);
        if (StringUtils.isBlank(taskId)) {
            writeScoreStandardDownloadError(response, "任务ID不能为空");
            return;
        }
        EnterpriseDocUploadDo doc = findScoreStandardDoc(taskId);
        if (doc == null) {
            writeScoreStandardDownloadError(response, "暂未上传评分标准文件");
            return;
        }
        String uploadPath = bootdoConfig.getUploadPath();
        String relative = doc.getUrl().replaceFirst("^/files/", "");
        File file = new File(uploadPath, relative);
        if (!file.isFile()) {
            writeScoreStandardDownloadError(response, "评分标准文件不存在或已被删除");
            return;
        }
        String fileName = StringUtils.isNotBlank(doc.getFileName()) ? doc.getFileName() : file.getName();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"score_standard\"; filename*=UTF-8''" + encoded);
        response.setContentLengthLong(file.length());
        try (FileInputStream in = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        }
    }

    private EnterpriseDocUploadDo findScoreStandardDoc(String taskId) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        // 任务级附件：仅按 taskId 查，不限定 pro_id=0（避免与库中 pro_id 写法不一致导致查不到）
        List<EnterpriseDocUploadDo> docList = fileService.listTaskDocInfo(params);
        if (docList == null) {
            return null;
        }
        for (EnterpriseDocUploadDo row : docList) {
            if (SURVER_SCORE_STANDARD_FILE_TYPE.equals(row.getFileType())
                    && StringUtils.isNotBlank(row.getUrl())) {
                return row;
            }
        }
        return null;
    }

    private void writeScoreStandardDownloadError(HttpServletResponse response, String msg) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(msg);
    }

    /**
     * 评分标准表格
     * @return
     */
    @RequestMapping("/standardTable")
    public String scoreStandardView() {
        return prefix + "/specialist/score/score_standard_main";
    }

    /**
     * 优秀工程勘察设计软件与优秀工程标准设计评审标准分值表
     * @return
     */
    @RequestMapping("/standardScore")
    public String scoreStandardScoreView() {
        return prefix + "/specialist/score/score_pro_standard_table";
    }

    /**
     * 优秀工程设计评审标准分值表
     * @return
     */
    @RequestMapping("/standardDesign")
    public String scoreStandardDesignView() {
        return prefix + "/specialist/score/score_pro_destin_table";
    }


    /**
     * 去打分页面
     * @param params
     * @param map
     * @return
     */
    @RequestMapping("/toScore")
    public String toScorePage(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Object proSubType = params.get("proSubType");
        map.put("proSubType", proSubType);
        map.put("major", params.get("major"));
        //科技奖只有一个内容信息进行评审，因此子项为0
        params.put("uid", getUserId());

        List<AwardScoreDetailInfo> scoreList = specialistService.getProScoreDetails(params);
        double totalScore = 0;
        for (AwardScoreDetailInfo scoreInfo : scoreList) {
            if (StringUtils.isBlank(scoreInfo.getScoreTxt())) {
                map.put(scoreInfo.getScoreKey(), scoreInfo.getScoreVal());
            } else {
                map.put(scoreInfo.getScoreKey(), scoreInfo.getScoreTxt());
            }
            totalScore += scoreInfo.getScoreVal();
        }
        map.put("totalScore", totalScore);
        map.put("itemId", 0);
        EnterpriseProjectInfoDo projectInfoDo = awardEnterpriseProjectService.get(params.get("proId").toString());
        map.put("proInfo", projectInfoDo);
        return prefix + "/specialist/score/score_major_group_table";
    }

       /**
     * 添加专家账号
     * @return
     */
    @RequestMapping("/associationViewScore")
    @RequiresPermissions("surveraward:specialist_score:check")
    public String expertCheckScore(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        List<String> majorList = awardEnterpriseProjectService.getProMajorList(params);
        map.put("profession", majorList);
        //用于入库标记账号的奖项类型
        String proType = EnumProjectType.SURVER_PRO.getProType();
        map.put("proType", proType);
        Map<String, Object> selParams = new HashMap<>();
        selParams.put("taskId", params.get("taskId"));
        selParams.put("groupName", params.get("major"));
        selParams.put("proType", params.get("proSubType"));
        List<ExpertGroupDO> selList = expertGroupService.list(selParams);
        map.put("selInfoList", selList);
        map.put("proSubType", params.get("proSubType"));

        return prefix + "/specialist/score/surver_expert_check_score";
    }

    @RequestMapping("/toCheckScoreProList")
    public String toCheckScoreProList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        map.put("account", params.get("account"));
        map.put("major", params.get("major"));
        map.put("proType", params.get("proSubType"));
        return prefix + "/specialist/score/surver_expert_check_score_pro_list";
    }


      /**
     * 获取当前用户参与的项目信息
     *
     * @param map
     * @return
     */
    @RequestMapping("/getCurLeaderPro")
    @ResponseBody
    public PageUtils getDataByScoreType(String proType, String major, String account, ModelMap map) {
        //根据用户id获取当前分派的打分任务列表
        Map<String, Object> params = new HashMap<>();
        params.put("proType", "surver_pro");
        if (StringUtils.isBlank(major)) {
            //默认指定一个不存在的专业
            major = "暂无NULL";
        }
        params.put("scoreMajor", major);
        String[] accArr = account.split("\\(");
        if (accArr.length > 1) {
            account = accArr[accArr.length - 1].replace(")", "");
        }
        if (StringUtils.isBlank(account)) {
            //指定一个暂无账号
            account = "-123La暂无";
        }

        params.put("scoreAccount", account);
        params.put("applyType", "surver");
        List<EnterpriseProjectInfoDo> list = awardEnterpriseProjectService.list(params);
        int total = awardEnterpriseProjectService.count(params);
        PageUtils pageUtils = new PageUtils(list, total);
        return pageUtils;
    }

    /**
     * 获取待打分的项目列表（排除已评级淘汰的项目）
     */
    @RequestMapping("/getScoringProjects")
    @ResponseBody
    @RequiresPermissions("surveraward:score:prolist")
    public R getScoringProjects(@RequestParam Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        String proSubType = (String) params.get("proSubType");

        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }

        // 获取当前专家用户ID
        Long expertUid = getUserId();

        // 查询该任务下该子奖项的项目（未淘汰 + 专家所在专家组的项目）
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("taskId", taskId);
        queryParams.put("proSubType", proSubType);
        queryParams.put("expertUid", expertUid);

        List<Map<String, Object>> projects = awardEnterpriseProjectService.listSurverProjects(queryParams);

        // 为每个项目附加已保存的打分数据
        if (projects != null) {
            for (Map<String, Object> project : projects) {
                Integer proId = (Integer) project.get("proId");
                SurverExpertScoringDO scoring = scoringService.getByTaskProExpert(taskId, proId, expertUid);
                if (scoring != null) {
                    Map<String, Object> scoreData = new HashMap<>();
                    scoreData.put("technicalLevel", scoring.getTechnicalLevel());
                    scoreData.put("technicalDifficulty", scoring.getTechnicalDifficulty());
                    scoreData.put("technicalInnovation", scoring.getTechnicalInnovation());
                    scoreData.put("economicBenefit", scoring.getEconomicBenefit());
                    scoreData.put("materialQuality", scoring.getMaterialQuality());
                    scoreData.put("overallTechnicalLevel", scoring.getOverallTechnicalLevel());
                    scoreData.put("difficultyInnovation", scoring.getDifficultyInnovation());
                    scoreData.put("digitalDesignLevel", scoring.getDigitalDesignLevel());
                    scoreData.put("environmentSafety", scoring.getEnvironmentSafety());
                    scoreData.put("designQuality", scoring.getDesignQuality());
                    scoreData.put("energySaving", scoring.getEnergySaving());
                    scoreData.put("greenConstruction", scoring.getGreenConstruction());
                    scoreData.put("promotability", scoring.getPromotability());
                    scoreData.put("totalScore", scoring.getTotalScore());
                    scoreData.put("opinionGrade", scoring.getOpinionGrade());
                    scoreData.put("opinionText", scoring.getOpinionText());
                    project.put("scoreData", scoreData);
                }
            }
        }

        return R.ok().put("data", projects);
    }

    /**
     * 保存打分数据
     */
    @RequestMapping("/saveScoring")
    @ResponseBody
    @RequiresPermissions("surveraward:score:prolist")
    public R saveScoring(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        // 安全转换 proId，兼容 String 和 Integer 类型
        Object proIdObj = params.get("proId");
        Integer proId = null;
        if (proIdObj != null) {
            proId = proIdObj instanceof Integer ? (Integer) proIdObj : Integer.parseInt(proIdObj.toString());
        }
        String proSubType = (String) params.get("proSubType");
        Map<String, Object> scoreData = (Map<String, Object>) params.get("scoreData");

        if (StringUtils.isBlank(taskId) || proId == null) {
            return R.error("参数不完整");
        }

        // 检查是否已确认
        Long expertUid = getUserId();
        if (scoringService.isConfirmed(taskId, expertUid)) {
            return R.error("已确认打分结果，无法修改");
        }

        // 查询是否已有记录
        SurverExpertScoringDO existing = scoringService.getByTaskProExpert(taskId, proId, expertUid);

        if (existing != null) {
            // 更新
            updateScoringData(existing, scoreData, proSubType);
            scoringService.update(existing);
        } else {
            // 新增
            SurverExpertScoringDO scoring = new SurverExpertScoringDO();
            scoring.setTaskId(taskId);
            scoring.setProId(proId);
            scoring.setProSubType(proSubType);
            scoring.setExpertUid(expertUid);
            updateScoringData(scoring, scoreData, proSubType);
            scoringService.save(scoring);
        }

        return R.ok("保存成功");
    }

    /**
     * 更新打分数据
     */
    private void updateScoringData(SurverExpertScoringDO scoring, Map<String, Object> scoreData, String proSubType) {
        scoring.setTotalScore((Integer) scoreData.get("totalScore"));
        scoring.setOpinionGrade((String) scoreData.get("opinionGrade"));
        scoring.setOpinionText((String) scoreData.get("opinionText"));

        switch (proSubType) {
            case "contribution":
                scoring.setTechnicalLevel((Integer) scoreData.get("technicalLevel"));
                scoring.setTechnicalDifficulty((Integer) scoreData.get("technicalDifficulty"));
                scoring.setTechnicalInnovation((Integer) scoreData.get("technicalInnovation"));
                scoring.setEconomicBenefit((Integer) scoreData.get("economicBenefit"));
                scoring.setMaterialQuality((Integer) scoreData.get("materialQuality"));
                break;
            case "design":
                scoring.setOverallTechnicalLevel((Integer) scoreData.get("overallTechnicalLevel"));
                scoring.setDifficultyInnovation((Integer) scoreData.get("difficultyInnovation"));
                scoring.setDigitalDesignLevel((Integer) scoreData.get("digitalDesignLevel"));
                scoring.setEnvironmentSafety((Integer) scoreData.get("environmentSafety"));
                scoring.setDesignQuality((Integer) scoreData.get("designQuality"));
                scoring.setEnergySaving((Integer) scoreData.get("energySaving"));
                scoring.setGreenConstruction((Integer) scoreData.get("greenConstruction"));
                scoring.setMaterialQuality((Integer) scoreData.get("materialQuality"));
                break;
            case "software":
            case "standard":
                scoring.setTechnicalLevel((Integer) scoreData.get("technicalLevel"));
                scoring.setTechnicalDifficulty((Integer) scoreData.get("technicalDifficulty"));
                scoring.setTechnicalInnovation((Integer) scoreData.get("technicalInnovation"));
                scoring.setPromotability((Integer) scoreData.get("promotability"));
                scoring.setEconomicBenefit((Integer) scoreData.get("economicBenefit"));
                scoring.setMaterialQuality((Integer) scoreData.get("materialQuality"));
                break;
        }
    }

    /**
     * 确认打分结果
     */
    @RequestMapping("/confirmScoring")
    @ResponseBody
    @RequiresPermissions("surveraward:score:prolist")
    public R confirmScoring(@RequestParam String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }

        Long expertUid = getUserId();

        // 检查是否已确认
        if (scoringService.isConfirmed(taskId, expertUid)) {
            return R.error("已确认打分结果，无法重复确认");
        }

        // 校验是否所有项目都已评分（排除已回避的项目）
        String[] proSubTypes = {"contribution", "design", "software", "standard"};
        List<String> unscoredProjects = new ArrayList<>();

        for (String proSubType : proSubTypes) {
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("taskId", taskId);
            queryParams.put("proSubType", proSubType);
            queryParams.put("expertUid", expertUid);

            List<Map<String, Object>> projects = awardEnterpriseProjectService.listSurverProjects(queryParams);

            if (projects != null) {
                for (Map<String, Object> project : projects) {
                    // 跳过已回避的项目
                    Object isAvoidedObj = project.get("isAvoided");
                    if (isAvoidedObj != null && (isAvoidedObj instanceof Number) && ((Number) isAvoidedObj).intValue() == 1) {
                        continue;
                    }

                    Integer proId = (Integer) project.get("proId");
                    SurverExpertScoringDO scoring = scoringService.getByTaskProExpert(taskId, proId, expertUid);
                    if (scoring == null || scoring.getTotalScore() == null) {
                        String proCode = (String) project.get("proCode");
                        String topicName = (String) project.get("topicName");
                        unscoredProjects.add(proCode + " - " + topicName);
                    }
                }
            }
        }

        if (!unscoredProjects.isEmpty()) {
            String msg = "以下项目尚未评分，无法确认：\n" + String.join("\n", unscoredProjects);
            return R.error(msg);
        }

        scoringService.confirmScoring(taskId, expertUid);
        return R.ok("确认成功");
    }

    /**
     * 下载打分结果
     */
    @RequestMapping("/downloadScoringResult")
    @RequiresPermissions("surveraward:score:prolist")
    public void downloadScoringResult(@RequestParam String taskId, HttpServletResponse response) throws IOException {
        // TODO: 实现下载功能
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("下载功能开发中");
    }

    /**
     * 获取评级淘汰的项目列表
     */
    @RequestMapping("/getEliminatedProjects")
    @ResponseBody
    @RequiresPermissions("surveraward:score:prolist")
    public R getEliminatedProjects(@RequestParam Map<String, Object> params) {
        String taskId = (String) params.get("taskId");

        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }

        // 获取当前专家用户ID
        Long expertUid = getUserId();

        // 查询该任务下评级淘汰的项目
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("taskId", taskId);
        queryParams.put("expertUid", expertUid);
        queryParams.put("eliminateType", "rating");

        List<Map<String, Object>> projects = awardEnterpriseProjectService.listSurverEliminatedProjects(queryParams);

        // 为每个项目附加已保存的主评意见数据
        if (projects != null) {
            for (Map<String, Object> project : projects) {
                Integer proId = (Integer) project.get("proId");
                SurverExpertScoringDO scoring = scoringService.getByTaskProExpert(taskId, proId, expertUid);
                if (scoring != null) {
                    project.put("opinionGrade", scoring.getOpinionGrade());
                    project.put("opinionText", scoring.getOpinionText());
                }
            }
        }

        return R.ok().put("data", projects);
    }

    /**
     * 保存淘汰项目的主评意见
     */
    @RequestMapping("/saveEliminatedOpinion")
    @ResponseBody
    @RequiresPermissions("surveraward:score:prolist")
    public R saveEliminatedOpinion(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        Integer proId = (Integer) params.get("proId");
        String opinionGrade = (String) params.get("opinionGrade");
        String opinionText = (String) params.get("opinionText");

        if (StringUtils.isBlank(taskId) || proId == null) {
            return R.error("参数不完整");
        }

        // 检查是否已确认
        Long expertUid = getUserId();
        if (scoringService.isConfirmed(taskId, expertUid)) {
            return R.error("已确认打分结果，无法修改");
        }

        // 查询是否已有记录
        SurverExpertScoringDO existing = scoringService.getByTaskProExpert(taskId, proId, expertUid);

        if (existing != null) {
            // 更新主评意见
            existing.setOpinionGrade(opinionGrade);
            existing.setOpinionText(opinionText);
            scoringService.update(existing);
        } else {
            // 新增
            SurverExpertScoringDO scoring = new SurverExpertScoringDO();
            scoring.setTaskId(taskId);
            scoring.setProId(proId);
            scoring.setExpertUid(expertUid);
            scoring.setOpinionGrade(opinionGrade);
            scoring.setOpinionText(opinionText);
            scoringService.save(scoring);
        }

        return R.ok("保存成功");
    }

}
