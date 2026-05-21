package com.bootdo.activiti.controller;

import com.bootdo.activiti.dao.QcGroupDao;
import com.bootdo.activiti.domain.EnterpriseDocUploadDo;
import com.bootdo.activiti.domain.EnterpriseProjectInfoDo;
import com.bootdo.activiti.domain.PublishAwardTaskDo;
import com.bootdo.activiti.domain.QcGroupDO;
// 新增：专家分组（与已有的 QcGroup/分组管理 不是同一个功能点）
import com.bootdo.activiti.domain.AwardExpertGroupDO;
import com.bootdo.activiti.service.AwardEnterpriseProjectService;
import com.bootdo.activiti.service.AwardExpertGroupService;
import com.bootdo.activiti.service.AwardFlowService;
import com.bootdo.activiti.service.QcGroupService;
import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.controller.BaseScienceProController;
import com.bootdo.common.domain.DictDO;
import com.bootdo.common.domain.FileDO;
import com.bootdo.common.domain.Tree;
import com.bootdo.common.service.DictService;
import com.bootdo.common.service.FileService;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.*;
import com.bootdo.cpe.petroleum_engineering_award.domain.OilQualityConfirmFileDO;
import com.bootdo.cpe.petroleum_engineering_award.domain.OilQualityProSituationDO;
import com.bootdo.cpe.petroleum_engineering_award.service.OilQualityConfirmFileService;
import com.bootdo.cpe.petroleum_engineering_award.service.OilQualityProSituationService;
import com.bootdo.cpe.service.ExpertGroupService;
import com.bootdo.cpe.service.ImportCheckExcelDataService;
import com.bootdo.cpe.service.ImportCheckExcelUpdateService;
import com.bootdo.cpe.service.SurverEnterpriseSortInfoService;
import com.bootdo.cpe.service.SurverAwardService;
import com.bootdo.system.domain.EnterpriPersonalInfoDO;
import com.bootdo.system.domain.EnterpriTeamInfoDO;
import com.bootdo.system.domain.EnterpriseChengguoBaseInfoDO;
import com.bootdo.system.domain.UserDO;
import com.bootdo.system.service.EnterpriPersonalInfoService;
import com.bootdo.system.service.EnterpriTeamInfoService;
import com.bootdo.system.service.EnterpriseChengguoBaseInfoService;
import com.bootdo.system.service.UserService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.bootdo.cpe.service.QcReviewResultRecordService;
import com.bootdo.cpe.service.SurverReviewDesignResultService;
import com.bootdo.cpe.service.SurverReviewSoftResultService;
import com.bootdo.cpe.service.SurverReviewStandardResultService;
import com.bootdo.cpe.service.SurverReviewSurverResultService;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.Map.Entry;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.*;
import com.bootdo.cpe.dto.QcProDataDto;
import com.bootdo.cpe.domain.QcReviewResultRecordDO;
import com.bootdo.cpe.petroleum_engineering_award.domain.OilQualityConfirmFileDO;
import com.bootdo.cpe.service.ExpertGroupService;
import com.bootdo.cpe.service.ImportCheckExcelDataService;
import com.bootdo.cpe.service.ImportCheckExcelUpdateService;
import com.bootdo.cpe.service.SurverEnterpriseSortInfoService;
import com.bootdo.cpe.service.QcReviewResultRecordService;
import com.bootdo.cpe.service.QcAwardService;
import static com.bootdo.common.config.Constant.*;

@Controller
@RequestMapping("/enterprise_pro")
public class AwardEnterpriseProjectController extends BaseScienceProController {
    @Autowired
    private AwardEnterpriseProjectService awardEnterpriseProjectService;
    @Autowired
    private ExpertGroupService expertGroupService;
    @Autowired
    private DictService dictService;
    @Autowired
    private FileService sysFileService;
    @Autowired
    private BootdoConfig bootdoConfig;
    @Autowired
    private UserService userService;
    @Autowired
    TaskService taskService;
    @Autowired
    private AwardFlowService awardFlowService;
    @Autowired
    private OilQualityProSituationService qualityProSituationService;
    @Autowired
    private OilQualityConfirmFileService qualityConfirmFileService;
    @Autowired
    private SurverEnterpriseSortInfoService surverEnterpriseSortInfoService;
    @Autowired
    private ImportCheckExcelDataService importCheckExcelDataService;
    @Autowired
    private ImportCheckExcelUpdateService importCheckExcelUpdateService;
    @Autowired
    private EnterpriPersonalInfoService enterpriPersonalInfoService;
    @Autowired
    private EnterpriTeamInfoService enterpriTeamInfoService;
    @Autowired
    private EnterpriseChengguoBaseInfoService enterpriseChengguoBaseInfoService;
    @Autowired
    private QcGroupService qcGroupService;
    @Autowired
    private QcGroupDao qcGroupDao;
    // 新增：专家分组服务
    @Autowired
    private AwardExpertGroupService awardExpertGroupService;
    @Autowired
    private QcReviewResultRecordService qcReviewResultRecordService;
    @Autowired
    private QcAwardService qcAwardService;
    @Autowired
    private SurverAwardService surverAwardService;
    @Autowired
    private SurverReviewDesignResultService surverReviewDesignResultService;
    @Autowired
    private SurverReviewSoftResultService surverReviewSoftResultService;
    @Autowired
    private SurverReviewStandardResultService surverReviewStandardResultService;
    @Autowired
    private SurverReviewSurverResultService surverReviewSurverResultService;
    @RequestMapping("/to_list/{taskId}")
    public String toProList(@PathVariable("taskId") String taskId, ModelMap map) {
        map.put("apply_type", "");
        map.put("publishTaskId", taskId);
        PublishAwardTaskDo taskDo = awardFlowService.getAwardTaskById(taskId);
        String procInsId = taskDo.getProcInsId();
        boolean isReview = false;
        if (StringUtils.isNotBlank(procInsId)) {
            List<Task> taskList = taskService.createTaskQuery()
                    .processInstanceId(procInsId).list();
            Task task = taskList.size() > 0 ? taskList.get(0) : null;
            if (task != null) {
                String defKey = task.getTaskDefinitionKey();
                isReview = defKey.equals("ass_validate_pro");
            }
        }
        map.put("isReview", isReview);
        return "act/award/enterprise_doc_award_pro_list";
    }

