package com.bootdo.cpe.controller.qc;

import com.bootdo.activiti.domain.*;
import com.bootdo.activiti.service.AwardEnterpriseProjectService;
import com.bootdo.activiti.service.AwardPublishTaskService;
import com.bootdo.activiti.service.QcGroupService;
import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.controller.BaseQcProController;
import com.bootdo.common.domain.FileDO;
import com.bootdo.common.service.FileService;
import com.bootdo.common.utils.DateUtils;
import com.bootdo.common.utils.FileType;
import com.bootdo.common.utils.FileUtil;
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
import com.bootdo.cpe.service.SurverReviewSoftResultService;
import com.bootdo.system.domain.UserDO;
import com.bootdo.system.service.UserService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import com.bootdo.cpe.domain.QcProStatEnum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.bootdo.cpe.domain.QcGroupApplyInfoDO;
import com.bootdo.cpe.domain.QcGroupMember;
import com.bootdo.cpe.service.QcGroupApplyInfoService;
import com.bootdo.cpe.service.QcExpertEliminateConfirmedService;
import com.bootdo.cpe.service.QcGroupMemberService;
import static com.bootdo.common.config.Constant.ROLE_QC_EXTERNAL_EMPLOYMENT_ID;
import static com.bootdo.common.config.Constant.ROLE_QC_SPECIALIST_ID;
import com.bootdo.cpe.domain.science_process.ScienceAssignUserInfo;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.http.HttpServletResponse;

/**
 * QC奖任务
 * QC流程/形审/分派入口
 * /qcProcess
 *
 * @author houzb
 * @version 1.0
 * @date 2022-02-07 17:33
 */
@RequestMapping("/qcProcess")
@Controller
public class QcProcessController extends BaseQcProController {
    private String prefix = "cpe/qc";
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
    private QcGroupApplyInfoService qcGroupApplyInfoService;
    @Autowired
    private QcGroupService qcGroupService;

