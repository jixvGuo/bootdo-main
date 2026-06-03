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
import com.bootdo.cpe.service.SurverExpertAvoidanceService;
import com.bootdo.cpe.service.SurverExpertScoringService;
import com.bootdo.cpe.utils.SurverExpertScoringExportUtils;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.bootdo.common.config.Constant.ROLE_SPECIALIST_ID;
import static com.bootdo.common.config.Constant.ROLE_SURVER_SPECALIST_ID;
import static com.bootdo.common.config.Constant.ROLE_ASSOCIATION_LEADER;
import static com.bootdo.common.config.Constant.ROLE_SURVER_ASSOCIATION_ID;
import static com.bootdo.common.config.Constant.ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID;
import static com.bootdo.common.config.Constant.ROLE_SURVER_GROUP_CONTACT_ID;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    @Autowired
    private SurverExpertAvoidanceService surverExpertAvoidanceService;

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
    public R rejectScoringConfirm(@RequestParam String taskId, @RequestParam Long expertUid) {
        // 权限检查：仅管理员/协会领导/协会联系人/外聘专家/小组联络人可操作
        UserDO user = getUser();
        List<Long> roleIdList = user != null ? user.getRoleIds() : Collections.emptyList();
        boolean isAdmin = roleIdList.contains(ROLE_ASSOCIATION_LEADER)
                || roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)
                || roleIdList.contains(ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID)
                || roleIdList.contains(ROLE_SURVER_GROUP_CONTACT_ID);
        if (!isAdmin) {
            return R.error("无权操作：仅管理员可驳回专家打分确认");
        }

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
                    // 如果没有数据，创建带表头的空sheet
                    createSheet(workbook, sheetName, sheetData, headerStyle, titleStyle, dataStyle, "全部");
                    continue;
                }

                // 原：按专家单行 totalScore 排序（现由 createSheet 内按项目平均分排序）
                // Collections.sort(sheetData, ...);

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

    /** 导出表「专家名称」下固定 5 列（与模板 A~M 一致） */
    private static final int EXPORT_EXPERT_COL_COUNT = 5;

    private static final class ExportExpertColumn {
        Long expertUid;
        String name;
    }

    /**
     * 创建Sheet页
     */
    private void createSheet(Workbook workbook, String sheetName, List<Map<String, Object>> data,
                            CellStyle headerStyle, CellStyle titleStyle, CellStyle dataStyle,
                            String groupName) {
        Sheet sheet = workbook.createSheet(sheetName);

        List<ExportExpertColumn> expertColumns = resolveExportExpertColumns(data, EXPORT_EXPERT_COL_COUNT);
        List<Map<String, Object>> projectRows = buildExportProjectRows(data, expertColumns);
        Collections.sort(projectRows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                BigDecimal scoreA = toBigDecimal(a.get("avgScore"));
                BigDecimal scoreB = toBigDecimal(b.get("avgScore"));
                if (scoreA == null) scoreA = BigDecimal.ZERO;
                if (scoreB == null) scoreB = BigDecimal.ZERO;
                return scoreB.compareTo(scoreA);
            }
        });

        // 第1行：标题行（合并单元格）
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("专业组评价打分表（" + groupName + "）");
        titleCell.setCellStyle(titleStyle);
        // 合并A1到M1
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

        // 第2-3行：表头（与模板一致，共 13 列 A~M）
        Row headerRow1 = sheet.createRow(1);
        Row headerRow2 = sheet.createRow(2);

        String[] leadHeaders = {"序号", "申报账号", "项目编号", "项目名称"};
        for (int c = 0; c < leadHeaders.length; c++) {
            Cell cell = headerRow1.createCell(c);
            cell.setCellValue(leadHeaders[c]);
            cell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, c, c));
        }

        // 专家名称（第 2 行跨 5 列）+ 第 3 行：实际专家姓名（原 KC1-1 … KC1-5 占位）
        Cell expertNameCell = headerRow1.createCell(4);
        expertNameCell.setCellValue("专家名称");
        expertNameCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 4, 8));
        for (int j = 0; j < EXPORT_EXPERT_COL_COUNT; j++) {
            Cell nameCell = headerRow2.createCell(4 + j);
            String label = j < expertColumns.size() ? expertColumns.get(j).name : "";
            nameCell.setCellValue(label);
            nameCell.setCellStyle(headerStyle);
        }

        String[] tailHeaders = {"项目平均分（100）", "专家推荐等级", "形式审查意见", "备注"};
        int[] tailCols = {9, 10, 11, 12};
        for (int i = 0; i < tailHeaders.length; i++) {
            Cell cell = headerRow1.createCell(tailCols[i]);
            cell.setCellValue(tailHeaders[i]);
            cell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, tailCols[i], tailCols[i]));
        }

        // 第4行开始：数据行（每行一个项目，专家列填各专家 totalScore）
        int rowNum = 3;
        for (int i = 0; i < projectRows.size(); i++) {
            Map<String, Object> item = projectRows.get(i);
            Row dataRow = sheet.createRow(rowNum++);

            int cellIdx = 0;
            Cell cell0 = dataRow.createCell(cellIdx++);
            cell0.setCellValue(i + 1);
            cell0.setCellStyle(dataStyle);

            Cell cell1 = dataRow.createCell(cellIdx++);
            cell1.setCellValue(getStringValue(item.get("declareAccount")));
            cell1.setCellStyle(dataStyle);

            Cell cell2 = dataRow.createCell(cellIdx++);
            cell2.setCellValue(getStringValue(item.get("proCode")));
            cell2.setCellStyle(dataStyle);

            Cell cell3 = dataRow.createCell(cellIdx++);
            cell3.setCellValue(getStringValue(item.get("topicName")));
            cell3.setCellStyle(dataStyle);

            @SuppressWarnings("unchecked")
            List<Integer> expertScores = (List<Integer>) item.get("expertScores");
            for (int j = 0; j < EXPORT_EXPERT_COL_COUNT; j++) {
                Cell scoreCell = dataRow.createCell(cellIdx++);
                Integer score = expertScores != null && j < expertScores.size() ? expertScores.get(j) : null;
                if (score != null) {
                    scoreCell.setCellValue(score.doubleValue());
                } else {
                    scoreCell.setCellValue("");
                }
                scoreCell.setCellStyle(dataStyle);
            }

            Cell avgCell = dataRow.createCell(cellIdx++);
            Object avgScoreObj = item.get("avgScore");
            if (avgScoreObj instanceof BigDecimal) {
                avgCell.setCellValue(((BigDecimal) avgScoreObj).doubleValue());
            } else if (avgScoreObj instanceof Number) {
                avgCell.setCellValue(((Number) avgScoreObj).doubleValue());
            }
            avgCell.setCellStyle(dataStyle);

            Cell gradeCell = dataRow.createCell(cellIdx++);
            gradeCell.setCellValue(getStringValue(item.get("opinionGrade")));
            gradeCell.setCellStyle(dataStyle);

            Cell opinionCell = dataRow.createCell(cellIdx++);
            opinionCell.setCellValue(getStringValue(item.get("opinionText")));
            opinionCell.setCellStyle(dataStyle);

            Cell remarkCell = dataRow.createCell(cellIdx++);
            remarkCell.setCellValue("");
            remarkCell.setCellStyle(dataStyle);
        }

        int[] colWidths = {8, 15, 15, 30, 12, 12, 12, 12, 12, 15, 15, 20, 20};
        for (int i = 0; i < colWidths.length; i++) {
            sheet.setColumnWidth(i, colWidths[i] * 256);
        }
    }

    /** 从导出数据中解析专家列（按 expertUid 升序，最多 5 人） */
    private List<ExportExpertColumn> resolveExportExpertColumns(List<Map<String, Object>> data, int maxCols) {
        Map<Long, String> ordered = new TreeMap<>();
        for (Map<String, Object> item : data) {
            Long uid = toLong(item.get("expertUid"));
            if (uid == null) {
                continue;
            }
            ordered.putIfAbsent(uid, getStringValue(item.get("expertName")));
        }
        List<ExportExpertColumn> cols = new ArrayList<>();
        for (Map.Entry<Long, String> e : ordered.entrySet()) {
            if (cols.size() >= maxCols) {
                break;
            }
            ExportExpertColumn col = new ExportExpertColumn();
            col.expertUid = e.getKey();
            String name = e.getValue();
            col.name = StringUtils.isNotBlank(name) ? name : ("专家" + e.getKey());
            cols.add(col);
        }
        return cols;
    }

    /** 按项目聚合：一行一项目，专家列对应该专家 totalScore */
    private List<Map<String, Object>> buildExportProjectRows(List<Map<String, Object>> data,
                                                             List<ExportExpertColumn> expertColumns) {
        Map<Integer, LinkedHashMap<Long, Map<String, Object>>> byPro = new LinkedHashMap<>();
        for (Map<String, Object> item : data) {
            Integer proId = toInt(item.get("proId"));
            Long uid = toLong(item.get("expertUid"));
            if (proId == null) {
                continue;
            }
            byPro.computeIfAbsent(proId, k -> new LinkedHashMap<>());
            if (uid != null) {
                byPro.get(proId).put(uid, item);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LinkedHashMap<Long, Map<String, Object>> expertMap : byPro.values()) {
            if (expertMap.isEmpty()) {
                continue;
            }
            Map<String, Object> sample = expertMap.values().iterator().next();
            Map<String, Object> row = new HashMap<>();
            row.put("declareAccount", sample.get("declareAccount"));
            row.put("proCode", sample.get("proCode"));
            row.put("topicName", sample.get("topicName"));

            List<Integer> expertScores = new ArrayList<>();
            List<BigDecimal> validScores = new ArrayList<>();
            LinkedHashSet<String> grades = new LinkedHashSet<>();
            LinkedHashSet<String> opinions = new LinkedHashSet<>();
            for (ExportExpertColumn ec : expertColumns) {
                Map<String, Object> exp = expertMap.get(ec.expertUid);
                Integer ts = exp != null ? toInt(exp.get("totalScore")) : null;
                expertScores.add(ts);
                // 排除空值（回避的评分在数据库中为null）
                if (ts != null) {
                    validScores.add(BigDecimal.valueOf(ts));
                }
                if (exp != null) {
                    String g = getStringValue(exp.get("opinionGrade"));
                    if (StringUtils.isNotBlank(g)) {
                        grades.add(g);
                    }
                    String o = getStringValue(exp.get("opinionText"));
                    if (StringUtils.isNotBlank(o)) {
                        opinions.add(o);
                    }
                }
            }
            row.put("expertScores", expertScores);
            // 计算项目平均分：≥3个有效分时去掉最高最低再平均，1-2个直接平均，保留两位小数
            row.put("avgScore", calcAvgScore(validScores));
            row.put("opinionGrade", String.join("；", grades));
            row.put("opinionText", String.join("；", opinions));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 计算项目平均分：
     * 1. 排除回避和空值
     * 2. ≥3 个有效分时，去掉最高最低再平均
     * 3. 1-2 个有效分时，直接平均
     * 4. 保持两位有效小数
     */
    private BigDecimal calcAvgScore(List<BigDecimal> validScores) {
        if (validScores == null || validScores.isEmpty()) {
            return null;
        }
        List<BigDecimal> toAvg = new ArrayList<>(validScores);
        // ≥3 个有效分时，去掉最高最低
        if (toAvg.size() >= 3) {
            toAvg.sort(BigDecimal::compareTo);
            toAvg.remove(0);                    // 去掉最低分
            toAvg.remove(toAvg.size() - 1);     // 去掉最高分
        }
        // 计算平均值
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal s : toAvg) {
            sum = sum.add(s);
        }
        return sum.divide(BigDecimal.valueOf(toAvg.size()), 2, RoundingMode.HALF_UP);
    }

    private Integer toInt(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 原：KC 列填各打分维度（technicalLevel 等），与表头「专家名称」含义不符
    /*
    private List<Integer> getScoresByType(Map<String, Object> item, String proSubType) {
        ...
    }
    */

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
        System.out.println("[勘察奖] getScoringProjects 方法被调用");
        String taskId = (String) params.get("taskId");
        String proSubType = (String) params.get("proSubType");

        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }

        // 获取当前专家用户ID
        Long expertUid = getUserId();

        // 展示列表时也触发自动回避检查，确保所有回避状态是最新的
        try {
            // 从专家绑定信息中获取公司名称（UserDO中的companyName可能为空）
            Map<String, Object> bindQuery = new HashMap<>();
            bindQuery.put("userId", String.valueOf(expertUid));
            bindQuery.put("proType", "surver_pro_group");
            bindQuery.put("taskId", taskId);
            List<ExpertGroupDO> bindings = expertGroupService.list(bindQuery);
            if (bindings != null && !bindings.isEmpty()) {
                String expertCompany = bindings.get(0).getCompany();
                if (StringUtils.isNotBlank(expertCompany)) {
                    int avoidCount = surverExpertAvoidanceService.autoAvoidByCompany(taskId, expertUid.intValue(), expertCompany);
                    System.out.println("[勘察奖自动回避] 展示列表时触发：专家单位[" + expertCompany + "]，自动回避 " + avoidCount + " 个项目");
                } else {
                    System.out.println("[勘察奖自动回避] 展示列表时触发：专家单位为空，跳过自动回避");
                }
            } else {
                System.out.println("[勘察奖自动回避] 展示列表时触发：未找到专家绑定信息");
            }
        } catch (Exception e) {
            // 自动回避失败不影响列表展示
            System.err.println("[勘察奖自动回避] 展示列表时触发失败: " + e.getMessage());
            e.printStackTrace();
        }

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
     * 下载打分结果（专家本人：按模板四 Sheet 导出评分明细 + 电子签章）
     */
    @RequestMapping("/downloadScoringResult")
    @RequiresPermissions("surveraward:score:prolist")
    public void downloadScoringResult(@RequestParam String taskId, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(taskId)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("任务ID不能为空");
            return;
        }
        UserDO user = getUser();
        if (user == null) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("用户未登录");
            return;
        }
        Long expertUid = getUserId();
        String resolvedTaskId = resolveSurverScoreTaskId();
        if (StringUtils.isNotBlank(resolvedTaskId)) {
            taskId = resolvedTaskId;
        }
        if (StringUtils.isBlank(taskId)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("无法解析任务ID");
            return;
        }
        try {
            String groupName = resolveExpertGroupName(taskId, expertUid);
            String signPath = resolveExpertSignDiskPath(taskId, expertUid);
            Map<String, List<SurverExpertScoringExportUtils.ExportRow>> rowsBySubType =
                    buildExpertScoringExportRows(taskId, expertUid);
            String expertName = StringUtils.isNotBlank(user.getName()) ? user.getName() : user.getUsername();
            SurverExpertScoringExportUtils.exportByTemplate(response, groupName, rowsBySubType, signPath, expertName);
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("导出失败：" + e.getMessage());
        }
    }

    // 原：占位
    // public void downloadScoringResult(...) { response.getWriter().write("下载功能开发中"); }

    private String resolveExpertGroupName(String taskId, Long expertUid) {
        Map<String, Object> eq = new HashMap<>();
        eq.put("userId", String.valueOf(expertUid));
        eq.put("taskId", taskId);
        eq.put("proType", "surver_pro_group");
        List<ExpertGroupDO> bindings = expertGroupService.list(eq);
        if (bindings != null && !bindings.isEmpty() && StringUtils.isNotBlank(bindings.get(0).getGroupName())) {
            return bindings.get(0).getGroupName();
        }
        return "专业组";
    }

    private String resolveExpertSignDiskPath(String taskId, Long expertUid) {
        try {
            Map<String, Object> eq = new HashMap<>();
            eq.put("userId", String.valueOf(expertUid));
            eq.put("taskId", taskId);
            eq.put("proType", "surver_pro_group");
            List<ExpertGroupDO> list = expertGroupService.list(eq);
            if (list == null || list.isEmpty() || StringUtils.isBlank(list.get(0).getExpertSignUrl())) {
                return null;
            }
            String signUrl = list.get(0).getExpertSignUrl();
            String uploadRoot = bootdoConfig.getUploadPath();
            if (uploadRoot.endsWith("/**")) {
                uploadRoot = uploadRoot.substring(0, uploadRoot.length() - 3);
            }
            return uploadRoot + signUrl.replace("/files/", "/");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, List<SurverExpertScoringExportUtils.ExportRow>> buildExpertScoringExportRows(
            String taskId, Long expertUid) {
        Map<String, List<SurverExpertScoringExportUtils.ExportRow>> map = new LinkedHashMap<>();
        String[] subTypes = {"contribution", "design", "software", "standard"};
        for (String proSubType : subTypes) {
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("taskId", taskId);
            queryParams.put("proSubType", proSubType);
            queryParams.put("expertUid", expertUid);
            List<Map<String, Object>> projects = awardEnterpriseProjectService.listSurverProjects(queryParams);
            List<SurverExpertScoringExportUtils.ExportRow> rows = new ArrayList<>();
            if (projects != null) {
                for (Map<String, Object> project : projects) {
                    rows.add(toScoringExportRow(taskId, expertUid, proSubType, project));
                }
            }
            map.put(proSubType, rows);
        }
        return map;
    }

    private SurverExpertScoringExportUtils.ExportRow toScoringExportRow(String taskId, Long expertUid,
                                                                        String proSubType,
                                                                        Map<String, Object> project) {
        SurverExpertScoringExportUtils.ExportRow row = new SurverExpertScoringExportUtils.ExportRow();
        row.declareAccount = exportStr(project.get("declareAccount"));
        row.proCode = exportStr(project.get("proCode"));
        row.topicName = exportStr(project.get("topicName"));
        row.avoided = exportTruthy(project.get("isAvoided"));

        Integer proId = exportInt(project.get("proId"));
        if (proId == null || row.avoided) {
            return row;
        }
        SurverExpertScoringDO scoring = scoringService.getByTaskProExpert(taskId, proId, expertUid);
        if (scoring == null) {
            return row;
        }
        row.totalScore = scoring.getTotalScore();
        row.opinionGrade = scoring.getOpinionGrade();
        row.opinionText = scoring.getOpinionText();
        fillExportScores(row, scoring, proSubType);
        return row;
    }

    private void fillExportScores(SurverExpertScoringExportUtils.ExportRow row, SurverExpertScoringDO scoring,
                                  String proSubType) {
        switch (proSubType) {
            case "contribution":
                putScore(row, "technicalLevel", scoring.getTechnicalLevel());
                putScore(row, "technicalDifficulty", scoring.getTechnicalDifficulty());
                putScore(row, "technicalInnovation", scoring.getTechnicalInnovation());
                putScore(row, "economicBenefit", scoring.getEconomicBenefit());
                putScore(row, "materialQuality", scoring.getMaterialQuality());
                break;
            case "design":
                putScore(row, "overallTechnicalLevel", scoring.getOverallTechnicalLevel());
                putScore(row, "difficultyInnovation", scoring.getDifficultyInnovation());
                putScore(row, "digitalDesignLevel", scoring.getDigitalDesignLevel());
                putScore(row, "environmentSafety", scoring.getEnvironmentSafety());
                putScore(row, "designQuality", scoring.getDesignQuality());
                putScore(row, "energySaving", scoring.getEnergySaving());
                putScore(row, "greenConstruction", scoring.getGreenConstruction());
                putScore(row, "materialQuality", scoring.getMaterialQuality());
                break;
            case "software":
            case "standard":
                putScore(row, "technicalLevel", scoring.getTechnicalLevel());
                putScore(row, "technicalDifficulty", scoring.getTechnicalDifficulty());
                putScore(row, "technicalInnovation", scoring.getTechnicalInnovation());
                putScore(row, "promotability", scoring.getPromotability());
                putScore(row, "economicBenefit", scoring.getEconomicBenefit());
                putScore(row, "materialQuality", scoring.getMaterialQuality());
                break;
            default:
                break;
        }
    }

    private static void putScore(SurverExpertScoringExportUtils.ExportRow row, String field, Integer val) {
        if (val != null) {
            row.scores.put(field, val);
        }
    }

    private static String exportStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private static Integer exportInt(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean exportTruthy(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue() == 1;
        }
        return "1".equals(obj.toString()) || Boolean.TRUE.equals(obj);
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
        // 安全转换 proId，兼容 String 和 Integer 类型
        Object proIdObj = params.get("proId");
        Integer proId = null;
        if (proIdObj != null) {
            proId = proIdObj instanceof Integer ? (Integer) proIdObj : Integer.parseInt(proIdObj.toString());
        }
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
            // 新增 - 直接从项目表查询 pro_sub_type
            EnterpriseProjectInfoDo projectInfo = awardEnterpriseProjectService.get(String.valueOf(proId));
            String proSubType = projectInfo != null ? projectInfo.getProSubType() : null;

            SurverExpertScoringDO scoring = new SurverExpertScoringDO();
            scoring.setTaskId(taskId);
            scoring.setProId(proId);
            scoring.setProSubType(proSubType);
            scoring.setExpertUid(expertUid);
            scoring.setOpinionGrade(opinionGrade);
            scoring.setOpinionText(opinionText);
            scoringService.save(scoring);
        }

        return R.ok("保存成功");
    }

}
