package com.bootdo.cpe.controller.surver;

import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.common.domain.FileDO;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.*;
import com.bootdo.cpe.service.*;
import com.bootdo.cpe.utils.AwardSurverSubTypeEnum;
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
        } else if (roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)) {
            //todo 临时使用协会联系人的用户id
            params.put("associationUserId", roleIdList.contains(ROLE_SURVER_OFFLINE_VIEW_ID) ? 101 : user.getUserId());
        } else if (roleIdList.contains(ROLE_ENTERPRISE_SURVER_ID)) {
            //企业用户查看自己创建项目
            params.put("enterpriseUid", uid);
        } else if(roleIdList.contains(ROLE_SURVER_SPECALIST_ID)) {
            //评审专家
            params.put("scoreSpecialistUid", uid);
        // 新增：勘察奖小组联络人(86) 仅看绑定分组下的项目
        } else if (roleIdList.contains(ROLE_SURVER_GROUP_CONTACT_ID)) {
            params.put("contactUserId", uid);
        } else {
            //分派给自己的项目
            params.put("ass_assign_uid", uid);
        }
        getProListParamsByRole(params);

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

        List<SurverProjectInfo> proDataDtoList = surverAwardService.listProInfo(query);
        int total = surverAwardService.countProInfo(query);
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
        if (roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)) {
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

        String[] header = {
                "序号", "proId", "项目编号", "项目类别", "项目名称", "申报单位", "专业", "人员名单", "申报账号", "申报联系方式", "分组", "形审结果", "形审评语", "状态"
        };

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
            row.put("形审结果", safe(pro.getLatestReviewResult()));
            row.put("形审评语", safe(pro.getLatestReviewRemarks()));
            row.put("状态", safe(pro.getApplyStat()));
            rows.add(row);
        }

        try {
            PoiWordUtils.downSurveyAwardExcel(header, rows, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 勘察奖项目列表导出详情（按当前分奖项导出申报表字段）
     */
    @RequestMapping("/exportDetailExcel")
    public void exportDetailExcel(HttpServletResponse response, @RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        UserDO user = getUser();
        Long uid = getUserId();
        List<Long> roleIdList = user.getRoleIds();
        if (roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)) {
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

        String proSubType = params.get("proSubType") == null ? "" : params.get("proSubType").toString();
        if (StringUtils.isBlank(proSubType)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        List<SurverProjectInfo> proList = surverAwardService.listProInfo(params);
        String[] header;
        List<Map<String, String>> rows = new ArrayList<>();

        if ("design".equals(proSubType)) {
            header = new String[]{"序号", "项目编号", "项目名称", "申报单位", "申报专业", "工程起止时间", "投产时间", "验收部门", "验收时间", "建筑规模", "建筑面积", "设计概(预)算", "竣工决算", "超概算", "主要设计单位", "协作单位", "联系人", "电话/手机", "传真"};
            for (SurverProjectInfo pro : proList) {
                SurverDesignApplyTableInfoDO t = getDesignTable(pro.getProId(), params.get("taskId"));
                Map<String, String> row = new LinkedHashMap<>();
                row.put("序号", safe(pro.getId()));
                row.put("项目编号", safe(pro.getProCode()));
                row.put("项目名称", safe(t == null ? pro.getProName() : t.getProName()));
                row.put("申报单位", safe(pro.getApplyCompany()));
                row.put("申报专业", safe(t == null ? "" : t.getApplyMajor()));
                row.put("工程起止时间", safe(t == null ? "" : t.getProStartEndTime()));
                row.put("投产时间", safe(t == null ? "" : t.getUseTime()));
                row.put("验收部门", safe(t == null ? "" : t.getAcceptanceDepartment()));
                row.put("验收时间", safe(t == null ? "" : t.getAcceptanceTime()));
                row.put("建筑规模", safe(t == null ? "" : t.getBuildScope()));
                row.put("建筑面积", safe(t == null ? "" : t.getBuildArea()));
                row.put("设计概(预)算", safe(t == null ? "" : t.getDesignBudget()));
                row.put("竣工决算", safe(t == null ? "" : t.getCompletionFinalAccounts()));
                row.put("超概算", safe(t == null ? "" : t.getOverestimated()));
                row.put("主要设计单位", safe(t == null ? "" : t.getMainDesignCompany()));
                row.put("协作单位", safe(t == null ? "" : t.getCooperationUnit()));
                row.put("联系人", safe(t == null ? "" : t.getApplyConcat()));
                row.put("电话/手机", safe(t == null ? "" : t.getApplyPhone()));
                row.put("传真", safe(t == null ? "" : t.getFax()));
                rows.add(row);
            }
        } else if ("software".equals(proSubType)) {
            header = new String[]{"序号", "项目编号", "软件全称", "软件简称", "申报专业", "软件类型", "软件类别", "任务来源", "开发起止时间", "试用时间", "鉴定部门", "鉴定时间", "评测公司", "评测时间", "主编单位", "协作单位", "联系人", "电话/手机", "传真"};
            for (SurverProjectInfo pro : proList) {
                SurverSoftApplyTableInfoDO t = getSoftTable(pro.getProId(), params.get("taskId"));
                Map<String, String> row = new LinkedHashMap<>();
                row.put("序号", safe(pro.getId()));
                row.put("项目编号", safe(pro.getProCode()));
                row.put("软件全称", safe(t == null ? pro.getProName() : t.getSoftName()));
                row.put("软件简称", safe(t == null ? "" : t.getSoftShortName()));
                row.put("申报专业", safe(t == null ? "" : t.getApplyMajor()));
                row.put("软件类型", safe(t == null ? "" : t.getSoftType()));
                row.put("软件类别", safe(t == null ? "" : t.getSoftCategory()));
                row.put("任务来源", safe(t == null ? "" : t.getSoftTaskSource()));
                row.put("开发起止时间", safe(t == null ? "" : t.getSoftStartEnd()));
                row.put("试用时间", safe(t == null ? "" : t.getSoftTrialTime()));
                row.put("鉴定部门", safe(t == null ? "" : t.getIdentificationDepartment()));
                row.put("鉴定时间", safe(t == null ? "" : t.getIdentificationTime()));
                row.put("评测公司", safe(t == null ? "" : t.getEvaluationCompany()));
                row.put("评测时间", safe(t == null ? "" : t.getEvaluationTime()));
                row.put("主编单位", safe(t == null ? "" : t.getEditorChief()));
                row.put("协作单位", safe(t == null ? "" : t.getCooperationUnit()));
                row.put("联系人", safe(t == null ? "" : t.getContactName()));
                row.put("电话/手机", safe(t == null ? "" : t.getContactPhone()));
                row.put("传真", safe(t == null ? "" : t.getFax()));
                rows.add(row);
            }
        } else if ("standard".equals(proSubType)) {
            header = new String[]{"序号", "项目编号", "图集名称", "图集号", "申报专业", "设计起止时间", "批准立项文件号", "批准实施文件号", "主编单位", "协作单位", "申报单位联系人", "电话/手机", "传真"};
            for (SurverProjectInfo pro : proList) {
                SurverStandardApplyTableInfoDO t = getStandardTable(pro.getProId(), params.get("taskId"));
                Map<String, String> row = new LinkedHashMap<>();
                row.put("序号", safe(pro.getId()));
                row.put("项目编号", safe(pro.getProCode()));
                row.put("图集名称", safe(t == null ? pro.getProName() : t.getGalleryName()));
                row.put("图集号", safe(t == null ? "" : t.getAtlasNumber()));
                row.put("申报专业", safe(t == null ? "" : t.getApplyMajor()));
                row.put("设计起止时间", safe(t == null ? "" : t.getDesignStartEnd()));
                row.put("批准立项文件号", safe(t == null ? "" : t.getApprovalDocumentNumber()));
                row.put("批准实施文件号", safe(t == null ? "" : t.getApprovedDocumentNumber()));
                row.put("主编单位", safe(t == null ? "" : t.getDditorChief()));
                row.put("协作单位", safe(t == null ? "" : t.getCooperationUnit()));
                row.put("申报单位联系人", safe(t == null ? "" : t.getReportingContactPerson()));
                row.put("电话/手机", safe(t == null ? "" : t.getReportingContactPhone()));
                row.put("传真", safe(t == null ? "" : t.getFax()));
                rows.add(row);
            }
        } else if ("contribution".equals(proSubType)) {
            header = new String[]{"序号", "项目编号", "项目名称", "申报专业", "工程建设时间", "工程起止时间", "验收时间", "验收部门", "计划编号", "勘察起止时间", "勘察面积/长度", "主要勘察单位", "协作单位", "通讯地址", "邮政编码", "联系人", "联系电话", "传真"};
            for (SurverProjectInfo pro : proList) {
                SurverBaseApplyTableInfoDO t = getContributionTable(pro.getProId(), params.get("taskId"));
                Map<String, String> row = new LinkedHashMap<>();
                row.put("序号", safe(pro.getId()));
                row.put("项目编号", safe(pro.getProCode()));
                row.put("项目名称", safe(t == null ? pro.getProName() : t.getProName()));
                row.put("申报专业", safe(t == null ? "" : t.getApplyMajor()));
                row.put("工程建设时间", safe(t == null ? "" : t.getProBuildTime()));
                row.put("工程起止时间", safe(t == null ? "" : t.getProStartEnd()));
                row.put("验收时间", safe(t == null ? "" : t.getAcceptanceTime()));
                row.put("验收部门", safe(t == null ? "" : t.getAcceptanceDepartment()));
                row.put("计划编号", safe(t == null ? "" : t.getPlanNumber()));
                row.put("勘察起止时间", safe(t == null ? "" : t.getSurveyStartEnd()));
                row.put("勘察面积/长度", safe(t == null ? "" : t.getSurveyAreaLength()));
                row.put("主要勘察单位", safe(t == null ? "" : t.getMainSurveyUnit()));
                row.put("协作单位", safe(t == null ? "" : t.getCooperationUnit()));
                row.put("通讯地址", safe(t == null ? "" : t.getMailingAddress()));
                row.put("邮政编码", safe(t == null ? "" : t.getPostCode()));
                row.put("联系人", safe(t == null ? "" : t.getContactName()));
                row.put("联系电话", safe(t == null ? "" : t.getContactPhone()));
                row.put("传真", safe(t == null ? "" : t.getContactFox()));
                rows.add(row);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            PoiWordUtils.downSurveyAwardExcel(header, rows, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private String safe(Object val) {
        return val == null ? "" : val.toString();
    }
}
