package com.bootdo.cpe.controller.surver;

import com.bootdo.activiti.domain.AssignProjectDataDo;
import com.bootdo.activiti.domain.PublishAwardTaskDo;
import com.bootdo.activiti.service.AwardEnterpriseProjectService;
import com.bootdo.activiti.service.AwardPublishTaskService;
// 原：勘察奖专业组管理复用 QC 分组（ass_qc_group）作为专业组主数据
// import com.bootdo.activiti.service.QcGroupService;
// import com.bootdo.activiti.domain.QcGroupDO;
// 新：改为使用用户前期新增的"专家分组管理" (ass_award_expert_group) 作为主数据
import com.bootdo.activiti.service.QcGroupService;          // 仍保留引用以兼容其它旧逻辑
import com.bootdo.activiti.domain.QcGroupDO;                // 仍保留引用以兼容其它旧逻辑
import com.bootdo.activiti.service.AwardExpertGroupService;
import com.bootdo.activiti.domain.AwardExpertGroupDO;
import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.ExpertGroupDO;
import com.bootdo.cpe.domain.QcReviewResultRecordDO;
import com.bootdo.cpe.domain.SurverProjectInfo;
import com.bootdo.cpe.domain.SurverReviewProBaseInfo;
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
// 新增：用于"小组联络人绑定"按钮可见性 & 该角色仅看到绑定专业组
import static com.bootdo.common.config.Constant.ROLE_ASSOCIATION_LEADER;
import static com.bootdo.common.config.Constant.ROLE_SURVER_ASSOCIATION_ID;
import static com.bootdo.common.config.Constant.ROLE_SURVER_GROUP_CONTACT_ID;
import static com.bootdo.common.config.Constant.ROLE_ADMIN_ID;
// 新增：用于管理员（70角色）签章/工作单位列可见性控制（参考 QC 奖 isQcAssociationContactRole70）
import static com.bootdo.common.config.Constant.ROLE_QC_ASSOCIATION_ID;
// 新增：签章上传所需依赖
import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.domain.FileDO;
import com.bootdo.common.service.FileService;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashSet;
import java.util.Set;

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
    // 原代码：勘察奖专业组管理依赖 QcGroupService（与 QC 共用 ass_qc_group 表）
    // @Autowired
    // private QcGroupService qcGroupService;
    // 新代码：保留 QcGroupService 注入以兼容，但专业组列表来源改为 AwardExpertGroupService
    @Autowired
    private QcGroupService qcGroupService;
    @Autowired
    private AwardExpertGroupService awardExpertGroupService;

    // 新增：签章上传依赖（参考 QcProcessController）
    @Autowired
    private FileService fileService;
    @Autowired
    private BootdoConfig bootdoConfig;

    /**
     * 从专家分组绑定反查真实 taskId，避免前端页面 taskId 与管理员侧不一致。
     * 逻辑与 SurverProController.getSurverProList 中 ROLE_SURVER_SPECALIST_ID 分支一致。
     * @param uid 当前登录用户 ID
     * @param fallbackTaskId 前端传入的 taskId，找不到绑定时回退使用
     * @return 真实 taskId
     */
    private String resolveExpertTaskId(Long uid, String fallbackTaskId) {
        Map<String, Object> bindQuery = new HashMap<>();
        bindQuery.put("userId", String.valueOf(uid));
        bindQuery.put("proType", "surver_pro_group");
        List<ExpertGroupDO> expertBindings = expertGroupService.list(bindQuery);
        if (expertBindings != null && !expertBindings.isEmpty()) {
            String bindTaskId = expertBindings.get(0).getTaskId();
            if (StringUtils.isNotBlank(bindTaskId)) {
                return bindTaskId;
            }
        }
        return fallbackTaskId;
    }

    // Phase B 新增：勘察奖"淘汰管理"弹窗后端服务
    @Autowired
    private com.bootdo.cpe.service.SurverExpertEliminateService surverExpertEliminateService;
    // Phase C 新增：专家侧确认提交快照 + 回避
    @Autowired
    private com.bootdo.cpe.service.SurverExpertEliminateConfirmedService surverExpertEliminateConfirmedService;
    @Autowired
    private com.bootdo.cpe.service.SurverExpertAvoidanceService surverExpertAvoidanceService;
    // 管理员回避管理：查询专家分配的项目列表
    @Autowired
    private com.bootdo.cpe.service.SurverAwardService surverAwardService;
    /** 专家打分页：审核意见 / 主评意见（独立表） */
    @Autowired
    private com.bootdo.cpe.service.SurverExpertReviewOpinionService surverExpertReviewOpinionService;


    /**
     * 勘察奖小组联络人(86)任务选择页面
     * 展示该用户所有绑定了 surver_view_scope 的任务列表，每个任务可点击进入专业组管理
     */
    @RequestMapping("/toSurverContactTaskList")
    public String toSurverContactTaskList(ModelMap map) {
        UserDO user = getUser();
        String userId = String.valueOf(user.getUserId());

        // 获取该用户绑定的所有不重复 taskId
        List<String> taskIds = expertGroupService.getDistinctTaskIdsByUserAndProType(userId, "surver_view_scope");

        // 组装任务信息列表
        List<Map<String, Object>> taskList = new java.util.ArrayList<>();
        if (taskIds != null) {
            for (String taskId : taskIds) {
                com.bootdo.activiti.domain.PublishAwardTaskDo task = awardPublishTaskService.get(taskId);
                if (task == null) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("taskId", taskId);
                item.put("taskName", task.getTaskName() != null ? task.getTaskName() : "未命名任务");
                item.put("awardName", task.getAwardName() != null ? task.getAwardName() : "");
                item.put("applyStartDate", task.getApplyStartDate());
                item.put("applyEndDate", task.getApplyEndDate());
                // 获取该任务下用户绑定的分组名列表
                Map<String, Object> bindingQuery = new HashMap<>();
                bindingQuery.put("taskId", taskId);
                bindingQuery.put("userId", userId);
                bindingQuery.put("proType", "surver_view_scope");
                List<ExpertGroupDO> bindings = expertGroupService.list(bindingQuery);
                List<String> groupNames = new java.util.ArrayList<>();
                for (ExpertGroupDO b : bindings) {
                    if (b.getGroupName() != null && !b.getGroupName().trim().isEmpty()) {
                        groupNames.add(b.getGroupName().trim());
                    }
                }
                item.put("groupNames", groupNames);
                taskList.add(item);
            }
        }

        map.put("taskList", taskList);
        map.put("userName", user.getName() != null ? user.getName() : user.getUsername());
        return prefix + "/score/surver_contact_task_list";
    }

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
        // 解析并写入当前勘察奖任务ID：请求无 taskId 时取最新勘察任务；
        // 同时补全 proType 等查询条件（见 BaseSurverController.packageAwardTaskId）
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

        // 填充形审人员姓名
        if (records != null) {
            for (Object obj : records) {
                if (obj instanceof SurverReviewProBaseInfo) {
                    SurverReviewProBaseInfo base = (SurverReviewProBaseInfo) obj;
                    try {
                        java.lang.reflect.Method getOptUid = obj.getClass().getMethod("getOptUid");
                        Object uidObj = getOptUid.invoke(obj);
                        if (uidObj instanceof Integer && (Integer) uidObj > 0) {
                            UserDO u = userService.get((long) (int) uidObj);
                            if (u != null) {
                                String displayName = u.getUsername() != null ? u.getUsername() : u.getName();
                                base.setReviewerName(displayName != null ? displayName : String.valueOf(uidObj));
                            } else {
                                base.setReviewerName(String.valueOf(uidObj));
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("[DEBUG-reviewerName] error: " + e.getMessage() + ", class=" + obj.getClass().getName());
                    }
                }
            }
        }
        return R.ok().put("data", records == null ? Collections.emptyList() : records);
    }

    // ==========================================================================
    // 新增：勘察奖"专业组管理"页面（参考 QC 奖 major_group_admin.html）
    // 路由前缀：/cpe/suverProcess/toSurverMajorGroupAdmin、/cpe/suverProcess/surver_major_group/*
    // 与现有 /cpe/suverProcess/toAddSpecialist 不冲突
    // 数据：
    //   - 专业组主数据复用 ass_qc_group（按 taskId 维度），与勘察奖现有"分组管理"共用
    //   - 专家绑定：ExpertGroupService（add_special_info），proType = "surver_pro_group"
    // ==========================================================================
    private static final String SURVER_PRO_GROUP_TYPE = "surver_pro_group";

    /**
     * 跳转到勘察奖专业组管理页面
     */
    @RequestMapping("/toSurverMajorGroupAdmin")
    public String toSurverMajorGroupAdmin(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        String taskId = params.get("taskId") == null ? "" : params.get("taskId").toString();
        // 原代码：复用 ass_qc_group 作为勘察奖专业组主数据（已注释，与用户"专家分组管理"不是同一数据源）
        // List<QcGroupDO> groupList = qcGroupService.getGroupsByTaskId(taskId);
        // map.put("qcGroupList", groupList == null ? new ArrayList<QcGroupDO>() : groupList);
        // 新代码：使用"专家分组管理"(ass_award_expert_group) 作为主数据源
        List<AwardExpertGroupDO> groupList = awardExpertGroupService.getGroupsByTaskId(taskId);
        map.put("qcGroupList", groupList == null ? new ArrayList<AwardExpertGroupDO>() : groupList);

        // 当前任务下已绑定的勘察奖专业组专家
        Map<String, Object> selParams = new HashMap<>();
        selParams.put("taskId", taskId);
        selParams.put("proType", SURVER_PRO_GROUP_TYPE);
        if (params.get("major") != null) {
            selParams.put("groupName", params.get("major"));
        }
        List<ExpertGroupDO> selList = expertGroupService.list(selParams);
        // 原代码：直接放入 selInfoList，未做角色级别可见性过滤
        // map.put("selInfoList", selList == null ? new ArrayList<ExpertGroupDO>() : selList);
        // 新代码：按角色处理可见性 & 提供"小组联络人绑定"按钮可见性标志（参考 QC 形审专家绑定）
        UserDO currentUser = getUser();
        List<Long> currentRoleIds = currentUser == null || currentUser.getRoleIds() == null
                ? new ArrayList<Long>() : currentUser.getRoleIds();

        boolean canManageContactBinding = currentRoleIds.contains(ROLE_ASSOCIATION_LEADER)
                || currentRoleIds.contains(ROLE_SURVER_ASSOCIATION_ID);
        map.put("canManageContactBinding", canManageContactBinding);
        // 标记当前用户是否是"勘察奖小组联络人"（前端用于隐藏"重新分组"和"形审专家绑定"按钮）
        boolean isSurverGroupContactRole = currentRoleIds.contains(ROLE_SURVER_GROUP_CONTACT_ID);
        map.put("isSurverGroupContactRole", isSurverGroupContactRole);
        // 新增：标记当前用户是否为QC奖协会联系人（70角色），控制签章/工作单位列可见性（参考 QC 奖 isQcAssociationContactRole70）
        boolean isQcAssociationContactRole70 = currentRoleIds.contains(ROLE_QC_ASSOCIATION_ID);
        map.put("isQcAssociationContactRole70", isQcAssociationContactRole70);

        // 勘察奖小组联络人：按 surver_view_scope 绑定记录过滤可见专业组；无绑定则显示空数据
        if (isSurverGroupContactRole) {
            Map<String, Object> bindingQuery = new HashMap<>();
            bindingQuery.put("taskId", taskId);
            bindingQuery.put("userId", String.valueOf(getUserId()));
            bindingQuery.put("proType", "surver_view_scope");
            List<ExpertGroupDO> bindings = expertGroupService.list(bindingQuery);
            Set<String> allowedGroups = new HashSet<>();
            for (ExpertGroupDO b : bindings) {
                if (b.getGroupName() != null) {
                    allowedGroups.add(b.getGroupName().trim());
                }
            }
            // 过滤左侧专业组列表
            if (groupList != null) {
                groupList = groupList.stream()
                        .filter(g -> g.getName() != null && allowedGroups.contains(g.getName().trim()))
                        .collect(java.util.stream.Collectors.toList());
                map.put("qcGroupList", groupList);
            }
            // 过滤右侧专家列表
            if (selList != null) {
                selList = selList.stream()
                        .filter(e -> e.getGroupName() != null && allowedGroups.contains(e.getGroupName().trim()))
                        .collect(java.util.stream.Collectors.toList());
            }
        }
        map.put("selInfoList", selList == null ? new ArrayList<ExpertGroupDO>() : selList);

        map.put("proType", SURVER_PRO_GROUP_TYPE);
        map.put("major", params.get("major") == null ? "" : params.get("major").toString());
        return prefix + "/score/major_group_admin";
    }

    // ==========================================================================
    // 新增：勘察奖专家"签章上传"（参考 /qcProcess/uploadExpertSign 与 /toUploadExpertSign）
    // ==========================================================================

    /**
     * 跳转到签章上传页面（layer 弹窗 iframe 调用）
     */
    @RequestMapping("/toUploadExpertSign")
    public String toSurverUploadExpertSignPage(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Object loginAccountObj = params.get("loginAccount");
        if (loginAccountObj != null) {
            List<Long> list = userService.getUidByLoginUserName(loginAccountObj.toString());
            if (!list.isEmpty()) {
                map.put("expertUid", list.get(0));
            }
        }
        map.put("loginAccount", params.get("loginAccount"));
        map.put("trIndex", params.get("trIndex"));
        return prefix + "/score/surver_expert_sign_upload";
    }

    /**
     * 接收签章图片上传：保存图片到磁盘 + sys_file 表 + 更新 add_special_info.signature_id
     */
    @ResponseBody
    @RequestMapping("/uploadExpertSign")
    public R uploadSurverExpertSign(String taskId, Long expertUid,
                                    @RequestPart("file[]") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return R.error("请选择要上传的签章图片");
        }
        if (expertUid == null) {
            return R.error("专家ID不能为空");
        }
        MultipartFile file = files[0];
        String originalName = file.getOriginalFilename();
        if (originalName == null) originalName = "sign.png";
        String curDate = DateUtils.getCurDate();
        String[] dateArr = curDate.split("-");
        String folder = dateArr[0] + "/" + dateArr[1] + "/sign_" + expertUid + "/";
        String uploadPath = bootdoConfig.getUploadPath() + folder;
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String fileName = "sign_" + System.currentTimeMillis() + RandomStringUtils.randomAlphanumeric(4) + ext;
        String fileUrl = "/files/" + folder + fileName;
        try {
            FileUtil.uploadFile(file.getBytes(), uploadPath, fileName);
        } catch (Exception e) {
            return R.error("文件保存失败：" + e.getMessage());
        }
        FileDO fileDO = new FileDO(FileType.fileType(fileName), fileUrl, new java.util.Date());
        fileService.save(fileDO);
        expertGroupService.updateExpertSignId(fileDO.getId(), taskId, expertUid);
        return R.ok().put("fileUrl", fileUrl);
    }

    // ==========================================================================
    // 新增：勘察奖"小组联络人"分组绑定（参考 QcProcessController 形审专家绑定）
    //   - proType = "surver_view_scope"
    //   - userId  = sys_user.user_id
    //   - groupName = 专家组名称（来自 ass_award_expert_group）
    // ==========================================================================

    /**
     * 获取所有"勘察奖小组联络人"用户列表 + 当前任务下的绑定专业组
     */
    @ResponseBody
    @RequestMapping("/getSurverContactGroupBindings")
    public R getSurverContactGroupBindings(@RequestParam String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        // 1) 拉取该角色下的全部用户
        Map<String, Object> userQuery = new HashMap<>();
        userQuery.put("roleId", String.valueOf(ROLE_SURVER_GROUP_CONTACT_ID));
        List<UserDO> contactUsers = userService.list(userQuery);

        // 2) 查询任务下所有 surver_view_scope 绑定
        Map<String, Object> bindingQuery = new HashMap<>();
        bindingQuery.put("taskId", taskId);
        bindingQuery.put("proType", "surver_view_scope");
        List<ExpertGroupDO> allBindings = expertGroupService.list(bindingQuery);

        Map<String, List<String>> bindingMap = new HashMap<>();
        for (ExpertGroupDO b : allBindings) {
            String uid = b.getUserId();
            if (uid == null) continue;
            bindingMap.computeIfAbsent(uid, k -> new ArrayList<>()).add(b.getGroupName());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserDO u : contactUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getUserId());
            item.put("username", u.getUsername());
            item.put("name", u.getName());
            List<String> boundGroups = bindingMap.getOrDefault(String.valueOf(u.getUserId()), new ArrayList<>());
            item.put("boundGroups", boundGroups);
            result.add(item);
        }

        // 3) 返回当前任务下最新的分组列表（从 ass_award_expert_group 实时读取，不依赖页面 DOM 快照）
        List<AwardExpertGroupDO> latestGroups = awardExpertGroupService.getGroupsByTaskId(taskId);
        List<String> groupNames = new ArrayList<>();
        if (latestGroups != null) {
            for (AwardExpertGroupDO g : latestGroups) {
                if (g.getName() != null && !g.getName().trim().isEmpty()) {
                    groupNames.add(g.getName().trim());
                }
            }
        }

        return R.ok().put("data", result).put("groups", groupNames);
    }

    /**
     * 保存某个小组联络人在当前任务下可见的专业组列表（先清旧再写新，每个组一条）
     */
    @ResponseBody
    @RequestMapping("/saveSurverContactGroupBinding")
    public R saveSurverContactGroupBinding(@RequestParam String taskId,
                                           @RequestParam String expertUserId,
                                           @RequestParam(value = "groupNames", required = false, defaultValue = "") String groupNames) {
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(expertUserId)) {
            return R.error("参数不完整");
        }
        expertGroupService.deleteByUserIdAndTaskIdAndProType(expertUserId, taskId, "surver_view_scope");
        if (StringUtils.isNotBlank(groupNames)) {
            String[] groups = groupNames.split(",");
            for (String groupName : groups) {
                groupName = groupName.trim();
                if (groupName.isEmpty()) continue;
                ExpertGroupDO binding = new ExpertGroupDO();
                binding.setUserId(expertUserId);
                binding.setTaskId(taskId);
                binding.setGroupName(groupName);
                binding.setProType("surver_view_scope");
                expertGroupService.directSave(binding);
            }
        }
        return R.ok("绑定保存成功");
    }

    /**
     * 列表查询：某任务/某专业组下已绑定的专家
     */
    @ResponseBody
    @RequestMapping("/surver_major_group/expert/list")
    public R listSurverMajorGroupExperts(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") == null ? "" : params.get("taskId").toString();
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务 ID 不能为空");
        }
        Map<String, Object> q = new HashMap<>();
        q.put("taskId", taskId);
        q.put("proType", SURVER_PRO_GROUP_TYPE);
        if (params.get("groupName") != null) {
            q.put("groupName", params.get("groupName").toString());
        }
        List<ExpertGroupDO> list = expertGroupService.list(q);

        // 新增：勘察奖小组联络人(86) 仅能看到已绑定分组的专家
        UserDO currentUser = getUser();
        List<Long> currentRoleIds = currentUser == null || currentUser.getRoleIds() == null
                ? new ArrayList<Long>() : currentUser.getRoleIds();
        if (currentRoleIds.contains(ROLE_SURVER_GROUP_CONTACT_ID)) {
            Map<String, Object> bindingQuery = new HashMap<>();
            bindingQuery.put("taskId", taskId);
            bindingQuery.put("userId", String.valueOf(getUserId()));
            bindingQuery.put("proType", "surver_view_scope");
            List<ExpertGroupDO> bindings = expertGroupService.list(bindingQuery);
            Set<String> allowedGroups = new java.util.HashSet<>();
            for (ExpertGroupDO b : bindings) {
                if (b.getGroupName() != null) {
                    allowedGroups.add(b.getGroupName().trim());
                }
            }
            if (list != null) {
                list = list.stream()
                        .filter(e -> e.getGroupName() != null && allowedGroups.contains(e.getGroupName().trim()))
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        return R.ok().put("data", list == null ? Collections.emptyList() : list);
    }

    /**
     * 保存（新增/更新）勘察奖专业组专家绑定
     */
    @ResponseBody
    @RequestMapping("/surver_major_group/expert/save")
    public R saveSurverMajorGroupExpert(ExpertGroupDO expert) {
        if (expert == null
                || StringUtils.isBlank(expert.getTaskId())
                || StringUtils.isBlank(expert.getGroupName())
                || StringUtils.isBlank(expert.getLoginAccount())) {
            return R.error("参数不完整：taskId/groupName/loginAccount 必填");
        }
        // 强制 proType，避免与其它业务串数据
        expert.setProType(SURVER_PRO_GROUP_TYPE);

        // 补充 userId：前端只传 loginAccount，需在后端查出数字 userId
        // add_special_info.user_id 用于专家登录后的项目列表 JOIN（scoreSpecialistUid = userId）
        if (StringUtils.isBlank(expert.getUserId())) {
            List<Long> uids = userService.getUidByLoginUserName(expert.getLoginAccount());
            if (uids != null && !uids.isEmpty()) {
                expert.setUserId(String.valueOf(uids.get(0)));
            }
        }

        // 同任务+同分组+同登录账号已存在则视为更新
        Map<String, Object> q = new HashMap<>();
        q.put("taskId", expert.getTaskId());
        q.put("proType", SURVER_PRO_GROUP_TYPE);
        q.put("groupName", expert.getGroupName());
        q.put("loginAccount", expert.getLoginAccount());
        List<ExpertGroupDO> exist = expertGroupService.list(q);
        if (exist != null && !exist.isEmpty()) {
            ExpertGroupDO old = exist.get(0);
            expert.setId(old.getId());
            if (expertGroupService.update(expert) > 0) {
                return R.ok();
            }
            return R.error("更新失败");
        }
        if (expertGroupService.directSave(expert) > 0) {
            return R.ok();
        }
        return R.error("保存失败");
    }

    /**
     * 移除勘察奖专业组下某个专家绑定（按 id 或 taskId+groupName+loginAccount）
     */
    @ResponseBody
    @RequestMapping("/surver_major_group/expert/remove")
    public R removeSurverMajorGroupExpert(@RequestParam Map<String, Object> params) {
        Object idObj = params.get("id");
        if (idObj != null && StringUtils.isNotBlank(idObj.toString())) {
            try {
                Integer id = Integer.valueOf(idObj.toString());
                if (expertGroupService.remove(id) > 0) {
                    return R.ok();
                }
                return R.error("删除失败");
            } catch (NumberFormatException e) {
                return R.error("id 参数非法");
            }
        }
        // 兜底：按 taskId+groupName+loginAccount 删除（取查到的第一条）
        String taskId = params.get("taskId") == null ? "" : params.get("taskId").toString();
        String groupName = params.get("groupName") == null ? "" : params.get("groupName").toString();
        String loginAccount = params.get("loginAccount") == null ? "" : params.get("loginAccount").toString();
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(loginAccount)) {
            return R.error("缺少必要参数");
        }
        Map<String, Object> q = new HashMap<>();
        q.put("taskId", taskId);
        q.put("proType", SURVER_PRO_GROUP_TYPE);
        if (StringUtils.isNotBlank(groupName)) {
            q.put("groupName", groupName);
        }
        q.put("loginAccount", loginAccount);
        List<ExpertGroupDO> exist = expertGroupService.list(q);
        if (exist == null || exist.isEmpty()) {
            return R.error("未找到对应记录");
        }
        if (expertGroupService.remove(exist.get(0).getId()) > 0) {
            return R.ok();
        }
        return R.error("删除失败");
    }

    /**
     * 获取勘察奖某任务下的所有专业组（前端下拉选项 / 左侧分组列表的 AJAX 入口）
     */
    @ResponseBody
    @RequestMapping("/surver_major_group/group/list")
    public R listSurverMajorGroups(@RequestParam("taskId") String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务 ID 不能为空");
        }
        // 原代码：从 ass_qc_group 读取（与"专家分组管理"不是同一表）
        // List<QcGroupDO> list = qcGroupService.getGroupsByTaskId(taskId);
        // return R.ok().put("data", list == null ? Collections.emptyList() : list);
        // 新代码：从 ass_award_expert_group（"专家分组管理"）读取
        List<AwardExpertGroupDO> list = awardExpertGroupService.getGroupsByTaskId(taskId);
        return R.ok().put("data", list == null ? Collections.emptyList() : list);
    }

    // ==========================================================================
    // 新增：勘察奖"专业组-淘汰"相关接口
    // 目前实现最小可用：以 add_special_info.eliminate_over 字段作为
    //   "该专家本轮淘汰名单是否已提交" 的状态标记（0=未提交，1=已提交）
    //   与 QC 奖 eliminate_over 语义一致，互相独立（通过 pro_type=surver_pro_group 隔离）
    // 后续若需 per-project 淘汰明细，可追加独立表（建议 ass_award_expert_eliminate）
    // ==========================================================================

    /**
     * 切换/设置某专家的"淘汰提交"状态（0/1）
     *   - 参数 id（ExpertGroupDO 主键）或 taskId+groupName+loginAccount 二选一
     *   - 参数 eliminateOver 省略时 → 自动翻转
     */
    @ResponseBody
    @RequestMapping("/surver_major_group/expert/toggleEliminateOver")
    public R toggleEliminateOver(@RequestParam Map<String, Object> params) {
        ExpertGroupDO target = null;
        Object idObj = params.get("id");
        if (idObj != null && StringUtils.isNotBlank(idObj.toString())) {
            try {
                target = expertGroupService.get(Integer.valueOf(idObj.toString()));
            } catch (NumberFormatException e) {
                return R.error("id 参数非法");
            }
        } else {
            String taskId = params.get("taskId") == null ? "" : params.get("taskId").toString();
            String groupName = params.get("groupName") == null ? "" : params.get("groupName").toString();
            String loginAccount = params.get("loginAccount") == null ? "" : params.get("loginAccount").toString();
            if (StringUtils.isBlank(taskId) || StringUtils.isBlank(loginAccount)) {
                return R.error("缺少必要参数（id 或 taskId+loginAccount）");
            }
            Map<String, Object> q = new HashMap<>();
            q.put("taskId", taskId);
            q.put("proType", SURVER_PRO_GROUP_TYPE);
            if (StringUtils.isNotBlank(groupName)) {
                q.put("groupName", groupName);
            }
            q.put("loginAccount", loginAccount);
            List<ExpertGroupDO> exist = expertGroupService.list(q);
            if (exist != null && !exist.isEmpty()) {
                target = exist.get(0);
            }
        }
        if (target == null) {
            return R.error("未找到对应的专家绑定记录");
        }

        Integer next;
        if (params.get("eliminateOver") != null
                && StringUtils.isNotBlank(params.get("eliminateOver").toString())) {
            try {
                next = Integer.valueOf(params.get("eliminateOver").toString());
                if (next != 0 && next != 1) {
                    return R.error("eliminateOver 只允许 0/1");
                }
            } catch (NumberFormatException e) {
                return R.error("eliminateOver 非法");
            }
        } else {
            Integer cur = target.getEliminateOver() == null ? 0 : target.getEliminateOver();
            next = (cur == 1) ? 0 : 1;
        }

        ExpertGroupDO updater = new ExpertGroupDO();
        updater.setId(target.getId());
        updater.setEliminateOver(next);
        if (expertGroupService.update(updater) > 0) {
            return R.ok().put("eliminateOver", next);
        }
        return R.error("更新淘汰状态失败");
    }

    /**
     * 批量重置某任务下所有"勘察奖专业组"专家的淘汰状态为 0
     *   - 与 QC 的"更新分组（重置淘汰）"语义类似，但只影响 pro_type=surver_pro_group 的记录
     */
    @ResponseBody
    @RequestMapping("/surver_major_group/expert/resetEliminateOver")
    public R resetEliminateOver(@RequestParam("taskId") String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务 ID 不能为空");
        }
        Map<String, Object> q = new HashMap<>();
        q.put("taskId", taskId);
        q.put("proType", SURVER_PRO_GROUP_TYPE);
        List<ExpertGroupDO> exist = expertGroupService.list(q);
        if (exist == null || exist.isEmpty()) {
            return R.ok().put("msg", "当前任务下没有勘察奖专业组专家可重置").put("count", 0);
        }
        int ok = 0;
        for (ExpertGroupDO e : exist) {
            if (e.getEliminateOver() != null && e.getEliminateOver() == 1) {
                ExpertGroupDO updater = new ExpertGroupDO();
                updater.setId(e.getId());
                updater.setEliminateOver(0);
                ok += expertGroupService.update(updater);
            }
        }
        return R.ok().put("count", ok);
    }

    // ============================================================
    // Phase B - 管理员"淘汰管理"接口（勘察奖 awardId=2）
    //   存储模型:
    //   - 专家侧: 评级 A/B/C/D 落 ass_surver_expert_eliminate (+确认快照表)
    //   - 管理员侧: 淘汰状态 0/1 落 4 张申报子表的 eliminated 字段
    //   权限: 仅协会管理员可写; 候选汇总和已确认列表对管理员可见
    // ============================================================

    /**
     * 候选淘汰池 - 按 (taskId[, proSubType]) 聚合专家评级 + 当前 eliminated 状态
     * 入参: taskId(必填), proSubType(选填, 不传=全部4类汇总)
     * 出参: { code, list:[{proId, proSubType, proCode, topicName, companyName, groupName,
     *                       gradeA, gradeB, gradeC, gradeD, gradeEmpty, totalRows,
     *                       expertGrades:"张三:A|李四:D", eliminated}] }
     */
    @ResponseBody
    @RequestMapping("/eliminate/listCandidates")
    public R listEliminateCandidates(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proSubType = params.get("proSubType") != null ? params.get("proSubType").toString() : null;
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        try {
            UserDO viewer = getUser();
            Long contactScopeUid = resolveSurverElimContactScopeUserId(viewer);
            List<Map<String, Object>> list = surverExpertEliminateService.aggregateCandidates(taskId, proSubType, contactScopeUid);
            if (list == null) {
                list = Collections.emptyList();
            } else if (contactScopeUid != null) {
                // 与导出 listExpertEvalDetail 一致：按「联络人可见项目 id」再过滤一遍，避免聚合 SQL 与主表 JOIN 边界导致漏过滤
                list = new ArrayList<>(list);
                Set<Integer> allowed = new HashSet<>(surverExpertEliminateService.listProIdsVisibleToSurverContact(taskId, contactScopeUid));
                list.removeIf(row -> {
                    Object pid = row.get("proId");
                    if (pid == null) {
                        return true;
                    }
                    int id = pid instanceof Number ? ((Number) pid).intValue() : Integer.parseInt(pid.toString());
                    return !allowed.contains(id);
                });
            }
            return R.ok().put("list", list);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 已确认淘汰列表 - 4 张申报子表 eliminated=1 的项目
     * 入参: taskId(必填)
     * 出参: { code, list:[{proSubType, proId, proCode, topicName, companyName, groupName, major}] }
     */
    @ResponseBody
    @RequestMapping("/eliminate/listConfirmed")
    public R listConfirmedEliminate(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        try {
            List<Map<String, Object>> list = surverExpertEliminateService.listConfirmedEliminated(taskId);
            return R.ok().put("list", list == null ? Collections.emptyList() : list);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 设置/取消单个项目的"淘汰状态"
     * 入参: proSubType(必填: design/software/standard/contribution),
     *       proId(必填), eliminated(必填: 0|1)
     * 校验: 仅协会管理员可调用; consulting 不在 Phase A 改造范围, 拒绝写入。
     */
    @ResponseBody
    @RequestMapping("/eliminate/setEliminated")
    public R setEliminated(@RequestParam Map<String, Object> params) {
        // 权限校验: 协会管理员 / 协会领导
        UserDO user = getUser();
        List<Long> roleIdList = user != null ? user.getRoleIds() : Collections.emptyList();
        boolean isLeader = roleIdList.contains(ROLE_ASSOCIATION_LEADER)
                || roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)
                || roleIdList.contains(ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID)
                || roleIdList.contains(ROLE_SURVER_GROUP_CONTACT_ID);
        if (!isLeader) {
            return R.error("无权操作: 仅协会管理员/外聘人员/小组联络人可设置淘汰状态");
        }
        String proSubType = params.get("proSubType") != null ? params.get("proSubType").toString() : "";
        String proIdStr   = params.get("proId")      != null ? params.get("proId").toString()      : "";
        String eliminatedStr = params.get("eliminated") != null ? params.get("eliminated").toString() : "";
        if (StringUtils.isBlank(proSubType) || StringUtils.isBlank(proIdStr) || StringUtils.isBlank(eliminatedStr)) {
            return R.error("缺少必填参数 proSubType / proId / eliminated");
        }
        Set<String> allowedSubTypes = new HashSet<>();
        Collections.addAll(allowedSubTypes, "design", "software", "standard", "contribution");
        if (!allowedSubTypes.contains(proSubType)) {
            return R.error("当前奖项 (" + proSubType + ") 暂不支持淘汰管理");
        }
        Integer proId, eliminated;
        try {
            proId = Integer.valueOf(proIdStr);
            eliminated = Integer.valueOf(eliminatedStr);
        } catch (NumberFormatException ex) {
            return R.error("proId / eliminated 必须是整数");
        }
        if (eliminated != 0 && eliminated != 1) {
            return R.error("eliminated 仅允许 0 或 1");
        }
        Long contactScopeUid = resolveSurverElimContactScopeUserId(user);
        if (contactScopeUid != null) {
            if (surverExpertEliminateService.countProInSurverContactScope(proId, contactScopeUid) <= 0) {
                return R.error("无权操作：该项目不在您绑定的专家组范围内");
            }
        }
        // 先确保子表存在记录（以默认值0插入，保证后续 UPDATE 的返回值能准确反映是否发生变化）
        surverExpertEliminateService.insertMinimalIfNotExists(proSubType, proId, 0);
        // 再更新 eliminated 字段（SQL 含 AND eliminated != #{eliminated}，值相同时 rows=0 也属正常）
        surverExpertEliminateService.updateEliminatedBySubType(proSubType, proId, eliminated);
        return R.ok(eliminated == 1 ? "已确认淘汰" : "已取消淘汰");
    }

    /**
     * 按 proCode 反查项目 - 用于"导入淘汰名单 Excel" 时前端按申报编号定位 proId/proSubType
     * 入参: taskId, proCode
     * 出参: { code, data:{proId, proCode, proSubType} 或 null }
     */
    @ResponseBody
    @RequestMapping("/eliminate/findByProCode")
    public R findEliminateByProCode(@RequestParam Map<String, Object> params) {
        String taskId  = params.get("taskId")  != null ? params.get("taskId").toString()  : "";
        String proCode = params.get("proCode") != null ? params.get("proCode").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proCode)) {
            return R.error("缺少必填参数 taskId / proCode");
        }
        Map<String, Object> info = surverExpertEliminateService.findProInfoByProCode(taskId, proCode.trim());
        return R.ok().put("data", info);
    }

    // ============================================================
    // Phase C - 专家侧"淘汰评级"接口
    //   存储:
    //   - 等级 A/B/C/D 落 ass_surver_expert_eliminate (活动表, 可改)
    //   - 回避   落 ass_surver_expert_avoidance
    //   - 确认提交 → 拷贝 active 行至 ass_surver_expert_eliminate_confirmed (冻结)
    //   - 锁定判定: 当前 (expert, task) 在快照表存在记录 → 已提交
    // ============================================================

    /**
     * 拉取当前专家在某 task 下"我已经评了的等级 + 回避情况"
     * 入参: taskId(必填)
     * 出参: { code, eliminateOver(0|1=已确认提交), gradedCount, avoidedCount,
     *         grades:{ proId: 'A|B|C|D' }, avoidances:[proId, ...] }
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/listMyGrades")
    public R listMyGrades(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(taskId)) return R.error("任务ID不能为空");
        taskId = resolveExpertTaskId(uid, taskId);

        // 1) 是否已确认提交（看快照表是否有当前专家+任务的记录）
        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", uid);
        snapQ.put("taskId", taskId);
        int snapCnt = surverExpertEliminateConfirmedService.count(snapQ);
        int eliminateOver = snapCnt > 0 ? 1 : 0;

        // 2) 当前活动表中我已评的等级 (proId → grade)
        Map<String, Object> activeQ = new HashMap<>();
        activeQ.put("expertUid", uid);
        activeQ.put("taskId", taskId);
        activeQ.put("deleted", 0);
        List<com.bootdo.cpe.domain.SurverExpertEliminateDO> myGrades = surverExpertEliminateService.list(activeQ);
        Map<Integer, String> gradeMap = new HashMap<>();
        Map<Integer, String> remarkMap = new HashMap<>();
        for (com.bootdo.cpe.domain.SurverExpertEliminateDO g : myGrades) {
            if (g.getProId() != null) {
                gradeMap.put(g.getProId(), g.getGrade());
                if (g.getRemark() != null) remarkMap.put(g.getProId(), g.getRemark());
            }
        }

        // 3) 当前我已回避的项目集合
        List<Integer> avoidedProIds = surverExpertAvoidanceService.getAvoidedProIds(taskId, uid.intValue());

        // 4) 该专家在该任务下通过专家分组可见的项目总数
        int totalAssigned = surverExpertEliminateService.countAssignedProjects(taskId, uid);

        return R.ok()
                .put("eliminateOver", eliminateOver)
                .put("gradedCount", myGrades.size())
                .put("avoidedCount", avoidedProIds == null ? 0 : avoidedProIds.size())
                .put("totalCount", totalAssigned)
                .put("grades", gradeMap)
                .put("remarks", remarkMap)
                .put("avoidances", avoidedProIds == null ? Collections.emptyList() : avoidedProIds);
    }

    /**
     * 专家侧：当前任务下本人已保存的「专家审核意见 + 主评意见」
     * 入参: taskId(必填)
     * 出参: auditOpinions{proId→agree|disagree}, mainReviewTexts{proId→text}, mainReviewSubmitted{proId→0|1}
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/review/listMy")
    public R listMyReviewOpinions(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        taskId = resolveExpertTaskId(uid, taskId);
        List<com.bootdo.cpe.domain.SurverExpertReviewOpinionDO> rows =
                surverExpertReviewOpinionService.listByTaskAndExpert(taskId, uid);
        Map<Integer, String> auditMap = new HashMap<>();
        Map<Integer, String> mainTextMap = new HashMap<>();
        Map<Integer, Integer> mainSubmittedMap = new HashMap<>();
        if (rows != null) {
            for (com.bootdo.cpe.domain.SurverExpertReviewOpinionDO r : rows) {
                if (r.getProId() == null) {
                    continue;
                }
                int pid = r.getProId();
                if (StringUtils.isNotBlank(r.getAuditOpinion())) {
                    auditMap.put(pid, r.getAuditOpinion());
                }
                if (r.getMainReviewText() != null) {
                    mainTextMap.put(pid, r.getMainReviewText());
                }
                int submitted = (r.getMainReviewSubmitted() != null && r.getMainReviewSubmitted() == 1) ? 1 : 0;
                mainSubmittedMap.put(pid, submitted);
            }
        }
        return R.ok()
                .put("auditOpinions", auditMap)
                .put("mainReviewTexts", mainTextMap)
                .put("mainReviewSubmitted", mainSubmittedMap);
    }

    /**
     * 专家侧：保存「专家审核意见」
     * 入参: taskId, proId, proSubType, auditOpinion（空串表示清空）
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/review/saveAudit")
    public R saveExpertReviewAudit(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String proSubType = params.get("proSubType") != null ? params.get("proSubType").toString() : "";
        String auditOpinion = params.get("auditOpinion") != null ? params.get("auditOpinion").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr)) {
            return R.error("缺少必填参数 taskId / proId");
        }
        taskId = resolveExpertTaskId(uid, taskId);
        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", uid);
        snapQ.put("taskId", taskId);
        if (surverExpertEliminateConfirmedService.count(snapQ) > 0) {
            return R.error("您已确认提交，无法再修改。");
        }
        Integer proId;
        try {
            proId = Integer.valueOf(proIdStr);
        } catch (NumberFormatException e) {
            return R.error("proId 必须是整数");
        }
        try {
            surverExpertReviewOpinionService.saveAudit(taskId, uid, proId, proSubType, auditOpinion);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("保存失败");
        }
    }

    /**
     * 专家侧：保存「主评意见」
     * 入参: taskId, proId, proSubType, mainReviewText, submitMain（默认 true：首次提交；false：已提交后仅改正文）
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/review/saveMain")
    public R saveExpertReviewMain(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String proSubType = params.get("proSubType") != null ? params.get("proSubType").toString() : "";
        String mainReviewText = params.get("mainReviewText") != null ? params.get("mainReviewText").toString() : "";
        boolean submitMain = true;
        Object sm = params.get("submitMain");
        if (sm != null) {
            String s = sm.toString().trim();
            submitMain = "1".equals(s) || "true".equalsIgnoreCase(s);
        }
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr)) {
            return R.error("缺少必填参数 taskId / proId");
        }
        taskId = resolveExpertTaskId(uid, taskId);
        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", uid);
        snapQ.put("taskId", taskId);
        if (surverExpertEliminateConfirmedService.count(snapQ) > 0) {
            return R.error("您已确认提交，无法再修改。");
        }
        Integer proId;
        try {
            proId = Integer.valueOf(proIdStr);
        } catch (NumberFormatException e) {
            return R.error("proId 必须是整数");
        }
        try {
            surverExpertReviewOpinionService.saveMain(taskId, uid, proId, proSubType, mainReviewText, submitMain);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("保存失败");
        }
    }

    /**
     * 管理员侧：查看某项目下所有专家的评级详情（姓名、等级、评级理由）
     * 入参: taskId, proId
     * 出参: { code, data: [ { expertName, grade, remark }, ... ] }
     */
    @ResponseBody
    @RequestMapping("/eliminate/expertGradeDetails")
    public R expertGradeDetails(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr)) {
            return R.error("缺少必填参数 taskId / proId");
        }
        Map<String, Object> q = new HashMap<>();
        q.put("taskId", taskId);
        q.put("proId", Integer.valueOf(proIdStr));
        q.put("deleted", 0);
        List<com.bootdo.cpe.domain.SurverExpertEliminateDO> list = surverExpertEliminateService.list(q);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.bootdo.cpe.domain.SurverExpertEliminateDO item : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("expertName", item.getExpertName());
            m.put("grade", item.getGrade());
            m.put("remark", item.getRemark());
            result.add(m);
        }
        return R.ok().put("data", result);
    }

    /**
     * 专家侧：下载淘汰评语 — 仅当前专家本人提交的评级及评语（不含其他专家）
     * 入参: taskId(必填), proSubType(选填，与打分页子奖项一致)
     * 出参: { code, list:[{ proSubType, proCode, topicName, companyName, declareAccount, groupName, expertGroupName,
     *                       expertName, expertUid, grade, remark, eliminated }] }
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/listGroupEliminateDetail")
    public R listGroupEliminateDetailForExpert(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proSubType = params.get("proSubType") != null ? params.get("proSubType").toString() : null;
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        taskId = resolveExpertTaskId(uid, taskId);
        if (StringUtils.isBlank(proSubType)) {
            proSubType = null;
        }
        try {
            List<Map<String, Object>> list = surverExpertEliminateService.listMyExpertGroupEliminateDetail(
                    taskId, proSubType, uid);
            return R.ok().put("list", list == null ? Collections.emptyList() : list);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 保存/更新单条评级 (upsert)
     * 入参: taskId, proId, proSubType, grade(A/B/C/D), 可选: proCode/topicName/companyName/groupName/expertName/remark
     * 校验: 已确认提交后(快照表存在记录) 拒绝写入
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/saveGrade")
    public R saveExpertGrade(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String proSubType = params.get("proSubType") != null ? params.get("proSubType").toString() : "";
        String grade = params.get("grade") != null ? params.get("grade").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr) || StringUtils.isBlank(proSubType)) {
            return R.error("缺少必填参数 taskId / proId / proSubType");
        }
        Integer proId;
        try { proId = Integer.valueOf(proIdStr); }
        catch (NumberFormatException e) { return R.error("proId 必须是整数"); }

        // 修复：从专家分组绑定反查真实 taskId，避免前端传入的 taskId 不一致
        taskId = resolveExpertTaskId(uid, taskId);

        // 锁定校验（使用修正后的 taskId）
        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", uid);
        snapQ.put("taskId", taskId);
        if (surverExpertEliminateConfirmedService.count(snapQ) > 0) {
            return R.error("您已确认提交，无法再修改评级。");
        }

        // grade 为空 → 清除已有评级
        if (StringUtils.isBlank(grade)) {
            com.bootdo.cpe.domain.SurverExpertEliminateDO exist = surverExpertEliminateService.getByUnique(taskId, proId, uid);
            if (exist != null) {
                surverExpertEliminateService.remove(exist.getId());
                return R.ok("已清除评级");
            }
            return R.ok("无需清除");
        }

        Set<String> allowedGrades = new HashSet<>();
        Collections.addAll(allowedGrades, "A", "B", "C", "D");
        if (!allowedGrades.contains(grade)) {
            return R.error("等级仅允许 A/B/C/D");
        }

        com.bootdo.cpe.domain.SurverExpertEliminateDO row = new com.bootdo.cpe.domain.SurverExpertEliminateDO();
        row.setTaskId(taskId);
        row.setProId(proId);
        row.setProSubType(proSubType);
        row.setExpertUid(uid);
        row.setGrade(grade);

        // 从数据库补全冗余字段（不再依赖前端传递）
        // 专用 SQL 直接查 pro_code, chengguo, pro_group_name, companyName
        Map<String, Object> snapshot = surverExpertEliminateService.getProjectSnapshotInfo(proId);
        if (snapshot != null) {
            if (snapshot.get("proCode") != null)     row.setProCode(snapshot.get("proCode").toString());
            if (snapshot.get("topicName") != null)   row.setTopicName(snapshot.get("topicName").toString());
            if (snapshot.get("groupName") != null)   row.setGroupName(snapshot.get("groupName").toString());
            if (snapshot.get("companyName") != null)  row.setCompanyName(snapshot.get("companyName").toString());
        } else {
            // 兜底：前端传了就用
            if (params.get("proCode") != null)     row.setProCode(params.get("proCode").toString());
            if (params.get("topicName") != null)   row.setTopicName(params.get("topicName").toString());
            if (params.get("companyName") != null) row.setCompanyName(params.get("companyName").toString());
            if (params.get("groupName") != null)   row.setGroupName(params.get("groupName").toString());
        }
        // 2. 专家姓名：当前登录用户
        UserDO expertUser = userService.get(uid);
        if (expertUser != null) {
            row.setExpertName(expertUser.getName());
        } else if (params.get("expertName") != null) {
            row.setExpertName(params.get("expertName").toString());
        }
        if (params.get("remark") != null) row.setRemark(params.get("remark").toString());
        row.setDeleted(0);

        try {
            Long id = surverExpertEliminateService.saveOrUpdateGrade(row);
            return R.ok().put("id", id);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 这个接口不用了，专家不能自己回避
     * 专家手动回避一个项目（写 ass_surver_expert_avoidance, type=manual）
     * 入参: taskId, proId, 可选 reason
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/avoid")
    public R expertAvoid(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr)) {
            return R.error("缺少必填参数 taskId / proId");
        }
        // 锁定校验
        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", uid);
        snapQ.put("taskId", taskId);
        if (surverExpertEliminateConfirmedService.count(snapQ) > 0) {
            return R.error("您已确认提交，无法再修改回避状态。");
        }
        Integer proId;
        try { proId = Integer.valueOf(proIdStr); }
        catch (NumberFormatException e) { return R.error("proId 必须是整数"); }

        boolean ok = surverExpertAvoidanceService.manualAvoid(taskId, proId, uid.intValue(), uid.intValue(), reason);
        return ok ? R.ok("已回避") : R.error("回避失败（可能已存在回避记录）");
    }

    /**
     * 这个接口不用了，专家不能自己回避
     * 专家取消对某项目的回避
     * 入参: taskId, proId
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/cancelAvoid")
    public R expertCancelAvoid(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr)) {
            return R.error("缺少必填参数 taskId / proId");
        }
        // 锁定校验
        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", uid);
        snapQ.put("taskId", taskId);
        if (surverExpertEliminateConfirmedService.count(snapQ) > 0) {
            return R.error("您已确认提交，无法再修改回避状态。");
        }
        Integer proId;
        try { proId = Integer.valueOf(proIdStr); }
        catch (NumberFormatException e) { return R.error("proId 必须是整数"); }

        boolean ok = surverExpertAvoidanceService.cancelAvoidance(taskId, proId, uid.intValue());
        return ok ? R.ok("已取消回避") : R.error("没有可取消的回避记录");
    }

    /**
     * 专家确认提交评级名单
     * - 把活动表中 (expertUid, taskId, deleted=0) 全量拷贝到快照表
     * - 同 (expertUid, taskId) 已存在快照则拒绝（避免重复提交）
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/confirmSubmit")
    public R expertConfirmSubmit(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(taskId)) return R.error("任务ID不能为空");
        taskId = resolveExpertTaskId(uid, taskId);

        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", uid);
        snapQ.put("taskId", taskId);
        if (surverExpertEliminateConfirmedService.count(snapQ) > 0) {
            return R.error("您已确认提交过，无需重复提交");
        }

        // 统计已评级数 + 已回避数
        Map<String, Object> activeQ = new HashMap<>();
        activeQ.put("expertUid", uid);
        activeQ.put("taskId", taskId);
        activeQ.put("deleted", 0);
        int gradedCnt = surverExpertEliminateService.count(activeQ);
        if (gradedCnt == 0) {
            return R.error("您当前还没有任何评级，无法提交");
        }

        // 检查是否所有被分派的项目都已评级或已回避
        int totalAssigned = surverExpertEliminateService.countAssignedProjects(taskId, uid);
        List<Integer> avoidedProIds = surverExpertAvoidanceService.getAvoidedProIds(taskId, uid.intValue());
        int avoidedCnt = (avoidedProIds == null) ? 0 : avoidedProIds.size();
        int processedCnt = gradedCnt + avoidedCnt;
        if (totalAssigned > 0 && processedCnt < totalAssigned) {
            int remaining = totalAssigned - processedCnt;
            return R.error("还有 " + remaining + " 个项目未评级且未回避，请全部处理后再提交（共 "
                    + totalAssigned + " 项，已评级 " + gradedCnt + " 项，已回避 " + avoidedCnt + " 项）");
        }

        try {
            int rows = surverExpertEliminateConfirmedService.copyFromActiveByExpertAndTask(uid, taskId);
            return R.ok("已确认提交 " + rows + " 条评级");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("提交失败: " + e.getMessage());
        }
    }

    /**
     * 撤回确认提交 - 已禁用，确认提交后不可撤回
     */
    @ResponseBody
    @RequestMapping("/eliminate/expert/cancelConfirmSubmit")
    public R expertCancelConfirmSubmit(@RequestParam Map<String, Object> params) {
        return R.error("确认提交后不可撤回，如需修改请联系管理员");
    }

    /**
     * 管理员驳回专家的淘汰确认提交（删除快照表记录，保留活动表评级数据）
     * 入参: taskId, expertUid
     */
    @ResponseBody
    @RequestMapping("/eliminate/admin/rejectConfirmSubmit")
    public R adminRejectEliminateSubmit(@RequestParam Map<String, Object> params) {
        UserDO user = getUser();
        List<Long> roleIdList = user != null ? user.getRoleIds() : Collections.emptyList();
        boolean isAdmin = roleIdList.contains(ROLE_ASSOCIATION_LEADER)
                || roleIdList.contains(ROLE_SURVER_ASSOCIATION_ID)
                || roleIdList.contains(ROLE_SURVER_EXTERNAL_EMPLOYMENT_ID)
                || roleIdList.contains(ROLE_SURVER_GROUP_CONTACT_ID);
        if (!isAdmin) {
            return R.error("无权操作：仅管理员可驳回专家淘汰提交");
        }
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String expertUidStr = params.get("expertUid") != null ? params.get("expertUid").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(expertUidStr)) {
            return R.error("缺少必填参数 taskId / expertUid");
        }
        Long expertUid;
        try { expertUid = Long.valueOf(expertUidStr); }
        catch (NumberFormatException e) { return R.error("expertUid 格式错误"); }

        Map<String, Object> snapQ = new HashMap<>();
        snapQ.put("expertUid", expertUid);
        snapQ.put("taskId", taskId);
        if (surverExpertEliminateConfirmedService.count(snapQ) == 0) {
            return R.error("该专家尚未确认提交，无需驳回");
        }
        int rows = surverExpertEliminateConfirmedService.deleteByExpertAndTask(expertUid, taskId);
        return R.ok("已驳回，删除快照记录 " + rows + " 条，专家可重新修改评级");
    }

    // ============================================================
    // 管理员侧回避管理（专业组管理页面使用）
    // ============================================================

    /**
     * 管理员加载指定专家的项目列表（含回避状态）
     * 入参: taskId, expertUserId
     * 出参: { code:0, rows:[{proId, topicName, proCode, companyName, groupName, proStat, isAvoided}] }
     */
    @ResponseBody
    @RequestMapping("/avoidance/expertProjects")
    public R getExpertProjectsForAvoidance(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String expertUserIdStr = params.get("expertUserId") != null ? params.get("expertUserId").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(expertUserIdStr)) {
            return R.error("缺少参数 taskId / expertUserId");
        }
        Integer expertUserId;
        try { expertUserId = Integer.valueOf(expertUserIdStr); }
        catch (NumberFormatException e) { return R.error("expertUserId 格式错误"); }

        // 复用 surverAwardService 查询该专家分配的项目
        Map<String, Object> q = new HashMap<>();
        q.put("taskId", taskId);
        q.put("scoreSpecialistUid", expertUserId);
        q.put("offset", 0);
        q.put("limit", 9999);
        Query query = new Query(q);
        List<SurverProjectInfo> proList = surverAwardService.listProInfo(query);

        // 获取该专家已回避的项目集合
        List<Integer> avoidedProIds = surverExpertAvoidanceService.getAvoidedProIds(taskId, expertUserId);
        Set<Integer> avoidedSet = new HashSet<>(avoidedProIds == null ? Collections.emptyList() : avoidedProIds);

        // 组装返回
        List<Map<String, Object>> rows = new ArrayList<>();
        if (proList != null) {
            for (SurverProjectInfo p : proList) {
                Map<String, Object> row = new HashMap<>();
                row.put("proId", p.getProId());
                row.put("topicName", p.getProName());
                row.put("proCode", p.getProCode());
                row.put("applyId", p.getDeclareAccount());
                row.put("unitName", p.getApplyCompany());
                row.put("groupDesc", p.getQcGroupName());
                row.put("proStat", p.getProStat());
                row.put("isAvoided", avoidedSet.contains(p.getProId()));
                rows.add(row);
            }
        }
        return R.ok().put("rows", rows);
    }

    /**
     * 管理员为指定专家设置回避
     * 入参: taskId, proId, expertUserId, reason(可选)
     */
    @ResponseBody
    @RequestMapping("/avoidance/manualAvoid")
    public R adminManualAvoid(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String expertUserIdStr = params.get("expertUserId") != null ? params.get("expertUserId").toString() : "";
        String reason = params.get("reason") != null ? params.get("reason").toString() : "管理员手动回避";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr) || StringUtils.isBlank(expertUserIdStr)) {
            return R.error("缺少必填参数");
        }
        Integer proId, expertUserId;
        try {
            proId = Integer.valueOf(proIdStr);
            expertUserId = Integer.valueOf(expertUserIdStr);
        } catch (NumberFormatException e) { return R.error("参数格式错误"); }

        boolean ok = surverExpertAvoidanceService.manualAvoid(taskId, proId, expertUserId, getUserId().intValue(), reason);
        return ok ? R.ok("设置成功") : R.error("设置失败（可能已存在回避记录）");
    }

    /**
     * 管理员为指定专家取消回避
     * 入参: taskId, proId, expertUserId
     */
    @ResponseBody
    @RequestMapping("/avoidance/cancelAvoid")
    public R adminCancelAvoid(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String expertUserIdStr = params.get("expertUserId") != null ? params.get("expertUserId").toString() : "";
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proIdStr) || StringUtils.isBlank(expertUserIdStr)) {
            return R.error("缺少必填参数");
        }
        Integer proId, expertUserId;
        try {
            proId = Integer.valueOf(proIdStr);
            expertUserId = Integer.valueOf(expertUserIdStr);
        } catch (NumberFormatException e) { return R.error("参数格式错误"); }

        boolean ok = surverExpertAvoidanceService.cancelAvoidance(taskId, proId, expertUserId);
        return ok ? R.ok("已取消回避") : R.error("没有可取消的回避记录");
    }

    /**
     * 勘察奖淘汰：纯「小组联络人」返回其 userId 用于服务端按 surver_view_scope 过滤；
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
}
