package com.bootdo.cpe.controller.surver;

import com.bootdo.activiti.domain.AssignProjectDataDo;
import com.bootdo.activiti.domain.PublishAwardTaskDo;
import com.bootdo.activiti.service.AwardEnterpriseProjectService;
import com.bootdo.activiti.service.AwardPublishTaskService;
import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.common.utils.R;
import com.bootdo.common.utils.StringUtils;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.ExpertGroupDO;
import com.bootdo.cpe.domain.QcReviewResultRecordDO;
import com.bootdo.cpe.domain.science_process.ScienceAssignCountInfo;
import com.bootdo.cpe.domain.science_process.ScienceAssignExternalProInfo;
import com.bootdo.cpe.dto.QcProDataDto;
import com.bootdo.cpe.service.ExpertGroupService;
import com.bootdo.cpe.service.QcAwardService;
import com.bootdo.cpe.service.QcReviewResultRecordService;
import com.bootdo.cpe.service.SurverReviewDesignResultService;
import com.bootdo.cpe.service.SurverReviewSoftResultService;
import com.bootdo.cpe.service.SurverReviewStandardResultService;
import com.bootdo.cpe.service.SurverReviewConsultResultService;
import com.bootdo.cpe.service.SurverReviewSurverResultService;
import com.bootdo.system.domain.UserDO;
import com.bootdo.system.service.UserService;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bootdo.common.config.Constant.ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID;

/**
 * surver奖任务
 *
 * @author houzb
 * @version 1.0
 * @date 2022-03-30 21:15
 */
@RequestMapping("/cpe/suverProcess")
@Controller
public class SurverProcessController extends BaseSurverController {
    private String prefix = "cpe/survey";
    @Autowired
    private UserService userService;
    @Autowired
    private AwardEnterpriseProjectService awardEnterpriseProjectService;
    @Autowired
    private AwardPublishTaskService awardPublishTaskService;
    @Autowired
    private QcAwardService qcAwardService;
    @Autowired
    private QcReviewResultRecordService qcReviewResultRecordService;
    @Autowired
    private ExpertGroupService expertGroupService;
    @Autowired
    private SurverReviewDesignResultService surverReviewDesignResultService;
    @Autowired
    private SurverReviewSoftResultService surverReviewSoftResultService;
    @Autowired
    private SurverReviewStandardResultService surverReviewStandardResultService;
    @Autowired
    private SurverReviewConsultResultService surverReviewConsultResultService;
    @Autowired
    private SurverReviewSurverResultService surverReviewSurverResultService;


       /**
     * 撤回提交审核项目
     *
     * @param proId
     * @return
     */
    @RequestMapping("/cancelCheck")
    @ResponseBody
    // @RequiresPermissions(value = {"cpe:surverApplyInfo:cancelReview", "cpe:surverApplyInfo:review"}, logical = Logical.OR)
    public R cancelCheckPro(Integer proId) {
        if (proId != null && proId > 0 && qcAwardService.updateProApply(proId) > 0) {
            return R.ok();
        }
        return R.error();
    }

     /**
     * 形式审查
     * @return
     */
    @RequestMapping("/toReivew")
    @RequiresPermissions("cpe:surverApplyInfo:review")
    public String toReviewPro(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Map<String, Object> proInfoParams = new HashMap<>();
        proInfoParams.put("id", params.get("groupInfoId"));
        List<QcProDataDto> qcProDataDtoList = qcAwardService.listProInfo(proInfoParams);
        map.put("qcProData", qcProDataDtoList.size() > 0 ? qcProDataDtoList.get(0) : new QcProDataDto());

        //获取最近一次审核信息
        Map<String, Object> reviewParams = new HashMap<>();
        reviewParams.put("proId", params.get("proId"));
        reviewParams.put("sort", "id");
        reviewParams.put("order", " desc");
        reviewParams.put("offset", 0);
        reviewParams.put("limit", 1);
        List<QcReviewResultRecordDO> reviewResultRecordDOList = qcReviewResultRecordService.list(reviewParams);
        QcReviewResultRecordDO reviewResultRecordDO = reviewResultRecordDOList.size() > 0 ? reviewResultRecordDOList.get(0) : new QcReviewResultRecordDO();
        map.put("reviewResult", reviewResultRecordDO);
        return prefix + "/check/qc_check_template";
    }