    /**
     * 跳转到要个人形式审查申报的项目列表
     *
     * @param map
     * @retur
     */
    @RequestMapping("/to_apply_formal_personal_pros")
    @RequiresPermissions("act:award:apply_pros")
    public String toApplyPersonalFormalProList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        map.put("apply_type", "personal");
        return "act/award/formal/enterprise_formal_personal_award_pro_list";
    }

    /**
     * 跳转到要申报的项目列表
     *
     * @param map
     * @return
     */
    @RequestMapping("/to_apply_team_pros")
    @RequiresPermissions("act:award:apply_pros")
    public String toApplyTeamProList(@RequestParam Map<String, Object> params, ModelMap map) {
        UserDO user = getUser();
        List<Long> roleIdList = user.getRoleIds();
        isTaskIsApply(map, params, roleIdList, user);
        packageAwardTaskId(map, params);
        if(roleIdList.contains(ROLE_SCIENCE_ASSOCIATION_ID)) {
            //TODO 临时取消下发专业，需要根据项目状态判断是否下发
            /*List<DictDO> dictDOList = new ArrayList<>();
            DictDO selOptionDo = new DictDO();
            selOptionDo.setName("请选择");
            dictDOList.add(selOptionDo);
            List<DictDO> dictDOS = dictService.listByType("profession_type");
            dictDOList.addAll(dictDOS);
            map.put("specialTypeList", dictDOList);*/
        }
        map.put("apply_pro_type", "team");
        return "cpe/science/pro_list_team";
    }


    /**
     * 团队 形式审查
     *
     * @param map
     * @return
     */
    @RequestMapping("/to_apply_formal_team_pros")
    @RequiresPermissions("act:award:apply_pros")
    public String toApplyTeamFormalProList(@RequestParam Map<String, Object> params, ModelMap map) {
        map.put("apply_type", "team");
        packageAwardTaskId(map, params);
        return "act/award/formal/enterprise_team_formal_pro_list";
    }


    /**
     * 跳转到要申报的项目列表
     *
     * @param map
     * @return
     */
    @RequestMapping("/to_apply_science_pros")
    @RequiresPermissions("act:award:apply_pros")
    public String toApplyProList(@RequestParam Map<String, Object> params, ModelMap map) {
        map.put("apply_type", "science");
        packageAwardTaskId(map, params);
        List<PublishAwardTaskDo> taskDoList = awardFlowService.getAwardLastTasksByAwardType(20, EnumAwardType.SCIENCE.getAwrdType());
        map.put("taskList", taskDoList);
        return "act/award/enterprise_doc_award_pro_list";
    }


    /**
     * 获取专业审核的项目列表
     *
     * @return
     */
    @RequestMapping("/review_specialist_pros")
    @ResponseBody
    public List<EnterpriseProjectInfoDo> getSpecialistReviewPros(int majorId) {
        Map<String, Object> params = new HashMap<>();
        params.put("uid", getUserId());
        params.put("majorId", majorId);
        List<EnterpriseProjectInfoDo> list = awardEnterpriseProjectService.getSpecialistReviewPros(params);
        return list;
    }

    @ResponseBody
    @GetMapping("/list")
    public PageUtils list(@RequestParam Map<String, Object> params, ModelMap map) {
        //获取用户的角色不同角色查看不同的信息,超级管理员全部的,发布者全部的,分派的人员查看分派的,企业只能看自己创建的,专家可以查看分派的
        //是否为超级管理员全部的,发布者
        packageAwardTaskId(map, params);
        UserDO user = getUser();
        Long uid = getUserId();
        List<Long> roleIdList = user.getRoleIds();
        if (roleIdList.contains(ROLE_SCIENCE_ASSOCIATION_ID)) {
            params.put("associationUserId", user.getUserId());
        } else if (roleIdList.contains(ROLE_ENTERPRISE_SCIENCE_ID)) {
            //企业用户查看自己创建项目
            params.put("enterpriseUid", uid);
        } else {
            //分派给自己的项目
            params.put("ass_assign_uid", uid);
        }
        getProListParamsByRole(params);
        Query query = new Query(params);
        List<EnterpriseProjectInfoDo> proList = awardEnterpriseProjectService.list(query);
        int total = awardEnterpriseProjectService.count(query);
        PageUtils pageUtils = new PageUtils(proList, total);
        return pageUtils;
    }

    @ResponseBody
    @RequestMapping("/create_pro")
    public R createProject(EnterpriseProjectInfoDo projectInfoDo) {
        long uid = getUserId();
        projectInfoDo.setCreateUid(uid);
        long majorId = projectInfoDo.getMajorId();
        if (majorId == 0 || "选择专业".equals(projectInfoDo.getMajor().trim())) {
            return R.error(100, "请选择专业");
        }
        if (majorId > 0) {
            DictDO dictDO = dictService.get(majorId);
            String name = dictDO.getName();
            String major = projectInfoDo.getMajor();
            if (name.equals(major.trim())) {
                //如果用户填写的专业已经存在了，则清除填写的名称
                projectInfoDo.setMajor("");
            } else {
                //用户填写了新的专业名称,新增到专业列表里面
                DictDO majorDo = new DictDO();
                majorDo.setType("major");
                majorDo.setName(name.trim());
                majorDo.setValue("major_" + RandomStringUtils.randomAlphanumeric(5));
                dictService.save(majorDo);
                //设置新增的专业id
                projectInfoDo.setMajorId(majorDo.getId());
            }
        }

        if (awardEnterpriseProjectService.save(projectInfoDo) > 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("proId", projectInfoDo.getId());
            return R.ok(result);
        }
        return R.error();
    }


    /**
     * 上传项目资料文件
     * 存储目录 年/月/u_用户id
     *
     * @param files
     * @return
     */
    @RequestMapping("/upload_doc")
    @ResponseBody
    public R uploadEnterpriseDoc(String taskId, Integer proId, String fileType,String departId, Long expertUid, @RequestPart("file[]") MultipartFile[] files) {
        List<EnterpriseDocUploadDo> uploadFileList = new ArrayList<>();
        int len = files.length;
        long uid = getUserId();
        String curDate = DateUtils.getCurDate();
        String[] dateArr = curDate.split("-");
        String userFolderPath = dateArr[0] + "/" + dateArr[1] + "/u_" + uid + "/";
        String uploadPath = bootdoConfig.getUploadPath() + userFolderPath;
        File fileFolder = new File(uploadPath);
        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }
        String fileUrl = "";
        for (int i = 0; i < len; i++) {
            MultipartFile file = files[i];
            String fileName = file.getOriginalFilename();
            int index = fileName.lastIndexOf(".");
            fileName = fileName.substring(0, index) + "_" + System.currentTimeMillis() + RandomStringUtils.randomAlphanumeric(4) + fileName.substring(index);


            fileUrl = "/files/" + userFolderPath + fileName;
            FileDO sysFile = new FileDO(FileType.fileType(fileName), fileUrl, new Date());
            try {
                FileUtil.uploadFile(file.getBytes(), uploadPath, fileName);
            } catch (Exception e) {
                e.printStackTrace();
                return R.error();
            }
            int rstCount = sysFileService.save(sysFile);
            boolean isExpertSign = "expert_sign".equals(fileType);
            String importFileType = fileType;
            if ("import_check_result_qc".equals(fileType)) {
                // 兼容前端缓存/按钮误传：根据任务奖项自动纠正导入类型
                PublishAwardTaskDo taskDo = awardFlowService.getAwardTaskById(taskId);
                if (taskDo != null && "2".equals(taskDo.getAwardId())) {
                    importFileType = "import_check_result_surver";
                }
            }
            if (rstCount > 0) {
                if("import_check_result_qc".equals(importFileType)) {
                    // ========== QC奖导入逻辑 ==========
                    String path = uploadPath + fileName;
                    try {
                        FileInputStream inputStream = new FileInputStream(new File(path));
                        Workbook workbook = getWorkbook(inputStream, fileName);
                        Sheet sheet = workbook.getSheetAt(0);
                        int lastRowNum = sheet.getLastRowNum();

                        // 验证表头
                        Row headerRow = sheet.getRow(0);
                        if(headerRow == null) {
                            workbook.close();
                            return R.error("Excel 文件格式错误，缺少表头");
                        }

                        // 动态查找列位置
                        Map<String, Integer> headerMap = new HashMap<>();
                        for(int h = 0; h < headerRow.getLastCellNum(); h++) {
                            String headerName = getCellValue(headerRow.getCell(h)).trim();
                            headerMap.put(headerName, h);
                        }

                        // 检查必要的列是否存在
                        Integer applyAccountColIndex = headerMap.get("申报账号");
                        Integer reviewResultColIndex = headerMap.get("形审结果");
                        Integer reviewCommentColIndex = headerMap.get("形审评语");
                        Integer qcGroupColIndex = headerMap.get("分组");

                        if(applyAccountColIndex == null) {
                            workbook.close();
                            return R.error("Excel 表头缺少'申报账号'列，请使用系统导出的 QC 项目列表模板");
                        }

                        // 检查分组列是否存在（兼容旧版本导出文件）
                        boolean hasGroupColumn = qcGroupColIndex != null;

                        // 查询该 taskId 下所有 QC 项目
                        Map<String, Object> allParams = new HashMap<>();
                        allParams.put("taskId", taskId);
                        List<QcProDataDto> qcProList = qcAwardService.listProInfo(allParams);

                        // 构建申报账号 (apply_id) 到项目ID映射
                        Map<String, Integer> accountToProIdMap = new HashMap<>();
                        for (QcProDataDto pro : qcProList) {
                            String applyId = pro.getApplyId();
                            if (StringUtils.isNotBlank(applyId) && pro.getProId() != null) {
                                accountToProIdMap.put(applyId, pro.getProId());
                            }
                        }

                        if (accountToProIdMap.isEmpty()) {
                            workbook.close();
                            return R.error("当前任务未查询到QC项目数据，请确认任务ID与导入模板是否匹配");
                        }

                        // 查询所有可用的分组信息 (用于验证分组名称)
                        List<QcGroupDO> groupList = qcGroupService.getGroupsByTaskId(taskId);
                        Map<String, QcGroupDO> groupNameToGroupMap = new HashMap<>();
                        for(QcGroupDO group : groupList) {
                            if(StringUtils.isNotBlank(group.getName())) {
                                groupNameToGroupMap.put(group.getName(), group);
                            }
                        }

                        // 读取数据行并统计
                        int successCount = 0;
                        int failCount = 0;
                        StringBuilder failMsg = new StringBuilder();

                        for(int rowNum = 1; rowNum <= lastRowNum; rowNum++) {
                            Row row = sheet.getRow(rowNum);
                            if(row == null) {
                                continue;
                            }

                            // 检查该行是否为空行 (所有单元格都为空)
                            boolean isEmptyRow = true;
                            for(int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
                                String cellValue = getCellValue(row.getCell(cellNum)).trim();
                                if(StringUtils.isNotBlank(cellValue)) {
                                    isEmptyRow = false;
                                    break;
                                }
                            }
                            if(isEmptyRow) {
                                continue;
                            }

                            // 申报账号 (用于查找项目)
                            String applyAccount = getCellValue(row.getCell(applyAccountColIndex)).trim();
                            // 形审结果 (如果存在该列)
                            String reviewResult = reviewResultColIndex != null ? getCellValue(row.getCell(reviewResultColIndex)).trim() : "";
                            // 形审评语 (如果存在该列)
                            String reviewComment = reviewCommentColIndex != null ? getCellValue(row.getCell(reviewCommentColIndex)).trim() : "";
                            // 分组名称 (如果存在该列)
                            String qcGroupName = hasGroupColumn && qcGroupColIndex != null ? getCellValue(row.getCell(qcGroupColIndex)).trim() : "";

                            if(StringUtils.isBlank(applyAccount)) {
                                failCount++;
                                failMsg.append("第" + (rowNum+1) + "行申报账号为空; ");
                                continue;
                            }

                            // 从缓存映射中获取项目ID（QC: apply_id；勘察: declare_account）
                            Integer importProId = accountToProIdMap.get(applyAccount);

                            if(importProId == null) {
                                failCount++;
                                failMsg.append("第" + (rowNum+1) + "行未找到对应项目 (" + applyAccount + "); ");
                                continue;
                            }

                            if(importProId == 0) {
                                failCount++;
                                failMsg.append("第" + (rowNum+1) + "行项目 ID 无效; ");
                                continue;
                            }

                            // 保存QC形审结果记录
                            if(StringUtils.isNotBlank(reviewResult) || StringUtils.isNotBlank(reviewComment)) {
                                QcReviewResultRecordDO reviewRecord = new QcReviewResultRecordDO();
                                reviewRecord.setProId(importProId);
                                reviewRecord.setTaskId(taskId);
                                reviewRecord.setOptUid((int) uid);
                                reviewRecord.setReviewResult(reviewResult); // 形审结果
                                reviewRecord.setOpinionDesc(reviewComment); // 形审评语
                                reviewRecord.setCreated(new Date());
                                qcReviewResultRecordService.save(reviewRecord);
                            }

                            // 更新分组信息 (如果 Excel 中包含分组列且值不为空)
                            if(hasGroupColumn && StringUtils.isNotBlank(qcGroupName)) {
                                // 验证分组名称是否存在
                                QcGroupDO targetGroup = groupNameToGroupMap.get(qcGroupName);
                                if(targetGroup != null) {
                                    // 更新项目分组
                                    Map<String, Object> updateGroupParams = new HashMap<>();
                                    updateGroupParams.put("proId", importProId);
                                    updateGroupParams.put("groupId", targetGroup.getGroupid());
                                    awardEnterpriseProjectService.updateProGroup(updateGroupParams);
                                } else {
                                    // 分组名称不存在，记录警告但不阻止导入
                                    failCount++;
                                    failMsg.append("第" + (rowNum+1) + "行分组'" + qcGroupName + "'不存在，已跳过; ");
                                }
                            }

                            successCount++;
                        }

                        workbook.close();

                        String resultMsg = "导入完成！成功：" + successCount + "条";
                        if(failCount > 0) {
                            resultMsg += "，失败：" + failCount + "条";
                            if(failMsg.length() > 0) {
                                resultMsg += "<br/>" + failMsg.toString();
                            }
                        }

                        // 将结果信息返回到前端，由前端统一弹窗提示
                        R result = failCount > 0 ? R.error(resultMsg) : R.ok(resultMsg);
                        result.put("showMsg", true); // 标记需要前端弹窗显示
                        return result;

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        R result = R.error("文件不存在");
                        result.put("showMsg", true);
                        return result;
                    } catch (Exception e) {
                        e.printStackTrace();
                        R result = R.error("导入失败：" + e.getMessage());
                        result.put("showMsg", true);
                        return result;
                    }
                }
                else if("import_check_result_surver".equals(importFileType)) {
                    // 勘察奖独立导入逻辑：根据 proId 更新项目编号、申报账号、分组、形审结果、形审评语
                    String path = uploadPath + fileName;
                    try {
                        FileInputStream inputStream = new FileInputStream(new File(path));
                        Workbook workbook = getWorkbook(inputStream, fileName);
                        Sheet sheet = workbook.getSheetAt(0);
                        int lastRowNum = sheet.getLastRowNum();

                        Row headerRow = sheet.getRow(0);
                        if (headerRow == null) {
                            workbook.close();
                            return R.error("Excel 文件格式错误，缺少表头");
                        }

                        //列的位置不固定，所以需要根据表头名称来获取列的位置
                        Map<String, Integer> headerMap = new HashMap<>();
                        for (int h = 0; h < headerRow.getLastCellNum(); h++) {
                            String headerName = getCellValue(headerRow.getCell(h)).trim();
                            headerMap.put(headerName, h);
                        }

                        Integer proIdColIndex = headerMap.get("proId");
                        Integer proCodeColIndex = headerMap.get("项目编号");
                        Integer declareAccountColIndex = headerMap.get("申报账号");
                        Integer qcGroupColIndex = headerMap.get("分组");
                        Integer reviewResultColIndex = headerMap.get("形审结果");
                        Integer reviewCommentColIndex = headerMap.get("形审评语");
                        // 添加是否查新列
                        Integer noveltyColIndex = resolveImportHeaderIndex(headerMap, "是否有查新");

                        if (proIdColIndex == null) {
                            workbook.close();
                            return R.error("Excel 表头缺少'proId'列，请使用勘察奖项目列表导出模板");
                        }

                        List<QcGroupDO> groupList = qcGroupService.getGroupsByTaskId(taskId);
                        Map<String, QcGroupDO> groupNameToGroupMap = new HashMap<>();
                        for (QcGroupDO group : groupList) {
                            if (StringUtils.isNotBlank(group.getName())) {
                                groupNameToGroupMap.put(group.getName(), group);
                            }
                        }

                        // 导入前快照：仅当 Excel 与库中值不一致时才写入并计入更新条数
                        Map<Integer, SurverProjectInfo> proSnapshotMap = buildSurverProSnapshotMap(taskId);

                        int failCount = 0;
                        int proCodeUpdateCount = 0;
                        int declareAccountUpdateCount = 0;
                        int groupUpdateCount = 0;
                        int reviewUpdateCount = 0;
                        int noveltyUpdateCount = 0;
                        StringBuilder failMsg = new StringBuilder();

                        for (int rowNum = 1; rowNum <= lastRowNum; rowNum++) {
                            Row row = sheet.getRow(rowNum);
                            if (row == null) {
                                continue;
                            }

                            boolean isEmptyRow = true;
                            for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
                                String cellValue = getCellValue(row.getCell(cellNum)).trim();
                                if (StringUtils.isNotBlank(cellValue)) {
                                    isEmptyRow = false;
                                    break;
                                }
                            }
                            if (isEmptyRow) {
                                continue;
                            }

                            String proIdStr = getCellValue(row.getCell(proIdColIndex)).trim();
                            if (StringUtils.isBlank(proIdStr)) {
                                failCount++;
                                failMsg.append("第" + (rowNum + 1) + "行proId为空; ");
                                continue;
                            }

                            Integer importProId;
                            try {
                                importProId = Integer.parseInt(proIdStr);
                            } catch (Exception ex) {
                                failCount++;
                                failMsg.append("第" + (rowNum + 1) + "行proId格式错误(" + proIdStr + "); ");
                                continue;
                            }

                            EnterpriseProjectInfoDo projectDO = awardEnterpriseProjectService.get(String.valueOf(importProId));
                            if (projectDO == null || !taskId.equals(projectDO.getPublishTaskId())) {
                                failCount++;
                                failMsg.append("第" + (rowNum + 1) + "行proId不存在或不属于当前任务; ");
                                continue;
                            }

                            String proCode = proCodeColIndex == null ? "" : getCellValue(row.getCell(proCodeColIndex)).trim();
                            String declareAccount = declareAccountColIndex == null ? "" : getCellValue(row.getCell(declareAccountColIndex)).trim();
                            String qcGroupName = qcGroupColIndex == null ? "" : getCellValue(row.getCell(qcGroupColIndex)).trim();
                            String reviewResult = reviewResultColIndex == null ? "" : getCellValue(row.getCell(reviewResultColIndex)).trim();
                            String reviewComment = reviewCommentColIndex == null ? "" : getCellValue(row.getCell(reviewCommentColIndex)).trim();
                            String noveltyValRaw = noveltyColIndex == null ? "" : getCellValue(row.getCell(noveltyColIndex));
                            String noveltyVal = normalizeImportNovelty(noveltyValRaw);

                            SurverProjectInfo curPro = proSnapshotMap.get(importProId);
                            boolean noveltyInvalid = false;

                            // 1) 更新项目编号
                            if (StringUtils.isNotBlank(proCode)
                                    && curPro != null
                                    && importCellChanged(proCode, curPro.getProCode())) {
                                qcAwardService.updateProResultCode(importProId, proCode);
                                proCodeUpdateCount++;
                            }

                            // 2) 更新申报账号（仅值变化时写入）
                            if (declareAccountColIndex != null
                                    && curPro != null
                                    && importCellChanged(declareAccount, curPro.getDeclareAccount())) {
                                qcAwardService.updateProDeclareAccount(importProId, declareAccount);
                                declareAccountUpdateCount++;
                            }

                            // 3) 更新分组
                            if (StringUtils.isNotBlank(qcGroupName)) {
                                QcGroupDO targetGroup = groupNameToGroupMap.get(qcGroupName);
                                if (targetGroup != null) {
                                    String dbGroup = curPro != null ? curPro.getQcGroupName() : "";
                                    if (importCellChanged(qcGroupName, dbGroup)) {
                                        Map<String, Object> updateGroupParams = new HashMap<>();
                                        updateGroupParams.put("proId", importProId);
                                        updateGroupParams.put("groupId", targetGroup.getGroupid());
                                        awardEnterpriseProjectService.updateProGroup(updateGroupParams);
                                        groupUpdateCount++;
                                    }
                                } else {
                                    failCount++;
                                    failMsg.append("第" + (rowNum + 1) + "行分组'" + qcGroupName + "'不存在，已跳过分组更新; ");
                                }
                            }

                            // 4) 更新形审结果、形审评语（与库中最新记录比对，一致则不新增）
                            if (StringUtils.isNotBlank(reviewResult) || StringUtils.isNotBlank(reviewComment)) {
                                String dbResult = curPro != null ? stripHtmlForImportCompare(curPro.getLatestReviewResult()) : "";
                                String dbComment = curPro != null ? stripHtmlForImportCompare(curPro.getLatestReviewRemarks()) : "";
                                boolean reviewChanged = importCellChanged(reviewResult, dbResult)
                                        || importCellChanged(reviewComment, dbComment);
                                if (reviewChanged) {
                                    String proSubType = curPro != null ? curPro.getProSubType() : findSurverProSubTypeByTaskAndProId(taskId, importProId);
                                    if (StringUtils.isBlank(proSubType)) {
                                        failCount++;
                                        failMsg.append("第" + (rowNum + 1) + "行未识别勘察奖项目类别，已跳过形审结果写入; ");
                                    } else {
                                        saveSurverReviewRecord(proSubType, importProId, taskId, (int) uid, reviewResult, reviewComment);
                                        reviewUpdateCount++;
                                    }
                                }
                            }

                            // 5) 是否有查新（仅允许 是/否；列为空表示清空，仅与库中不一致时写入）
                            if (noveltyColIndex != null) {
                                String dbNovelty = normalizeImportNovelty(qcAwardService.getExtSurverNovelty(importProId));
                                if (StringUtils.isBlank(noveltyVal)) {
                                    if (StringUtils.isNotBlank(dbNovelty)) {
                                        qcAwardService.updateExtSurverNovelty(importProId, null);
                                        noveltyUpdateCount++;
                                    }
                                } else if (!isValidImportNovelty(noveltyVal)) {
                                    failCount++;
                                    noveltyInvalid = true;
                                    failMsg.append("第" + (rowNum + 1) + "行「是否有查新」仅能填「是」或「否」，当前为: " + noveltyValRaw.trim() + "; ");
                                } else if (!noveltyVal.equals(dbNovelty)) {
                                    qcAwardService.updateExtSurverNovelty(importProId, noveltyVal);
                                    noveltyUpdateCount++;
                                }
                            }
                        }

                        workbook.close();

                        StringBuilder resultMsg = new StringBuilder();
                        resultMsg.append("导入完成");
                        List<String> updateParts = new ArrayList<>();
                        if (proCodeUpdateCount > 0) {
                            updateParts.add("项目编号 " + proCodeUpdateCount + " 条");
                        }
                        if (declareAccountUpdateCount > 0) {
                            updateParts.add("申报账号 " + declareAccountUpdateCount + " 条");
                        }
                        if (groupUpdateCount > 0) {
                            updateParts.add("分组 " + groupUpdateCount + " 条");
                        }
                        if (reviewUpdateCount > 0) {
                            updateParts.add("形审结果/评语 " + reviewUpdateCount + " 条");
                        }
                        if (noveltyUpdateCount > 0) {
                            updateParts.add("是否有查新 " + noveltyUpdateCount + " 条");
                        }
                        if (!updateParts.isEmpty()) {
                            resultMsg.append("，已更新：").append(String.join("；", updateParts));
                        } else if (failCount == 0) {
                            resultMsg.append("，无数据被更新");
                            if (noveltyColIndex == null) {
                                resultMsg.append("（Excel 无「是否有查新」列时请使用最新导出模板）");
                            }
                        }
                        if (failCount > 0) {
                            resultMsg.append("；失败 ").append(failCount).append(" 行");
                            if (failMsg.length() > 0) {
                                resultMsg.append("<br/>").append(failMsg.toString());
                            }
                        }

                        R result = failCount > 0 ? R.error(resultMsg.toString()) : R.ok(resultMsg.toString());
                        result.put("showMsg", true);
                        result.put("noveltyUpdateCount", noveltyUpdateCount);
                        return result;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        R result = R.error("文件不存在");
                        result.put("showMsg", true);
                        return result;
                    } catch (Exception e) {
                        e.printStackTrace();
                        R result = R.error("导入失败：" + e.getMessage());
                        result.put("showMsg", true);
                        return result;
                    }
                }
                else if("import_check_result".equals(fileType)) {
                    //上传形式审查结果记录
                    String path = uploadPath + fileName;
                    try {
                        FileInputStream inputStream = new FileInputStream(new File(path));
                        Map<String, List<Object[]>> resultMap = ExcelUtils.readCpeCheckResultExcel(inputStream);
                        Map<String, Object> params = new HashMap<>();
                        params.put("taskId", taskId);
                        List<EnterpriseChengguoBaseInfoDO> chengguoList = enterpriseChengguoBaseInfoService.list(params);
                        Map<String, List<EnterpriseChengguoBaseInfoDO>> chengguoMap = chengguoList.stream().collect(Collectors.groupingBy(EnterpriseChengguoBaseInfoDO::getChengguoName));
                        Map<String, EnterpriseChengguoBaseInfoDO> chengguoProCodeMap = new HashMap<>();
                        chengguoList.stream().forEach(c->{
                            String proNum = c.getProResultCode();
                            if(StringUtils.isNotBlank(proNum)) {
                                chengguoProCodeMap.put(proNum, c);
                            }
                        });
                        //获取团队
                        List<EnterpriTeamInfoDO> teamList = enterpriTeamInfoService.list(params);
                        Map<String,List<EnterpriTeamInfoDO>> teamMap = teamList.stream().collect(Collectors.groupingBy(EnterpriTeamInfoDO::getTeamName));
                        Map<String, EnterpriTeamInfoDO> teamProCodeMap = new HashMap<>();
                        teamList.stream().forEach(t->{
                            String proNum = t.getProResultCode();
                            if(StringUtils.isNotBlank(proNum)) {
                                teamProCodeMap.put(proNum, t);
                            }
                        });
                        //根据taskId获取个人
                        List<EnterpriPersonalInfoDO> personList = enterpriPersonalInfoService.list(params);
                        Map<String, List<EnterpriPersonalInfoDO>> personMap = personList.stream().collect(Collectors.groupingBy(EnterpriPersonalInfoDO::getUserName));
                        Map<String, EnterpriPersonalInfoDO> personProCodeMap = new HashMap<>();
                        personList.stream().forEach(p->{
                            String proNum = p.getProResultCode();
                            if(StringUtils.isNotBlank(proNum)) {
                                personProCodeMap.put(proNum, p);
                            }
                        });
                        //保存excel数据
                        List<ImportCheckExcelDataDO> excelDataDOList = new ArrayList<>();
                        for(String key: resultMap.keySet()) {
                            List<Object[]> objList = resultMap.get(key);
                            objList.stream().forEach(cellArr->{
                                ImportCheckExcelDataDO checkExcelDataDO = new ImportCheckExcelDataDO();
                                checkExcelDataDO.setTaskId(taskId);
                                checkExcelDataDO.setOptUid((int) uid);
                                String[] arr = key.split("#");
                                String sheetIndex = arr[0];
                                checkExcelDataDO.setExcelTabName(arr.length > 1 ? arr[1] : key);
                                String awardType = "0".equals(sheetIndex) ? "科技奖" : ("1".equals(sheetIndex) ? "团队" : "个人");

                                checkExcelDataDO.setAwardType(awardType);
                                String excelNum = cellArr[0] == null ? "" : cellArr[0].toString();
                                checkExcelDataDO.setExcelNum(excelNum.trim());
                                checkExcelDataDO.setApplyAccount(cellArr[1] == null ? "" : cellArr[1].toString());
                                //科技：成果名称，团队：团队名称,个人：个人名称
                                checkExcelDataDO.setResultName(cellArr[2] == null ? "" : cellArr[2].toString());
                                String eNum = checkExcelDataDO.getExcelNum();
                                if("0".equals(sheetIndex)){
                                   List<EnterpriseChengguoBaseInfoDO> cList = chengguoMap.get(checkExcelDataDO.getResultName());
                                   if(cList == null) {
                                       EnterpriseChengguoBaseInfoDO c = chengguoProCodeMap.get(eNum);
                                       if(c != null) {
                                           cList = new ArrayList<>();
                                           cList.add(c);
                                       }
                                   }
                                   checkExcelDataDO.setProId(cList != null && cList.size() > 0 && StringUtils.isNotBlank(cList.get(0).getProId()) ? Integer.parseInt(cList.get(0).getProId()) : 0);
                                }else if("1".equals(sheetIndex)) {
                                    List<EnterpriTeamInfoDO> tList = teamMap.get(checkExcelDataDO.getResultName());
                                    if(tList == null) {
                                        EnterpriTeamInfoDO t = teamProCodeMap.get(eNum);
                                        if(t != null) {
                                            tList = new ArrayList<>();
                                            tList.add(t);
                                        }
                                    }
                                   checkExcelDataDO.setProId(tList != null && tList.size() > 0 ? tList.get(0).getProId() : 0);
                                }else if("2".equals(sheetIndex)) {
                                    List<EnterpriPersonalInfoDO> pList = personMap.get(checkExcelDataDO.getResultName());
                                    if(pList == null) {
                                       EnterpriPersonalInfoDO p = personProCodeMap.get(eNum);
                                       if(p != null) {
                                           pList = new ArrayList<>();
                                           pList.add(p);
                                       }
                                    }
                                    checkExcelDataDO.setProId(pList != null && pList.size() > 0 ? pList.get(0).getProId() : 0);
                                }
                                checkExcelDataDO.setApplyEnterprise(cellArr[3] == null ? "" : cellArr[3].toString());
                                checkExcelDataDO.setCheckOpinion(cellArr[4] == null ? "" : cellArr[4].toString());
                                checkExcelDataDO.setCheckStat(cellArr[5] == null ? "" : cellArr[5].toString());

                                String validateRst = "";
                                if(checkExcelDataDO.getCheckStat().equals(EnumApplyEnterpriseProStat.NO_SCORE.getStatShowStr())) {
                                    validateRst = EnumApplyEnterpriseProStat.NO_SCORE.getStat();
                                }else if(checkExcelDataDO.getCheckStat().equals(EnumApplyEnterpriseProStat.SCORE.getStatShowStr())) {
                                    validateRst = EnumApplyEnterpriseProStat.SCORE.getStat();
                                }else if(checkExcelDataDO.getCheckStat().equals(EnumApplyEnterpriseProStat.REJECT.getStatShowStr())) {
                                    validateRst = EnumApplyEnterpriseProStat.REJECT.getStat();
                                }else if(checkExcelDataDO.getCheckStat().equals(EnumApplyEnterpriseProStat.DEFER_SCORE.getStatShowStr())) {
                                    validateRst = EnumApplyEnterpriseProStat.DEFER_SCORE.getStat();
                                }
                                checkExcelDataDO.setValidateResult(validateRst);

                                checkExcelDataDO.setMajor(cellArr[6] == null ? "" : cellArr[6].toString());
                                checkExcelDataDO.setProGroupName(cellArr[7] == null ? "" : cellArr[7].toString());
                                excelDataDOList.add(checkExcelDataDO);
                            });
                        }

                        importCheckExcelDataService.saveBatch(excelDataDOList);

                        //录入形式审查结果
                        importCheckExcelUpdateService.addScienceProValidateResult(taskId);
                        importCheckExcelUpdateService.updateScienceProMajorAndGroup(excelDataDOList);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }else if("science_personal_head".equals(fileType)) {
                    //科技奖个人头像上传
                }else if(StringUtils.isNotBlank(departId)) {
                    SurverEnterpriseSortInfoDO sortInfoDO = new SurverEnterpriseSortInfoDO();
                    sortInfoDO.setFileId(sysFile.getId().intValue());
                    sortInfoDO.setOptUid((int) uid);
                    sortInfoDO.setTaskId(taskId);
                    sortInfoDO.setDepartmentId(Integer.parseInt(departId));
                    sortInfoDO.setStorePath(sysFile.getUrl());
                    surverEnterpriseSortInfoService.save(sortInfoDO);
                }else if (isExpertSign) {
                  expertGroupService.updateExpertSignId(sysFile.getId(), taskId, expertUid);
                } else {
                    // 勘察奖评分标准：同任务下再次上传时替换旧文件（仅保留最新一份供专家下载）
                    if ("surver_score_standard_file".equals(fileType) && StringUtils.isNotBlank(taskId)) {
                        Map<String, Object> oldDocQuery = new HashMap<>();
                        oldDocQuery.put("taskId", taskId);
                        List<EnterpriseDocUploadDo> oldDocs = sysFileService.listTaskDocInfo(oldDocQuery);
                        if (oldDocs != null) {
                            for (EnterpriseDocUploadDo old : oldDocs) {
                                if ("surver_score_standard_file".equals(old.getFileType())) {
                                    sysFileService.deleteEnterpriseDoc(old.getId());
                                }
                            }
                        }
                    }
                    EnterpriseDocUploadDo uploadDo = new EnterpriseDocUploadDo();
                    uploadDo.setTaskId(taskId);
                    uploadDo.setFileId(sysFile.getId());
                    uploadDo.setProId(proId + "");
                    uploadDo.setFileType(fileType);
                    uploadDo.setFileName(fileName);
                    uploadDo.setUrl(fileUrl);
                    uploadDo.setCreated(DateUtils.getCurDateTime());
                    sysFileService.saveEnterpriseDoc(uploadDo);

                    if (fileType.equals(EnumProjectType.OIL_PRO_QUALITY.getProType() + "_desc") ||
                            fileType.equals(EnumProjectType.OIL_PRO_QUALITY_GOLD.getProType() + "_desc")) {
                        qualityProSituationService.removeByProId(proId);
                        OilQualityProSituationDO qualityProSituationDO = new OilQualityProSituationDO();
                        qualityProSituationDO.setProId(proId);
                        qualityProSituationDO.setFileId(sysFile.getId());
                        qualityProSituationDO.setOptUid(getUserId());
                        qualityProSituationDO.setUrl(fileUrl);
                        qualityProSituationDO.setTaskId(taskId);
                        qualityProSituationService.save(qualityProSituationDO);
                    }
                    if (fileType.startsWith(EnumProjectType.OIL_PRO_QUALITY.getProType() + "_confirm") ||
                            fileType.startsWith(EnumProjectType.OIL_PRO_QUALITY_GOLD.getProType() + "_confirm") ||
                            fileType.startsWith(EnumProjectType.OIL_PRO_INSTALL.getProType() + "_confirm")
                    ) {
                        OilQualityConfirmFileDO confirmFileDO = new OilQualityConfirmFileDO();
                        confirmFileDO.setFileId(sysFile.getId());
                        String[] arr = fileType.split("-");
                        confirmFileDO.setFileType(arr[arr.length - 1]);
                        confirmFileDO.setProId(proId);
                        confirmFileDO.setOptUid(getUserId());
                        confirmFileDO.setTaskId(taskId);
                        confirmFileDO.setUrl(fileUrl);
                        qualityConfirmFileService.save(confirmFileDO);
                    }

                    uploadFileList.add(uploadDo);
                }
            }
        }
        R rst = R.ok();
        rst.put("fileType", fileType);
        rst.put("files", uploadFileList);
        rst.put("fileSize", uploadFileList.size());
        rst.put("fileUrl", fileUrl);
        return rst;
    }


    @ResponseBody
    @RequestMapping("/to_getworker")
    public R toAssignProWorer(@RequestParam Map<String, Object> params, ModelMap map) {
        params.put("roleId", "65");
        List<UserDO> assWorkers = userService.list(params);
        map.put("assWorkers", assWorkers);

        R rst = R.ok();
        rst.put("allWorkers", assWorkers);
        return rst;
    }


    @ResponseBody
    @RequestMapping("/manage_professiondd")
    public String getDataByTaskIdType(@RequestParam Map<String, Object> params, ModelMap map) {
//         List<EnterpriseProjectInfoDo> list =  awardEnterpriseProjectService.getAllProList(params);
//        String procInsId = taskDo.getProcInsId();
//        boolean isAssign = true;
//        if(StringUtils.isNotBlank(procInsId)) {
//            List<Task> taskList = taskService.createTaskQuery()
//                    .processInstanceId(procInsId).list();
//            Task task = taskList.size() > 0 ? taskList.get(0) : null;
//            if(task != null) {
//                String defKey = task.getTaskDefinitionKey();
//                isAssign = defKey.equals("distribute_pros");
//            }
//        }
//        map.put("isAssign",isAssign);
//        map.put("publishTaskId",publishTaskId);
//        params.put("roleId","65");
//        if(isAssign) {
//            //如果是分派阶段则可查询进行分派
//            List<UserDO> assWorkers = userService.list(params);
//            map.put("assWorkers", assWorkers);
//        }
//        // 企业申请的数据
//        map.put("assPros", list);
//
//        List<String> majo = new ArrayList<>();
//
//        for(EnterpriseProjectInfoDo workerUid:list){
//            if (workerUid.getMajor() != null && workerUid.getMajor().length() > 0)
//                if (!majo.contains(workerUid.getMajor())){
//                    majo.add(workerUid.getMajor());
//                }
//        }
//        map.put("profession", majo);

        return "act/award/association_profession_manage";
    }


    /**
     * 待分配专业 分配给外聘人员
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("/manage_profession/external")
    public R toManageProfessionExternal(@RequestParam Map<String, Object> params) {
        params.put("roleId", ROLE_SCIENCE_EXTERNAL_EMPLOYMENT_ID);
        List<EnterpriseProjectInfoDo> list = awardEnterpriseProjectService.getAllProList(params);

        R rst = R.ok();
        rst.put("list", list);
        return rst;

    }


    @GetMapping("/pro_tree")
    @ResponseBody
    String tree(@RequestParam Map<String, Object> params) {
        getProListParamsByRole(params);
        Tree<EnterpriseProjectInfoDo> tree = awardEnterpriseProjectService.getTree(params);
        return JSONUtils.gson.toJson(tree);
    }


    /**
     * 跳转到分组管理页面
     */
    @RequestMapping("/to_group_manage/{taskId}")
    public String toGroupManage(@PathVariable("taskId") String taskId, ModelMap map) {
        map.put("taskId", taskId);
        return "act/award/group_manage";
    }
    /**
     * 跳转到新增分组页面
     */
    @RequestMapping("/group/add/{taskId}")
    public String groupAdd(@PathVariable("taskId") String taskId, ModelMap map) {
        map.put("taskId", taskId);
        return "act/award/group_form";
    }

    /**
     * 跳转到编辑分组页面
     */
    @RequestMapping("/group/edit/{taskId}/{groupid}")
    public String groupEdit(@PathVariable("taskId") String taskId,
                            @PathVariable("groupid") Integer groupid, ModelMap map) {
        map.put("taskId", taskId);
        QcGroupDO group = qcGroupService.get(taskId,groupid);
        map.put("group", group);
        return "act/award/group_form";
    }

    /**
     * 获取分组列表
     */
    @ResponseBody
    @GetMapping("/group/list")
    public PageUtils groupList(@RequestParam Map<String, Object> params) {
        if (params.get("taskId") != null) {
            params.put("taskid", params.get("taskId").toString());
        }
        Query query = new Query(params);
        List<QcGroupDO> list = qcGroupService.list(query);
        int total = qcGroupService.count(query);
        PageUtils pageUtils = new PageUtils(list, total);
        return pageUtils;
    }

    /**
     * 保存分组
     */
    @ResponseBody
    @RequestMapping("/group/save")
    public R groupSave(QcGroupDO qcGroup) {
        // 检查分组名是否已存在
        if (qcGroupService.isGroupNameExist(qcGroup.getTaskid(), qcGroup.getName())) {
            return R.error("分组名称已存在");
        }

        if (qcGroup.getGroupid() == null || qcGroup.getGroupid() == 0) {
            // 新增分组，生成新的 groupid
            List<QcGroupDO> groups = qcGroupService.getGroupsByTaskId(qcGroup.getTaskid());
            int maxGroupId = 0;
            for (QcGroupDO group : groups) {
                if (group.getGroupid() > maxGroupId) {
                    maxGroupId = group.getGroupid();
                }
            }
            qcGroup.setGroupid(maxGroupId + 1);
            if (qcGroupService.save(qcGroup) > 0) {
                return R.ok();
            }
        } else {
            // 更新分组，检查名称是否被其他分组使用
            QcGroupDO existGroup = qcGroupDao.isGroupNameExistExcludeGroupId(qcGroup.getTaskid(), qcGroup.getName(), qcGroup.getGroupid());
            if (existGroup != null) {
                return R.error("分组名称已存在");
            }
            if (qcGroupService.update(qcGroup) > 0) {
                if(qcGroupService.updatepro(qcGroup)>0){
                    return R.ok();
                };

            }
        }
        return R.error();
    }

    /**
     * 删除分组
     */
    @ResponseBody
    @RequestMapping("/group/remove")
    public R groupRemove(Integer groupid) {
        if (qcGroupService.remove(groupid) > 0) {
            return R.ok();
        }
        return R.error();
    }
    /**
     * 检查并删除分组（单个）
     */
    @ResponseBody
    @RequestMapping("/group/check_and_remove")
    public R groupCheckAndRemove(Integer groupid, String taskId) {
        // 检查是否有项目已分配到该分组
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("groupId", groupid);

        int count = awardEnterpriseProjectService.countProByGroupId(params);
        if (count > 0) {
            return R.error("该分组下已有 " + count + " 个项目，无法删除！");
        }

        if (qcGroupService.remove(groupid) > 0) {
            return R.ok();
        }
        return R.error();
    }

    /**
     * 批量检查并删除分组
     */
    @ResponseBody
    @RequestMapping("/group/batch_check_and_remove")
    public R groupBatchCheckAndRemove(Integer[] groupids, String taskId) {
        List<String> errorMessages = new ArrayList<>();

        if (groupids == null || groupids.length == 0) {
            return R.error("请选择要删除的分组");
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            return R.error("任务 ID 不能为空");
        }

        for (Integer groupid : groupids) {
            // 检查是否有项目已分配到该分组
            Map<String, Object> params = new HashMap<>();
            params.put("taskId", taskId);
            params.put("groupId", groupid);

            int count = awardEnterpriseProjectService.countProByGroupId(params);
            if (count > 0) {
                QcGroupDO group = qcGroupService.get(taskId,groupid);
                errorMessages.add("分组【" + (group != null ? group.getName() : groupid) + "】下已有 " + count + " 个项目，无法删除！");
                continue;
            }

            qcGroupService.remove(groupid);
        }

        if (errorMessages.size() > 0) {
            StringBuilder errorMsg = new StringBuilder("以下分组无法删除：<br/>");
            for (String msg : errorMessages) {
                errorMsg.append(msg).append("<br/>");
            }
            return R.error(errorMsg.toString());
        }

        return R.ok();
    }


    /**
     * 批量删除分组
     */
    @ResponseBody
    @RequestMapping("/group/batchRemove")
    public R groupBatchRemove(Integer[] groupids) {
        if (qcGroupService.batchRemove(groupids) > 0) {
            return R.ok();
        }
        return R.error();
    }

    /**
     * 将项目分配到分组
     */
    @ResponseBody
    @RequestMapping("/assign_to_group")
    public R assignToGroup(@RequestParam Integer proId, @RequestParam Integer groupId) {
        Map<String, Object> params = new HashMap<>();
        params.put("proId", proId);
        params.put("groupId", groupId);

        if (awardEnterpriseProjectService.updateProGroup(params) > 0) {
            return R.ok();
        }
        return R.error();
    }
    /** 勘察奖导入：预加载当前任务下项目列表字段，用于与 Excel 比对是否真有变更 */
    private Map<Integer, SurverProjectInfo> buildSurverProSnapshotMap(String taskId) {
        List<SurverProjectInfo> list = surverAwardService.listProImportSnapshot(taskId);
        Map<Integer, SurverProjectInfo> map = new HashMap<>();
        if (list == null) {
            return map;
        }
        for (SurverProjectInfo p : list) {
            if (p.getProId() > 0) {
                map.put(p.getProId(), p);
            }
        }
        return map;
    }

    private static String importCellNorm(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean importCellChanged(String excelVal, String dbVal) {
        return !importCellNorm(excelVal).equals(importCellNorm(dbVal));
    }

    // 解析Excel表头列索引,容错匹配Excel表头名称
    private static Integer resolveImportHeaderIndex(Map<String, Integer> headerMap, String headerName) {
        // 如果headerMap为空或者headerName为空，则返回null
        if (headerMap == null || StringUtils.isBlank(headerName)) {
            return null;
        }
        // 从headerMap中通过表头列名获取索引idx
        Integer idx = headerMap.get(headerName);
        // 如果idx不为空，则返回idx
        if (idx != null) {
            return idx;
        }
        // 如果idx为空，则遍历headerMap进行精确匹配
        // 完全限定名写法或者import  时间复杂度为O(n)而不是O(1)
        for (Entry<String, Integer> entry : headerMap.entrySet()) {
            if (entry.getKey() != null &&
                    entry.getKey()
                         //去除BOM标记: \uFEFF是UTF-8文件的字节顺序标记，可能导致字符串比较失败
                         .replace("\uFEFF", "")
                         .trim().equals(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** 规范「是否有查新」：是/否；无法识别时返回 trim 后原文供校验提示 */
    private static String normalizeImportNovelty(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\uFEFF", "").replace('\u00A0', ' ').trim();
        if (s.isEmpty()) {
            return "";
        }
        if ("是".equals(s) || "Y".equalsIgnoreCase(s) || "YES".equalsIgnoreCase(s) || "1".equals(s) || "有".equals(s)) {
            return "是";
        }
        if ("否".equals(s) || "N".equalsIgnoreCase(s) || "NO".equalsIgnoreCase(s) || "0".equals(s) || "无".equals(s)) {
            return "否";
        }
        return s;
    }

    private static boolean isValidImportNovelty(String normalized) {
        return "是".equals(normalized) || "否".equals(normalized);
    }

    private static final org.apache.poi.ss.usermodel.DataFormatter IMPORT_CELL_FORMATTER =
            new org.apache.poi.ss.usermodel.DataFormatter();

    /** 与导出 Excel 一致：形审字段去 HTML 后再比对 */
    private static String stripHtmlForImportCompare(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String t = s;
        t = t.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        t = t.replaceAll("(?is)<style[^>]*>.*?</style>", "");
        t = t.replaceAll("(?i)<br\\s*/?>", "\n");
        t = t.replaceAll("(?i)</p\\s*>", "\n");
        t = t.replaceAll("(?i)</div\\s*>", "\n");
        t = t.replace('\u00A0', ' ');
        t = t.replaceAll("<[^>]+>", "");
        t = t.replaceAll("(?i)&nbsp;|&#160;", " ");
        t = t.replaceAll("&lt;", "<");
        t = t.replaceAll("&gt;", ">");
        t = t.replaceAll("&quot;", "\"");
        t = t.replaceAll("&#39;", "'");
        t = t.replaceAll("&amp;", "&");
        t = t.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        t = t.replaceAll("\\n\\s*\\n+", "\n");
        return t.trim();
    }

    private String findSurverProSubTypeByTaskAndProId(String taskId, Integer proId) {
        if (StringUtils.isBlank(taskId) || proId == null) {
            return "";
        }
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("proId", proId);
        List<SurverProjectInfo> list = surverAwardService.listProInfo(params);
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.get(0).getProSubType();
    }

    private void saveSurverReviewRecord(String proSubType, Integer proId, String taskId, Integer optUid,
                                        String reviewResult, String reviewComment) {
        Date now = new Date();
        if ("design".equals(proSubType)) {
            SurverReviewDesignResultDO reviewDO = new SurverReviewDesignResultDO();
            reviewDO.setProId(proId);
            reviewDO.setTaskId(taskId);
            reviewDO.setOptUid(optUid);
            reviewDO.setReviewResult(reviewResult);
            reviewDO.setRemarks(reviewComment);
            reviewDO.setCreated(now);
            surverReviewDesignResultService.save(reviewDO);
        } else if ("software".equals(proSubType)) {
            SurverReviewSoftResultDO reviewDO = new SurverReviewSoftResultDO();
            reviewDO.setProId(proId);
            reviewDO.setTaskId(taskId);
            reviewDO.setOptUid(optUid);
            reviewDO.setReviewResult(reviewResult);
            reviewDO.setRemarks(reviewComment);
            reviewDO.setCreated(now);
            surverReviewSoftResultService.save(reviewDO);
        } else if ("standard".equals(proSubType)) {
            SurverReviewStandardResultDO reviewDO = new SurverReviewStandardResultDO();
            reviewDO.setProId(proId);
            reviewDO.setTaskId(taskId);
            reviewDO.setOptUid(optUid);
            reviewDO.setReviewResult(reviewResult);
            reviewDO.setRemarks(reviewComment);
            reviewDO.setCreated(now);
            surverReviewStandardResultService.save(reviewDO);
        } else if ("contribution".equals(proSubType)) {
            SurverReviewSurverResultDO reviewDO = new SurverReviewSurverResultDO();
            reviewDO.setProId(proId);
            reviewDO.setTaskId(taskId);
            reviewDO.setOptUid(optUid);
            reviewDO.setReviewResult(reviewResult);
            reviewDO.setRemarks(reviewComment);
            reviewDO.setCreated(now);
            surverReviewSurverResultService.save(reviewDO);
        }
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        try {
            String formatted = IMPORT_CELL_FORMATTER.formatCellValue(cell);
            if (formatted != null) {
                return formatted.trim();
            }
        } catch (Exception ignored) {
            // fallback below
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == (long) numVal) {
                    return String.valueOf((long) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    switch (cell.getCachedFormulaResultType()) {
                        case STRING:
                            return cell.getStringCellValue().trim();
                        case NUMERIC:
                            double fv = cell.getNumericCellValue();
                            if (fv == (long) fv) {
                                return String.valueOf((long) fv);
                            }
                            return String.valueOf(fv);
                        case BOOLEAN:
                            return String.valueOf(cell.getBooleanCellValue());
                        default:
                            return "";
                    }
                } catch (Exception e) {
                    return "";
                }
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * 根据文件扩展名获取工作簿对象
     */
    private Workbook getWorkbook(java.io.InputStream inputStream, String fileName) throws java.io.IOException {
        if(fileName.endsWith(".xlsx")) {
            return new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream);
        } else {
            return new org.apache.poi.hssf.usermodel.HSSFWorkbook(inputStream);
        }
    }

    // ============================================================
    // 专家分组管理（与上方"分组管理 / qcGroup / ass_qc_group"不是同一功能点）
    // 新增表：ass_award_expert_group / ass_award_pro_expert_group
    // 路由前缀：/enterprise_pro/expert_group/*
    // 适用于勘察奖任务管理下四个子 tab 的课题专家分组
    // ============================================================

    /**
     * 跳转到专家分组管理页面
     */
    @RequestMapping("/to_expert_group_manage/{taskId}")
    public String toExpertGroupManage(@PathVariable("taskId") String taskId, ModelMap map) {
        map.put("taskId", taskId);
        return "act/award/expert_group_manage";
    }

    /**
     * 跳转到新增专家分组页面
     */
    @RequestMapping("/expert_group/add/{taskId}")
    public String expertGroupAdd(@PathVariable("taskId") String taskId, ModelMap map) {
        map.put("taskId", taskId);
        return "act/award/expert_group_form";
    }

    /**
     * 跳转到编辑专家分组页面
     */
    @RequestMapping("/expert_group/edit/{taskId}/{groupid}")
    public String expertGroupEdit(@PathVariable("taskId") String taskId,
                                  @PathVariable("groupid") Integer groupid, ModelMap map) {
        map.put("taskId", taskId);
        AwardExpertGroupDO group = awardExpertGroupService.get(taskId, groupid);
        map.put("group", group);
        return "act/award/expert_group_form";
    }

    /**
     * 获取专家分组列表
     */
    @ResponseBody
    @GetMapping("/expert_group/list")
    public PageUtils expertGroupList(@RequestParam Map<String, Object> params) {
        if (params.get("taskId") != null) {
            params.put("taskid", params.get("taskId").toString());
        }
        Query query = new Query(params);
        List<AwardExpertGroupDO> list = awardExpertGroupService.list(query);
        int total = awardExpertGroupService.count(query);
        return new PageUtils(list, total);
    }

    /**
     * 保存专家分组（新增 / 编辑）
     */
    @ResponseBody
    @RequestMapping("/expert_group/save")
    public R expertGroupSave(AwardExpertGroupDO group) {
        if (group == null || group.getTaskid() == null || group.getName() == null
                || group.getName().trim().isEmpty()) {
            return R.error("参数不完整");
        }

        if (group.getGroupid() == null || group.getGroupid() == 0) {
            // 新增：检查名称重复，并生成新 groupid
            if (awardExpertGroupService.isGroupNameExist(group.getTaskid(), group.getName())) {
                return R.error("专家分组名称已存在");
            }
            List<AwardExpertGroupDO> groups = awardExpertGroupService.getGroupsByTaskId(group.getTaskid());
            int maxGroupId = 0;
            for (AwardExpertGroupDO g : groups) {
                if (g.getGroupid() != null && g.getGroupid() > maxGroupId) {
                    maxGroupId = g.getGroupid();
                }
            }
            group.setGroupid(maxGroupId + 1);
            if (awardExpertGroupService.save(group) > 0) {
                return R.ok();
            }
        } else {
            // 编辑：排除自身后检查名称重复
            AwardExpertGroupDO exist = awardExpertGroupService.isGroupNameExistExcludeGroupId(
                    group.getTaskid(), group.getName(), group.getGroupid());
            if (exist != null) {
                return R.error("专家分组名称已存在");
            }
            if (awardExpertGroupService.update(group) > 0) {
                return R.ok();
            }
        }
        return R.error();
    }

    /**
     * 删除单个专家分组（含课题分配关系清理）
     */
    @ResponseBody
    @RequestMapping("/expert_group/remove")
    public R expertGroupRemove(String taskId, Integer groupid) {
        if (taskId == null || groupid == null) {
            return R.error("参数不完整");
        }
        if (awardExpertGroupService.remove(taskId, groupid) > 0) {
            return R.ok();
        }
        return R.error();
    }

    /**
     * 检查并删除单个专家分组（若分组下仍有课题则拒绝）
     */
    @ResponseBody
    @RequestMapping("/expert_group/check_and_remove")
    public R expertGroupCheckAndRemove(String taskId, Integer groupid) {
        if (taskId == null || groupid == null) {
            return R.error("参数不完整");
        }
        int count = awardExpertGroupService.countProByGroupId(taskId, groupid);
        if (count > 0) {
            return R.error("该专家分组下已有 " + count + " 个课题，无法删除！");
        }
        if (awardExpertGroupService.remove(taskId, groupid) > 0) {
            return R.ok();
        }
        return R.error();
    }

    /**
     * 批量检查并删除专家分组
     */
    @ResponseBody
    @RequestMapping("/expert_group/batch_check_and_remove")
    public R expertGroupBatchCheckAndRemove(Integer[] groupids, String taskId) {
        if (groupids == null || groupids.length == 0) {
            return R.error("请选择要删除的专家分组");
        }
        if (taskId == null || taskId.trim().isEmpty()) {
            return R.error("任务 ID 不能为空");
        }

        List<String> errorMessages = new ArrayList<>();
        for (Integer gid : groupids) {
            int count = awardExpertGroupService.countProByGroupId(taskId, gid);
            if (count > 0) {
                AwardExpertGroupDO g = awardExpertGroupService.get(taskId, gid);
                errorMessages.add("专家分组【" + (g != null ? g.getName() : gid)
                        + "】下已有 " + count + " 个课题，无法删除！");
                continue;
            }
            awardExpertGroupService.remove(taskId, gid);
        }

        if (!errorMessages.isEmpty()) {
            StringBuilder sb = new StringBuilder("以下专家分组无法删除：<br/>");
            for (String msg : errorMessages) {
                sb.append(msg).append("<br/>");
            }
            return R.error(sb.toString());
        }
        return R.ok();
    }

    /**
     * 批量删除专家分组（不做项目占用检查；保留以兼容前端）
     */
    @ResponseBody
    @RequestMapping("/expert_group/batchRemove")
    public R expertGroupBatchRemove(Integer[] groupids, String taskId) {
        if (taskId == null || groupids == null || groupids.length == 0) {
            return R.error("参数不完整");
        }
        if (awardExpertGroupService.batchRemove(taskId, groupids) > 0) {
            return R.ok();
        }
        return R.error();
    }

    /**
     * 将课题分配到专家分组（用于四个子 tab 的课题列表"选择专家分组"）
     */
    @ResponseBody
    @RequestMapping("/expert_group/assign")
    public R assignProToExpertGroup(@RequestParam String taskId,
                                    @RequestParam Integer proId,
                                    @RequestParam Integer groupId) {
        if (taskId == null || proId == null || groupId == null) {
            return R.error("参数不完整");
        }
        if (awardExpertGroupService.assignProToGroup(taskId, proId, groupId) > 0) {
            return R.ok();
        }
        return R.error();
    }

    /**
     * 查询课题当前所属专家分组 ID（前端可用于回显）
     */
    @ResponseBody
    @GetMapping("/expert_group/pro_group")
    public R getProExpertGroup(@RequestParam String taskId, @RequestParam Integer proId) {
        Integer groupId = awardExpertGroupService.getProGroupId(taskId, proId);
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        return R.ok(data);
    }

    /**
     * 批量返回某任务下所有课题的专家分组归属（含名称），用于前端列表渲染。
     * 返回格式：{ code:0, data:[ { proid, groupid, name }, ... ] }
     */
    @ResponseBody
    @GetMapping("/expert_group/pro_assignments")
    public R listProExpertGroupAssignments(@RequestParam String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return R.error("任务 ID 不能为空");
        }
        List<Map<String, Object>> list = awardExpertGroupService.listProAssignments(taskId);
        Map<String, Object> data = new HashMap<>();
        data.put("data", list);
        return R.ok(data);
    }

    /**
     * 导出"上传专家分组"模板（含当前项目数据，表头去掉后四项"分组、形审结果、形审评语、状态"，添加"专业分组"列）
     */
    @RequestMapping("/expert_group/exportTemplate")
    public void exportExpertGroupTemplate(HttpServletResponse response, @RequestParam String taskId) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        // 查询全部四个子类型的项目
        String[] subTypes = {"contribution", "design", "software", "standard"};
        List<SurverProjectInfo> proList = new ArrayList<>();
        for (String subType : subTypes) {
            Map<String, Object> queryParams = new HashMap<>(params);
            queryParams.put("proSubType", subType);
            List<SurverProjectInfo> subList = surverAwardService.listProInfo(queryParams);
            if (subList != null && !subList.isEmpty()) {
                proList.addAll(subList);
            }
        }
        // 获取当前专家分组分配情况
        List<Map<String, Object>> assignments = awardExpertGroupService.listProAssignments(taskId);
        Map<Integer, String> proGroupNameMap = new HashMap<>();
        if (assignments != null) {
            for (Map<String, Object> a : assignments) {
                Object pidObj = a.get("proid");
                Object nameObj = a.get("name");
                if (pidObj != null && nameObj != null) {
                    proGroupNameMap.put(Integer.parseInt(pidObj.toString()), nameObj.toString());
                }
            }
        }

        String[] header = {"序号", "proId", "项目编号", "项目类别", "项目名称", "申报单位", "专业", "人员名单", "申报账号", "申报联系方式", "专业分组"};
        List<Map<String, String>> rows = new ArrayList<>();
        int idx = 1;
        for (SurverProjectInfo pro : proList) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("序号", String.valueOf(idx++));
            row.put("proId", String.valueOf(pro.getProId()));
            row.put("项目编号", pro.getProCode() != null ? pro.getProCode() : "");
            row.put("项目类别", pro.getProSubTypeStr() != null ? pro.getProSubTypeStr() : "");
            row.put("项目名称", pro.getProName() != null ? pro.getProName() : "");
            row.put("申报单位", pro.getApplyCompany() != null ? pro.getApplyCompany() : "");
            row.put("专业", pro.getMajor() != null ? pro.getMajor() : "");
            row.put("人员名单", pro.getMemberList() != null ? pro.getMemberList() : "");
            row.put("申报账号", pro.getDeclareAccount() != null ? pro.getDeclareAccount() : "");
            row.put("申报联系方式", pro.getApplyAccount() != null ? pro.getApplyAccount() : "");
            row.put("专业分组", proGroupNameMap.getOrDefault(pro.getProId(), ""));
            rows.add(row);
        }

        try {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "attachment;filename=" +
                    java.net.URLEncoder.encode("专家分组导入模板.xls", "UTF-8"));
            org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
            org.apache.poi.hssf.usermodel.HSSFSheet sheet = workbook.createSheet("专家分组模板");
            sheet.setDefaultColumnWidth(18);
            org.apache.poi.hssf.usermodel.HSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.YELLOW.index);
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.hssf.usermodel.HSSFRow headRow = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                org.apache.poi.hssf.usermodel.HSSFCell cell = headRow.createCell(i);
                cell.setCellValue(header[i]);
                cell.setCellStyle(headerStyle);
            }
            for (int i = 0; i < rows.size(); i++) {
                org.apache.poi.hssf.usermodel.HSSFRow row = sheet.createRow(i + 1);
                Map<String, String> rowData = rows.get(i);
                for (int j = 0; j < header.length; j++) {
                    String val = rowData.get(header[j]);
                    row.createCell(j).setCellValue(val == null ? "" : val);
                }
            }
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 导入"上传专家分组"Excel，按 proId 匹配更新专家分组
     */
    @ResponseBody
    @RequestMapping("/expert_group/importExcel")
    public R importExpertGroupExcel(@RequestParam("file") MultipartFile file, @RequestParam String taskId) {
        if (file == null || file.isEmpty()) {
            return R.error("请选择文件");
        }
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        try {
            Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return R.error("表格为空");
            }

            // 读取表头，找到 proId 和 专业分组 列的索引
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return R.error("表头为空");
            }
            int proIdCol = -1;
            int groupNameCol = -1;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String cellVal = getCellStringValue(headerRow.getCell(i));
                if ("proId".equals(cellVal.trim())) {
                    proIdCol = i;
                } else if ("专业分组".equals(cellVal.trim())) {
                    groupNameCol = i;
                }
            }
            if (proIdCol < 0 || groupNameCol < 0) {
                return R.error("表头缺少'proId'或'专业分组'列");
            }

            // 预加载该任务下的所有专家分组 name -> groupId 的映射
            List<AwardExpertGroupDO> allGroups = awardExpertGroupService.getGroupsByTaskId(taskId);
            Map<String, Integer> groupNameIdMap = new HashMap<>();
            if (allGroups != null) {
                for (AwardExpertGroupDO g : allGroups) {
                    if (g.getName() != null) {
                        groupNameIdMap.put(g.getName().trim(), g.getGroupid());
                    }
                }
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder failMsg = new StringBuilder();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String proIdStr = getCellStringValue(row.getCell(proIdCol)).trim();
                String groupName = getCellStringValue(row.getCell(groupNameCol)).trim();
                if (proIdStr.isEmpty()) continue;
                if (groupName.isEmpty()) continue;

                Integer proId;
                try {
                    proId = Integer.parseInt(proIdStr.contains(".") ? proIdStr.substring(0, proIdStr.indexOf(".")) : proIdStr);
                } catch (NumberFormatException e) {
                    failCount++;
                    failMsg.append("第").append(i + 1).append("行proId格式错误; ");
                    continue;
                }

                Integer groupId = groupNameIdMap.get(groupName);
                if (groupId == null) {
                    failCount++;
                    failMsg.append("第").append(i + 1).append("行分组'").append(groupName).append("'不存在; ");
                    continue;
                }

                awardExpertGroupService.assignProToGroup(taskId, proId, groupId);
                successCount++;
            }

            String msg = "导入完成：成功 " + successCount + " 条";
            if (failCount > 0) {
                msg += "，失败 " + failCount + " 条。" + failMsg.toString();
            }
            return R.ok(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("导入异常: " + e.getMessage());
        }
    }

    private String getCellStringValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        cell.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : "";
    }
}
