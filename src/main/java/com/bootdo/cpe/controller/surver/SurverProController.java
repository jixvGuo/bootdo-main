package com.bootdo.cpe.controller.surver;

import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.common.domain.FileDO;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.*;
import com.bootdo.cpe.service.*;
import com.bootdo.cpe.utils.AwardSurverSubTypeEnum;
import com.bootdo.cpe.utils.SurverEliminateExcelExportUtils;
import com.bootdo.system.domain.SurverExcellentApplyTableInfoDO;
import com.bootdo.system.domain.UserDO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

import static com.bootdo.common.config.Constant.*;

/**
 * 勘察奖项目列表信息
 *
 * @author houzb
 * @version 1.0
 * @date 2022-03-31 21:27
 */
@Controller
@RequestMapping("/surverPro")
public class SurverProController extends BaseSurverController {
    private String prefix = "cpe/survey";

    /** 勘察奖项目列表导出 Excel 表头（「是否有查新」固定为最后一列） */
    private static final String[] SURVER_PRO_LIST_EXPORT_HEADERS = {
            "序号", "proId", "项目编号", "项目类别", "项目名称", "申报单位", "专业", "人员名单",
            "申报账号", "申报联系方式", "分组", "形审结果", "形审评语", "状态", "是否有查新"
    };

    @Autowired
    private SurverAwardService surverAwardService;
    @Autowired
    private QcAwardService qcAwardService;
    @Autowired
    private BootdoConfig bootdoConfig;
    @Autowired
    private AwardEnterpriseProjectCommonService awardEnterpriseProjectCommonService;
    @Autowired
    private SurverDesignApplyTableInfoService surverDesignApplyTableInfoService;
    @Autowired
    private SurverSoftApplyTableInfoService surverSoftApplyTableInfoService;
    @Autowired
    private SurverStandardApplyTableInfoService surverStandardApplyTableInfoService;
    @Autowired
    private SurverExcellentApplyTableInfoService surverExcellentApplyTableInfoService;
    @Autowired
    private SurverBaseApplyTableInfoService surverBaseApplyTableInfoService;
    @Autowired
    private ExpertGroupService expertGroupService;
    @Autowired
    private com.bootdo.activiti.service.AwardPublishTaskService awardPublishTaskService;
    @Autowired
    private SurverDesignApplyProjectProfileService surverDesignApplyProjectProfileService;
    @Autowired
    private SurverExcellentApplyProjectProfileService surverExcellentApplyProjectProfileService;
    @Autowired
    private SurverStandardApplyProjectProfileService surverStandardApplyProjectProfileService;
    @Autowired
    private SurverSoftApplyProjectProfileService surverSoftApplyProjectProfileService;
    @Autowired
    private com.bootdo.cpe.service.SurverExpertEliminateService surverExpertEliminateService;

    @RequiresPermissions("surveraward:to:prolist")
    @RequestMapping("/toProListMain")
    public String toProListMain(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        return prefix + "/surver_pro_list_main";
    }