    @Autowired
    private QcGroupMemberService qcGroupMemberService;
    @Autowired
    private com.bootdo.cpe.service.QcExpertAvoidanceService qcExpertAvoidanceService;
    @Autowired
    private com.bootdo.cpe.service.QcExpertEliminateService qcExpertEliminateService;
    @Autowired
    private QcExpertEliminateConfirmedService qcExpertEliminateConfirmedService;
    /**
     * 去分配项目给工作人员
     *
     * @return
     */
    @RequiresPermissions("asso:task:assign")
    @RequestMapping("/toAssign")
    public String toAssignPro(@RequestParam Map<String, Object> params, ModelMap map) {
//         原代码：long roleId = ROLE_QC_EXTERNAL_EMPLOYMENT_ID;
        // 新代码：分派专家时使用QC奖评审专家角色(85)查询专家列表
//        long roleId = ROLE_QC_SPECIALIST_ID;
        long roleId = ROLE_QC_EXTERNAL_EMPLOYMENT_ID;
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
        String taskStatStr = taskDo.getTaskStatStr();
        boolean allowAssign = isAssgin || "形式审查".equals(taskStatStr);
        if(!allowAssign) {
            //分派时间已截止
            map.put("tipMsg", "分派外聘人员时间已结束,如需调整联系协会任务发布人员");
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
        return prefix + "/check/qc_check_assignment";
    }

    @Autowired
    FileService fileService;
    @Autowired
    AwardEnterpriseProjectService projectService;
    @Autowired
    private BootdoConfig bootdoConfig;
    /**
     * 提交审核项目
     *
     * @param proId
     * @return
     */
    @RequestMapping("/subCheck")
    @ResponseBody
    public R subCheckPro(Integer proId) {
        // 质量奖检测附件内容是否完整
        EnterpriseProjectInfoDo projectInfoDo = projectService.get(Integer.toString(proId));
        if (projectInfoDo.getProType().equalsIgnoreCase("qc_group")){
            List<String> types  = fileService.fileTypeList(Integer.toString(proId));
            if (types.size() < 9){
                return R.error("请上传所有附件");
            }
        }
        if (proId != null && proId > 0 && qcAwardService.updateProCheck(proId) > 0) {
            return R.ok();
        }
        return R.error();
    }

    /**
     * 撤回提交审核项目
     *
     * @param proId
     * @return
     */
    @RequestMapping("/cancelCheck")
    @ResponseBody
    @RequiresPermissions("cpe:qcGroupApplyInfo:cancelReview")
    public R cancelCheckPro(Integer proId) {
        if (proId != null && proId > 0 && qcAwardService.updateProApply(proId) > 0) {
            return R.ok();
        }
        return R.error();
    }
    /**
     * 形审驳回项目
     */
    @RequestMapping("/reject")
    @ResponseBody
    @RequiresPermissions("cpe:qcGroupApplyInfo:ass_validate_pro")
    public R rejectPro(Integer proId) {
        if (proId == null || proId <= 0) {
            return R.error("参数错误");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("proId", proId);
        params.put("proStat", QcProStatEnum.REJECT.getProStat()); // reject
        int rst = qcAwardService.updateProStat(params);
        return rst > 0 ? R.ok("驳回成功") : R.error("驳回失败");
    }

    @RequestMapping("/getQcGroups")
    @ResponseBody
    public R getQcGroups(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        String taskId = (String) params.get("taskId");
        List<QcGroupDO> groupList = qcGroupService.getGroupsByTaskId(taskId);
        return R.ok().put("list", groupList);
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
        if("__NO_GROUP__".equals(qcGroupName)) {
            params.put("qcGroupName", "");
            params.put("noGroup", true);
        }

        Map<String,Object> userParamMap = new HashMap<>();
        userParamMap.put("username", asWorkerName);
        userParamMap.put("qcgroupname", qcGroupName);
        List<UserDO> userList = userService.list(userParamMap);
        System.out.println("qcGroupName"+qcGroupName);
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
        String awardType = params.get("awardType").toString();
        String[] workerNameArr = asWorkerName.split(",");
        List<Long> workerUidList = userService.getUidsByLoginUserNames(workerNameArr);
        // String proIds = params.get("proIds").toString();
        // if (StringUtils.isBlank(proIds)) {
        //     awardEnterpriseProjectService.removeByExtUid(workerUidList, taskId, awardType);
        //     return R.error(2, "分派项目已清空");
        // }
        // String[] proIdArr = proIds.split(",");
        // List<AssignProjectDataDo> assignProjectDataDoList = new ArrayList<>();
        // for (long wuid : workerUidList) {
        //     for (String proId : proIdArr) {
        //         long pid = Long.parseLong(proId);
        //         AssignProjectDataDo assignData = new AssignProjectDataDo(assignUid, wuid, pid);
        //         assignProjectDataDoList.add(assignData);
        //     }
        // }
        // awardEnterpriseProjectService.removeByExtUid(workerUidList, taskId, awardType);
        // awardEnterpriseProjectService.assignPro(assignProjectDataDoList);

        // return R.ok();
        String proIds = params.get("proIds") == null ? "" : params.get("proIds").toString();

        // 先清空该外聘人员在当前任务+奖项下的历史分派（实现“取消”）
        awardEnterpriseProjectService.removeByExtUid(workerUidList, taskId, awardType);

        // 右侧为空：表示用户要清空该外聘人员分派
        if (StringUtils.isBlank(proIds)) {
            return R.ok("分派已更新（已清空）");
        }

        // 重建右侧列表对应的分派（实现“新增/保留”）
        String[] proIdArr = proIds.split(",");
        List<AssignProjectDataDo> assignProjectDataDoList = new ArrayList<>();
        for (long wuid : workerUidList) {
            for (String proId : proIdArr) {
                if (StringUtils.isNotBlank(proId)) {
                    long pid = Long.parseLong(proId.trim());
                    assignProjectDataDoList.add(new AssignProjectDataDo(assignUid, wuid, pid));
                }
            }
        }

        if (assignProjectDataDoList.isEmpty()) {
            return R.ok("分派已更新");
        }
        awardEnterpriseProjectService.assignPro(assignProjectDataDoList);
        // 修复：分派专家后，将被分派项目的状态更新为"score"（专家打分），
        // 否则QcAwardMapper中 pro.pro_stat = 'score' 条件永远不满足，专家看不到项目
        for (String proId : proIdArr) {
            if (StringUtils.isNotBlank(proId)) {
                Map<String, Object> statParams = new HashMap<>();
                statParams.put("proId", Integer.parseInt(proId.trim()));
                statParams.put("proStat", "score");
                qcAwardService.updateProStat(statParams);
            }
        }
        return R.ok("分派已更新");
    }

    /**
     * 形式审查
     * @return
     */
    @RequestMapping("/toReivew")
    @RequiresPermissions("cpe:qcGroupApplyInfo:ass_validate_pro")
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
        // ===== 无 iframe 模板所需数据：groupInfo / docUploadDoList / memberList =====
        String proId = params.get("proId") == null ? null : params.get("proId").toString();

        // 1) 基本信息
        QcGroupApplyInfoDO groupInfo = new QcGroupApplyInfoDO();
        if (StringUtils.isNotBlank(proId)) {
            Map<String, Object> groupParams = new HashMap<>();
            groupParams.put("proId", proId);
            List<QcGroupApplyInfoDO> groupList = qcGroupApplyInfoService.list(groupParams);
            if (groupList != null && !groupList.isEmpty()) {
                groupInfo = groupList.get(0);
            }
        }
        map.put("groupInfo", groupInfo);

        // 2) 附件列表
        List<EnterpriseDocUploadDo> docUploadDoList = fileService.listUploadEnterpriseDocs(params);
        map.put("docUploadDoList", docUploadDoList);

        // 3) 成员列表
        List<QcGroupMember> memberList = new ArrayList<>();
        if (StringUtils.isNotBlank(proId)) {
            memberList = qcGroupMemberService.getByProid(proId);
        }
        map.put("memberList", memberList);
        return prefix + "/check/qc_check_template";
    }

    /**
     * 添加专家账号
     * @return
     */
    @RequestMapping("/toAddSpecialist")
    public String toAddSpecialistUser(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        //用于入库标记账号的奖项类型
        String proType = EnumProjectType.QC_PRO_GROUP.getProType();
        map.put("proType", proType);

        // 动态加载QC分组列表
        String taskId = (String) params.get("taskId");
        List<QcGroupDO> qcGroupList = qcGroupService.getGroupsByTaskId(taskId);
        map.put("qcGroupList", qcGroupList);

        Map<String, Object> selParams = new HashMap<>();
        selParams.put("taskId", params.get("taskId"));
        selParams.put("groupName", params.get("major"));
        selParams.put("proType", proType);
        List<ExpertGroupDO> selList = expertGroupService.list(selParams);
        map.put("selInfoList", selList);

        return prefix + "/score/major_group_admin";
    }

    // 原代码：重置时无条件将所有专家的eliminateOver设为0，导致已确认提交的专家需要二次提交
    // /**
    //  * 重置所有专家的淘汰记录（管理员操作）
    //  * 将 ass_qc_expert_eliminate 记录软删除，并重置所有专家的 eliminate_over=0
    //  */
    // @ResponseBody
    // @RequestMapping("/resetAllEliminate")
    // public R resetAllEliminate(@RequestParam Map<String, Object> params) {
    //     String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
    //     if (StringUtils.isBlank(taskId)) {
    //         return R.error("任务ID不能为空");
    //     }
    //     try {
    //         int deletedCount = qcExpertEliminateService.batchSoftDeleteByTaskId(taskId);
    //         Map<String, Object> expertQuery = new HashMap<>();
    //         expertQuery.put("taskId", taskId);
    //         expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
    //         List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
    //         int resetCount = 0;
    //         for (ExpertGroupDO expert : expertList) {
    //             if (expert.getEliminateOver() != null && expert.getEliminateOver() == 1) {
    //                 expert.setEliminateOver(0);
    //                 expertGroupService.update(expert);
    //                 resetCount++;
    //             }
    //         }
    //         return R.ok("已撤销" + deletedCount + "条淘汰记录，重置" + resetCount + "位专家的淘汰提交状态");
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return R.error("操作失败：" + e.getMessage());
    //     }
    // }

    /**
     * 新代码：重置所有专家的淘汰记录（管理员操作）
     * 将 ass_qc_expert_eliminate 记录软删除
     * 仅重置尚未首次确认提交的专家的 eliminate_over=0
     * 已有快照数据（首次确认过）的专家保持 eliminate_over=1，无需二次提交
     */
    @ResponseBody
    @RequestMapping("/resetAllEliminate")
    public R resetAllEliminate(@RequestParam Map<String, Object> params) {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        try {
            // 1. 批量软删除该任务下所有淘汰记录
            int deletedCount = qcExpertEliminateService.batchSoftDeleteByTaskId(taskId);
            
            // 2. 仅重置尚未首次确认的专家的 eliminate_over=0
            Map<String, Object> expertQuery = new HashMap<>();
            expertQuery.put("taskId", taskId);
            expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
            int resetCount = 0;
            int skippedCount = 0;
            for (ExpertGroupDO expert : expertList) {
                if (expert.getEliminateOver() != null && expert.getEliminateOver() == 1) {
                    // 检查快照表中是否已有该专家的首次确认数据
                    if (StringUtils.isBlank(expert.getUserId())) {
                        continue;
                    }
                    Map<String, Object> confirmedCheck = new HashMap<>();
                    confirmedCheck.put("expertUid", Long.valueOf(expert.getUserId()));
                    confirmedCheck.put("taskId", taskId);
                    int confirmedCount = qcExpertEliminateConfirmedService.count(confirmedCheck);
                    if (confirmedCount > 0) {
                        // 已有首次确认快照，不重置eliminateOver，专家无需二次提交
                        skippedCount++;
                    } else {
                        // 未首次确认，重置eliminateOver
                        expert.setEliminateOver(0);
                        expertGroupService.update(expert);
                        resetCount++;
                    }
                }
            }
            
            return R.ok("已撤销" + deletedCount + "条淘汰记录，重置" + resetCount + "位专家的淘汰提交状态，" + skippedCount + "位已确认专家保持锁定");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * QC专业组管理 - 添加/更新专家
     * （参考 ScienceController.toAddExpert，简化版不更新 pro_group_name）
     */
    @ResponseBody
    @RequestMapping("/expert/add")
    public R toAddExpert(ExpertGroupDO expertGroupDO) {
        // 原代码：没有try-catch，异常会被全局处理器捕获返回"服务器错误，请联系管理员"
        // 新代码：添加try-catch，返回具体错误信息便于排查
        try {
            String loginAccountObj = expertGroupDO.getLoginAccount();
            if (loginAccountObj != null) {
                expertGroupDO.setLoginAccount(loginAccountObj.trim());
            }

            Integer id = expertGroupDO.getId();
            if (id == null) {
                Map<String, Object> params = new HashMap<>();
                params.put("loginAccount", expertGroupDO.getLoginAccount());
                params.put("taskId", expertGroupDO.getTaskId());
                params.put("proType", expertGroupDO.getProType());
                // 判断是更新还是新增
                List<ExpertGroupDO> expertGroupDOList = expertGroupService.list(params);
                if (expertGroupDOList.size() > 0) {
                    id = expertGroupDOList.get(0).getId();
                    if (id != null && id > 0) {
                        expertGroupDO.setId(id);
                    }
                }
            }
            int tag = 0;
            if (id != null) {
                tag = expertGroupService.update(expertGroupDO);
            } else {
                tag = expertGroupService.save(expertGroupDO);
            }
            if (tag > 0) {
                // 保存/更新成功后，触发自动回避检查（仅针对QC项目）
                if ("qc_group".equals(expertGroupDO.getProType())
                    && StringUtils.isNotBlank(expertGroupDO.getTaskId())
                    && StringUtils.isNotBlank(expertGroupDO.getUserId())
                    && StringUtils.isNotBlank(expertGroupDO.getCompany())) {
                    try {
                        qcExpertAvoidanceService.autoAvoidByCompany(
                            expertGroupDO.getTaskId(),
                            Integer.parseInt(expertGroupDO.getUserId()),
                            expertGroupDO.getCompany()
                        );
                    } catch (Exception e) {
                        // 自动回避失败不影响主流程
                        e.printStackTrace();
                    }
                }
                R r = R.ok();
                r.put("id", expertGroupDO.getUserId());
                return r;
            } else {
                if (tag == -100) {
                    return R.error("修改的用户已存在,请删除后再操作");
                }
                return R.error("添加数据出错");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("更新专家信息异常: " + e.getMessage());
        }
    }

    /**
     * QC专业组管理 - 移出专家
     * （参考 ScienceController.toRemoveExpert）
     *
     * 原代码（v1）：使用逻辑删除delByLoginAccount + delUserByAccount，
     *   且count==0时跳过sys_user删除，导致账号残留
     * 新代码（v3）：改用物理删除removeByLoginAccount，确保数据彻底清除，
     *   同时返回诊断信息便于确认
     */
    /**
     * QC专家签章文件上传
     * 保存图片到磁盘 + sys_file表 + 更新 add_special_info.signature_id
     */
    @RequestMapping("/uploadExpertSign")
    @ResponseBody
    public R uploadExpertSign(String taskId, Long expertUid,
                              @org.springframework.web.bind.annotation.RequestPart("file[]") MultipartFile[] files) {
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

    @ResponseBody
    @PostMapping("/expert/remove")
    public R toRemoveExpert(String loginAccount) {
        try {
            if (StringUtils.isBlank(loginAccount)) {
                return R.error("账号为空，无法移除");
            }
            loginAccount = loginAccount.trim();

            // 1. 先查询sys_user确认账号是否存在
            List<Long> uidList = userService.getUidByLoginUserName(loginAccount);

            // 原代码（v3）：物理删除，会导致打分/淘汰数据丢失关联
            // // 2. 物理删除 add_special_info 中的记录
            // int expertTag = expertGroupService.removeByLoginAccount(loginAccount);
            // // 3. 物理删除 sys_user 中的记录
            // int userTag = userService.removeByLoginAccount(loginAccount);

            // 新代码（v4）：改为逻辑删除，保留数据可恢复
            // 2. 逻辑删除 add_special_info（设deleted=1）
            int expertTag = expertGroupService.delByLoginAccount(loginAccount);
            // 3. 逻辑删除 sys_user（设deleted=1）
            int userTag = userService.delUserByAccount(loginAccount);

            R r = R.ok();
            r.put("expertDeleted", expertTag);
            r.put("userDeleted", userTag);
            r.put("loginAccount", loginAccount);
            return r;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[QC移出专家] 异常: " + e.getMessage());
            return R.error("移出专家异常: " + e.getMessage());
        }
    }

    /**
     * 检查专家是否有打分或淘汰数据（移出前二次确认用）
     */
    @RequestMapping("/expert/checkData")
    @ResponseBody
    public R checkExpertData(@RequestParam String loginAccount, @RequestParam(required = false) String taskId) {
        try {
            if (StringUtils.isBlank(loginAccount)) {
                return R.error("账号为空");
            }
            List<Long> uidList = userService.getUidByLoginUserName(loginAccount.trim());
            if (uidList == null || uidList.isEmpty()) {
                return R.ok().put("hasScore", false).put("hasEliminate", false);
            }
            Long uid = uidList.get(0);

            // 检查淘汰数据
            Map<String, Object> elimParams = new HashMap<>();
            elimParams.put("expertUid", uid);
            if (StringUtils.isNotBlank(taskId)) {
                elimParams.put("taskId", taskId);
            }
            elimParams.put("deleted", 0);
            int elimCount = qcExpertEliminateService.count(elimParams);

            // 检查快照表
            Map<String, Object> confirmedParams = new HashMap<>();
            confirmedParams.put("expertUid", uid);
            if (StringUtils.isNotBlank(taskId)) {
                confirmedParams.put("taskId", taskId);
            }
            int confirmedCount = qcExpertEliminateConfirmedService.count(confirmedParams);

            boolean hasEliminate = elimCount > 0 || confirmedCount > 0;

            return R.ok().put("hasEliminate", hasEliminate)
                         .put("elimCount", elimCount)
                         .put("confirmedCount", confirmedCount);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * QC专业组管理 - 上传专家签章页面
     * （参考 ScienceController.toUploadConfirmFilePage）
     */
    @RequestMapping("/toUploadExpertSign")
    public String toUploadExpertSignPage(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Object loginAccountObj = params.get("loginAccount");
        if (loginAccountObj != null) {
            List<Long> list = userService.getUidByLoginUserName(loginAccountObj.toString());
            if (list.size() > 0) {
                map.put("expertUid", list.get(0));
            }
        }
        map.put("loginAccount", params.get("loginAccount"));
        map.put("trIndex", params.get("trIndex"));
        return prefix + "/score/qc_expert_sign_upload";
    }

    /**
     * 导出单个专家的淘汰Word文档（初审不合格成果表）
     * 读取快照表数据，如果快照表无数据则降级读取当前有效淘汰记录
     */
    @RequestMapping("/exportEliminateWord")
    public void exportEliminateWord(@RequestParam String taskId,
                                     @RequestParam String expertUid,
                                     @RequestParam(value = "expertName", required = false, defaultValue = "") String expertName,
                                     @RequestParam(value = "year", required = false, defaultValue = "2026") String year,
                                     HttpServletResponse response) {
        try {
            Long uid = Long.valueOf(expertUid);

            // 查询专家签章图片磁盘路径
            String signImagePath = null;
            Map<String, Object> expertQuery = new HashMap<>();
            expertQuery.put("userId", expertUid);
            expertQuery.put("taskId", taskId);
            expertQuery.put("proType", com.bootdo.cpe.domain.EnumProjectType.QC_PRO_GROUP.getProType());
            List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
            if (expertList != null && !expertList.isEmpty()) {
                String signUrl = expertList.get(0).getExpertSignUrl();
                if (StringUtils.isNotBlank(signUrl)) {
                    // signUrl格式: /files/2026/04/sign_xxx/sign_xxx.png
                    // 磁盘路径: uploadPath + 去掉"/files/"前缀的部分
                    String relativePath = signUrl.startsWith("/files/") ? signUrl.substring("/files/".length()) : signUrl;
                    signImagePath = bootdoConfig.getUploadPath() + relativePath;
                }
            }

            // 先查快照表
            Map<String, Object> confirmedParams = new HashMap<>();
            confirmedParams.put("expertUid", uid);
            confirmedParams.put("taskId", taskId);
            List<com.bootdo.cpe.domain.QcExpertEliminateConfirmedDO> confirmedList =
                    qcExpertEliminateConfirmedService.list(confirmedParams);

            List<Map<String, Object>> dataList = new ArrayList<>();

            if (confirmedList != null && !confirmedList.isEmpty()) {
                // 快照表有数据
                for (com.bootdo.cpe.domain.QcExpertEliminateConfirmedDO item : confirmedList) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("proCode", item.getProCode());
                    row.put("topicName", item.getTopicName());
                    row.put("groupName", item.getGroupName());
                    row.put("companyName", item.getCompanyName());
                    row.put("reason", item.getReason());
                    dataList.add(row);
                }
            } else {
                // 降级读取当前有效淘汰记录
                Map<String, Object> elimParams = new HashMap<>();
                elimParams.put("expertUid", uid);
                elimParams.put("taskId", taskId);
                elimParams.put("deleted", 0);
                List<com.bootdo.cpe.domain.QcExpertEliminateDO> elimList =
                        qcExpertEliminateService.list(elimParams);
                if (elimList != null) {
                    for (com.bootdo.cpe.domain.QcExpertEliminateDO item : elimList) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("proCode", item.getProCode());
                        row.put("topicName", item.getTopicName());
                        row.put("groupName", item.getGroupName());
                        row.put("companyName", item.getCompanyName());
                        row.put("reason", item.getReason());
                        dataList.add(row);
                    }
                }
            }

            com.bootdo.cpe.utils.PoiWordEliminateUtils.exportEliminateWord(response, expertName, dataList, year, signImagePath);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":1,\"msg\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (Exception ignored) {
            }
        }
    }

}