    /**
     * 添加专家账号
     * @return
     */
    @RequestMapping("/toAddSpecialist")
    @RequiresPermissions("surveraward:specialist:select")
    public String toAddSpecialistUser(@RequestParam Map<String, Object> params, ModelMap map) {
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

        return prefix + "/specialist/specialist_major_group";
    }


    //--------------------------------------分割线-------------------------


    /**
     * 去分配项目给工作人员
     *
     * @return
     */
    @RequiresPermissions("asso:task:assign")
    @RequestMapping("/toAssign")
    public String toAssignPro(@RequestParam Map<String, Object> params, ModelMap map) {
        long roleId = ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID;
        packageAwardTaskId(map, params);
        params.put("roleId", roleId);

        ScienceAssignCountInfo countInfo = awardEnterpriseProjectService.getAssignCountInfo(params);
        int validateCount = countInfo.getValidateCount();
        if(validateCount == 0) {
            //已分派完
            map.put("tipMsg", "还没有人提交申请审核！可督促申请提交");
            return "enterprise/tip_msg";
        }
        String taskId = (String) params.get("taskId");
        PublishAwardTaskDo taskDo = awardPublishTaskService.get(taskId);
        taskDo.initStat();
        boolean isAssgin = taskDo.getIsAssign();
        if(!isAssgin) {
            //分派时间已截止
            map.put("tipMsg", "分外外聘人员时间已结束,如需调整联系协会任务发布人员");
            return "enterprise/tip_msg";
        }

        //如果是分派阶段则可查询进行分派
        Map<String, Object> param = new HashMap<>();
        param.put("roleId", roleId);
        List<UserDO> assWorkers = userService.list(param);
        map.put("assWorkers", assWorkers);
        long exUid = 0L;
        for(UserDO aw:assWorkers){
            List<Long> roleIds = aw.getRoleIds();
            if(roleIds.contains(roleId)) {
                map.put("firstWorkerName", aw.getName());
                exUid = aw.getUserId();
                map.put("extUserId", exUid);
                break;
            }
        }
        return prefix + "/check/assign_review_task";
    }

    /**
     * 提交审核项目
     *
     * @param proId
     * @return
     */
    @RequestMapping("/subCheck")
    @ResponseBody
    public R subCheckPro(Integer proId) {
        if (proId != null && proId > 0 && qcAwardService.updateProCheck(proId) > 0) {
            return R.ok();
        }
        return R.error();
    }



    /**
     * 选择分派外聘人员的项目信息
     * @param params
     * @param map
     * @return
     */
    @RequestMapping("/queryAssignPro")
    @ResponseBody
    public R queryAssignProList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        String asWorkerName = (String) params.get("asWorkerName");
        String qcGroupName = (String) params.get("qcGroupName");
        // 处理"未分组"的特殊标识
        if ("__NO_GROUP__".equals(qcGroupName)) {
            params.put("qcGroupName", "");
            params.put("noGroup", true);
        }

        Map<String,Object> userParamMap = new HashMap<>();
        userParamMap.put("username", asWorkerName);
        List<UserDO> userList = userService.list(userParamMap);
        long finalExUid = userList.size() > 0 ? userList.get(0).getUserId() : 0;

        List<ScienceAssignExternalProInfo> list = awardEnterpriseProjectService.getAssignExtProList(params);
        List<ScienceAssignExternalProInfo> noAssignList = new ArrayList<>();
        List<ScienceAssignExternalProInfo> assignList = new ArrayList<>();
        AtomicInteger noAsAuto = new AtomicInteger(1);
        AtomicInteger asAuto = new AtomicInteger(1);
        list.stream().forEach(as->{
            long curExtUid = as.getExtUserId();
            if(curExtUid == 0) {
                as.setSerNum(noAsAuto.getAndIncrement());
                noAssignList.add(as);
            }else if(finalExUid == curExtUid){
                as.setSerNum(asAuto.getAndIncrement());
                assignList.add(as);
            }
        });