    /**
     * 查看项目列表
     * @param map
     * @param params
     * @return
     */
    @RequiresPermissions("surveraward:to:prolist")
    @RequestMapping("/toProList")
    public String toProList(ModelMap map, @RequestParam Map<String, Object> params) {
        packageAwardTaskId(map, params);
        map.put("proSubType", params.get("proSubType"));
        UserDO user = getUser();
        List<Long> roleIdList = user.getRoleIds();
        boolean isAssociationLeader = roleIdList != null && roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID);
        boolean isEnterpriseUser = roleIdList != null && roleIdList.contains(ROLE_ENTERPRISE_SURVER_ID);
        boolean isAdmin = roleIdList != null && roleIdList.contains(ROLE_ADMIN_ID);
        map.put("isAssociationLeader", isAssociationLeader || isAdmin);
        map.put("isEnterpriseUser", isEnterpriseUser);
        // 新增：勘察奖小组联络人(86) 标志，用于显示独有的"专家分组管理"入口按钮
        boolean isSurverGroupContact = roleIdList != null && roleIdList.contains(ROLE_SURVER_GROUP_CONTACT_ID);
        map.put("isSurverGroupContact", isSurverGroupContact);
        return prefix + "/surver_pro_list";
    }


    /**
     * 获取项目列表
     * @return
     */
    @RequestMapping("/get/proList")
    @ResponseBody
    public PageUtils getSurverProList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        UserDO user = getUser();
        Long uid = getUserId();
        List<Long> roleIdList = user.getRoleIds();
        if (roleIdList.contains(ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID)) {
            // 勘察奖协会外聘人员(75)：查看全部项目，不按分派过滤
        } else if (roleIdList.contains(ROLE_QC_EXTERNAL_EMPLOYMENT_ID)
                || roleIdList.contains(ROLE_SCIENCE_EXTERNAL_EMPLOYMENT_ID)) {
            // 其他外聘人员：只看分派给自己的项目
            params.put("ass_assign_uid", uid);
        } else if (roleIdList.contains(ROLE_ASSOCIATION_LEADER)
                || roleIdList.contains(ROLE_ADMIN_ID)) {
            // 协会领导/管理员：查看全部项目
        } else if (roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)) {
            // 协会联系人：按协会维度查看
            params.put("associationUserId", roleIdList.contains(ROLE_SURVER_OFFLINE_VIEW_ID) ? 101 : user.getUserId());
        } else if (roleIdList.contains(ROLE_ENTERPRISE_SURVER_ID)) {
            //企业用户查看自己创建项目
            params.put("enterpriseUid", uid);
        } else if(roleIdList.contains(ROLE_SURVER_SPECALIST_ID)) {
            //评审专家
            params.put("scoreSpecialistUid", uid);
            // 自动匹配专家绑定的任务ID
            Map<String, Object> bindQuery = new HashMap<>();
            bindQuery.put("userId", String.valueOf(uid));
            bindQuery.put("proType", "surver_pro_group");
            List<com.bootdo.cpe.domain.ExpertGroupDO> expertBindings = expertGroupService.list(bindQuery);
            if (expertBindings != null && !expertBindings.isEmpty()) {
                String bindTaskId = expertBindings.get(0).getTaskId();
                if (bindTaskId != null && !bindTaskId.isEmpty()) {
                    params.put("taskId", bindTaskId);
                }
            }
        // 新增：勘察奖小组联络人(86) 仅看绑定分组下的项目
        } else if (roleIdList.contains(ROLE_SURVER_GROUP_CONTACT_ID)) {
            params.put("contactUserId", uid);
        } else {
            //分派给自己的项目
            params.put("ass_assign_uid", uid);
        }
        boolean isLeaderOrAdmin = roleIdList.contains(ROLE_ASSOCIATION_LEADER) || roleIdList.contains(ROLE_ADMIN_ID);
        if (!isLeaderOrAdmin) {
            getProListParamsByRole(params);
        } else {
            // 领导/管理员直接查看当前任务下全部项目，不叠加角色限制
            params.remove("ass_assign_uid");
            params.remove("ass_worker_uid");
            params.remove("associationUserId");
            params.remove("enterpriseUid");
            params.remove("scoreSpecialistUid");
            params.remove("contactUserId");
            params.remove("createUid");
        }

        params.put("proStatStr", "");
        Object keyWordObj = params.get("keyWord");
        if (keyWordObj != null) {
            List<String> proStatList = QcProStatEnum.getStatValByKey(keyWordObj.toString());
            if (proStatList.size() > 0) {
                String str = new String();
                for (String s : proStatList) {
                    str +=  s + ",";
                }
                params.put("proStatStr", str.substring(0, str.length() - 1));
            }
        }

        Query query = new Query(params);

        // [DEBUG-expert] 临时调试：打印查询参数
        System.out.println("[DEBUG-expert] params=" + params);
        // [DEBUG-expert] 检查专家绑定数据
        if (params.get("scoreSpecialistUid") != null) {
            try {
                String debugTaskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
                String debugUid = params.get("scoreSpecialistUid").toString();
                // 1. 检查 add_special_info 中专家绑定
                Map<String, Object> debugQuery = new HashMap<>();
                debugQuery.put("userId", debugUid);
                debugQuery.put("proType", "surver_pro_group");
                List<com.bootdo.cpe.domain.ExpertGroupDO> bindings = expertGroupService.list(debugQuery);
                System.out.println("[DEBUG-expert] add_special_info bindings for uid=" + debugUid + ", proType=surver_pro_group: " + (bindings != null ? bindings.size() : 0));
                if (bindings != null) {
                    for (com.bootdo.cpe.domain.ExpertGroupDO b : bindings) {
                        System.out.println("[DEBUG-expert]   binding: groupName=" + b.getGroupName() + ", taskId=" + b.getTaskId());
                    }
                }
            } catch (Exception e) {
                System.out.println("[DEBUG-expert] debug error: " + e.getMessage());
            }
        }

        List<SurverProjectInfo> proDataDtoList = surverAwardService.listProInfo(query);
        int total = surverAwardService.countProInfo(query);

        // [DEBUG-expert] 临时调试：打印结果数量
        System.out.println("[DEBUG-expert] listSize=" + proDataDtoList.size() + ", total=" + total);

        PageUtils pageUtils = new PageUtils(proDataDtoList, total);
        return pageUtils;
    }

   /**
     * 保存成果编号
     * @param proId
     * @param resultCode
     * @return
     */
    @RequestMapping("/saveCode")
    @ResponseBody
    @RequiresPermissions("cpe:surverApplyInfo:saveCode")
    public R updateProResultCode(int proId, String resultCode, String declareAccount) {
        boolean hasResultCode = StringUtils.isNotBlank(resultCode);
        boolean hasDeclareAccount = declareAccount != null;
        if(!hasResultCode && !hasDeclareAccount) {
            return R.error("请填写要保存的内容");
        }

        int rst = 0;
        if(hasResultCode) {
            rst += qcAwardService.updateProResultCode(proId, resultCode);
        }
        if(hasDeclareAccount) {
            rst += qcAwardService.updateProDeclareAccount(proId, declareAccount == null ? null : declareAccount.trim());
        }
        return  rst > 0 ? R.ok("保存成功") : R.error("保存失败");
    }

    /**
     * 获取项目申报账号
     */
    @RequestMapping("/getDeclareAccount")
    @ResponseBody
    public R getDeclareAccount(@RequestParam int proId) {
        String account = qcAwardService.getDeclareAccount(proId);
        return R.ok().put("declareAccount", account == null ? "" : account);
    }

    /**
     * 下载项目中的文件列表
     */
    @RequestMapping("/downloadProDocFiles")
    @RequiresPermissions("cpe:surverApplyInfo:downloadFiles")
    @ResponseBody
    public R downloadUpDocFiles(Integer proId, String fileType) {
        if (proId == null) {
            return R.error("选择要下载的项目");
        }
        List<String> fileTypeList = AwardSurverSubTypeEnum.getFileTypeList(fileType);
        Map<String, Object> params = new HashMap<>();
        params.put("proId", proId);
        params.put("fileTypeList", fileTypeList);
        List<FileDO> fileDOList = surverAwardService.getUploadFileList(params);

        String uploadPath = bootdoConfig.getUploadPath();

        Set<String> filePathList = new HashSet<>();
        fileDOList.stream().forEach(f -> {
            String url = f.getUrl();
            String filePath = uploadPath + "/" + url.replaceAll("/files/", "");
            filePathList.add(filePath);
        });
        String zipFileName = System.currentTimeMillis() + "_" + proId ;
        AwardEnterpriseProjectDO projectDO = awardEnterpriseProjectCommonService.get(proId);
        if(projectDO != null) {
            zipFileName = projectDO.getChengguo() + "_" + proId  + "_" + System.currentTimeMillis();
        }

        //打包数据下发
        String curDate = DateUtils.getCurDate();
        String[] dateArr = curDate.split("-");
        String userFolderPath =  dateArr[0] + "/" + dateArr[1] + "/u_" + getUserId() + "/zip";
        String storeZipFolder = uploadPath + userFolderPath ;
        File zipFolder = new File(storeZipFolder);
        if(!zipFolder.exists()) {
            zipFolder.mkdirs();
        }

        List<String> fList = new ArrayList<>();
        filePathList.stream().forEach(f->{
            fList.add(f);
        });
        try {
            ZipCompress.compress(storeZipFolder, zipFileName, fList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String zipUrl = "/files/" + userFolderPath + "/" + zipFileName + ".zip";
        R result = R.ok();
        result.put("zipUrl", zipUrl);
        return result;
    }

    /**
     * 删除项目
     */
    @RequestMapping("/remove/groupInfo")
    @ResponseBody
    @RequiresPermissions("cpe:surverDesiginMajorMaterialsUseInfo:remove")
    public R removeGroupInfo(Integer id, Integer proId) {
        if (proId == null) {
            return R.error("缺少项目ID");
        }
        int rst = awardEnterpriseProjectCommonService.remove(proId);
        return rst > 0 ? R.ok("删除成功") : R.error("删除失败");
    }

    /**
     * 勘察奖项目列表导出Excel（项目列表列 + 对应申报表字段）
     */
    @RequestMapping("/exportExcel")
    public void exportExcel(HttpServletResponse response, @RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        UserDO user = getUser();
        Long uid = getUserId();
        List<Long> roleIdList = user.getRoleIds();
        if (roleIdList.contains(ROLE_ASSOCIATION_LEADER) || roleIdList.contains(ROLE_ADMIN_ID)) {
            // 协会领导/管理员：不额外限制项目来源
        } else if (roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)) {
            params.put("associationUserId", roleIdList.contains(ROLE_SURVER_OFFLINE_VIEW_ID) ? 101 : user.getUserId());
        } else if (roleIdList.contains(ROLE_ENTERPRISE_SURVER_ID)) {
            params.put("enterpriseUid", uid);
        } else if (roleIdList.contains(ROLE_SURVER_SPECALIST_ID)) {
            params.put("scoreSpecialistUid", uid);
        } else {
            params.put("ass_assign_uid", uid);
        }
        boolean isLeaderOrAdmin = roleIdList.contains(ROLE_ASSOCIATION_LEADER) || roleIdList.contains(ROLE_ADMIN_ID);
        if (!isLeaderOrAdmin) {
            getProListParamsByRole(params);
        } else {
            params.remove("ass_assign_uid");
            params.remove("ass_worker_uid");
            params.remove("associationUserId");
            params.remove("enterpriseUid");
            params.remove("scoreSpecialistUid");
            params.remove("contactUserId");
            params.remove("createUid");
        }

        params.remove("offset");
        params.remove("limit");

        List<SurverProjectInfo> proList = new ArrayList<>();
        Object proSubTypeObj = params.get("proSubType");
        if (proSubTypeObj == null || StringUtils.isBlank(proSubTypeObj.toString())) {
            // 未指定项目类别时，固定导出四个奖项
            String[] exportSubTypes = {"contribution", "design", "software", "standard"};
            for (String subType : exportSubTypes) {
                Map<String, Object> queryParams = new HashMap<>(params);
                queryParams.put("proSubType", subType);
                List<SurverProjectInfo> subList = surverAwardService.listProInfo(queryParams);
                if (subList != null && !subList.isEmpty()) {
                    proList.addAll(subList);
                }
            }
        } else {
            proList = surverAwardService.listProInfo(params);
        }

        String[] header = SURVER_PRO_LIST_EXPORT_HEADERS;

        List<Map<String, String>> rows = new ArrayList<>();
        for (SurverProjectInfo pro : proList) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("序号", safe(pro.getId()));
            row.put("proId", safe(pro.getProId()));
            row.put("项目编号", safe(pro.getProCode()));
            row.put("项目类别", safe(pro.getProSubTypeStr()));
            row.put("项目名称", safe(pro.getProName()));
            row.put("申报单位", safe(pro.getApplyCompany()));
            row.put("专业", safe(pro.getMajor()));
            row.put("人员名单", safe(pro.getMemberList()));
            row.put("申报账号", safe(pro.getDeclareAccount()));
            row.put("申报联系方式", safe(pro.getApplyAccount()));
            row.put("分组", safe(pro.getQcGroupName()));
            row.put("形审结果", stripHtmlTagsForExcel(safe(pro.getLatestReviewResult())));
            row.put("形审评语", stripHtmlTagsForExcel(safe(pro.getLatestReviewRemarks())));
            row.put("状态", safe(pro.getApplyStat()));
            row.put("是否有查新", safe(pro.getExtSurverNovelty()));
            rows.add(row);
        }

        try {
            PoiWordUtils.downSurveyAwardExcel(header, rows, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================= 原版 exportEliminateExcel（POI 拼接「专家评审汇总.xls」，已注释保留） =========================
    // /**
    //  * 导出"专家评审汇总"Excel
    //  * 列：序号, 项目类别, 项目编号, 课题名称, 小组名称, 申报单位, 专家名称, 项目评级, 初审意见
    //  * 全部项目，每位专家一行；无专家评级记录的项目后六列留空
    //  */
    // @RequestMapping("/exportEliminateExcel")
    // public void exportEliminateExcel(HttpServletResponse response, @RequestParam Map<String, Object> params, ModelMap map) {
    //     packageAwardTaskId(map, params);
    //     String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
    //     if (taskId.isEmpty()) {
    //         try { response.getWriter().write("暂无数据导出（未找到有效任务）"); } catch (Exception ignored) {}
    //         return;
    //     }
    //     UserDO exporter = getUser();
    //     Long contactScopeUid = resolveSurverElimContactScopeUserId(exporter);
    //     List<Map<String, Object>> dataList = surverExpertEliminateService.listExpertEvalDetail(taskId, contactScopeUid);
    //     if (dataList == null || dataList.isEmpty()) {
    //         try { response.getWriter().write("暂无数据导出"); } catch (Exception ignored) {}
    //         return;
    //     }
    //     String[] header = {"序号", "项目类别", "项目编号", "课题名称", "小组名称", "申报单位", "专家名称", "项目评级", "初审意见"};
    //     Map<String, String> subTypeMap = new java.util.LinkedHashMap<>();
    //     subTypeMap.put("design",       "优秀设计奖");
    //     subTypeMap.put("software",     "计算机软件奖");
    //     subTypeMap.put("standard",     "标准设计奖");
    //     subTypeMap.put("contribution", "优秀勘察奖");
    //     subTypeMap.put("consulting",   "优秀咨询奖");
    //     try {
    //         org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
    //         org.apache.poi.hssf.usermodel.HSSFSheet sheet = workbook.createSheet("专家评审汇总");
    //         sheet.setDefaultColumnWidth(22);
    //         sheet.setColumnWidth(8, 50 * 256);
    //         org.apache.poi.hssf.usermodel.HSSFCellStyle headerStyle = workbook.createCellStyle();
    //         headerStyle.setFillForegroundColor(org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.YELLOW.getIndex());
    //         headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
    //         org.apache.poi.hssf.usermodel.HSSFRow headRow = sheet.createRow(0);
    //         for (int i = 0; i < header.length; i++) {
    //             org.apache.poi.hssf.usermodel.HSSFCell cell = headRow.createCell(i);
    //             cell.setCellValue(new org.apache.poi.hssf.usermodel.HSSFRichTextString(header[i]));
    //             cell.setCellStyle(headerStyle);
    //         }
    //         for (int i = 0; i < dataList.size(); i++) {
    //             Map<String, Object> d = dataList.get(i);
    //             String rawSubType = d.get("proSubType") != null ? d.get("proSubType").toString() : "";
    //             String subTypeName = subTypeMap.getOrDefault(rawSubType, rawSubType);
    //             String[] vals = {
    //                 String.valueOf(i + 1),
    //                 subTypeName,
    //                 safe(d.get("proCode")),
    //                 safe(d.get("topicName")),
    //                 safe(d.get("groupName")),
    //                 safe(d.get("companyName")),
    //                 safe(d.get("expertName")),
    //                 safe(d.get("grade")),
    //                 safe(d.get("remark"))
    //             };
    //             org.apache.poi.hssf.usermodel.HSSFRow dataRow = sheet.createRow(i + 1);
    //             for (int j = 0; j < vals.length; j++) {
    //                 dataRow.createCell(j).setCellValue(new org.apache.poi.hssf.usermodel.HSSFRichTextString(stripHtmlTagsForExcel(vals[j])));
    //             }
    //         }
    //         String fileName = java.net.URLEncoder.encode("专家评审汇总.xls", "UTF-8").replaceAll("\\+", "%20");
    //         response.setContentType("application/octet-stream");
    //         response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
    //         response.flushBuffer();
    //         workbook.write(response.getOutputStream());
    //         workbook.close();
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
    // ========================= 原版 exportEliminateExcel END =========================

    /**
     * 导出「确认淘汰名单」：按 classpath 模板 {@code excel/surver_eliminate_template.xlsx} 填充，
     * 按专家组分块展示（每块含标题、表头、项目行、评级说明区）。
     */
    @RequestMapping("/exportEliminateExcel")
    public void exportEliminateExcel(HttpServletResponse response, @RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (taskId.isEmpty()) {
            try {
                response.getWriter().write("暂无数据导出（未找到有效任务）");
            } catch (Exception ignored) {
            }
            return;
        }
        UserDO exporter = getUser();
        Long contactScopeUid = resolveSurverElimContactScopeUserId(exporter);
        List<Map<String, Object>> rows = surverExpertEliminateService.listEliminateExportRows(taskId, contactScopeUid);
        if (contactScopeUid != null && rows != null && !rows.isEmpty()) {
            Set<Integer> allowed = new HashSet<>(surverExpertEliminateService.listProIdsVisibleToSurverContact(taskId, contactScopeUid));
            rows = new ArrayList<>(rows);
            rows.removeIf(row -> {
                Object pid = row.get("proId");
                if (pid == null) {
                    return true;
                }
                int id = pid instanceof Number ? ((Number) pid).intValue() : Integer.parseInt(pid.toString());
                return !allowed.contains(id);
            });
        }
        try {
            SurverEliminateExcelExportUtils.exportByTemplate(response, rows);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.getWriter().write("导出失败: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 导入"确认淘汰名单"Excel
     * 通过 proID 列（B 列, index=1）识别项目，通过「淘汰状态」列（O 列, index=14；兼容旧版 K 列 index=10）更新 eliminated
     * 淘汰状态合法值：已淘汰(→1) / 未淘汰(→0)
     */
    @ResponseBody
    @RequestMapping("/importEliminateExcel")
    public R importEliminateExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                  @RequestParam Map<String, Object> params,
                                  ModelMap map) {
        if (file == null || file.isEmpty()) {
            return R.error("请选择要上传的文件");
        }
        packageAwardTaskId(map, params);
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (resolveSurverElimContactScopeUserId(getUser()) != null) {
            return R.error("小组联络人无权批量导入淘汰名单，请在页面中逐项操作");
        }

        // 批量查询 proId → proSubType 映射
        Map<Integer, String> proSubTypeMap = new HashMap<>();
        String[] exportSubTypes = {"contribution", "design", "software", "standard"};
        for (String subType : exportSubTypes) {
            Map<String, Object> qp = new HashMap<>(params);
            qp.put("proSubType", subType);
            qp.remove("offset");
            qp.remove("limit");
            List<SurverProjectInfo> sub = surverAwardService.listProInfo(qp);
            if (sub != null) {
                for (SurverProjectInfo p : sub) {
                    proSubTypeMap.put(p.getProId(), subType);
                }
            }
        }

        int successCount = 0, skipCount = 0, noChangeCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        try (java.io.InputStream is = file.getInputStream()) {
            org.apache.poi.ss.usermodel.Workbook wb;
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            if (originalName.endsWith(".xlsx")) {
                wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(is);
            } else {
                wb = new org.apache.poi.hssf.usermodel.HSSFWorkbook(is);
            }
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;
                org.apache.poi.ss.usermodel.Cell proIdCell = row.getCell(1);
                if (proIdCell == null) { skipCount++; continue; }

                String proIdStr = getCellStr(proIdCell).trim();
                if (proIdStr.isEmpty() || "proID".equalsIgnoreCase(proIdStr)) { skipCount++; continue; }
                if (proIdStr.contains("专家组名称")) { skipCount++; continue; }

                org.apache.poi.ss.usermodel.Cell statusCell = row.getCell(14);
                if (statusCell == null || getCellStr(statusCell).trim().isEmpty()) {
                    statusCell = row.getCell(10);
                }
                if (statusCell == null) { skipCount++; continue; }

                String statusStr = getCellStr(statusCell).trim();
                if (statusStr.isEmpty()) { skipCount++; continue; }

                Integer proId;
                try { proId = Integer.parseInt(proIdStr); } catch (NumberFormatException e) {
                    errors.add("第" + (r + 1) + "行 proid 非整数: " + proIdStr); continue;
                }
                if (!"已淘汰".equals(statusStr) && !"未淘汰".equals(statusStr)) {
                    errors.add("第" + (r + 1) + "行淘汰状态值非法(仅允许'已淘汰'或'未淘汰'): " + statusStr); continue;
                }

                String proSubType = proSubTypeMap.get(proId);
                if (proSubType == null) { errors.add("第" + (r + 1) + "行 proid=" + proId + " 未找到对应项目"); continue; }

                int eliminated = "已淘汰".equals(statusStr) ? 1 : 0;
                // 始终以默认值0新建记录（若已存在则不插入），确保 updateEliminatedBySubType 的
                // 返回值准确反映"状态是否真正发生变化"，避免把"新建记录但值未变"误计为更新
                surverExpertEliminateService.insertMinimalIfNotExists(proSubType, proId, 0);
                int updated  = surverExpertEliminateService.updateEliminatedBySubType(proSubType, proId, eliminated);
                if (updated > 0) successCount++; else noChangeCount++;
            }
            wb.close();
        } catch (Exception e) {
            return R.error("文件解析失败: " + e.getMessage());
        }

        int totalRows = successCount + noChangeCount + skipCount + errors.size();
        String msg = "导入完成（共 " + totalRows + " 行）：成功更新 " + successCount + " 条";
        if (noChangeCount > 0) msg += "，状态未变 " + noChangeCount + " 条";
        if (skipCount > 0) msg += "，跳过空行 " + skipCount + " 条";
        if (!errors.isEmpty()) msg += "，异常 " + errors.size() + " 条: " + String.join("; ", errors.subList(0, Math.min(3, errors.size())));
        return R.ok(msg);
    }

    /**
     * 勘察奖淘汰导出/导入：纯「小组联络人」返回其 userId 用于按 surver_view_scope 过滤导出；
     * 含协会领导/管理员/协会联系人/协会外聘时返回 null（全任务）。
     */
    private Long resolveSurverElimContactScopeUserId(UserDO user) {
        if (user == null) {
            return null;
        }
        List<Long> roleIdList = user.getRoleIds();
        if (roleIdList == null || roleIdList.isEmpty()) {
            return null;
        }
        if (!roleIdList.contains(ROLE_SURVER_GROUP_CONTACT_ID)) {
            return null;
        }
        boolean fullAccess = roleIdList.contains(ROLE_ASSOCIATION_LEADER)
                || roleIdList.contains(ROLE_ADMIN_ID)
                || roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)
                || roleIdList.contains(ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID);
        if (fullAccess) {
            return null;
        }
        return user.getUserId();
    }

    /** 读取 POI 单元格为字符串 */
    private String getCellStr(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellTypeEnum()) {
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case STRING:  return cell.getStringCellValue();
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: try { return String.valueOf((long) cell.getNumericCellValue()); } catch (Exception e) { return cell.getStringCellValue(); }
            default: return "";
        }
    }

    // ========================= 原版导出详情（已注释，保留参考） =========================
    // /**
    //  * 勘察奖项目列表导出详情（按当前分奖项导出申报表字段）- 旧版，单 Sheet 简单表头
    //  */
    // @RequestMapping("/exportDetailExcel")
    // public void exportDetailExcel(HttpServletResponse response, @RequestParam Map<String, Object> params, ModelMap map) {
    //     packageAwardTaskId(map, params);
    //     UserDO user = getUser();
    //     Long uid = getUserId();
    //     List<Long> roleIdList = user.getRoleIds();
    //     if (roleIdList.contains(ROLE_ASSOCIATION_LEADER) || roleIdList.contains(ROLE_ADMIN_ID)) {
    //         params.put("ass_assign_uid", uid);
    //     } else if (roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)) {
    //         params.put("associationUserId", roleIdList.contains(ROLE_SURVER_OFFLINE_VIEW_ID) ? 101 : user.getUserId());
    //     } else if (roleIdList.contains(ROLE_ENTERPRISE_SURVER_ID)) {
    //         params.put("enterpriseUid", uid);
    //     } else if (roleIdList.contains(ROLE_SURVER_SPECALIST_ID)) {
    //         params.put("scoreSpecialistUid", uid);
    //     } else {
    //         params.put("ass_assign_uid", uid);
    //     }
    //     getProListParamsByRole(params);
    //     params.remove("offset");
    //     params.remove("limit");
    //     String proSubType = params.get("proSubType") == null ? "" : params.get("proSubType").toString();
    //     if (StringUtils.isBlank(proSubType)) { response.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
    //     List<SurverProjectInfo> proList = surverAwardService.listProInfo(params);
    //     String[] header; List<Map<String, String>> rows = new ArrayList<>();
    //     // ... (设计/软件/标准/勘察各子分支填充 rows) ...
    //     try { PoiWordUtils.downSurveyAwardExcel(header, rows, response); } catch (Exception e) { e.printStackTrace(); }
    // }
    // ========================= 原版导出详情 END =========================

    /**
     * 新版：导出"申报项目基本信息一览表"（4 Sheet，沿用模板样式）
     * 模板文件：classpath:excel/surver_detail_template.xlsx
     * Sheet 映射：设计=design, 勘察=contribution, 标准=standard, 软件=software
     */
    @RequestMapping("/exportDetailExcel")
    public void exportDetailExcel(HttpServletResponse response, @RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        UserDO user = getUser();
        Long uid = getUserId();
        List<Long> roleIdList = user.getRoleIds();
        // 角色过滤（与原版一致）
        if (roleIdList.contains(ROLE_ASSOCIATION_LEADER) || roleIdList.contains(ROLE_ADMIN_ID)) {
            params.put("ass_assign_uid", uid);
        } else if (roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)) {
            params.put("associationUserId", roleIdList.contains(ROLE_SURVER_OFFLINE_VIEW_ID) ? 101 : user.getUserId());
        } else if (roleIdList.contains(ROLE_ENTERPRISE_SURVER_ID)) {
            params.put("enterpriseUid", uid);
        } else if (roleIdList.contains(ROLE_SURVER_SPECALIST_ID)) {
            params.put("scoreSpecialistUid", uid);
        } else {
            params.put("ass_assign_uid", uid);
        }
        getProListParamsByRole(params);
        params.remove("offset");
        params.remove("limit");

        Object taskIdObj = params.get("taskId");

        try (java.io.InputStream tplIs = getClass().getClassLoader().getResourceAsStream("excel/surver_detail_template.xlsx")) {
            if (tplIs == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(tplIs);

            // Sheet 名称 → proSubType 映射
            String[][] sheetMapping = {
                {"设计", "design"}, {"勘察", "contribution"}, {"标准", "standard"}, {"软件", "software"}
            };
            // 各 Sheet 数据起始行（0-indexed）
            // int[] dataStartRows = {5, 5, 4, 4}; // 设计/勘察=row6, 标准/软件=row5
            int[] dataStartRows = {5, 4, 4, 4}; // 勘察改为row5(index=4)，消除表头与数据间的空白行

            for (int si = 0; si < sheetMapping.length; si++) {
                String sheetName = sheetMapping[si][0];
                String subType = sheetMapping[si][1];
                int startRow = dataStartRows[si];

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = wb.getSheet(sheetName);
                if (sheet == null) continue;

                // 清除模板中的示例数据行
                for (int r = sheet.getLastRowNum(); r >= startRow; r--) {
                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                    if (row != null) sheet.removeRow(row);
                }
                // 勘察sheet模板在startRow-1(第5行)也有示例数据，只清空单元格值，不删除行（保持合并区域完整）
                // [已调整：勘察startRow改为4，清除循环已覆盖该行，无需单独处理]
                // if ("勘察".equals(sheetName)) {
                //     org.apache.poi.ss.usermodel.Row extraRow = sheet.getRow(startRow - 1);
                //     if (extraRow != null) {
                //         for (int c = extraRow.getLastCellNum() - 1; c >= 0; c--) {
                //             org.apache.poi.ss.usermodel.Cell cell = extraRow.getCell(c);
                //             if (cell != null) cell.setCellValue("");
                //         }
                //     }
                // }

                // 查询该子类型项目
                Map<String, Object> qp = new HashMap<>(params);
                qp.put("proSubType", subType);
                List<SurverProjectInfo> proList = surverAwardService.listProInfo(qp);

                for (int i = 0; i < proList.size(); i++) {
                    SurverProjectInfo pro = proList.get(i);
                    org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(startRow + i);

                    // 公共前 17 列（所有 Sheet 共用）
                    setCellVal(row, 0, String.valueOf(i + 1));        // 序号
                    setCellVal(row, 1, pro.getQcGroupName());          // 分组编号
                    setCellVal(row, 2, String.valueOf(pro.getProId())); // ID
                    setCellVal(row, 3, pro.getDeclareAccount());       // 申报账号
                    setCellVal(row, 4, pro.getProCode());              // 项目编号
                    setCellVal(row, 5, pro.getProName());              // 项目名称
                    setCellVal(row, 6, pro.getApplyCompany());         // 申报单位
                    // col 7(H): 主要设计/勘察/主编单位 — 按子类型
                    // col 8(I): 协作单位
                    // col 9(J): 申报专业
                    setCellVal(row, 10, pro.getProSubTypeStr());  // K 类别（项目类型）
                    setCellVal(row, 11, getRecommendedGrade(pro.getProId(), taskIdObj, subType)); // L 申报单位推荐等级
                    // col 14(O): 人员名单
                    setCellVal(row, 14, pro.getMemberList());

                    // 按子类型填充特有字段
                    if ("design".equals(subType)) {
                        fillDesignRow(row, pro, taskIdObj);
                    } else if ("contribution".equals(subType)) {
                        fillContributionRow(row, pro, taskIdObj);
                    } else if ("standard".equals(subType)) {
                        fillStandardRow(row, pro, taskIdObj);
                    } else if ("software".equals(subType)) {
                        fillSoftwareRow(row, pro, taskIdObj);
                    }
                }
            }

            // 输出
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = java.net.URLEncoder.encode("申报项目详细信息一览表.xlsx", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            wb.write(response.getOutputStream());
            wb.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 导出 Excel 时去掉富文本 HTML（如 &lt;p&gt;、&lt;br&gt;），避免单元格出现标签字面量。
     * 块级结束标签换为换行，便于保留段落感；再做常见 HTML 实体还原（&amp; 最后处理）。
     */
    private static String stripHtmlTagsForExcel(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String t = s;
        t = t.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        t = t.replaceAll("(?is)<style[^>]*>.*?</style>", "");
        t = t.replaceAll("(?i)<br\\s*/?>", "\n");
        t = t.replaceAll("(?i)</p\\s*>", "\n");
        t = t.replaceAll("(?i)</div\\s*>", "\n");
        t = t.replaceAll("<[^>]+>", "");
        t = t.replace('\u00A0', ' ');
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

    /** 单元格赋值工具（统一去 HTML，避免富文本字段导出带标签） */
    private void setCellVal(org.apache.poi.xssf.usermodel.XSSFRow row, int col, String val) {
        if (val == null) {
            val = "";
        }
        val = stripHtmlTagsForExcel(val);
        row.createCell(col).setCellValue(val);
    }

    // ---------- 设计 Sheet（模板含「建设单位」与「其他」间 3 列仅表头；数据列较原 68 列右移 3）----------
    private void fillDesignRow(org.apache.poi.xssf.usermodel.XSSFRow row, SurverProjectInfo pro, Object taskIdObj) {
        SurverDesignApplyTableInfoDO t = getDesignTable(pro.getProId(), taskIdObj);
        setCellVal(row, 7,  t == null ? "" : safe(t.getMainDesignCompany()));     // H 主要设计单位
        setCellVal(row, 8,  t == null ? "" : cleanMeaningless(safe(t.getCooperationUnit()))); // I 协作单位
        setCellVal(row, 9,  t == null ? safe(pro.getMajor()) : safe(t.getApplyMajor())); // J 申报专业
        setCellVal(row, 17, t == null ? "" : safe(t.getProStartEndTime()));       // R 设计起止时间
        setCellVal(row, 18, t == null ? "" : safe(t.getUseTime()));              // S 投产时间
        setCellVal(row, 19, t == null ? "" : safe(t.getAcceptanceDepartment())); // T 验收部门
        setCellVal(row, 20, t == null ? "" : safe(t.getAcceptanceTime()));       // U 验收时间
        setCellVal(row, 22, t == null ? "" : safe(t.getBuildScope()));           // W 建设规模
        setCellVal(row, 23, t == null ? "" : safe(t.getBuildArea()));            // X 建筑面积
        setCellVal(row, 24, t == null ? "" : safe(t.getDesignBudget()));         // Y 设计概预算
        setCellVal(row, 25, t == null ? "" : safe(t.getCompletionFinalAccounts())); // Z 竣工决算
        setCellVal(row, 26, t == null ? "" : safe(t.getOverestimated()));        // AA 超概算的原因
        // AB(col27) 简单介绍：只保留表头，不导出数据
        setCellVal(row, 45, t == null ? "" : cleanMeaningless(safe(t.getAwardLevel()))); // AT 曾获奖励、级别（模板 surver_detail_template「设计」sheet）
        // BE～BG(col56～58)：「建设单位」与「其他」之间新增三列表头（仅模板，无数据导出）
        // 尾部共用列（相对旧模板右移 3 列）
        // setCellVal(row, 60, getProjectDescription(pro.getProId(), taskIdObj, "design")); // BI 项目简介（原版：导出全文，已注释保留）
        setCellVal(row, 60, getProjectDescriptionWordCount(pro.getProId(), taskIdObj, "design")); // BI 项目简介（字数）
        // BJ(col61) 形审初评发现问题汇总：只保留表头，不导出数据
        // BK(col62) 形审初评意见：只保留表头，不导出数据
        // BO(col66) 申报人：只保留表头，不导出数据
        // BP(col67) 申报人电话：只保留表头，不导出数据
        setCellVal(row, 68, t == null ? "" : safe(t.getApplyConcat()));   // BQ 单位联系人
        setCellVal(row, 69, t == null ? "" : safe(t.getApplyPhone()));    // BR 单位联系人电话
    }

    // ---------- 勘察 Sheet（57 列）数据行填充 ----------
    // 模板列位: … Y=勘察面积; AM(col38)=曾获奖励级别; AU(col46)=项目简介字数
    private void fillContributionRow(org.apache.poi.xssf.usermodel.XSSFRow row, SurverProjectInfo pro, Object taskIdObj) {
        SurverBaseApplyTableInfoDO t = getContributionTable(pro.getProId(), taskIdObj);
        setCellVal(row, 7,  t == null ? "" : safe(t.getMainSurveyUnit()));     // H 主要勘察单位
        setCellVal(row, 8,  t == null ? "" : cleanMeaningless(safe(t.getCooperationUnit()))); // I 协作单位
        setCellVal(row, 9,  t == null ? safe(pro.getMajor()) : safe(t.getApplyMajor())); // J 申报专业
        setCellVal(row, 17, t == null ? "" : safe(t.getProStartEnd()));        // R 工程起止时间
        setCellVal(row, 18, t == null ? "" : safe(t.getProBuildTime()));       // S 工程建成时间
        setCellVal(row, 19, t == null ? "" : safe(t.getAcceptanceDepartment())); // T 验收部门
        setCellVal(row, 20, t == null ? "" : safe(t.getAcceptanceTime()));     // U 验收时间
        setCellVal(row, 21, t == null ? "" : safe(t.getTaskResource()));       // V 任务来源
        setCellVal(row, 22, t == null ? "" : safe(t.getPlanNumber()));         // W 计划编号
        setCellVal(row, 23, t == null ? "" : safe(t.getSurveyStartEnd()));     // X 勘察起止时间
        setCellVal(row, 24, t == null ? "" : safe(t.getSurveyAreaLength()));   // Y 勘察面积或线路长度
        // Z(col25) 简单介绍：只保留表头，不导出数据
        setCellVal(row, 38, t == null ? "" : cleanMeaningless(safe(t.getAwardReceivedLevel()))); // AM 曾获奖励、级别（「勘察」sheet）
        // 尾部共用列
        // setCellVal(row, 46, getProjectDescription(pro.getProId(), taskIdObj, "contribution")); // AU 项目简介（原版：导出全文，已注释保留）
        setCellVal(row, 46, getProjectDescriptionWordCount(pro.getProId(), taskIdObj, "contribution")); // AU 项目简介（字数）
        setCellVal(row, 47, safe(pro.getLatestReviewRemarks()));  // AV 形审初评发现问题汇总
        // AW(col48) 形审初评意见：只保留表头，不导出数据
        // BA(col52) 申报人：只保留表头，不导出数据
        // BB(col53) 申报人电话：只保留表头，不导出数据
        setCellVal(row, 54, t == null ? "" : safe(t.getContactName()));  // BC 单位联系人
        setCellVal(row, 55, t == null ? "" : safe(t.getContactPhone())); // BD 单位联系人电话
    }

    // ---------- 标准 Sheet（39 列）数据行填充 ----------
    // 模板列位: R=图集名称, S=图集号, T=设计起止时间, U=批准立项文件号, V=批准实施文件号, AA=曾获奖励级别, AC=项目简介字数
    private void fillStandardRow(org.apache.poi.xssf.usermodel.XSSFRow row, SurverProjectInfo pro, Object taskIdObj) {
        SurverStandardApplyTableInfoDO t = getStandardTable(pro.getProId(), taskIdObj);
        setCellVal(row, 7,  t == null ? "" : safe(t.getDditorChief()));        // H 主编单位
        setCellVal(row, 8,  t == null ? "" : cleanMeaningless(safe(t.getCooperationUnit()))); // I 协作单位
        setCellVal(row, 9,  t == null ? safe(pro.getMajor()) : safe(t.getApplyMajor())); // J 申报专业
        setCellVal(row, 17, t == null ? "" : safe(t.getGalleryName()));        // R 图集名称
        setCellVal(row, 18, t == null ? "" : safe(t.getAtlasNumber()));        // S 图集号
        setCellVal(row, 19, t == null ? "" : safe(t.getDesignStartEnd()));     // T 设计起止时间
        setCellVal(row, 20, t == null ? "" : safe(t.getApprovalDocumentNumber()));  // U 批准立项文件号
        setCellVal(row, 21, t == null ? "" : safe(t.getApprovedDocumentNumber())); // V 批准实施文件号
        setCellVal(row, 26, t == null ? "" : cleanMeaningless(safe(t.getAwardReceived()))); // AA 曾获奖励、级别（「标准」sheet）
        // 尾部共用列
        // setCellVal(row, 28, getProjectDescription(pro.getProId(), taskIdObj, "standard")); // AC 项目简介（原版：导出全文，已注释保留）
        setCellVal(row, 28, getProjectDescriptionWordCount(pro.getProId(), taskIdObj, "standard")); // AC 项目简介（字数）
        setCellVal(row, 29, safe(pro.getLatestReviewRemarks()));  // AD 形审初评发现问题汇总
        // AE(col30) 形审初评意见：只保留表头，不导出数据
        // AI(col34) 申报人：只保留表头，不导出数据
        // AJ(col35) 申报人电话：只保留表头，不导出数据
        setCellVal(row, 36, t == null ? "" : safe(t.getReportingContactPerson())); // AK 单位联系人
        setCellVal(row, 37, t == null ? "" : safe(t.getReportingContactPhone())); // AL 单位联系人电话
    }

    // ---------- 软件 Sheet（45 列）数据行填充 ----------
    // 模板列位: R=软件简称, S=软件类型, T=软件类别, U=国家规范, V=任务来源, W=开发起止, X=试用, Y=鉴定部门, Z=鉴定时间, AA=评测公司, AB=评测时间, AI=项目简介
    private void fillSoftwareRow(org.apache.poi.xssf.usermodel.XSSFRow row, SurverProjectInfo pro, Object taskIdObj) {
        SurverSoftApplyTableInfoDO t = getSoftTable(pro.getProId(), taskIdObj);
        setCellVal(row, 5,  t == null ? safe(pro.getProName()) : safe(t.getSoftName())); // F 软件全称
        setCellVal(row, 7,  t == null ? "" : safe(t.getEditorChief()));        // H 主编单位
        setCellVal(row, 8,  t == null ? "" : cleanMeaningless(safe(t.getCooperationUnit()))); // I 协作单位
        setCellVal(row, 9,  t == null ? safe(pro.getMajor()) : safe(t.getApplyMajor())); // J 申报专业
        setCellVal(row, 17, t == null ? "" : safe(t.getSoftShortName()));      // R 软件简称
        setCellVal(row, 18, t == null ? "" : safe(t.getSoftType()));           // S 软件类型
        setCellVal(row, 19, t == null ? "" : safe(t.getSoftCategory()));       // T 软件类别
        setCellVal(row, 20, t == null ? "" : safe(t.getSoftNationalStandard())); // U 软件符合哪类现行国家规范
        setCellVal(row, 21, t == null ? "" : safe(t.getSoftTaskSource()));     // V 任务来源
        setCellVal(row, 22, t == null ? "" : safe(t.getSoftStartEnd()));       // W 开发起止时间
        setCellVal(row, 23, t == null ? "" : safe(t.getSoftTrialTime()));      // X 试用时间
        setCellVal(row, 24, t == null ? "" : safe(t.getIdentificationDepartment())); // Y 鉴定部门
        setCellVal(row, 25, t == null ? "" : safe(t.getIdentificationTime()));  // Z 鉴定时间
        setCellVal(row, 26, t == null ? "" : safe(t.getEvaluationCompany()));   // AA 评测公司
        setCellVal(row, 27, t == null ? "" : safe(t.getEvaluationTime()));      // AB 评测时间
        // 尾部共用列
        // setCellVal(row, 34, getProjectDescription(pro.getProId(), taskIdObj, "software")); // AI 项目简介（原版：导出全文，已注释保留）
        setCellVal(row, 34, getProjectDescriptionWordCount(pro.getProId(), taskIdObj, "software")); // AI 项目简介（字数）
        // AJ(col35) 形审初评发现问题汇总：只保留表头，不导出数据
        // AK(col36) 形审初评意见：只保留表头，不导出数据
        // AO(col40) 申报人：只保留表头，不导出数据
        // AP(col41) 申报人电话：只保留表头，不导出数据
        setCellVal(row, 42, t == null ? "" : safe(t.getContactName()));  // AQ 单位联系人
        setCellVal(row, 43, t == null ? "" : safe(t.getContactPhone())); // AR 单位联系人电话
    }

    private SurverDesignApplyTableInfoDO getDesignTable(Integer proId, Object taskId) {
        Map<String, Object> p = new HashMap<>();
        p.put("proId", proId);
        p.put("taskId", taskId);
        List<SurverDesignApplyTableInfoDO> list = surverDesignApplyTableInfoService.list(p);
        if (list == null || list.isEmpty()) {
            p.remove("taskId");
            list = surverDesignApplyTableInfoService.list(p);
        }
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private SurverSoftApplyTableInfoDO getSoftTable(Integer proId, Object taskId) {
        Map<String, Object> p = new HashMap<>();
        p.put("proId", proId);
        p.put("taskId", taskId);
        List<SurverSoftApplyTableInfoDO> list = surverSoftApplyTableInfoService.list(p);
        if (list == null || list.isEmpty()) {
            p.remove("taskId");
            list = surverSoftApplyTableInfoService.list(p);
        }
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private SurverStandardApplyTableInfoDO getStandardTable(Integer proId, Object taskId) {
        Map<String, Object> p = new HashMap<>();
        p.put("proId", proId);
        p.put("taskId", taskId);
        List<SurverStandardApplyTableInfoDO> list = surverStandardApplyTableInfoService.list(p);
        if (list == null || list.isEmpty()) {
            p.remove("taskId");
            list = surverStandardApplyTableInfoService.list(p);
        }
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private SurverExcellentApplyTableInfoDO getExcellentTable(Integer proId, Object taskId) {
        Map<String, Object> p = new HashMap<>();
        p.put("proId", proId);
        p.put("taskId", taskId);
        List<SurverExcellentApplyTableInfoDO> list = surverExcellentApplyTableInfoService.list(p);
        if (list == null || list.isEmpty()) {
            p.remove("taskId");
            list = surverExcellentApplyTableInfoService.list(p);
        }
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private SurverBaseApplyTableInfoDO getContributionTable(Integer proId, Object taskId) {
        Map<String, Object> p = new HashMap<>();
        p.put("proId", proId);
        p.put("taskId", taskId);
        List<SurverBaseApplyTableInfoDO> list = surverBaseApplyTableInfoService.list(p);
        if (list == null || list.isEmpty()) {
            p.remove("taskId");
            list = surverBaseApplyTableInfoService.list(p);
        }
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /** 从"项目简介表"获取申报单位推荐等级 */
    private String getRecommendedGrade(Integer proId, Object taskId, String subType) {
        Map<String, Object> p = new HashMap<>();
        p.put("proId", proId);
        p.put("taskId", taskId);
        try {
            switch (subType) {
                case "design": {
                    List<SurverDesignApplyProjectProfileDO> list = surverDesignApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverDesignApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getRecommendedGrade()) : "";
                }
                case "contribution": {
                    List<SurverExcellentApplyProjectProfileDO> list = surverExcellentApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverExcellentApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getRecommendedGrade()) : "";
                }
                case "standard": {
                    List<SurverStandardApplyProjectProfileDO> list = surverStandardApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverStandardApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getRecommendedGrade()) : "";
                }
                case "software": {
                    List<SurverSoftApplyProjectProfileDO> list = surverSoftApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverSoftApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getRecommendedGrade()) : "";
                }
                default: return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 项目简介「字数」统计（业务口径）：
     * <ul>
     *   <li>中文汉字、中文标点：每 1 个字符计 1 字</li>
     *   <li>英文、数字、英文标点：连续一段（仅 ASCII 可打印字符 33–126，不含空格）整体计 1 字</li>
     *   <li>空格、换行、制表符及 Unicode 空白：不计入</li>
     *   <li>其余非空白字符（如俄文、emoji）：每码点计 1 字</li>
     * </ul>
     */
    private int countZiForProjectDescription(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int charCount = Character.charCount(cp);
            if (isZiCountWhitespace(cp)) {
                i += charCount;
                continue;
            }
            if (Character.isIdeographic(cp)) {
                count++;
                i += charCount;
                continue;
            }
            if (isChinesePunctuationForZiCount(cp)) {
                count++;
                i += charCount;
                continue;
            }
            if (isAsciiEnglishRunCodePoint(cp)) {
                i += charCount;
                while (i < s.length()) {
                    int cp2 = s.codePointAt(i);
                    int cc2 = Character.charCount(cp2);
                    if (isZiCountWhitespace(cp2) || Character.isIdeographic(cp2)
                            || isChinesePunctuationForZiCount(cp2) || !isAsciiEnglishRunCodePoint(cp2)) {
                        break;
                    }
                    i += cc2;
                }
                count++;
                continue;
            }
            count++;
            i += charCount;
        }
        return count;
    }

    /** 不计入字数的空白（含空格、换行、制表符及常见 Unicode 空白） */
    private static boolean isZiCountWhitespace(int cp) {
        if (cp == ' ' || cp == '\t' || cp == '\n' || cp == '\r' || cp == '\f' || cp == '\u000b') {
            return true;
        }
        int t = Character.getType(cp);
        return t == Character.SPACE_SEPARATOR
                || t == Character.LINE_SEPARATOR
                || t == Character.PARAGRAPH_SEPARATOR;
    }

    /** 中文标点区（不含 U+3000 表意空格，该空格按空白处理） */
    private static boolean isChinesePunctuationForZiCount(int cp) {
        if (cp == 0x3000) {
            return false;
        }
        Character.UnicodeBlock b = Character.UnicodeBlock.of(cp);
        if (b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) {
            return true;
        }
        if (b == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS) {
            return true;
        }
        if (b == Character.UnicodeBlock.VERTICAL_FORMS) {
            return true;
        }
        if (b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            // 全角字母、全角数字按「西文连续串」或单字处理，不归入中文标点块
            if ((cp >= 0xFF21 && cp <= 0xFF3A) || (cp >= 0xFF41 && cp <= 0xFF5A) || (cp >= 0xFF10 && cp <= 0xFF19)) {
                return false;
            }
            return cp >= 0xFF01 && cp <= 0xFF5E;
        }
        return false;
    }

    /** 英文/数字/英文标点连续串中的字符：可打印 ASCII（33–126），不含空格 */
    private static boolean isAsciiEnglishRunCodePoint(int cp) {
        return cp > 32 && cp < 127;
    }

    /** 从"项目简介表"获取项目简介字数（按业务「汉字/中文标点 + 英文连续串」规则） */
    private String getProjectDescriptionWordCount(Integer proId, Object taskId, String subType) {
        String content = stripHtmlTagsForExcel(getProjectDescription(proId, taskId, subType));
        if (content == null || content.isEmpty()) {
            return "0字";
        }
        int count = countZiForProjectDescription(content);
        return count + "字";
    }

    /** 从"项目简介表"获取项目简介内容 */
    private String getProjectDescription(Integer proId, Object taskId, String subType) {
        Map<String, Object> p = new HashMap<>();
        p.put("proId", proId);
        p.put("taskId", taskId);
        try {
            switch (subType) {
                case "design": {
                    List<SurverDesignApplyProjectProfileDO> list = surverDesignApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverDesignApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getProjectDescription()) : "";
                }
                case "contribution": {
                    List<SurverExcellentApplyProjectProfileDO> list = surverExcellentApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverExcellentApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getProjectDescription()) : "";
                }
                case "standard": {
                    List<SurverStandardApplyProjectProfileDO> list = surverStandardApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverStandardApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getProjectDescription()) : "";
                }
                case "software": {
                    List<SurverSoftApplyProjectProfileDO> list = surverSoftApplyProjectProfileService.list(p);
                    if ((list == null || list.isEmpty()) && taskId != null) { p.remove("taskId"); list = surverSoftApplyProjectProfileService.list(p); }
                    return list != null && !list.isEmpty() ? safe(list.get(0).getProjectDescription()) : "";
                }
                default: return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String safe(Object val) {
        return val == null ? "" : val.toString();
    }

    /**
     * 清理无意义值："无"、"暂无"、"没有"、纯符号等 → 返回空串
     */
    private String cleanMeaningless(String val) {
        if (val == null) return "";
        val = val.trim();
        // 完全匹配常见无意义词
        if ("无".equals(val) || "暂无".equals(val) || "没有".equals(val) || "无。".equals(val)
                || "null".equalsIgnoreCase(val) || "N/A".equalsIgnoreCase(val)) {
            return "";
        }
        // 纯符号（只含 -/—_.\ 、空格等），视为无意义
        if (val.matches("^[\\-/—_.\\\\ \\s、，,。]+$")) {
            return "";
        }
        return val;
    }
}