        Map<String,Object> result = new HashMap<>();
        result.put("noAssignList", noAssignList);
        result.put("assignList", assignList);
        return R.ok(result);
    }

    @RequestMapping("/getSurverGroups")
    @ResponseBody
    public R getSurverGroups(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        // 分组管理写入的是 qc_group_name（同时兼容旧字段 pro_group_name）
        params.put("proType", EnumProjectType.SURVER_PRO.getProType());
        List<String> groupList = awardEnterpriseProjectService.getProGroupList(params);
        return R.ok().put("list", groupList);
    }


    @RequestMapping("/assignPro")
    @ResponseBody
    public R assignPro(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        String taskId = (String) params.get("taskId");
        //{"publishTaskId":"359110170ab640269a846230a0ea98b2vno2WA","proIds":"2,6","assWorkers":"10,7","assWorker":"10"}
        long assignUid = getUserId();
        String asWorkerName = params.get("asWorkerName").toString();
        if (StringUtils.isBlank(asWorkerName)) {
            return R.error(1, "请选择工作人员");
        }
        String[] workerNameArr = asWorkerName.split(",");
        List<Long> workerUidList = userService.getUidsByLoginUserNames(workerNameArr);
        String proIds = params.get("proIds") == null ? "" : params.get("proIds").toString();
        String awardType = params.get("awardType").toString();

        // 先清空该外聘人员在当前任务+奖项下的历史分派（支持“取消分配”）
        awardEnterpriseProjectService.removeByExtUid(workerUidList, taskId, awardType);

        // 右侧为空：表示清空
        if (StringUtils.isBlank(proIds)) {
            return R.ok("分派已更新（已清空）");
        }

        String[] proIdArr = proIds.split(",");
        List<AssignProjectDataDo> assignProjectDataDoList = new ArrayList<>();
        for (long wuid : workerUidList) {
            for (String proId : proIdArr) {
                if (StringUtils.isNotBlank(proId)) {
                    long pid = Long.parseLong(proId.trim());
                    AssignProjectDataDo assignData = new AssignProjectDataDo(assignUid, wuid, pid);
                    assignProjectDataDoList.add(assignData);
                }
            }
        }

        if (assignProjectDataDoList.isEmpty()) {
            return R.ok("分派已更新");
        }

        awardEnterpriseProjectService.assignPro(assignProjectDataDoList);

        return R.ok("分派已更新");
    }

    /**
     * 勘察奖形审记录（同QC）
     */
    @RequestMapping("/list/reviewRecords")
    @ResponseBody
    public R listReviewRecords(Integer proId, String proSubType) {
        if (proId == null) {
            return R.error("缺少项目ID");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("proId", proId);
        params.put("sort", "id");
        params.put("order", "desc");
        params.put("offset", 0);
        params.put("limit", 200);

        List<?> records;
        if ("design".equals(proSubType)) {
            records = surverReviewDesignResultService.list(params);
        } else if ("software".equals(proSubType)) {
            records = surverReviewSoftResultService.list(params);
        } else if ("standard".equals(proSubType)) {
            records = surverReviewStandardResultService.list(params);
        } else if ("consulting".equals(proSubType)) {
            records = surverReviewConsultResultService.list(params);
        } else if ("contribution".equals(proSubType)) {
            records = surverReviewSurverResultService.list(params);
        } else {
            // 未传子类时，兼容汇总全部子类
            List<Object> all = new ArrayList<>();
            all.addAll((List<?>) surverReviewDesignResultService.list(params));
            all.addAll((List<?>) surverReviewSoftResultService.list(params));
            all.addAll((List<?>) surverReviewStandardResultService.list(params));
            all.addAll((List<?>) surverReviewConsultResultService.list(params));
            all.addAll((List<?>) surverReviewSurverResultService.list(params));
            records = all;
        }

        return R.ok().put("data", records == null ? Collections.emptyList() : records);
    }

}
