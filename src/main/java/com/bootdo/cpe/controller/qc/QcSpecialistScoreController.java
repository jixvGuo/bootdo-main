package com.bootdo.cpe.controller.qc;

import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.controller.BaseQcProController;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.ExpertGroupDO;
import com.bootdo.cpe.domain.QcAppraiseActiveScoreDO;
import com.bootdo.cpe.domain.QcPresentScoreDO;
import com.bootdo.cpe.service.QcPresentScoreService;
import com.bootdo.cpe.domain.QcExpertEliminateDO;
import com.bootdo.cpe.domain.QcExpertEliminateConfirmedDO;
import com.bootdo.cpe.domain.QcProStatEnum;
import com.bootdo.cpe.domain.QcReviewResultRecordDO;
import com.bootdo.cpe.domain.QcResultInnovateScoreDO;
import com.bootdo.cpe.domain.QcResultSolveScoreDO;
import com.bootdo.cpe.dto.QcProDataDto;
import com.bootdo.cpe.service.ExpertGroupService;
import com.bootdo.cpe.service.QcAppraiseActiveScoreService;
import com.bootdo.cpe.service.QcAwardService;
import com.bootdo.cpe.service.QcExpertAvoidanceService;
import com.bootdo.cpe.service.QcExpertEliminateService;
import com.bootdo.cpe.service.QcExpertEliminateConfirmedService;
import com.bootdo.cpe.domain.QcGroupApplyInfoDO;
import com.bootdo.cpe.service.QcGroupApplyInfoService;
import com.bootdo.cpe.service.QcResultInnovateScoreService;
import com.bootdo.cpe.service.QcResultSolveScoreService;
import com.bootdo.cpe.service.QcReviewResultRecordService;
import com.bootdo.cpe.domain.QcScoreSubmitDO;
import com.bootdo.cpe.domain.QcScoreCalcResultDO;
import com.bootdo.cpe.service.QcScoreCalcDetailService;
import com.bootdo.cpe.service.QcScoreCalcResultService;
import com.bootdo.cpe.service.QcScoreCalculationService;
import com.bootdo.cpe.service.QcScoreSubmitService;
import com.bootdo.common.service.DictService;
import com.bootdo.common.domain.DictDO;
import com.bootdo.system.domain.UserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.bootdo.system.service.UserService;

import static com.bootdo.common.config.Constant.*;

/**
 * 专家打分信息表
 *
 * @author houzb
 * @version 1.0
 * @date 2022-03-16 6:47
 */
@RequestMapping("/qcScore")
@Controller
public class QcSpecialistScoreController extends BaseQcProController {
    private static final Logger logger = LoggerFactory.getLogger(QcSpecialistScoreController.class);
    private String prefix = "cpe/qc";

    @Autowired
    private QcAwardService qcAwardService;
    @Autowired
    private QcAppraiseActiveScoreService qcAppraiseActiveScoreService;
    @Autowired
    private QcGroupApplyInfoService qcGroupApplyInfoService;
    @Autowired
    private QcReviewResultRecordService qcReviewResultRecordService;
    @Autowired
    private QcExpertEliminateService qcExpertEliminateService;
    @Autowired
    private QcResultSolveScoreService qcResultSolveScoreService;
    @Autowired
    private QcResultInnovateScoreService qcResultInnovateScoreService;
    @Autowired
    private QcExpertAvoidanceService avoidanceService;
    @Autowired
    private DictService dictService;
    @Autowired
    private QcExpertEliminateConfirmedService qcExpertEliminateConfirmedService;
    @Autowired
    private ExpertGroupService expertGroupService;
    @Autowired
    private QcScoreCalculationService qcScoreCalculationService;
    @Autowired
    private QcScoreSubmitService qcScoreSubmitService;
    @Autowired
    private QcScoreCalcResultService qcScoreCalcResultService;
    @Autowired
    private QcScoreCalcDetailService qcScoreCalcDetailService;
    @Autowired
    private UserService userService;
    @Autowired
    private QcPresentScoreService qcPresentScoreService;
    @Autowired
    private BootdoConfig bootdoConfig;
    @Autowired
    private com.bootdo.activiti.service.AwardPublishTaskService awardPublishTaskService;

    // ==================== 原有代码 ====================

    /**
     * 去qc打分
     * @param params
     * @param map
     * @return
     */
    // 原代码：仅跳转页面，无数据绑定
    // @RequestMapping("/toScore")
    // public String toScorePage(@RequestParam Map<String, Object> params, ModelMap map) {
    //     packageAwardTaskId(map, params);
    //     return prefix + "/score/specialist_score";
    // }

    /**
     * 去qc意见
     * @param params
     * @param map
     * @return
     */
    // 原代码：仅跳转页面，无数据绑定
    // @RequestMapping("/toOpinion")
    // public String toOpinionPage(@RequestParam Map<String, Object> params, ModelMap map) {
    //     packageAwardTaskId(map, params);
    //     return prefix + "/score/specialist_opinion";
    // }

    // ==================== 新增代码：QC专家评审功能 ====================

    /**
     * 新增：QC专家项目列表页面
     * 专家登录后看到的项目列表入口
     */
    // 原代码：仅跳转页面，不传scoreIsOver
    // @RequestMapping("/toExpertProList")
    // public String toExpertProList(@RequestParam Map<String, Object> params, ModelMap map) {
    //     packageAwardTaskId(map, params);
    //     return prefix + "/score/qc_expert_pro_list";
    // }
    // 新代码v2：参考科技奖 toScoreProPersonalList，增加scoreIsOver判断
    // 增加try-catch防止score_over列不存在时页面无法加载

    /** 每个专家最多淘汰的项目数 */
    //  淘汰数量
    private static final int MAX_ELIMINATE_COUNT = 9;

    @RequestMapping("/toExpertProList")
    public String toExpertProList(@RequestParam Map<String, Object> params, ModelMap map) {
        // 【优化】专家角色强制使用add_special_info表中的taskId（这个表存储了专家分配时的正确任务ID）
        UserDO currentUser = getUser();
        if (currentUser.getRoleIds().contains(ROLE_QC_SPECIALIST_ID)) {
            try {
                // 从add_special_info表查询专家分配信息，该表中的task_id就是项目所属的任务ID
                Map<String, Object> expertQuery = new HashMap<>();
                expertQuery.put("userId", String.valueOf(getUserId()));
                expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());  // 使用枚举的字符串值
                expertQuery.put("deleted", 0);
                List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
                
                if (!expertList.isEmpty()) {
                    ExpertGroupDO expert = expertList.get(0);
                    String expertTaskId = expert.getTaskId();
                    String originalTaskId = params.get("taskId") != null ? params.get("taskId").toString() : "null";
                    params.put("taskId", expertTaskId);
//                    System.out.println("[专家任务强制覆盖] 专家userId=" + expert.getUserId()
//                        + ", groupName=" + expert.getGroupName()
//                        + ", 系统默认taskId=" + originalTaskId
//                        + ", 强制使用专家分配taskId=" + expertTaskId);
                }
            } catch (Exception e) {
                System.out.println("[专家任务强制覆盖] 查询失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        packageAwardTaskId(map, params);
        // 查询当前专家是否已提交最终打分（参考 eliminateOver 模式，从数据库字段读取）
        int scoreIsOver = 0;
        Long uid = getUserId();
        String currentTaskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        int presentScoreIsOver = 0;
        try {
            Map<String, Object> scoreQuery = new HashMap<>();
            scoreQuery.put("userId", String.valueOf(uid));
            scoreQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            if (StringUtils.isNotBlank(currentTaskId)) {
                scoreQuery.put("taskId", currentTaskId);
            }
            List<ExpertGroupDO> scoreList = expertGroupService.list(scoreQuery);
            if (!scoreList.isEmpty()) {
                ExpertGroupDO eg = scoreList.get(0);
                if (eg.getScoreOver() != null) scoreIsOver = eg.getScoreOver();
                if (eg.getPresentScoreOver() != null) presentScoreIsOver = eg.getPresentScoreOver();
            }
        } catch (Exception e) {
            scoreIsOver = 0;
        }
        map.put("scoreIsOver", scoreIsOver);
        map.put("presentScoreIsOver", presentScoreIsOver);
        map.put("userId", uid); // 传递用户ID给前端，用于回避功能

        // 传递第二阶段专家评审时间给前端，用于"发布分评分"按钮显示条件
        try {
            if (StringUtils.isNotBlank(currentTaskId)) {
                com.bootdo.activiti.domain.PublishAwardTaskDo taskDo = awardPublishTaskService.getProTaskByTaskId(currentTaskId);
                if (taskDo != null) {
                    map.put("expertStartTimeSecond", taskDo.getExpertStartTimeSecond());
                    map.put("expertEndTimeSecond", taskDo.getExpertEndTimeSecond());
                }
            }
        } catch (Exception e) {
            // 查询失败不影响页面加载
        }

        // 查询当前专家是否已确认提交淘汰名单
        int eliminateIsOver = 0;
        try {
            Map<String, Object> eliQuery = new HashMap<>();
            eliQuery.put("userId", String.valueOf(uid));
            eliQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            if (StringUtils.isNotBlank(currentTaskId)) {
                eliQuery.put("taskId", currentTaskId);
            }
            List<ExpertGroupDO> eliList = expertGroupService.list(eliQuery);
            if (!eliList.isEmpty() && eliList.get(0).getEliminateOver() != null) {
                eliminateIsOver = eliList.get(0).getEliminateOver();
            }
        } catch (Exception e) {
            eliminateIsOver = 0;
        }
        map.put("eliminateIsOver", eliminateIsOver);
        
        // 传递字典数据给前端，用于筛选下拉框
        try {
            List<DictDO> projectTypes = dictService.listByType("projectType");
            List<DictDO> classifications = dictService.listByType("classification");
            map.put("projectTypes", projectTypes);
            map.put("classifications", classifications);
        } catch (Exception e) {
            // 字典查询失败不影响页面加载
        }
        
        return prefix + "/score/qc_expert_pro_list";
    }

    /**
     * 新增：获取QC专家的项目列表数据（JSON）
     * 根据当前登录专家的userId，从add_special_info关联查询分配给该专家的项目
     */
    @RequestMapping("/get/expertProList")
    @ResponseBody
    public PageUtils getExpertProList(@RequestParam Map<String, Object> params, ModelMap map) {
        System.out.println("========== [QC专家项目列表] 方法入口 ==========");
//        System.out.println("[请求参数] params=" + params);
        
        // 【优化】专家角色强制使用add_special_info表中的taskId（这个表存储了专家分配时的正确任务ID）
        UserDO currentUser = getUser();
        if (currentUser.getRoleIds().contains(ROLE_QC_SPECIALIST_ID)) {
            try {
                // 从add_special_info表查询专家分配信息，该表中的task_id就是项目所属的任务ID
                Map<String, Object> expertQuery = new HashMap<>();
                expertQuery.put("userId", String.valueOf(getUserId()));
                expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());  // 使用枚举的字符串值
                expertQuery.put("deleted", 0);
                List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
                
                if (!expertList.isEmpty()) {
                    ExpertGroupDO expert = expertList.get(0);
                    String expertTaskId = expert.getTaskId();
                    String originalTaskId = params.get("taskId") != null ? params.get("taskId").toString() : "null";
                    params.put("taskId", expertTaskId);
//                    System.out.println("[专家任务强制覆盖-数据接口] 专家userId=" + expert.getUserId()
//                        + ", groupName=" + expert.getGroupName()
//                        + ", 系统默认taskId=" + originalTaskId
//                        + ", 强制使用专家分配taskId=" + expertTaskId);
                }
            } catch (Exception e) {
//                System.out.println("[专家任务强制覆盖-数据接口] 查询失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        packageAwardTaskId(map, params);
        UserDO user = getUser();
        Long uid = getUserId();
        List<Long> roleIdList = user.getRoleIds();
        
//        System.out.println("[用户信息] uid=" + uid + ", username=" + user.getUsername() + ", roleIds=" + roleIdList);
//        System.out.println("[角色检查] 是否包含QC专家角色(85)=" + roleIdList.contains(ROLE_QC_SPECIALIST_ID));
//        System.out.println("[角色检查] 是否包含管理员角色(1)=" + roleIdList.contains(ROLE_ADMIN_ID));

        // 原代码：if (roleIdList.contains(ROLE_QC_EXTERNAL_EMPLOYMENT_ID)) {
        // 新代码：使用QC奖评审专家角色(85)判断
        // if (roleIdList.contains(ROLE_QC_SPECIALIST_ID)) {
        //     params.put("scoreSpecialistUid", uid);
        // } else if (roleIdList.contains(ROLE_ADMIN_ID)) {
        // }
        // 新代码v2：增加诊断日志，排查专家看不到项目的原因
        if (roleIdList.contains(ROLE_QC_SPECIALIST_ID)) {
            System.out.println("[进入专家分支] 开始设置scoreSpecialistUid");
            // 如果没有传入scoreSpecialistUid，使用当前登录用户ID
            if (!params.containsKey("scoreSpecialistUid")) {
                // 【修复】将Long转为String，与add_special_info.user_id字段类型保持一致
                params.put("scoreSpecialistUid", String.valueOf(uid));
                System.out.println("[设置参数] scoreSpecialistUid=" + params.get("scoreSpecialistUid") + " (类型: String)");
            } else {
                System.out.println("[已有参数] scoreSpecialistUid=" + params.get("scoreSpecialistUid"));
            }
            System.out.println("[QC专家项目查询] uid=" + uid + ", username=" + user.getUsername()
                    + ", roleIds=" + roleIdList + ", taskId=" + params.get("taskId")
                    + ", scoreSpecialistUid=" + params.get("scoreSpecialistUid"));
        } else if (roleIdList.contains(ROLE_ADMIN_ID)) {
            System.out.println("[进入管理员分支] 查看全部项目");
        } else {
            System.out.println("[警告] 用户既不是专家也不是管理员，角色列表=" + roleIdList);
        }

        System.out.println("[查询参数] 最终传递给Mapper的params=" + params);
        Query query = new Query(params);
        // 获取 QC 项目列表数据
        List<QcProDataDto> proDataDtoList = qcAwardService.listProInfo(query);
        int total = qcAwardService.countProInfo(query);
        
        System.out.println("[查询结果] 项目数量=" + (proDataDtoList != null ? proDataDtoList.size() : 0) + ", 总数=" + total);
        if (proDataDtoList != null && !proDataDtoList.isEmpty()) {
            System.out.println("[查询结果] 第一个项目: " + proDataDtoList.get(0).getTopicName());
        } else {
            System.out.println("[查询结果] 未查询到任何项目！");
        }
        System.out.println("========== [QC专家项目列表] 方法结束 ==========");
        
        // 标记每个项目的回避状态
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String avoidanceFilter = params.get("avoidanceFilter") != null ? params.get("avoidanceFilter").toString() : "";
        
        // 确定要检查回避状态的专家ID（优先使用传入的scoreSpecialistUid，否则使用当前登录用户ID）
        Integer checkExpertUserId = uid.intValue();
        if (params.containsKey("scoreSpecialistUid")) {
            Object scoreSpecialistUidObj = params.get("scoreSpecialistUid");
            if (scoreSpecialistUidObj != null) {
                checkExpertUserId = Integer.parseInt(scoreSpecialistUidObj.toString());
            }
        }
        
        // 【新增】获取当前专家已淘汰的项目ID列表
        // 1：查询当前专家的淘汰记录
        Set<Integer> eliminatedProIds = new HashSet<>();
        if (roleIdList.contains(ROLE_QC_SPECIALIST_ID) && StringUtils.isNotBlank(taskId)) {
            Map<String, Object> eliminateParams = new HashMap<>();
            eliminateParams.put("expertUid", uid); // 只查当前专家的
            eliminateParams.put("taskId", taskId);
            eliminateParams.put("deleted", 0); // 只查有效的淘汰记录
            List<QcExpertEliminateDO> eliminatedList = qcExpertEliminateService.list(eliminateParams);
            if (eliminatedList != null && !eliminatedList.isEmpty()) {
                for (QcExpertEliminateDO eliminate : eliminatedList) {
                    // 提取所有被淘汰的项目ID
                    if (eliminate.getProId() != null) {
                        eliminatedProIds.add(eliminate.getProId());
                    }
                }
            }
//            System.out.println("[淘汰过滤] 当前专家uid=" + uid + " 已淘汰项目数=" + eliminatedProIds.size() + ", 项目IDs=" + eliminatedProIds);
        }
        
        if (StringUtils.isNotBlank(taskId) && proDataDtoList != null && !proDataDtoList.isEmpty()) {
            List<QcProDataDto> filteredList = new ArrayList<>();
            // 2：过滤项目列表
            for (QcProDataDto dto : proDataDtoList) {
                if (dto.getProId() != null) {
                    // 【新增】过滤掉当前专家已淘汰的项目
                        // 如果项目ID在淘汰列表中，跳过不显示
                    if (eliminatedProIds.contains(dto.getProId())) {
                        System.out.println("[淘汰过滤] 过滤掉已淘汰项目: proId=" + dto.getProId() + ", topicName=" + dto.getTopicName());
                        continue; // 跳过已淘汰的项目
                    }
                    
                    boolean isAvoided = avoidanceService.checkAvoidance(taskId, dto.getProId(), checkExpertUserId);
                    dto.setIsAvoided(isAvoided);
                    
                    // 根据筛选条件过滤
                    if (StringUtils.isBlank(avoidanceFilter)) {
                        filteredList.add(dto); // 全选
                    } else if ("1".equals(avoidanceFilter) && isAvoided) {
                        filteredList.add(dto); // 已回避
                    } else if ("0".equals(avoidanceFilter) && !isAvoided) {
                        filteredList.add(dto); // 未回避
                    }
                }
            }
            proDataDtoList = filteredList;
            total = filteredList.size();
        }
        return new PageUtils(proDataDtoList, total);
    }

    /**
     * 新增：跳转到QC打分页面（带数据绑定）
     * 查询项目基本信息和已有打分记录，传递给前端
     */
    @RequestMapping("/toScore")
    public String toScorePage(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Long uid = getUserId();
        String proId = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";

        // 1. 获取项目基本信息
        if (StringUtils.isNotBlank(proId)) {
            Map<String, Object> proParams = new HashMap<>();
            proParams.put("proId", proId);
            proParams.put("taskId", taskId);
            List<QcProDataDto> proList = qcAwardService.listProInfo(proParams);
            if (proList != null && !proList.isEmpty()) {
                map.put("proInfo", proList.get(0));
            }
        }

        // 2. 查询当前专家对该项目的已有打分记录
        Map<String, Object> scoreParams = new HashMap<>();
        scoreParams.put("optUid", uid);
        scoreParams.put("proId", proId);
        scoreParams.put("taskId", taskId);
        List<QcAppraiseActiveScoreDO> scoreList = qcAppraiseActiveScoreService.list(scoreParams);
        if (scoreList != null && !scoreList.isEmpty()) {
            map.put("scoreInfo", scoreList.get(0));
        }
        map.put("proId", proId);
        map.put("expertUid", uid);
        return prefix + "/score/specialist_score";
    }

    /**
     * 新增：跳转到QC评审意见页面（带数据绑定）
     */
    @RequestMapping("/toOpinion")
    public String toOpinionPage(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Long uid = getUserId();
        String proId = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";

        // 1. 获取项目基本信息
        if (StringUtils.isNotBlank(proId)) {
            Map<String, Object> proParams = new HashMap<>();
            proParams.put("proId", proId);
            proParams.put("taskId", taskId);
            List<QcProDataDto> proList = qcAwardService.listProInfo(proParams);
            if (proList != null && !proList.isEmpty()) {
                map.put("proInfo", proList.get(0));
            }
        }

        // 2. 查询当前专家对该项目的已有打分记录（含评审意见）
        Map<String, Object> scoreParams = new HashMap<>();
        scoreParams.put("optUid", uid);
        scoreParams.put("proId", proId);
        scoreParams.put("taskId", taskId);
        List<QcAppraiseActiveScoreDO> scoreList = qcAppraiseActiveScoreService.list(scoreParams);
        if (scoreList != null && !scoreList.isEmpty()) {
            map.put("scoreInfo", scoreList.get(0));
        }
        map.put("proId", proId);
        map.put("expertUid", uid);
        return prefix + "/score/specialist_opinion";
    }

    /**
     * 新增：保存或更新QC专家打分
     */
    @RequestMapping("/saveScore")
    @ResponseBody
    public R saveScore(QcAppraiseActiveScoreDO score) {
        Long uid = getUserId();
        score.setOptUid(uid.intValue());

        // 检查是否已有该专家对该项目的打分记录
        Map<String, Object> checkParams = new HashMap<>();
        checkParams.put("optUid", uid);
        checkParams.put("proId", score.getProId());
        checkParams.put("taskId", score.getTaskId());
        List<QcAppraiseActiveScoreDO> existList = qcAppraiseActiveScoreService.list(checkParams);
        int tag;
        if (existList != null && !existList.isEmpty()) {
            // 已有记录，更新
            score.setId(existList.get(0).getId());
            tag = qcAppraiseActiveScoreService.update(score);
        } else {
            // 新增记录
            tag = qcAppraiseActiveScoreService.save(score);
        }
        if (tag > 0) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }

    /**
     * 新增：获取当前专家对某项目的打分数据（JSON）
     */
    @RequestMapping("/getScore")
    @ResponseBody
    public R getScore(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        params.put("optUid", uid);
        List<QcAppraiseActiveScoreDO> scoreList = qcAppraiseActiveScoreService.list(params);
        R r = R.ok();
        if (scoreList != null && !scoreList.isEmpty()) {
            r.put("scoreInfo", scoreList.get(0));
        }
        return r;
    }

    // 原代码：submitScore 仅按单个proId提交
    // @RequestMapping("/submitScore")
    // @ResponseBody
    // public R submitScore(@RequestParam Map<String, Object> params) {
    //     String proId = params.get("proId") != null ? params.get("proId").toString() : "";
    //     if (StringUtils.isBlank(proId)) {
    //         return R.error("项目ID不能为空");
    //     }
    //     Map<String, Object> updateParams = new HashMap<>();
    //     updateParams.put("proId", Integer.parseInt(proId));
    //     updateParams.put("proStat", "score_done");
    //     int tag = qcAwardService.updateProStat(updateParams);
    //     if (tag > 0) {
    //         return R.ok("提交成功");
    //     }
    //     return R.error("提交失败");
    // }
    // 新代码：参考科技奖 scoreOver，标记当前专家所有打分记录为已提交
    /**
     * 专家提交最终打分结果（参考科技奖 /specialist/scoreOver）
     * 将当前专家的所有打分记录标记为 scoreOver=1
     */
    @RequestMapping("/submitScore")
    @ResponseBody
    public R submitScore(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";

        // ===== 前置校验：所有非回避、非淘汰项目必须已评分 =====
        Map<String, Object> proQueryParams = new HashMap<>();
        proQueryParams.put("scoreSpecialistUid", String.valueOf(uid));
        if (StringUtils.isNotBlank(taskId)) {
            proQueryParams.put("taskId", taskId);
        }
        List<QcProDataDto> allProjects = qcAwardService.listProInfo(proQueryParams);
        if (allProjects != null && !allProjects.isEmpty()) {
            // 获取已淘汰项目ID集合
            Set<Integer> eliminatedProIds = new HashSet<>();
            if (StringUtils.isNotBlank(taskId)) {
                Map<String, Object> elimParams = new HashMap<>();
                elimParams.put("expertUid", uid);
                elimParams.put("taskId", taskId);
                elimParams.put("deleted", 0);
                List<QcExpertEliminateDO> eliminatedList = qcExpertEliminateService.list(elimParams);
                if (eliminatedList != null) {
                    for (QcExpertEliminateDO e : eliminatedList) {
                        if (e.getProId() != null) eliminatedProIds.add(e.getProId());
                    }
                }
            }
            // 检查每个非淘汰、非回避项目是否已评分
            List<String> unscoredProjects = new ArrayList<>();
            for (QcProDataDto pro : allProjects) {
                if (pro.getProId() == null) continue;
                if (eliminatedProIds.contains(pro.getProId())) continue;
                boolean isAvoided = avoidanceService.checkAvoidance(taskId, pro.getProId(), uid.intValue());
                if (isAvoided) continue;
                Map<String, Object> checkScore = new HashMap<>();
                checkScore.put("optUid", uid);
                checkScore.put("proId", pro.getProId());
                checkScore.put("taskId", taskId);
                List<QcResultSolveScoreDO> solveCheck = qcResultSolveScoreService.list(checkScore);
                List<QcResultInnovateScoreDO> innovateCheck = qcResultInnovateScoreService.list(checkScore);
                if ((solveCheck == null || solveCheck.isEmpty()) && (innovateCheck == null || innovateCheck.isEmpty())) {
                    String name = StringUtils.isNotBlank(pro.getTopicName()) ? pro.getTopicName() : ("项目ID:" + pro.getProId());
                    unscoredProjects.add(name);
                }
            }
            if (!unscoredProjects.isEmpty()) {
                return R.error("以下项目尚未评分，请完成所有项目评分后再提交：" + String.join("、", unscoredProjects));
            }
        }
        // ===== 前置校验结束 =====

        // 查找当前专家在当前任务下的打分记录
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        if (StringUtils.isNotBlank(taskId)) {
            queryParams.put("taskId", taskId);
        }
        List<QcResultSolveScoreDO> solveList = qcResultSolveScoreService.list(queryParams);
        List<QcResultInnovateScoreDO> innovateList = qcResultInnovateScoreService.list(queryParams);
        if ((solveList == null || solveList.isEmpty())
                && (innovateList == null || innovateList.isEmpty())) {
            return R.error("尚未进行任何打分，无法提交");
        }
        int updated = 0;
        if (solveList != null) {
            for (QcResultSolveScoreDO score : solveList) {
                score.setScoreOver(1);
                updated += qcResultSolveScoreService.update(score);
            }
        }
        if (innovateList != null) {
            for (QcResultInnovateScoreDO score : innovateList) {
                score.setScoreOver(1);
                updated += qcResultInnovateScoreService.update(score);
            }
        }
        if (updated > 0) {
            // 标记 score_over=1（参考 submitEliminate 设置 eliminate_over=1）
            try {
                Map<String, Object> expertQuery = new HashMap<>();
                expertQuery.put("userId", String.valueOf(uid));
                expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
                if (StringUtils.isNotBlank(taskId)) {
                    expertQuery.put("taskId", taskId);
                }
                List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
                if (!expertList.isEmpty()) {
                    ExpertGroupDO expert = expertList.get(0);
                    expert.setScoreOver(1);
                    expertGroupService.update(expert);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // ★ 写入提交快照（INSERT IGNORE 保证幂等，唯一键冲突自动跳过）
            try {
                Date now = new Date();
                if (solveList != null) {
                    for (QcResultSolveScoreDO s : solveList) {
                        if (s.getAppraiseSum() == null) continue;
                        BigDecimal total;
                        try { total = new BigDecimal(s.getAppraiseSum()); } catch (Exception ignore) { continue; }
                        QcScoreSubmitDO snap = new QcScoreSubmitDO();
                        snap.setTaskId(taskId);
                        snap.setProId(s.getProId());
                        snap.setExpertUid(uid.intValue());
                        snap.setTopicType("solve");
                        snap.setTotalScore(total);
                        snap.setPhase(1);
                        snap.setSourceId(s.getId());
                        snap.setSubmitTime(now);
                        qcScoreSubmitService.saveIgnore(snap);
                    }
                }
                if (innovateList != null) {
                    for (QcResultInnovateScoreDO s : innovateList) {
                        if (s.getAppraiseSum() == null) continue;
                        BigDecimal total;
                        try { total = new BigDecimal(s.getAppraiseSum()); } catch (Exception ignore) { continue; }
                        QcScoreSubmitDO snap = new QcScoreSubmitDO();
                        snap.setTaskId(taskId);
                        snap.setProId(s.getProId());
                        snap.setExpertUid(uid.intValue());
                        snap.setTopicType("innovate");
                        snap.setTotalScore(total);
                        snap.setPhase(1);
                        snap.setSourceId(s.getId());
                        snap.setSubmitTime(now);
                        qcScoreSubmitService.saveIgnore(snap);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 提交成功后，触发分数计算并落库（初评 phase=1，从 submit 表读取）
            try {
                qcScoreCalculationService.batchCalculateAndSave(taskId, 1, uid.intValue());
            } catch (Exception e) {
                // 计算失败不影响提交成功的结果
                e.printStackTrace();
            }
            return R.ok("提交成功，共提交" + updated + "条打分记录");
        }
        return R.error("提交失败");
    }

    /**
     * 新增：撤回打分提交（参考科技奖 /specialist/scoreCancel）
     * 将当前专家的所有打分记录标记为 scoreOver=0
     */
    @RequestMapping("/cancelSubmitScore")
    @ResponseBody
    public R cancelSubmitScore(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        queryParams.put("scoreOver", 1);
        if (StringUtils.isNotBlank(taskId)) {
            queryParams.put("taskId", taskId);
        }
        List<QcResultSolveScoreDO> solveList = qcResultSolveScoreService.list(queryParams);
        List<QcResultInnovateScoreDO> innovateList = qcResultInnovateScoreService.list(queryParams);
        if ((solveList == null || solveList.isEmpty())
                && (innovateList == null || innovateList.isEmpty())) {
            return R.error("没有已提交的打分记录");
        }
        int updated = 0;
        if (solveList != null) {
            for (QcResultSolveScoreDO score : solveList) {
                score.setScoreOver(0);
                updated += qcResultSolveScoreService.update(score);
            }
        }
        if (innovateList != null) {
            for (QcResultInnovateScoreDO score : innovateList) {
                score.setScoreOver(0);
                updated += qcResultInnovateScoreService.update(score);
            }
        }
        if (updated > 0) {
            // 标记 score_over=0
            try {
                Map<String, Object> expertQuery = new HashMap<>();
                expertQuery.put("userId", String.valueOf(uid));
                expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
                if (StringUtils.isNotBlank(taskId)) {
                    expertQuery.put("taskId", taskId);
                }
                List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
                if (!expertList.isEmpty()) {
                    ExpertGroupDO expert = expertList.get(0);
                    expert.setScoreOver(0);
                    expertGroupService.update(expert);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return R.ok("撤回成功");
        }
        return R.error("撤回失败");
    }

    /**
     * 管理员驳回专家打分提交，允许专家重新修改并再次提交
     * 将指定专家的所有打分记录及 add_special_info 中的 score_over 重置为 0
     */
    @RequestMapping("/rejectScore")
    @ResponseBody
    public R rejectScore(@RequestParam Map<String, Object> params) {
        String expertUidStr = params.get("expertUid") != null ? params.get("expertUid").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (expertUidStr.isEmpty() || taskId.isEmpty()) {
            return R.error("参数不完整");
        }
        // 潜在问题：未初始化就使用
        Long expertUid;
        try {
            expertUid = Long.parseLong(expertUidStr);
        } catch (NumberFormatException e) {
            return R.error("专家ID格式错误");
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", expertUid);
        queryParams.put("taskId", taskId);
        // 查询该专家的问题解决型和创新型评分记录
        List<QcResultSolveScoreDO> solveList = qcResultSolveScoreService.list(queryParams);
        List<QcResultInnovateScoreDO> innovateList = qcResultInnovateScoreService.list(queryParams);
        int updated = 0;
        if (solveList != null) {
            for (QcResultSolveScoreDO score : solveList) {
                score.setScoreOver(0);
                updated += qcResultSolveScoreService.update(score);
            }
        }
        if (innovateList != null) {
            for (QcResultInnovateScoreDO score : innovateList) {
                score.setScoreOver(0);
                updated += qcResultInnovateScoreService.update(score);
            }
        }
        try {
            Map<String, Object> expertQuery = new HashMap<>();
            expertQuery.put("userId", expertUidStr);
            expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            expertQuery.put("taskId", taskId);
            // 查询该专家在任务中的分组信息，用于更新整体打分状态
            List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
            if (!expertList.isEmpty()) {
                ExpertGroupDO expert = expertList.get(0);
                expert.setScoreOver(0);
                expertGroupService.update(expert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok("驳回成功，该专家可重新修改并提交打分");
    }

    /**
     * 管理员驳回专家发布分提交，允许专家重新修改并再次提交
     * 将指定专家的所有发布分记录及 add_special_info 中的 present_score_over 重置为 0
     */
    @RequestMapping("/rejectPresentScore")
    @ResponseBody
    public R rejectPresentScore(@RequestParam Map<String, Object> params) {
        String expertUidStr = params.get("expertUid") != null ? params.get("expertUid").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (expertUidStr.isEmpty() || taskId.isEmpty()) {
            return R.error("参数不完整");
        }
        int expertUid;
        try {
            expertUid = Integer.parseInt(expertUidStr);
        } catch (NumberFormatException e) {
            return R.error("专家ID格式错误");
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", expertUid);
        queryParams.put("taskId", taskId);
        List<QcPresentScoreDO> scoreList = qcPresentScoreService.list(queryParams);
        if (scoreList != null) {
            for (QcPresentScoreDO score : scoreList) {
                score.setScoreOver(0);
                score.setUpdated(new Date());
                qcPresentScoreService.update(score);
            }
        }
        try {
            Map<String, Object> expertQuery = new HashMap<>();
            expertQuery.put("userId", expertUidStr);
            expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            expertQuery.put("taskId", taskId);
            List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
            if (!expertList.isEmpty()) {
                ExpertGroupDO expert = expertList.get(0);
                expert.setPresentScoreOver(0);
                expertGroupService.update(expert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok("驳回成功，该专家可重新修改并提交发布分");
    }

    /**
     * 新增：专家查看形式审查结果（无权限注解，专家可直接访问）
     * 参考 QcProcessController.toReivew，但不要求 @RequiresPermissions
     */
    @RequestMapping("/viewCheckResult")
    public String viewCheckResult(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        String proId = params.get("proId") != null ? params.get("proId").toString() : "";

        // 获取项目基本信息
        Map<String, Object> proInfoParams = new HashMap<>();
        proInfoParams.put("proId", proId);
        List<QcProDataDto> qcProDataDtoList = qcAwardService.listProInfo(proInfoParams);
        map.put("qcProData", qcProDataDtoList.size() > 0 ? qcProDataDtoList.get(0) : new QcProDataDto());

        // 获取最近一次审核信息
        Map<String, Object> reviewParams = new HashMap<>();
        reviewParams.put("proId", proId);
        reviewParams.put("sort", "id");
        reviewParams.put("order", " desc");
        reviewParams.put("offset", 0);
        reviewParams.put("limit", 1);
        List<QcReviewResultRecordDO> reviewResultRecordDOList = qcReviewResultRecordService.list(reviewParams);
        QcReviewResultRecordDO reviewResultRecordDO = reviewResultRecordDOList.size() > 0 ? reviewResultRecordDOList.get(0) : new QcReviewResultRecordDO();
        map.put("reviewResult", reviewResultRecordDO);

        // 专家查看形审结果，设为只读
        map.put("readonly", "1");
        return prefix + "/check/qc_check_result_view";
    }

    // ==================== 淘汰功能 ====================

    /** 每个专家最多淘汰的项目数 */
    // private static final int MAX_ELIMINATE_COUNT = 9;

    // ==================== 原代码：淘汰会更改项目状态 ====================
    // /**
    //  * 淘汰项目
    //  * 将项目状态置为"已淘汰"，记录淘汰理由和淘汰前状态
    //  */
    // @RequestMapping("/eliminate")
    // @ResponseBody
    // public R eliminate(@RequestParam Map<String, Object> params) {
    //     Long uid = getUserId();
    //     String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
    //     String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
    //     String reason = params.get("reason") != null ? params.get("reason").toString() : "";
    //     if (StringUtils.isBlank(proIdStr)) {
    //         return R.error("项目ID不能为空");
    //     }
    //     if (StringUtils.isBlank(reason)) {
    //         return R.error("淘汰理由不能为空");
    //     }
    //     int proId = Integer.parseInt(proIdStr);
    //
    //     // 检查是否已达到淘汰上限
    //     Map<String, Object> countParams = new HashMap<>();
    //     countParams.put("expertUid", uid);
    //     countParams.put("taskId", taskId);
    //     countParams.put("deleted", 0);
    //     int currentCount = qcExpertEliminateService.count(countParams);
    //     if (currentCount >= MAX_ELIMINATE_COUNT) {
    //         return R.error("您最多只能淘汰" + MAX_ELIMINATE_COUNT + "个项目，当前已淘汰" + currentCount + "个");
    //     }
    //
    //     // 检查该项目是否已被该专家淘汰
    //     Map<String, Object> checkParams = new HashMap<>();
    //     checkParams.put("expertUid", uid);
    //     checkParams.put("proId", proId);
    //     checkParams.put("taskId", taskId);
    //     checkParams.put("deleted", 0);
    //     int exists = qcExpertEliminateService.count(checkParams);
    //     if (exists > 0) {
    //         return R.error("该项目已被淘汰，请勿重复操作");
    //     }
    //
    //     // 获取当前项目状态
    //     Map<String, Object> proParams = new HashMap<>();
    //     proParams.put("proId", proId);
    //     proParams.put("taskId", taskId);
    //     List<QcProDataDto> proList = qcAwardService.listProInfo(proParams);
    //     if (proList == null || proList.isEmpty()) {
    //         return R.error("项目不存在或已无法访问");
    //     }
    //     String prevProStat = "";
    //     if (proList.get(0).getProStat() != null) {
    //         prevProStat = proList.get(0).getProStat();
    //     }
    //     if (QcProStatEnum.ELIMINATED.getProStat().equals(prevProStat)) {
    //         return R.error("该项目当前已是淘汰状态");
    //     }
    //
    //     // 保存淘汰记录
    //     QcExpertEliminateDO eliminate = new QcExpertEliminateDO();
    //     eliminate.setExpertUid(uid);
    //     eliminate.setProId(proId);
    //     eliminate.setTaskId(taskId);
    //     eliminate.setReason(reason);
    //     eliminate.setPrevProStat(prevProStat);
    //     eliminate.setDeleted(0);
    //     qcExpertEliminateService.save(eliminate);
    //
    //     // 更新项目状态为"已淘汰"
    //     Map<String, Object> statParams = new HashMap<>();
    //     statParams.put("proId", proId);
    //     statParams.put("proStat", QcProStatEnum.ELIMINATED.getProStat());
    //     qcAwardService.updateProStat(statParams);
    //
    //     return R.ok("淘汰成功");
    // }

    // ==================== 新代码：淘汰仅记录，不更改项目状态 ====================
    /**
     * 淘汰项目（新版）
     * 仅保存淘汰记录，不更改项目状态
     * 淘汰仅在当前专家的页面上隐藏项目，不影响其他专家和管理员
     */
    @RequestMapping("/eliminate")
    @ResponseBody
    public R eliminate(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String reason = params.get("reason") != null ? params.get("reason").toString() : "";
        if (StringUtils.isBlank(proIdStr)) {
            return R.error("项目ID不能为空");
        }
        if (StringUtils.isBlank(reason)) {
            return R.error("淘汰理由不能为空");
        }
        int proId = Integer.parseInt(proIdStr);

        // 检查是否已达到淘汰上限
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("expertUid", uid);
        countParams.put("taskId", taskId);
        countParams.put("deleted", 0);
        int currentCount = qcExpertEliminateService.count(countParams);
        if (currentCount >= MAX_ELIMINATE_COUNT) {
            return R.error("您最多只能淘汰" + MAX_ELIMINATE_COUNT + "个项目，当前已淘汰" + currentCount + "个");
        }

        // 原代码：仅检查deleted=0的记录，撤销后再淘汰会新增重复数据
        // Map<String, Object> checkParams = new HashMap<>();
        // checkParams.put("expertUid", uid);
        // checkParams.put("proId", proId);
        // checkParams.put("taskId", taskId);
        // checkParams.put("deleted", 0);
        // int exists = qcExpertEliminateService.count(checkParams);
        // if (exists > 0) {
        //     return R.error("该项目已被淘汰，请勿重复操作");
        // }

        // 新代码：先检查是否存在有效的淘汰记录（deleted=0），防止重复淘汰
        Map<String, Object> activeCheckParams = new HashMap<>();
        activeCheckParams.put("expertUid", uid);
        activeCheckParams.put("proId", proId);
        activeCheckParams.put("taskId", taskId);
        activeCheckParams.put("deleted", 0);
        int activeExists = qcExpertEliminateService.count(activeCheckParams);
        if (activeExists > 0) {
            return R.error("该项目已被淘汰，请勿重复操作");
        }

        // 获取当前项目状态（仅用于记录，不再检查是否已淘汰）
        Map<String, Object> proParams = new HashMap<>();
        proParams.put("proId", proId);
        proParams.put("taskId", taskId);
        List<QcProDataDto> proList = qcAwardService.listProInfo(proParams);
        if (proList == null || proList.isEmpty()) {
            return R.error("项目不存在或已无法访问");
        }
        String prevProStat = "";
        if (proList.get(0).getProStat() != null) {
            prevProStat = proList.get(0).getProStat();
        }

        // 原代码：每次都新增记录，撤销后再淘汰会产生重复数据
        // QcExpertEliminateDO eliminate = new QcExpertEliminateDO();
        // eliminate.setExpertUid(uid);
        // eliminate.setProId(proId);
        // eliminate.setTaskId(taskId);
        // eliminate.setReason(reason);
        // eliminate.setPrevProStat(prevProStat);
        // eliminate.setDeleted(0);
        // qcExpertEliminateService.save(eliminate);

        // 新代码：查找该专家+项目+任务的已撤销记录（deleted=1），存在则更新，不存在才新增
        Map<String, Object> cancelledCheckParams = new HashMap<>();
        cancelledCheckParams.put("expertUid", uid);
        cancelledCheckParams.put("proId", proId);
        cancelledCheckParams.put("taskId", taskId);
        cancelledCheckParams.put("deleted", 1);
        List<QcExpertEliminateDO> cancelledList = qcExpertEliminateService.list(cancelledCheckParams);
        if (cancelledList != null && !cancelledList.isEmpty()) {
            // 存在已撤销的记录，更新它而非新增
            QcExpertEliminateDO existRecord = cancelledList.get(0);
            existRecord.setReason(reason);
            existRecord.setPrevProStat(prevProStat);
            existRecord.setDeleted(0);
            existRecord.setCreated(new java.util.Date());
            qcExpertEliminateService.update(existRecord);
        } else {
            // 不存在任何记录，新增
            QcExpertEliminateDO eliminate = new QcExpertEliminateDO();
            eliminate.setExpertUid(uid);
            eliminate.setProId(proId);
            eliminate.setTaskId(taskId);
            eliminate.setReason(reason);
            eliminate.setPrevProStat(prevProStat);
            eliminate.setDeleted(0);
            qcExpertEliminateService.save(eliminate);
        }

        // 【修改】不再更新项目状态，淘汰仅在专家端页面过滤显示
        // Map<String, Object> statParams = new HashMap<>();
        // statParams.put("proId", proId);
        // statParams.put("proStat", QcProStatEnum.ELIMINATED.getProStat());
        // qcAwardService.updateProStat(statParams);

        return R.ok("淘汰成功");
    }

    /**
     * 获取淘汰项目列表
     * 用于管理员查看某任务下所有被淘汰的项目
     */
    // 原代码：不支持按专业组过滤
    // @RequestMapping("/getEliminatedProjects")
    // @ResponseBody
    // public R getEliminatedProjects(@RequestParam String taskId,
    //                                @RequestParam(value = "onlyMine", defaultValue = "0") String onlyMine) {
    //     if (StringUtils.isBlank(taskId)) {
    //         return R.error("任务ID不能为空");
    //     }
    //     Map<String, Object> params = new HashMap<>();
    //     params.put("taskId", taskId);
    //     params.put("deleted", 0);
    //     if ("1".equals(onlyMine)) {
    //         params.put("expertUid", getUserId());
    //     }
    //     List<QcExpertEliminateDO> list = qcExpertEliminateService.list(params);
    //     return R.ok().put("list", list);
    // }

    // 新代码：增加qcGroupName参数，支持按专业组过滤淘汰记录
    @RequestMapping("/getEliminatedProjects")
    @ResponseBody
    public R getEliminatedProjects(@RequestParam String taskId,
                                   @RequestParam(value = "onlyMine", defaultValue = "0") String onlyMine,
                                   @RequestParam(value = "qcGroupName", required = false) String qcGroupName) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("deleted", 0);
        if ("1".equals(onlyMine)) {
            params.put("expertUid", getUserId());
        }
        if (StringUtils.isNotBlank(qcGroupName)) {
            params.put("qcGroupName", qcGroupName);
        }
        
        List<QcExpertEliminateDO> list = qcExpertEliminateService.list(params);
        
        return R.ok().put("list", list);
    }

    /**
     * 获取首次确认提交的淘汰项目列表（快照数据）
     * 用于管理员/外聘人员导出淘汰名单，数据来自快照表，不受后续更新分组/重新淘汰影响
     * 如果快照表无数据（线上还未确认提交过），则降级读取当前有效淘汰记录
     */
    @RequestMapping("/getConfirmedEliminatedProjects")
    @ResponseBody
    public R getConfirmedEliminatedProjects(@RequestParam String taskId,
                                             @RequestParam(value = "qcGroupName", required = false) String qcGroupName) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }

        // 先查快照表
        Map<String, Object> confirmedParams = new HashMap<>();
        confirmedParams.put("taskId", taskId);
        if (StringUtils.isNotBlank(qcGroupName)) {
            confirmedParams.put("qcGroupName", qcGroupName);
        }
        List<QcExpertEliminateConfirmedDO> confirmedList = qcExpertEliminateConfirmedService.list(confirmedParams);

        if (confirmedList != null && !confirmedList.isEmpty()) {
            // 快照表有数据，返回快照数据
            return R.ok().put("list", confirmedList).put("source", "confirmed");
        }

        // 快照表无数据，降级读取当前有效淘汰记录（兼容线上还未确认提交的场景）
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("deleted", 0);
        if (StringUtils.isNotBlank(qcGroupName)) {
            params.put("qcGroupName", qcGroupName);
        }
        List<QcExpertEliminateDO> list = qcExpertEliminateService.list(params);
        return R.ok().put("list", list).put("source", "current");
    }

    // ==================== 原代码：撤销淘汰会恢复项目状态 ====================
    // /**
    //  * 撤销淘汰
    //  * 恢复项目淘汰前的状态
    //  */
    // @RequestMapping("/cancelEliminate")
    // @ResponseBody
    // public R cancelEliminate(@RequestParam Map<String, Object> params) {
    //     Long uid = getUserId();
    //     String idStr = params.get("id") != null ? params.get("id").toString() : "";
    //     if (StringUtils.isBlank(idStr)) {
    //         return R.error("记录ID不能为空");
    //     }
    //     int id = Integer.parseInt(idStr);
    //     QcExpertEliminateDO record = qcExpertEliminateService.get(id);
    //     if (record == null) {
    //         return R.error("淘汰记录不存在");
    //     }
    //     if (!uid.equals(record.getExpertUid())) {
    //         return R.error("无权操作他人的淘汰记录");
    //     }
    //     if (record.getDeleted() != null && record.getDeleted() == 1) {
    //         return R.error("该记录已被撤销");
    //     }
    //
    //     // 标记淘汰记录为已撤销
    //     QcExpertEliminateDO updateDO = new QcExpertEliminateDO();
    //     updateDO.setId(id);
    //     updateDO.setDeleted(1);
    //     qcExpertEliminateService.update(updateDO);
    //
    //     // 恢复项目状态
    //     String prevStat = StringUtils.isNotBlank(record.getPrevProStat()) ? record.getPrevProStat() : QcProStatEnum.SCIENCE_ASSIGN_EXPERTS.getProStat();
    //     if (QcProStatEnum.ELIMINATED.getProStat().equals(prevStat)) {
    //         prevStat = QcProStatEnum.SCIENCE_ASSIGN_EXPERTS.getProStat();
    //     }
    //     Map<String, Object> statParams = new HashMap<>();
    //     statParams.put("proId", record.getProId());
    //     statParams.put("proStat", prevStat);
    //     qcAwardService.updateProStat(statParams);
    //
    //     return R.ok("撤销淘汰成功");
    // }

    // ==================== 新代码：撤销淘汰仅删除记录，不恢复项目状态 ====================
    /**
     * 撤销淘汰（新版）
     * 仅标记淘汰记录为已删除，不恢复项目状态
     * 撤销后该专家可以重新看到该项目
     */
    @RequestMapping("/cancelEliminate")
    @ResponseBody
    public R cancelEliminate(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String idStr = params.get("id") != null ? params.get("id").toString() : "";
        if (StringUtils.isBlank(idStr)) {
            return R.error("记录ID不能为空");
        }
        int id = Integer.parseInt(idStr);
        QcExpertEliminateDO record = qcExpertEliminateService.get(id);
        if (record == null) {
            return R.error("淘汰记录不存在");
        }
        if (!uid.equals(record.getExpertUid())) {
            return R.error("无权操作他人的淘汰记录");
        }
        if (record.getDeleted() != null && record.getDeleted() == 1) {
            return R.error("该记录已被撤销");
        }

        // 标记淘汰记录为已撤销
        QcExpertEliminateDO updateDO = new QcExpertEliminateDO();
        updateDO.setId(id);
        updateDO.setDeleted(1);
        qcExpertEliminateService.update(updateDO);

        // 【修改】不再恢复项目状态，因为淘汰时就没有修改项目状态
        // String prevStat = StringUtils.isNotBlank(record.getPrevProStat()) ? record.getPrevProStat() : QcProStatEnum.SCIENCE_ASSIGN_EXPERTS.getProStat();
        // if (QcProStatEnum.ELIMINATED.getProStat().equals(prevStat)) {
        //     prevStat = QcProStatEnum.SCIENCE_ASSIGN_EXPERTS.getProStat();
        // }
        // Map<String, Object> statParams = new HashMap<>();
        // statParams.put("proId", record.getProId());
        // statParams.put("proStat", prevStat);
        // qcAwardService.updateProStat(statParams);

        return R.ok("撤销淘汰成功");
    }

    /**
     * 获取当前专家的淘汰名单
     */
    @RequestMapping("/getEliminateList")
    @ResponseBody
    public PageUtils getEliminateList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Long uid = getUserId();
        params.put("expertUid", uid);
        params.put("deleted", 0);

        List<QcExpertEliminateDO> list = qcExpertEliminateService.list(params);
        int total = qcExpertEliminateService.count(params);
        return new PageUtils(list, total);
    }

    /**
     * 获取当前专家的有效淘汰数量
     */
    @RequestMapping("/getEliminateCount")
    @ResponseBody
    public R getEliminateCount(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("expertUid", uid);
        countParams.put("taskId", taskId);
        countParams.put("deleted", 0);
        int count = qcExpertEliminateService.count(countParams);
        R r = R.ok();
        r.put("count", count);
        r.put("max", MAX_ELIMINATE_COUNT);
        return r;
    }

    // 原代码：确认提交时仅更新eliminate_over状态，不保存快照
    // /**
    //  * 确认提交淘汰名单
    //  * 校验淘汰数量是否满足要求（>=MAX_ELIMINATE_COUNT），满足则标记 eliminate_over=1
    //  */
    // @RequestMapping("/submitEliminate")
    // @ResponseBody
    // public R submitEliminate(@RequestParam Map<String, Object> params) {
    //     Long uid = getUserId();
    //     String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
    //     
    //     // 校验淘汰数量
    //     Map<String, Object> countParams = new HashMap<>();
    //     countParams.put("expertUid", uid);
    //     countParams.put("taskId", taskId);
    //     countParams.put("deleted", 0);
    //     int count = qcExpertEliminateService.count(countParams);
    //     if (count < MAX_ELIMINATE_COUNT) {
    //         return R.error("淘汰数量不足，需要淘汰" + MAX_ELIMINATE_COUNT + "个项目，当前已淘汰" + count + "个");
    //     }
    //     
    //     // 查找当前专家的 add_special_info 记录并更新 eliminate_over=1
    //     Map<String, Object> expertQuery = new HashMap<>();
    //     expertQuery.put("userId", String.valueOf(uid));
    //     expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
    //     if (StringUtils.isNotBlank(taskId)) {
    //         expertQuery.put("taskId", taskId);
    //     }
    //     List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
    //     if (expertList.isEmpty()) {
    //         return R.error("未找到专家分配记录");
    //     }
    //     ExpertGroupDO expert = expertList.get(0);
    //     expert.setEliminateOver(1);
    //     int result = expertGroupService.update(expert);
    //     if (result > 0) {
    //         return R.ok("淘汰名单确认提交成功");
    //     }
    //     return R.error("提交失败");
    // }

    /**
     * 新代码：确认提交淘汰名单
     * 1. 校验淘汰数量是否满足要求
     * 2. 如果快照表中该专家+任务没有记录（首次确认），则将当前有效淘汰记录复制到快照表
     * 3. 标记 eliminate_over=1
     */
    @RequestMapping("/submitEliminate")
    @ResponseBody
    public R submitEliminate(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        
        // 校验淘汰数量
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("expertUid", uid);
        countParams.put("taskId", taskId);
        countParams.put("deleted", 0);
        int count = qcExpertEliminateService.count(countParams);
        if (count < MAX_ELIMINATE_COUNT) {
            return R.error("淘汰数量不足，需要淘汰" + MAX_ELIMINATE_COUNT + "个项目，当前已淘汰" + count + "个");
        }
        
        // 首次确认提交时，将当前有效淘汰记录保存到快照表（后续更新分组/重新淘汰不影响快照）
        Map<String, Object> confirmedCountParams = new HashMap<>();
        confirmedCountParams.put("expertUid", uid);
        confirmedCountParams.put("taskId", taskId);
        int confirmedCount = qcExpertEliminateConfirmedService.count(confirmedCountParams);
        if (confirmedCount == 0) {
            // 快照表中无数据，说明是首次确认，批量复制
            qcExpertEliminateConfirmedService.batchSaveFromEliminate(uid, taskId);
        }
        
        // 查找当前专家的 add_special_info 记录并更新 eliminate_over=1
        Map<String, Object> expertQuery = new HashMap<>();
        expertQuery.put("userId", String.valueOf(uid));
        expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
        if (StringUtils.isNotBlank(taskId)) {
            expertQuery.put("taskId", taskId);
        }
        List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
        if (expertList.isEmpty()) {
            return R.error("未找到专家分配记录");
        }
        ExpertGroupDO expert = expertList.get(0);

        // 提交前先把当前专家当前任务的快照清空，再写入最新淘汰名单到 ass_qc_expert_eliminate_qr
        qcExpertEliminateService.deleteQrByExpertAndTask(uid, taskId);
        qcExpertEliminateService.saveCurrentEliminateListToQr(uid, taskId);

        expert.setEliminateOver(1);
        int result = expertGroupService.update(expert);
        if (result > 0) {
            return R.ok("淘汰名单确认提交成功");
        }
        return R.error("提交失败");
    }

    /**
     * 撤回淘汰名单确认提交
     */
    @RequestMapping("/cancelSubmitEliminate")
    @ResponseBody
    public R cancelSubmitEliminate(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        
        Map<String, Object> expertQuery = new HashMap<>();
        expertQuery.put("userId", String.valueOf(uid));
        expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
        if (StringUtils.isNotBlank(taskId)) {
            expertQuery.put("taskId", taskId);
        }
        List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
        if (expertList.isEmpty()) {
            return R.error("未找到专家分配记录");
        }
        ExpertGroupDO expert = expertList.get(0);
        expert.setEliminateOver(0);
        int result = expertGroupService.update(expert);
        if (result > 0) {
            return R.ok("已撤回淘汰名单提交");
        }
        return R.error("撤回失败");
    }

    /**
     * 跳转到数据修复页面
     * 注意：仅用于一次性数据修复，使用后请删除
     */

    @RequestMapping("/toRepairPage")
    public String toRepairPage() {
        return prefix + "/repair_eliminated_projects";
    }

    /**
     * 数据修复：恢复所有已淘汰项目的状态
     * 用于修复旧版淘汰功能造成的项目状态变更
     * 注意：此方法仅用于数据修复，执行后请注释掉或删除
     */
    @RequestMapping("/repairEliminatedProjects")
    @ResponseBody
    public R repairEliminatedProjects(@RequestParam(required = false) String taskId) {
        try {
            // 查询所有淘汰记录
            Map<String, Object> params = new HashMap<>();
            if (StringUtils.isNotBlank(taskId)) {
                params.put("taskId", taskId);
            }
            // 不限制deleted状态，包括已撤销的记录
            List<QcExpertEliminateDO> eliminateList = qcExpertEliminateService.list(params);

            if (eliminateList == null || eliminateList.isEmpty()) {
                return R.ok("没有找到需要修复的淘汰记录");
            }

            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            StringBuilder log = new StringBuilder();
            log.append("修复日志：\n");

            // 按项目ID去重，避免同一个项目被多次修复
            Map<Integer, QcExpertEliminateDO> proIdMap = new HashMap<>();
            for (QcExpertEliminateDO eliminate : eliminateList) {
                if (eliminate.getProId() != null) {
                    // 保留最早的淘汰记录（ID最小的）
                    if (!proIdMap.containsKey(eliminate.getProId()) ||
                        eliminate.getId() < proIdMap.get(eliminate.getProId()).getId()) {
                        proIdMap.put(eliminate.getProId(), eliminate);
                    }
                }
            }

            log.append("找到 ").append(eliminateList.size()).append(" 条淘汰记录，涉及 ")
               .append(proIdMap.size()).append(" 个项目\n\n");

            // 遍历每个项目进行修复
            for (Map.Entry<Integer, QcExpertEliminateDO> entry : proIdMap.entrySet()) {
                Integer proId = entry.getKey();
                QcExpertEliminateDO eliminate = entry.getValue();

                try {
                    // 查询项目当前状态
                    Map<String, Object> proParams = new HashMap<>();
                    proParams.put("proId", proId);
                    if (StringUtils.isNotBlank(eliminate.getTaskId())) {
                        proParams.put("taskId", eliminate.getTaskId());
                    }
                    List<QcProDataDto> proList = qcAwardService.listProInfo(proParams);

                    if (proList == null || proList.isEmpty()) {
                        log.append("[跳过] 项目ID=").append(proId).append(" 不存在\n");
                        skipCount++;
                        continue;
                    }

                    QcProDataDto project = proList.get(0);
                    String currentStat = project.getProStat();

                    // 只修复状态为"已淘汰"的项目
                    if (!QcProStatEnum.ELIMINATED.getProStat().equals(currentStat)) {
                        log.append("[跳过] 项目ID=").append(proId)
                           .append(" 当前状态=").append(currentStat)
                           .append(" (非已淘汰状态)\n");
                        skipCount++;
                        continue;
                    }

                    // 获取淘汰前的状态
                    String prevStat = eliminate.getPrevProStat();
                    if (StringUtils.isBlank(prevStat)) {
                        // 如果没有记录淘汰前状态，默认恢复为"专家打分"
                        prevStat = QcProStatEnum.SCIENCE_ASSIGN_EXPERTS.getProStat();
                        log.append("[警告] 项目ID=").append(proId)
                           .append(" 没有记录淘汰前状态，默认恢复为：").append(prevStat).append("\n");
                    }

                    // 恢复项目状态
                    Map<String, Object> updateParams = new HashMap<>();
                    updateParams.put("proId", proId);
                    updateParams.put("proStat", prevStat);
                    int result = qcAwardService.updateProStat(updateParams);

                    if (result > 0) {
                        log.append("[成功] 项目ID=").append(proId)
                           .append(" 课题名称=").append(project.getTopicName())
                           .append(" 从【已淘汰】恢复为【").append(prevStat).append("】\n");
                        successCount++;
                    } else {
                        log.append("[失败] 项目ID=").append(proId).append(" 更新失败\n");
                        errorCount++;
                    }

                } catch (Exception e) {
                    log.append("[异常] 项目ID=").append(proId)
                       .append(" 错误信息=").append(e.getMessage()).append("\n");
                    errorCount++;
                }
            }

            log.append("\n修复完成！\n");
            log.append("成功：").append(successCount).append(" 个\n");
            log.append("跳过：").append(skipCount).append(" 个\n");
            log.append("失败：").append(errorCount).append(" 个\n");

            System.out.println(log.toString());

            return R.ok(log.toString())
                    .put("successCount", successCount)
                    .put("skipCount", skipCount)
                    .put("errorCount", errorCount);

        } catch (Exception e) {
            e.printStackTrace();
            return R.error("修复失败：" + e.getMessage());
        }
    }

    // ==================== 成果打分（按课题类型） ====================

    @RequestMapping("/getSolveResultScore")
    @ResponseBody
    public R getSolveResultScore(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(proIdStr)) {
            return R.error("项目ID不能为空");
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        queryParams.put("proId", Integer.parseInt(proIdStr));
        queryParams.put("taskId", taskId);
        List<QcResultSolveScoreDO> list = qcResultSolveScoreService.list(queryParams);
        R r = R.ok();
        r.put("score", (list != null && !list.isEmpty()) ? list.get(0) : new QcResultSolveScoreDO());
        return r;
    }

    @RequestMapping("/saveSolveResultScore")
    @ResponseBody
    public R saveSolveResultScore(QcResultSolveScoreDO score) {
        Long uid = getUserId();
        score.setOptUid(uid.intValue());
        if (score.getProId() == null || StringUtils.isBlank(score.getTaskId())) {
            return R.error("项目信息缺失，无法保存");
        }
        score.setUpdated(new Date());
        if (score.getDeleted() == null) {
            score.setDeleted(0);
        }

        // BigDecimal total = BigDecimal.ZERO;
        // total = total.add(score.getSelSocre() == null ? BigDecimal.ZERO : score.getSelSocre());
        // total = total.add(score.getReasonScore() == null ? BigDecimal.ZERO : score.getReasonScore());
        // total = total.add(score.getStrategyExecuteScore() == null ? BigDecimal.ZERO : score.getStrategyExecuteScore());
        // total = total.add(score.getEffectScore() == null ? BigDecimal.ZERO : score.getEffectScore());
        // total = total.add(score.getReportScore() == null ? BigDecimal.ZERO : score.getReportScore());
        // total = total.add(score.getCharacteristicScore() == null ? BigDecimal.ZERO : score.getCharacteristicScore());
        // score.setAppraiseSum(total.toPlainString());

        BigDecimal total = BigDecimal.ZERO;
        total = total.add(score.getSelSocre() == null ? BigDecimal.ZERO : score.getSelSocre());
        total = total.add(score.getReasonScore() == null ? BigDecimal.ZERO : score.getReasonScore());
        total = total.add(score.getStrategyExecuteScore() == null ? BigDecimal.ZERO : score.getStrategyExecuteScore());
        total = total.add(score.getEffectScore() == null ? BigDecimal.ZERO : score.getEffectScore());
        total = total.add(score.getReportScore() == null ? BigDecimal.ZERO : score.getReportScore());
        total = total.add(score.getCharacteristicScore() == null ? BigDecimal.ZERO : score.getCharacteristicScore());
        total = total.add(score.getCertificateScore() == null ? BigDecimal.ZERO : score.getCertificateScore());
        total = total.add(score.getPracticalValueScore() == null ? BigDecimal.ZERO : score.getPracticalValueScore());
        score.setAppraiseSum(total.toPlainString());

        Map<String, Object> checkParams = new HashMap<>();
        checkParams.put("optUid", uid);
        checkParams.put("proId", score.getProId());
        checkParams.put("taskId", score.getTaskId());
        List<QcResultSolveScoreDO> existList = qcResultSolveScoreService.list(checkParams);
        int tag;
        if (existList != null && !existList.isEmpty()) {
            QcResultSolveScoreDO existing = existList.get(0);
            if (Integer.valueOf(1).equals(existing.getScoreOver())) {
                return R.error("已提交打分，不可再修改。如需修改请先撤回提交。");
            }
            score.setId(existing.getId());
            // 原代码：直接更新，sumRecommend默认值""会覆盖已保存的主评意见
            // tag = qcResultSolveScoreService.update(score);
            // 修复：保存分数时保留已有的主评意见和推荐等级，防止评价意见被清空
            score.setSumRecommend(existing.getSumRecommend());
            score.setRecommendLevel(existing.getRecommendLevel());
            tag = qcResultSolveScoreService.update(score);
        } else {
            tag = qcResultSolveScoreService.save(score);
        }
        if (tag > 0) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }

    @RequestMapping("/getInnovateResultScore")
    @ResponseBody
    public R getInnovateResultScore(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(proIdStr)) {
            return R.error("项目ID不能为空");
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        queryParams.put("proId", Integer.parseInt(proIdStr));
        queryParams.put("taskId", taskId);
        List<QcResultInnovateScoreDO> list = qcResultInnovateScoreService.list(queryParams);
        R r = R.ok();
        r.put("score", (list != null && !list.isEmpty()) ? list.get(0) : new QcResultInnovateScoreDO());
        return r;
    }

    @RequestMapping("/saveInnovateResultScore")
    @ResponseBody
    public R saveInnovateResultScore(QcResultInnovateScoreDO score) {
        Long uid = getUserId();
        score.setOptUid(uid.intValue());
        if (score.getProId() == null || StringUtils.isBlank(score.getTaskId())) {
            return R.error("项目信息缺失，无法保存");
        }
        score.setUpdated(new Date());
        if (score.getDeleted() == null) {
            score.setDeleted(0);
        }

        // BigDecimal total = BigDecimal.ZERO;
        // total = total.add(score.getSelSocre() == null ? BigDecimal.ZERO : score.getSelSocre());
        // total = total.add(score.getBestPlanScore() == null ? BigDecimal.ZERO : score.getBestPlanScore());
        // total = total.add(score.getStrategyExecuteScore() == null ? BigDecimal.ZERO : score.getStrategyExecuteScore());
        // total = total.add(score.getEffectScore() == null ? BigDecimal.ZERO : score.getEffectScore());
        // total = total.add(score.getReportScore() == null ? BigDecimal.ZERO : score.getReportScore());
        // total = total.add(score.getCharacteristicScore() == null ? BigDecimal.ZERO : score.getCharacteristicScore());
        // score.setAppraiseSum(total.toPlainString());

        BigDecimal total = BigDecimal.ZERO;
        total = total.add(score.getSelSocre() == null ? BigDecimal.ZERO : score.getSelSocre());
        total = total.add(score.getBestPlanScore() == null ? BigDecimal.ZERO : score.getBestPlanScore());
        total = total.add(score.getStrategyExecuteScore() == null ? BigDecimal.ZERO : score.getStrategyExecuteScore());
        total = total.add(score.getEffectScore() == null ? BigDecimal.ZERO : score.getEffectScore());
        total = total.add(score.getReportScore() == null ? BigDecimal.ZERO : score.getReportScore());
        total = total.add(score.getCharacteristicScore() == null ? BigDecimal.ZERO : score.getCharacteristicScore());

        total = total.add(score.getCertificateScore() == null ? BigDecimal.ZERO : score.getCertificateScore());
        
        total = total.add(score.getPracticalValueScore() == null ? BigDecimal.ZERO : score.getPracticalValueScore());
        
        score.setAppraiseSum(total.toPlainString());

        Map<String, Object> checkParams = new HashMap<>();
        checkParams.put("optUid", uid);
        checkParams.put("proId", score.getProId());
        checkParams.put("taskId", score.getTaskId());
        List<QcResultInnovateScoreDO> existList = qcResultInnovateScoreService.list(checkParams);
        int tag;
        if (existList != null && !existList.isEmpty()) {
            QcResultInnovateScoreDO existing = existList.get(0);
            if (Integer.valueOf(1).equals(existing.getScoreOver())) {
                return R.error("已提交打分，不可再修改。如需修改请先撤回提交。");
            }
            score.setId(existing.getId());
            // 原代码：直接更新，sumRecommend默认值""会覆盖已保存的主评意见
            // tag = qcResultInnovateScoreService.update(score);
            // 修复：保存分数时保留已有的主评意见和推荐等级，防止评价意见被清空
            score.setSumRecommend(existing.getSumRecommend());
            score.setRecommendLevel(existing.getRecommendLevel());
            tag = qcResultInnovateScoreService.update(score);
        } else {
            tag = qcResultInnovateScoreService.save(score);
        }
        if (tag > 0) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }

    /**
     * 获取主屏意见（推荐意见等级 + 评价意见）
     */
    @RequestMapping("/getRecommend")
    @ResponseBody
    public R getRecommend(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String topicType = params.get("topicType") != null ? params.get("topicType").toString() : "";
        if (StringUtils.isBlank(proIdStr) || StringUtils.isBlank(taskId)) {
            return R.error("参数缺失");
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        queryParams.put("proId", Integer.parseInt(proIdStr));
        queryParams.put("taskId", taskId);

        R r = R.ok();
        if ("创新型".equals(topicType)) {
            List<QcResultInnovateScoreDO> list = qcResultInnovateScoreService.list(queryParams);
            if (list != null && !list.isEmpty()) {
                r.put("recommendLevel", list.get(0).getRecommendLevel());
                r.put("sumRecommend", list.get(0).getSumRecommend());
            }
        } else {
            List<QcResultSolveScoreDO> list = qcResultSolveScoreService.list(queryParams);
            if (list != null && !list.isEmpty()) {
                r.put("recommendLevel", list.get(0).getRecommendLevel());
                r.put("sumRecommend", list.get(0).getSumRecommend());
            }
        }
        return r;
    }

    /**
     * 保存主屏意见（仅更新推荐意见等级 + 评价意见，不影响评分数据）
     */
    @RequestMapping("/saveRecommend")
    @ResponseBody
    public R saveRecommend(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String topicType = params.get("topicType") != null ? params.get("topicType").toString() : "";
        String recommendLevel = params.get("recommendLevel") != null ? params.get("recommendLevel").toString() : "";
        String sumRecommend = params.get("sumRecommend") != null ? params.get("sumRecommend").toString() : "";

        if (StringUtils.isBlank(proIdStr) || StringUtils.isBlank(taskId)) {
            return R.error("参数缺失");
        }

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        queryParams.put("proId", Integer.parseInt(proIdStr));
        queryParams.put("taskId", taskId);

        int tag;
        if ("创新型".equals(topicType)) {
            List<QcResultInnovateScoreDO> list = qcResultInnovateScoreService.list(queryParams);
            if (list != null && !list.isEmpty()) {
                QcResultInnovateScoreDO existing = list.get(0);
                existing.setRecommendLevel(recommendLevel);
                existing.setSumRecommend(sumRecommend);
                existing.setUpdated(new Date());
                tag = qcResultInnovateScoreService.update(existing);
            } else {
                QcResultInnovateScoreDO newScore = new QcResultInnovateScoreDO();
                newScore.setOptUid(uid.intValue());
                newScore.setProId(Integer.parseInt(proIdStr));
                newScore.setTaskId(taskId);
                newScore.setRecommendLevel(recommendLevel);
                newScore.setSumRecommend(sumRecommend);
                newScore.setDeleted(0);
                tag = qcResultInnovateScoreService.save(newScore);
            }
        } else {
            List<QcResultSolveScoreDO> list = qcResultSolveScoreService.list(queryParams);
            if (list != null && !list.isEmpty()) {
                QcResultSolveScoreDO existing = list.get(0);
                existing.setRecommendLevel(recommendLevel);
                existing.setSumRecommend(sumRecommend);
                existing.setUpdated(new Date());
                tag = qcResultSolveScoreService.update(existing);
            } else {
                QcResultSolveScoreDO newScore = new QcResultSolveScoreDO();
                newScore.setOptUid(uid.intValue());
                newScore.setProId(Integer.parseInt(proIdStr));
                newScore.setTaskId(taskId);
                newScore.setRecommendLevel(recommendLevel);
                newScore.setSumRecommend(sumRecommend);
                newScore.setDeleted(0);
                tag = qcResultSolveScoreService.save(newScore);
            }
        }
        if (tag > 0) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }

    /**
     * 查询项目平均分（排除回避专家，去最高最低分）
     */
    @RequestMapping("/getAverageScore")
    @ResponseBody
    public R getAverageScore(@RequestParam Map<String, Object> params) {
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String phaseStr = params.get("phase") != null ? params.get("phase").toString() : "";
        String withDetailStr = params.get("withDetail") != null ? params.get("withDetail").toString() : "";

        if (StringUtils.isBlank(proIdStr) || StringUtils.isBlank(taskId)) {
            return R.error("参数错误");
        }

        try {
            Integer proId = Integer.parseInt(proIdStr);
            Integer phase = StringUtils.isNotBlank(phaseStr) ? Integer.parseInt(phaseStr) : 1;

            Map<String, Object> q = new HashMap<>();
            q.put("taskId", taskId);
            q.put("proId", proId);
            q.put("phase", phase);
            q.put("deleted", 0);
            List<com.bootdo.cpe.domain.QcScoreCalcResultDO> list = qcScoreCalcResultService.list(q);
            if (list == null || list.isEmpty()) {
                return R.ok().put("averageScore", null).put("validCount", 0).put("usedCount", 0)
                        .put("message", "暂无计算结果");
            }

            // 默认返回最新一条（按id desc），同时保留历史列表
            com.bootdo.cpe.domain.QcScoreCalcResultDO latest = list.get(0);
            R r = R.ok();
            r.put("averageScore", latest.getAvgScore());
            r.put("validCount", latest.getValidCount());
            r.put("usedCount", latest.getUsedCount());
            r.put("resultId", latest.getId());
            r.put("phase", phase);
            r.put("history", list);

            if ("1".equals(withDetailStr) && latest.getId() != null) {
                Map<String, Object> dq = new HashMap<>();
                dq.put("resultId", latest.getId());
                List<com.bootdo.cpe.domain.QcScoreCalcDetailDO> details = qcScoreCalcDetailService.list(dq);
                r.put("details", details);
            }
            return r;
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查询某个专家的打分情况（以专家为维度）
     * 以"分配给该专家的项目"为基准，展示每个项目的详细评分项及回避状态
     * @param params taskId, expertUid
     */
    @GetMapping("/getExpertScores")
    @ResponseBody
    public R getExpertScores(@RequestParam Map<String, Object> params) {
        try {
            String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
            String expertUidStr = params.get("expertUid") != null ? params.get("expertUid").toString() : "";

            if (taskId.isEmpty() || expertUidStr.isEmpty()) {
                return R.error("参数不完整");
            }

            Integer expertUid;
            try {
                expertUid = Integer.parseInt(expertUidStr);
            } catch (NumberFormatException e) {
                return R.error("专家ID格式错误");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 1. 查询分配给该专家的所有项目（通过 add_special_info 关联）
            Map<String, Object> proQuery = new HashMap<>();
            proQuery.put("taskId", taskId);
            proQuery.put("scoreSpecialistUid", expertUid);
            List<QcProDataDto> assignedProjects = qcAwardService.listProInfo(proQuery);

            // 2. 获取该专家在该任务下的回避项目ID集合
            List<Integer> avoidedProIdList = avoidanceService.getAvoidedProIds(taskId, expertUid);
            Set<Integer> avoidedProIds = new HashSet<>(avoidedProIdList != null ? avoidedProIdList : Collections.emptyList());

            List<Map<String, Object>> result = new ArrayList<>();
            for (QcProDataDto pro : assignedProjects) {
                if (pro.getProId() == null) continue;

                Map<String, Object> item = new HashMap<>();
                item.put("proId", pro.getProId());
                item.put("proCode", pro.getApplyId());
                item.put("topicName", pro.getTopicName());
                item.put("groupName", pro.getGroupName());
                item.put("topicType", pro.getTopicType());
                item.put("completeUnit", pro.getCompleteUnit());
                item.put("companyName", pro.getCompanyName());
                item.put("groupMember", pro.getGroupMember());

                boolean isAvoided = avoidedProIds.contains(pro.getProId());
                item.put("isAvoided", isAvoided);

                if (isAvoided) {
                    item.put("totalScore", null);
                    item.put("recommendLevel", null);
                    item.put("scoreTime", "-");
                    item.put("scoreDetails", null);
                } else {
                    Map<String, Object> scoreQuery = new HashMap<>();
                    scoreQuery.put("taskId", taskId);
                    scoreQuery.put("proId", pro.getProId());
                    scoreQuery.put("optUid", expertUid);

                    if ("问题解决型".equals(pro.getTopicType())) {
                        List<QcResultSolveScoreDO> solveList = qcResultSolveScoreService.list(scoreQuery);
                        if (!solveList.isEmpty()) {
                            QcResultSolveScoreDO s = solveList.get(0);
                            item.put("totalScore", s.getAppraiseSum());
                            item.put("recommendLevel", s.getRecommendLevel());
                            item.put("scoreTime", s.getCreated() != null ? sdf.format(s.getCreated()) : "-");
                            List<Map<String, Object>> details = new ArrayList<>();
                            details.add(buildScoreDetail("选题得分", s.getSelSocre()));
                            details.add(buildScoreDetail("原因分析得分", s.getReasonScore()));
                            details.add(buildScoreDetail("对策与实施得分", s.getStrategyExecuteScore()));
                            details.add(buildScoreDetail("效果得分", s.getEffectScore()));
                            details.add(buildScoreDetail("成果报告得分", s.getReportScore()));
                            details.add(buildScoreDetail("特点得分", s.getCharacteristicScore()));
                            details.add(buildScoreDetail("持证情况得分", s.getCertificateScore()));
                            details.add(buildScoreDetail("成果实际价值评价得分", s.getPracticalValueScore()));
                            item.put("scoreDetails", details);
                        } else {
                            item.put("totalScore", null);
                            item.put("recommendLevel", null);
                            item.put("scoreTime", "-");
                            item.put("scoreDetails", null);
                        }
                    } else if ("创新型".equals(pro.getTopicType())) {
                        List<QcResultInnovateScoreDO> innovateList = qcResultInnovateScoreService.list(scoreQuery);
                        if (!innovateList.isEmpty()) {
                            QcResultInnovateScoreDO s = innovateList.get(0);
                            item.put("totalScore", s.getAppraiseSum());
                            item.put("recommendLevel", s.getRecommendLevel());
                            item.put("scoreTime", s.getCreated() != null ? sdf.format(s.getCreated()) : "-");
                            List<Map<String, Object>> details = new ArrayList<>();
                            details.add(buildScoreDetail("选题得分", s.getSelSocre()));
                            details.add(buildScoreDetail("提出方案并确定最佳方案", s.getBestPlanScore()));
                            details.add(buildScoreDetail("原因分析得分", s.getReasonScore()));
                            details.add(buildScoreDetail("对策与实施得分", s.getStrategyExecuteScore()));
                            details.add(buildScoreDetail("效果得分", s.getEffectScore()));
                            details.add(buildScoreDetail("成果报告得分", s.getReportScore()));
                            details.add(buildScoreDetail("特点得分", s.getCharacteristicScore()));
                            details.add(buildScoreDetail("持证情况得分", s.getCertificateScore()));
                            details.add(buildScoreDetail("成果实际价值评价得分", s.getPracticalValueScore()));
                            item.put("scoreDetails", details);
                        } else {
                            item.put("totalScore", null);
                            item.put("recommendLevel", null);
                            item.put("scoreTime", "-");
                            item.put("scoreDetails", null);
                        }
                    } else {
                        item.put("totalScore", null);
                        item.put("recommendLevel", null);
                        item.put("scoreTime", "-");
                        item.put("scoreDetails", null);
                    }
                }
                result.add(item);
            }

            // 查询专家的电子签章 URL
            String expertSignUrl = null;
            try {
                Map<String, Object> expertQuery = new HashMap<>();
                expertQuery.put("userId", String.valueOf(expertUid));
                expertQuery.put("taskId", taskId);
                expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
                List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
                if (!expertList.isEmpty() && expertList.get(0).getExpertSignUrl() != null) {
                    expertSignUrl = expertList.get(0).getExpertSignUrl();
                }
            } catch (Exception ignore) {}

            return R.ok().put("data", result).put("expertSignUrl", expertSignUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 导出专家打分情况为 Excel（含签章图片）
     */
    @GetMapping("/exportExpertScoresExcel")
    public void exportExpertScoresExcel(@RequestParam Map<String, Object> params,
                                        HttpServletResponse response) throws Exception {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String expertUidStr = params.get("expertUid") != null ? params.get("expertUid").toString() : "";
        String expertName = params.get("expertName") != null ? params.get("expertName").toString() : "专家";
        String groupName  = params.get("groupName")  != null ? params.get("groupName").toString()  : "";
        if (taskId.isEmpty() || expertUidStr.isEmpty()) { response.sendError(400, "参数不完整"); return; }
        Integer expertUid = Integer.parseInt(expertUidStr);

        // ---- 查数据（与 getExpertScores 相同逻辑）----
        Map<String, Object> proQuery = new HashMap<>();
        proQuery.put("taskId", taskId);
        proQuery.put("scoreSpecialistUid", expertUid);
        List<QcProDataDto> assignedProjects = qcAwardService.listProInfo(proQuery);
        List<Integer> avoidedProIdList = avoidanceService.getAvoidedProIds(taskId, expertUid);
        Set<Integer> avoidedProIds = new HashSet<>(avoidedProIdList != null ? avoidedProIdList : Collections.emptyList());

        List<Map<String, Object>> solveItems    = new ArrayList<>();
        List<Map<String, Object>> innovateItems = new ArrayList<>();
        for (QcProDataDto pro : assignedProjects) {
            if (pro.getProId() == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("proCode", pro.getApplyId()); item.put("topicName", pro.getTopicName());
            item.put("groupName", pro.getGroupName()); item.put("topicType", pro.getTopicType());
            item.put("completeUnit", pro.getCompleteUnit()); item.put("companyName", pro.getCompanyName());
            item.put("groupMember", pro.getGroupMember());
            boolean isAvoided = avoidedProIds.contains(pro.getProId());
            item.put("isAvoided", isAvoided);
            List<Map<String, Object>> details = new ArrayList<>();
            String totalScore = null; String recommendLevel = null;
            if (!isAvoided) {
                Map<String, Object> sq = new HashMap<>();
                sq.put("taskId", taskId); sq.put("proId", pro.getProId()); sq.put("optUid", expertUid);
                if ("问题解决型".equals(pro.getTopicType())) {
                    List<QcResultSolveScoreDO> sl = qcResultSolveScoreService.list(sq);
                    if (!sl.isEmpty()) {
                        QcResultSolveScoreDO s = sl.get(0);
                        totalScore = s.getAppraiseSum();
                        recommendLevel = s.getRecommendLevel();
                        details.add(buildScoreDetail("选题得分", s.getSelSocre()));
                        details.add(buildScoreDetail("原因分析得分", s.getReasonScore()));
                        details.add(buildScoreDetail("对策与实施得分", s.getStrategyExecuteScore()));
                        details.add(buildScoreDetail("效果得分", s.getEffectScore()));
                        details.add(buildScoreDetail("成果报告得分", s.getReportScore()));
                        details.add(buildScoreDetail("特点得分", s.getCharacteristicScore()));
                        details.add(buildScoreDetail("持证情况得分", s.getCertificateScore()));
                        details.add(buildScoreDetail("成果实际价值评价得分", s.getPracticalValueScore()));
                    }
                } else if ("创新型".equals(pro.getTopicType())) {
                    List<QcResultInnovateScoreDO> il = qcResultInnovateScoreService.list(sq);
                    if (!il.isEmpty()) {
                        QcResultInnovateScoreDO s = il.get(0);
                        totalScore = s.getAppraiseSum();
                        recommendLevel = s.getRecommendLevel();
                        details.add(buildScoreDetail("选题得分", s.getSelSocre()));
                        details.add(buildScoreDetail("提出方案并确定最佳方案", s.getBestPlanScore()));
                        details.add(buildScoreDetail("对策与实施得分", s.getStrategyExecuteScore()));
                        details.add(buildScoreDetail("效果得分", s.getEffectScore()));
                        details.add(buildScoreDetail("成果报告得分", s.getReportScore()));
                        details.add(buildScoreDetail("特点得分", s.getCharacteristicScore()));
                        details.add(buildScoreDetail("持证情况得分", s.getCertificateScore()));
                        details.add(buildScoreDetail("成果实际价值评价得分", s.getPracticalValueScore()));
                    }
                }
            }
            item.put("scoreDetails", details); item.put("totalScore", totalScore);
            item.put("recommendLevel", recommendLevel);
            if ("问题解决型".equals(pro.getTopicType())) solveItems.add(item);
            else if ("创新型".equals(pro.getTopicType()))  innovateItems.add(item);
        }

        // ---- 查签章路径 ----
        String signFilePath = null;
        try {
            Map<String, Object> eq = new HashMap<>();
            eq.put("userId", expertUidStr); eq.put("taskId", taskId);
            eq.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            List<ExpertGroupDO> el = expertGroupService.list(eq);
            if (!el.isEmpty() && el.get(0).getExpertSignUrl() != null) {
                String signUrl = el.get(0).getExpertSignUrl();
                String uploadRoot = bootdoConfig.getUploadPath();
                if (uploadRoot.endsWith("/**")) uploadRoot = uploadRoot.substring(0, uploadRoot.length() - 3);
                signFilePath = uploadRoot + signUrl.replace("/files/", "/");
            }
        } catch (Exception ignore) {}

        // ---- 生成 Excel ----
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String timeStr = new SimpleDateFormat("yyyy.M.d").format(new Date());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // 读取签章图片（如存在）
            byte[] signImgBytes = null;
            int signPicType = XSSFWorkbook.PICTURE_TYPE_PNG;
            if (signFilePath != null) {
                File sf = new File(signFilePath);
                if (sf.exists()) {
                    signImgBytes = Files.readAllBytes(sf.toPath());
                    String ext = signFilePath.substring(signFilePath.lastIndexOf('.') + 1).toLowerCase();
                    if ("jpg".equals(ext) || "jpeg".equals(ext)) signPicType = XSSFWorkbook.PICTURE_TYPE_JPEG;
                    else if ("gif".equals(ext)) signPicType = XSSFWorkbook.PICTURE_TYPE_GIF;
                }
            }
            int pictureIdx = signImgBytes != null ? wb.addPicture(signImgBytes, signPicType) : -1;

            buildExpertScoreSheet(wb, solveItems, groupName, year, timeStr, pictureIdx, false);
            buildExpertScoreSheet(wb, innovateItems, groupName, year, timeStr, pictureIdx, true);

            String fileName = URLEncoder.encode(expertName + "_打分情况.xlsx", "UTF-8").replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            wb.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

    private void buildExpertScoreSheet(XSSFWorkbook wb, List<Map<String, Object>> items,
                                       String groupName, int year, String timeStr,
                                       int pictureIdx, boolean isInnovate) {
        String sheetName = isInnovate ? "创新型" : "问题解决型";
        int nTotal = 17;
        XSSFSheet sheet = wb.createSheet(sheetName);

        // ---- 公共样式 ----
        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 11);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setWrapText(true);
        setBorder(titleStyle);

        XSSFCellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true); headerFont.setFontHeightInPoints((short) 9);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)217, (byte)225, (byte)242}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(headerStyle);

        XSSFCellStyle dataStyle = wb.createCellStyle();
        XSSFFont dataFont = wb.createFont();
        dataFont.setFontHeightInPoints((short) 9);
        dataStyle.setFont(dataFont);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setWrapText(true);
        setBorder(dataStyle);

        XSSFCellStyle avoidStyle = wb.createCellStyle();
        XSSFFont avoidFont0 = wb.createFont();
        avoidFont0.setFontHeightInPoints((short) 9); avoidFont0.setBold(true);
        avoidFont0.setColor(new XSSFColor(new byte[]{(byte)204,0,0}, null));
        avoidStyle.setFont(avoidFont0);
        avoidStyle.setAlignment(HorizontalAlignment.CENTER);
        avoidStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        avoidStyle.setWrapText(true);
        avoidStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255,(byte)255,0}, null));
        avoidStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(avoidStyle);

        XSSFCellStyle sigStyle = wb.createCellStyle();
        sigStyle.setAlignment(HorizontalAlignment.LEFT);
        sigStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(sigStyle);

        // ---- Row 0: 标题 ----
        String typeLabel = isInnovate ? "（创新型）" : "问题解决型";
        int valueCol0 = 12; // 成果实际价值评价列，资料分=valueCol0+1
        int titleEndCol = valueCol0 + 1;       // 资料分列
        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(24);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(year + "年石油工程建设优秀质量管理小组活动成果资料评分表——" + typeLabel + "  " + groupName);
        c0.setCellStyle(titleStyle);
        for (int c = 1; c < nTotal; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(c <= titleEndCol ? titleStyle : dataStyle); }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleEndCol));

        // ---- Row 1: 主表头 ----
        Row r1 = sheet.createRow(1);
        r1.setHeightInPoints(28);
        String[] fixedHeaders = {"组内序号", "申报账号", "课题名称", "小组名称", "分类"};
        for (int i = 0; i < fixedHeaders.length; i++) {
            Cell c = r1.createCell(i); c.setCellValue(fixedHeaders[i]); c.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, i, i));
        }
        r1.createCell(5).setCellValue("成果形成过程评价（100分）");
        r1.getCell(5).setCellStyle(headerStyle);
        int lastProcessCol = 11;
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 5, lastProcessCol));

        int valueCol = 12;
        Cell valCell = r1.createCell(valueCol);
        valCell.setCellValue("成果实际价值评价（20分）");
        valCell.setCellStyle(headerStyle);

        String[] tailHeaders = {"资料分\n（满分120*0.6）", "完成单位", "申报单位", "小组成员"};
        for (int i = 0; i < tailHeaders.length; i++) {
            int col = valueCol + 1 + i;
            Cell c = r1.createCell(col); c.setCellValue(tailHeaders[i]); c.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, col, col));
        }

        // ---- Row 2: 子表头 ----
        Row r2 = sheet.createRow(2);
        r2.setHeightInPoints(28);
        String[] solveSubHeaders = {"选题\n（13）", "原因分析\n（30）", "对策与实施\n（20）",
                "效果\n（20）", "成果报告\n（5）", "特点\n（8.5）", "持证情况\n（3.5）"};
        String[] innovateSubHeaders = {"选题\n（18）", "提出方案并确定最佳方案\n（30）", "对策与实施\n（20）",
                "效果\n（15）", "成果报告\n（5）", "特点\n（8.5）", "持证情况\n（3.5）"};
        String[] subHeaders = isInnovate ? innovateSubHeaders : solveSubHeaders;
        for (int i = 0; i < subHeaders.length; i++) {
            Cell c = r2.createCell(5 + i); c.setCellValue(subHeaders[i]); c.setCellStyle(headerStyle);
        }
        Cell valSubCell = r2.createCell(valueCol);
        valSubCell.setCellValue("1.技术价值 2.经济价值 3.社会价值 4.推广应用价值（共20分）");
        valSubCell.setCellStyle(headerStyle);
        // fill empty cells in rows 1&2 for merged areas
        for (int c = 0; c < nTotal; c++) {
            if (r1.getCell(c) == null) { Cell cell = r1.createCell(c); cell.setCellStyle(headerStyle); }
            if (r2.getCell(c) == null) { Cell cell = r2.createCell(c); cell.setCellStyle(dataStyle); }
        }

        // ---- 数据行 ----
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            Row row = sheet.createRow(3 + i);
            row.setHeightInPoints(24);
            setCell(row, 0, String.valueOf(i + 1), dataStyle);
            setCell(row, 1, str(item.get("proCode")), dataStyle);
            setCell(row, 2, str(item.get("topicName")), dataStyle);
            setCell(row, 3, str(item.get("groupName")), dataStyle);
            setCell(row, 4, str(item.get("topicType")), dataStyle);
            boolean isAvoided = Boolean.TRUE.equals(item.get("isAvoided"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> details = (List<Map<String, Object>>) item.get("scoreDetails");
            if (isAvoided) {
                for (int c = 5; c <= valueCol; c++) setCell(row, c, "", dataStyle);
                setCell(row, valueCol + 1, "回避", avoidStyle);
            } else if (details != null && !details.isEmpty()) {
                for (int di = 0; di < subHeaders.length && di < details.size(); di++) {
                    Object score = details.get(di).get("score");
                    setCell(row, 5 + di, score != null ? score.toString() : "", dataStyle);
                }
                // value col
                int valueDetailIdx = subHeaders.length;
                if (valueDetailIdx < details.size()) {
                    Object vs = details.get(valueDetailIdx).get("score");
                    setCell(row, valueCol, vs != null ? vs.toString() : "", dataStyle);
                }
                // 资料分 = 原始总分 * 0.6
                setCell(row, valueCol + 1, scaleScore(str(item.get("totalScore")), 0.6), dataStyle);
            } else {
                for (int c = 5; c <= valueCol; c++) setCell(row, c, "", dataStyle);
                setCell(row, valueCol + 1, "", dataStyle);
            }
            setCell(row, valueCol + 2, str(item.get("completeUnit")), dataStyle);
            setCell(row, valueCol + 3, str(item.get("companyName")), dataStyle);
            setCell(row, valueCol + 4, str(item.get("groupMember")), dataStyle);
        }

        // ---- 签章行 & 时间行（资料分列内，不超出该列） ----
        int sigRow = 3 + items.size();
        int sigCol = valueCol + 1; // 资料分列 col 13
        Row rSig = sheet.createRow(sigRow);
        rSig.setHeightInPoints(25);
        for (int c = 0; c < sigCol; c++) { Cell cell = rSig.createCell(c); cell.setCellStyle(dataStyle); }
        Cell sigCell = rSig.createCell(sigCol);
        sigCell.setCellValue("评委：");
        sigCell.setCellStyle(sigStyle);
        for (int c = sigCol + 1; c < nTotal; c++) { Cell cell = rSig.createCell(c); cell.setCellStyle(dataStyle); }

        Row rTime = sheet.createRow(sigRow + 1);
        rTime.setHeightInPoints(20);
        for (int c = 0; c < sigCol; c++) { Cell cell = rTime.createCell(c); cell.setCellStyle(dataStyle); }
        Cell timeCell = rTime.createCell(sigCol);
        timeCell.setCellValue("时间：" + timeStr);
        timeCell.setCellStyle(sigStyle);
        for (int c = sigCol + 1; c < nTotal; c++) { Cell cell = rTime.createCell(c); cell.setCellStyle(dataStyle); }

        // ---- 嵌入签章图片（仅资料分列内） ----
        if (pictureIdx >= 0) {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = new XSSFClientAnchor(400000, 0, 0, 0, sigCol, sigRow, sigCol + 1, sigRow + 1);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIdx);
        }

        // ---- 列宽 ----
        int[] solveWidths    = {1500,2500,5500,4000,2500,1800,2600,2600,1800,2300,1800,2300,6000,4000,5500,3500,4500};
        int[] innovateWidths = {1500,2500,5500,4000,2500,1800,4000,2600,1800,2300,1800,2300,6000,4000,5500,3500,4500};
        int[] widths = isInnovate ? innovateWidths : solveWidths;
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);
    }

    private void setBorder(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN); style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN);
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String str(Object obj) { return obj != null ? obj.toString() : ""; }

    private String scaleScore(String raw, double factor) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            double v = Double.parseDouble(raw) * factor;
            return v % 1 == 0 ? String.valueOf((int) v) : String.format("%.2f", v);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private Map<String, Object> buildScoreDetail(String label, BigDecimal score) {
        Map<String, Object> d = new HashMap<>();
        d.put("label", label);
        d.put("score", score);
        return d;
    }

    /**
     * 查询任务下所有课题 × 所有专家的打分矩阵，用于导出评分汇总表
     * @param params taskId
     */
    @GetMapping("/getTaskScoreMatrix")
    @ResponseBody
    public R getTaskScoreMatrix(@RequestParam Map<String, Object> params) {
        try {
            String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
            if (taskId.isEmpty()) return R.error("参数不完整");

            // 1. 获取该任务下所有专家（按 loginAccount 排序）
            Map<String, Object> expertQuery = new HashMap<>();
            expertQuery.put("taskId", taskId);
            expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            List<ExpertGroupDO> experts = expertGroupService.list(expertQuery);
            experts.sort((a, b) -> {
                String la = a.getLoginAccount() != null ? a.getLoginAccount() : "";
                String lb = b.getLoginAccount() != null ? b.getLoginAccount() : "";
                return la.compareTo(lb);
            });

            // 2. 获取该任务下所有项目
            Map<String, Object> proQuery = new HashMap<>();
            proQuery.put("taskId", taskId);
            List<QcProDataDto> projects = qcAwardService.listProInfo(proQuery);

            // 3. 批量查询全部打分记录（不按 optUid 过滤，一次拉全任务数据）
            Map<String, Object> scoreQuery = new HashMap<>();
            scoreQuery.put("taskId", taskId);
            List<QcResultSolveScoreDO> solveList = qcResultSolveScoreService.list(scoreQuery);
            Map<String, String> solveMap = new HashMap<>();
            for (QcResultSolveScoreDO s : solveList) {
                if (s.getProId() != null && s.getOptUid() != null)
                    solveMap.put(s.getProId() + "_" + s.getOptUid(), s.getAppraiseSum() != null ? s.getAppraiseSum() : "");
            }
            List<QcResultInnovateScoreDO> innovateList = qcResultInnovateScoreService.list(scoreQuery);
            Map<String, String> innovateMap = new HashMap<>();
            for (QcResultInnovateScoreDO s : innovateList) {
                if (s.getProId() != null && s.getOptUid() != null)
                    innovateMap.put(s.getProId() + "_" + s.getOptUid(), s.getAppraiseSum() != null ? s.getAppraiseSum() : "");
            }

            // 4. 批量获取每个专家的回避项目
            Map<Integer, Set<Integer>> avoidMap = new HashMap<>();
            for (ExpertGroupDO e : experts) {
                if (e.getUserId() == null) continue;
                try {
                    Integer uid = Integer.parseInt(e.getUserId());
                    List<Integer> avoidedProIds = avoidanceService.getAvoidedProIds(taskId, uid);
                    avoidMap.put(uid, new HashSet<>(avoidedProIds != null ? avoidedProIds : Collections.emptyList()));
                } catch (NumberFormatException ignored) {}
            }

            // 5. 构建专家信息列表
            List<Map<String, Object>> expertInfoList = new ArrayList<>();
            for (ExpertGroupDO e : experts) {
                Map<String, Object> ei = new HashMap<>();
                ei.put("uid", e.getUserId());
                ei.put("loginAccount", e.getLoginAccount());
                ei.put("expertName", e.getExpertName());
                expertInfoList.add(ei);
            }

            // 6. 构建项目行矩阵
            List<Map<String, Object>> projectRows = new ArrayList<>();
            for (QcProDataDto pro : projects) {
                if (pro.getProId() == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("proId", pro.getProId());
                row.put("proCode", pro.getApplyId());
                row.put("topicName", pro.getTopicName());
                row.put("groupName", pro.getGroupName());
                row.put("topicType", pro.getTopicType());
                row.put("qcGroupName", pro.getQcGroupName());
                row.put("completeUnit", pro.getCompleteUnit());
                row.put("companyName", pro.getCompanyName());
                row.put("groupMember", pro.getGroupMember());
                Map<String, Object> expertScores = new LinkedHashMap<>();
                for (ExpertGroupDO e : experts) {
                    if (e.getUserId() == null || e.getLoginAccount() == null) continue;
                    try {
                        Integer uid = Integer.parseInt(e.getUserId());
                        String key = pro.getProId() + "_" + uid;
                        Set<Integer> avoided = avoidMap.getOrDefault(uid, Collections.emptySet());
                        if (avoided.contains(pro.getProId())) {
                            expertScores.put(e.getLoginAccount(), "回避");
                        } else {
                            String score = solveMap.containsKey(key) ? solveMap.get(key) : innovateMap.get(key);
                            expertScores.put(e.getLoginAccount(), score != null ? score : "");
                        }
                    } catch (NumberFormatException ignored) {
                        expertScores.put(e.getLoginAccount(), "");
                    }
                }
                row.put("expertScores", expertScores);

                // 计算平均分：排除回避专家，≥3个有效分去最高最低（与弹窗getTaskScores逻辑一致）
                Set<Integer> thisProAvoidedUids = new HashSet<>();
                for (Map.Entry<Integer, Set<Integer>> entry : avoidMap.entrySet()) {
                    if (entry.getValue().contains(pro.getProId())) {
                        thisProAvoidedUids.add(entry.getKey());
                    }
                }
                List<BigDecimal> validScores = new ArrayList<>();
                String proTopicType = pro.getTopicType();
                if ("问题解决型".equals(proTopicType)) {
                    for (QcResultSolveScoreDO s : solveList) {
                        if (!pro.getProId().equals(s.getProId())) continue;
                        if (s.getOptUid() != null && thisProAvoidedUids.contains(s.getOptUid())) continue;
                        if (s.getAppraiseSum() != null) {
                            try { validScores.add(new BigDecimal(s.getAppraiseSum().toString())); } catch (NumberFormatException ignore) {}
                        }
                    }
                } else {
                    for (QcResultInnovateScoreDO s : innovateList) {
                        if (!pro.getProId().equals(s.getProId())) continue;
                        if (s.getOptUid() != null && thisProAvoidedUids.contains(s.getOptUid())) continue;
                        if (s.getAppraiseSum() != null) {
                            try { validScores.add(new BigDecimal(s.getAppraiseSum().toString())); } catch (NumberFormatException ignore) {}
                        }
                    }
                }
                BigDecimal avgScore = null;
                int validCount = validScores.size();
                if (validCount > 0) {
                    List<BigDecimal> toAvg = new ArrayList<>(validScores);
                    if (validCount >= 3) {
                        toAvg.sort(BigDecimal::compareTo);
                        toAvg.remove(0);
                        toAvg.remove(toAvg.size() - 1);
                    }
                    BigDecimal sum = BigDecimal.ZERO;
                    for (BigDecimal s : toAvg) sum = sum.add(s);
                    avgScore = sum.divide(BigDecimal.valueOf(toAvg.size()), 2, BigDecimal.ROUND_HALF_UP);
                }
                row.put("avgScore", avgScore);
                projectRows.add(row);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("experts", expertInfoList);
            result.put("projects", projectRows);
            return R.ok().put("data", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查询任务下所有课题的打分情况（以课题为维度）
     * @param params taskId
     */
    @GetMapping("/getTaskScores")
    @ResponseBody
    public R getTaskScores(@RequestParam Map<String, Object> params) {
        try {
            String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";

            if (taskId.isEmpty()) {
                return R.error("任务ID不能为空");
            }

            boolean isExternalExpert = false;
            Set<String> allowedQcGroups = new HashSet<>();
            UserDO currentUser = getUser();
            if (currentUser.getRoleIds().contains(ROLE_QC_EXTERNAL_EMPLOYMENT_ID)) {
                isExternalExpert = true;
                Map<String, Object> bindingQuery = new HashMap<>();
                bindingQuery.put("taskId", taskId);
                bindingQuery.put("userId", String.valueOf(getUserId()));
                bindingQuery.put("proType", "qc_view_scope");
                List<ExpertGroupDO> bindings = expertGroupService.list(bindingQuery);
                for (ExpertGroupDO b : bindings) {
                    if (b.getGroupName() != null) {
                        allowedQcGroups.add(b.getGroupName().trim());
                    }
                }
            }

            if (isExternalExpert && allowedQcGroups.isEmpty()) {
                return R.ok().put("data", new ArrayList<>());
            }

            Map<String, Object> projectQuery = new HashMap<>();
            projectQuery.put("taskId", taskId);
            List<QcGroupApplyInfoDO> projects = qcGroupApplyInfoService.list(projectQuery);

            Map<String, Object> allProQuery = new HashMap<>();
            allProQuery.put("taskId", taskId);
            List<QcProDataDto> allProInfo = qcAwardService.listProInfo(allProQuery);
            Map<Integer, String> proIdToQcGroup = new HashMap<>();
            if (allProInfo != null) {
                for (QcProDataDto dto : allProInfo) {
                    if (dto.getProId() != null && dto.getQcGroupName() != null) {
                        proIdToQcGroup.put(dto.getProId(), dto.getQcGroupName().trim());
                    }
                }
            }

            if (isExternalExpert) {
                final Set<String> finalAllowedGroups = allowedQcGroups;
                projects = projects.stream()
                        .filter(p -> {
                            String qcGrp = proIdToQcGroup.get(p.getProId());
                            return qcGrp != null && finalAllowedGroups.contains(qcGrp);
                        })
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (QcGroupApplyInfoDO project : projects) {
                Map<String, Object> item = new HashMap<>();
                item.put("proId", project.getProId());
                item.put("proCode", project.getApplyId());
                item.put("topicName", project.getTopicName());
                item.put("groupName", project.getGroupName());
                item.put("qcGroupName", proIdToQcGroup.get(project.getProId()));
                item.put("topicType", project.getTopicType());

                Map<String, Object> scoreQuery = new HashMap<>();
                scoreQuery.put("taskId", taskId);
                scoreQuery.put("proId", project.getProId());
                scoreQuery.put("deleted", 0);

                List<Integer> avoidedExpertIds = avoidanceService.getAvoidedExpertIds(taskId, project.getProId());
                List<BigDecimal> validScores = new ArrayList<>();
                if ("问题解决型".equals(project.getTopicType())) {
                    List<QcResultSolveScoreDO> scores = qcResultSolveScoreService.list(scoreQuery);
                    if (scores != null) {
                        for (QcResultSolveScoreDO s : scores) {
                            if (avoidedExpertIds != null && avoidedExpertIds.contains(s.getOptUid())) continue;
                            if (s.getAppraiseSum() != null) {
                                try { validScores.add(new BigDecimal(s.getAppraiseSum().toString())); } catch (NumberFormatException ignore) {}
                            }
                        }
                    }
                } else {
                    List<QcResultInnovateScoreDO> scores = qcResultInnovateScoreService.list(scoreQuery);
                    if (scores != null) {
                        for (QcResultInnovateScoreDO s : scores) {
                            if (avoidedExpertIds != null && avoidedExpertIds.contains(s.getOptUid())) continue;
                            if (s.getAppraiseSum() != null) {
                                try { validScores.add(new BigDecimal(s.getAppraiseSum().toString())); } catch (NumberFormatException ignore) {}
                            }
                        }
                    }
                }
                int validCount = validScores.size();
                item.put("scorerCount", validCount);

                BigDecimal avgScore = null;
                if (validCount > 0) {
                    List<BigDecimal> toAvg = new ArrayList<>(validScores);
                    if (validCount >= 3) {
                        toAvg.sort(BigDecimal::compareTo);
                        toAvg.remove(0);
                        toAvg.remove(toAvg.size() - 1);
                    }
                    BigDecimal sum = BigDecimal.ZERO;
                    for (BigDecimal s : toAvg) sum = sum.add(s);
                    avgScore = sum.divide(BigDecimal.valueOf(toAvg.size()), 2, BigDecimal.ROUND_HALF_UP);
                }
                item.put("avgScore", avgScore);
                result.add(item);
            }

            result.sort((a, b) -> {
                String g1 = a.get("qcGroupName") != null ? a.get("qcGroupName").toString() : "";
                String g2 = b.get("qcGroupName") != null ? b.get("qcGroupName").toString() : "";
                int groupCompare = g1.compareTo(g2);
                if (groupCompare != 0) {
                    return groupCompare;
                }
                String c1 = a.get("proCode") != null ? a.get("proCode").toString() : "";
                String c2 = b.get("proCode") != null ? b.get("proCode").toString() : "";
                return c1.compareTo(c2);
            });

            return R.ok().put("data", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查询单个课题的详细打分情况
     * @param params taskId, proId
     */
    @GetMapping("/getProjectScoreDetail")
    @ResponseBody
    public R getProjectScoreDetail(@RequestParam Map<String, Object> params) {
        try {
            String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
            String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";

            if (taskId.isEmpty() || proIdStr.isEmpty()) {
                return R.error("参数不完整");
            }

            Integer proId;
            try {
                proId = Integer.parseInt(proIdStr);
            } catch (NumberFormatException e) {
                return R.error("项目ID格式错误");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Map<String, Object> proQuery = new HashMap<>();
            proQuery.put("proId", String.valueOf(proId));
            List<QcGroupApplyInfoDO> proList = qcGroupApplyInfoService.list(proQuery);
            if (proList == null || proList.isEmpty()) {
                return R.error("项目不存在");
            }
            QcGroupApplyInfoDO project = proList.get(0);

            List<Integer> avoidedExpertIdList = avoidanceService.getAvoidedExpertIds(taskId, proId);
            Set<Integer> avoidedExpertIds = new HashSet<>(avoidedExpertIdList != null ? avoidedExpertIdList : Collections.emptyList());

            List<Map<String, Object>> scores = new ArrayList<>();
            Set<Integer> scoredExpertUids = new HashSet<>();

            if ("问题解决型".equals(project.getTopicType())) {
                Map<String, Object> query = new HashMap<>();
                query.put("taskId", taskId);
                query.put("proId", proId);
                List<QcResultSolveScoreDO> solveScores = qcResultSolveScoreService.list(query);

                Set<Long> userIds = new HashSet<>();
                for (QcResultSolveScoreDO s : solveScores) {
                    if (s.getOptUid() != null) userIds.add(s.getOptUid().longValue());
                }
                Map<Long, UserDO> userMap = new HashMap<>();
                for (Long uid : userIds) {
                    UserDO u = userService.get(uid);
                    if (u != null) userMap.put(uid, u);
                }

                for (QcResultSolveScoreDO score : solveScores) {
                    if (score.getOptUid() == null) continue;
                    UserDO user = userMap.get(score.getOptUid().longValue());
                    if (user != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("expertName", user.getName());
                        item.put("score", score.getAppraiseSum());
                        item.put("scoreTime", score.getCreated() != null ? sdf.format(score.getCreated()) : "-");
                        item.put("isAvoided", avoidedExpertIds.contains(score.getOptUid()));
                        scores.add(item);
                        scoredExpertUids.add(score.getOptUid());
                    }
                }
            } else if ("创新型".equals(project.getTopicType())) {
                Map<String, Object> query = new HashMap<>();
                query.put("taskId", taskId);
                query.put("proId", proId);
                List<QcResultInnovateScoreDO> innovateScores = qcResultInnovateScoreService.list(query);

                Set<Long> userIds = new HashSet<>();
                for (QcResultInnovateScoreDO s : innovateScores) {
                    if (s.getOptUid() != null) userIds.add(s.getOptUid().longValue());
                }
                Map<Long, UserDO> userMap = new HashMap<>();
                for (Long uid : userIds) {
                    UserDO u = userService.get(uid);
                    if (u != null) userMap.put(uid, u);
                }

                for (QcResultInnovateScoreDO score : innovateScores) {
                    if (score.getOptUid() == null) continue;
                    UserDO user = userMap.get(score.getOptUid().longValue());
                    if (user != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("expertName", user.getName());
                        item.put("score", score.getAppraiseSum());
                        item.put("scoreTime", score.getCreated() != null ? sdf.format(score.getCreated()) : "-");
                        item.put("isAvoided", avoidedExpertIds.contains(score.getOptUid()));
                        scores.add(item);
                        scoredExpertUids.add(score.getOptUid());
                    }
                }
            }

            Map<String, Object> proDataParams = new HashMap<>();
            proDataParams.put("proId", proId);
            proDataParams.put("taskId", taskId);
            List<QcProDataDto> proDataList = qcAwardService.listProInfo(proDataParams);
            String qcGroupName = (proDataList != null && !proDataList.isEmpty())
                    ? proDataList.get(0).getQcGroupName() : null;

            Map<String, Object> groupQuery = new HashMap<>();
            groupQuery.put("taskId", taskId);
            if (qcGroupName != null && !qcGroupName.isEmpty()) {
                groupQuery.put("groupName", qcGroupName);
            }
            List<ExpertGroupDO> groupExperts = expertGroupService.list(groupQuery);
            Set<Integer> groupExpertUids = new HashSet<>();
            for (ExpertGroupDO eg : groupExperts) {
                if (eg.getUserId() != null) {
                    try { groupExpertUids.add(Integer.parseInt(eg.getUserId())); } catch (NumberFormatException ignore) {}
                }
            }

            for (Integer avoidedUid : avoidedExpertIds) {
                if (!groupExpertUids.contains(avoidedUid)) continue;
                if (scoredExpertUids.contains(avoidedUid)) continue;
                UserDO user = userService.get(avoidedUid.longValue());
                Map<String, Object> item = new HashMap<>();
                item.put("expertName", user != null ? user.getName() : "(uid:" + avoidedUid + ")");
                item.put("score", null);
                item.put("scoreTime", "-");
                item.put("isAvoided", true);
                scores.add(item);
            }

            return R.ok().put("data", scores);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 导出任务评分矩阵 Excel（含样式）
     */
    @GetMapping("/exportTaskScoreMatrixExcel")
    public void exportTaskScoreMatrixExcel(@RequestParam Map<String, Object> params,
                                           HttpServletResponse response) throws Exception {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (taskId.isEmpty()) { response.sendError(400, "参数不完整"); return; }

        boolean isExternalExpert = false;
        Set<String> allowedQcGroups = new HashSet<>();
        UserDO currentUser = getUser();
        if (currentUser.getRoleIds().contains(ROLE_QC_EXTERNAL_EMPLOYMENT_ID)) {
            isExternalExpert = true;
            Map<String, Object> bindingQuery = new HashMap<>();
            bindingQuery.put("taskId", taskId);
            bindingQuery.put("userId", String.valueOf(getUserId()));
            bindingQuery.put("proType", "qc_view_scope");
            List<ExpertGroupDO> bindings = expertGroupService.list(bindingQuery);
            for (ExpertGroupDO b : bindings) {
                if (b.getGroupName() != null) {
                    allowedQcGroups.add(b.getGroupName().trim());
                }
            }
        }

        // ---- 复用 getTaskScoreMatrix 相同取数逻辑 ----
        Map<String, Object> expertQuery = new HashMap<>();
        expertQuery.put("taskId", taskId);
        expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
        List<ExpertGroupDO> experts = expertGroupService.list(expertQuery);
        if (isExternalExpert) {
            experts = experts.stream()
                    .filter(e -> e.getGroupName() != null && allowedQcGroups.contains(e.getGroupName().trim()))
                    .collect(Collectors.toList());
        }
        experts.sort((a, b) -> {
            String la = a.getLoginAccount() != null ? a.getLoginAccount() : "";
            String lb = b.getLoginAccount() != null ? b.getLoginAccount() : "";
            return la.compareTo(lb);
        });

        Map<String, Object> proQuery = new HashMap<>();
        proQuery.put("taskId", taskId);
        List<QcProDataDto> projects = qcAwardService.listProInfo(proQuery);
        if (isExternalExpert) {
            projects = projects.stream()
                    .filter(p -> p.getQcGroupName() != null && allowedQcGroups.contains(p.getQcGroupName().trim()))
                    .collect(Collectors.toList());
        }

        Map<String, Object> scoreQuery = new HashMap<>();
        scoreQuery.put("taskId", taskId);
        List<QcResultSolveScoreDO> solveList = qcResultSolveScoreService.list(scoreQuery);
        Map<String, String> solveMap = new HashMap<>();
        for (QcResultSolveScoreDO s : solveList)
            if (s.getProId() != null && s.getOptUid() != null)
                solveMap.put(s.getProId() + "_" + s.getOptUid(), s.getAppraiseSum() != null ? s.getAppraiseSum() : "");
        List<QcResultInnovateScoreDO> innovateList = qcResultInnovateScoreService.list(scoreQuery);
        Map<String, String> innovateMap = new HashMap<>();
        for (QcResultInnovateScoreDO s : innovateList)
            if (s.getProId() != null && s.getOptUid() != null)
                innovateMap.put(s.getProId() + "_" + s.getOptUid(), s.getAppraiseSum() != null ? s.getAppraiseSum() : "");

        Map<Integer, Set<Integer>> avoidMap  = new HashMap<>();
        Map<Integer, Set<Integer>> assignMap = new HashMap<>();
        for (ExpertGroupDO e : experts) {
            if (e.getUserId() == null) continue;
            try {
                Integer uid = Integer.parseInt(e.getUserId());
                List<Integer> ap = avoidanceService.getAvoidedProIds(taskId, uid);
                avoidMap.put(uid, new HashSet<>(ap != null ? ap : Collections.emptyList()));
                // 查询该专家实际分配的项目
                Map<String, Object> assignQuery = new HashMap<>();
                assignQuery.put("taskId", taskId);
                assignQuery.put("scoreSpecialistUid", uid);
                List<QcProDataDto> assignedList = qcAwardService.listProInfo(assignQuery);
                Set<Integer> assignedIds = new HashSet<>();
                if (assignedList != null) for (QcProDataDto p : assignedList) if (p.getProId() != null) assignedIds.add(p.getProId());
                assignMap.put(uid, assignedIds);
            } catch (NumberFormatException ignored) {}
        }

        // 按课题类型分组，计算每个项目的专家得分和平均分
        List<Map<String, Object>> solveRows    = new ArrayList<>();
        List<Map<String, Object>> innovateRows = new ArrayList<>();
        for (QcProDataDto pro : projects) {
            if (pro.getProId() == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("proCode", pro.getApplyId()); row.put("topicName", pro.getTopicName());
            row.put("groupName", pro.getGroupName()); row.put("topicType", pro.getTopicType());
            row.put("qcGroupName", pro.getQcGroupName());
            row.put("completeUnit", pro.getCompleteUnit()); row.put("companyName", pro.getCompanyName());
            row.put("groupMember", pro.getGroupMember());
            Map<String, String> expertScores = new LinkedHashMap<>();
            for (ExpertGroupDO e : experts) {
                if (e.getUserId() == null || e.getLoginAccount() == null) continue;
                try {
                    Integer uid = Integer.parseInt(e.getUserId());
                    String key = pro.getProId() + "_" + uid;
                    Set<Integer> avoided  = avoidMap.getOrDefault(uid, Collections.emptySet());
                    Set<Integer> assigned = assignMap.getOrDefault(uid, Collections.emptySet());
                    if (!assigned.contains(pro.getProId())) {
                        // 未分配该项目，留空
                        expertScores.put(e.getLoginAccount(), "");
                    } else if (avoided.contains(pro.getProId())) {
                        // 已分配且回避
                        expertScores.put(e.getLoginAccount(), "回避");
                    } else {
                        String sc = solveMap.containsKey(key) ? solveMap.get(key) : innovateMap.get(key);
                        expertScores.put(e.getLoginAccount(), sc != null ? sc : "");
                    }
                } catch (NumberFormatException ignored) { expertScores.put(e.getLoginAccount(), ""); }
            }
            row.put("expertScores", expertScores);
            // 平均分
            Set<Integer> avoidedUids = new HashSet<>();
            for (Map.Entry<Integer, Set<Integer>> entry : avoidMap.entrySet())
                if (entry.getValue().contains(pro.getProId())) avoidedUids.add(entry.getKey());
            List<BigDecimal> validScores = new ArrayList<>();
            if ("问题解决型".equals(pro.getTopicType())) {
                for (QcResultSolveScoreDO s : solveList) {
                    if (!pro.getProId().equals(s.getProId())) continue;
                    if (s.getOptUid() != null && avoidedUids.contains(s.getOptUid())) continue;
                    if (s.getAppraiseSum() != null) try { validScores.add(new BigDecimal(s.getAppraiseSum())); } catch (NumberFormatException ignore) {}
                }
            } else {
                for (QcResultInnovateScoreDO s : innovateList) {
                    if (!pro.getProId().equals(s.getProId())) continue;
                    if (s.getOptUid() != null && avoidedUids.contains(s.getOptUid())) continue;
                    if (s.getAppraiseSum() != null) try { validScores.add(new BigDecimal(s.getAppraiseSum())); } catch (NumberFormatException ignore) {}
                }
            }
            BigDecimal avg = null;
            int vc = validScores.size();
            if (vc > 0) {
                List<BigDecimal> ta = new ArrayList<>(validScores);
                if (vc >= 3) { ta.sort(BigDecimal::compareTo); ta.remove(0); ta.remove(ta.size()-1); }
                BigDecimal sum = BigDecimal.ZERO;
                for (BigDecimal v : ta) sum = sum.add(v);
                avg = sum.divide(BigDecimal.valueOf(ta.size()), 2, BigDecimal.ROUND_HALF_UP);
            }
            row.put("avgScore", avg);
            if ("问题解决型".equals(pro.getTopicType())) solveRows.add(row);
            else innovateRows.add(row);
        }

        int year = Calendar.getInstance().get(Calendar.YEAR);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // 排序：先按分派专业组，再按申报账号
            java.util.Comparator<Map<String, Object>> cmp = (a, b) -> {
                String g1 = a.get("qcGroupName") != null ? a.get("qcGroupName").toString() : "";
                String g2 = b.get("qcGroupName") != null ? b.get("qcGroupName").toString() : "";
                int gc = g1.compareTo(g2);
                if (gc != 0) return gc;
                String c1 = a.get("proCode") != null ? a.get("proCode").toString() : "";
                String c2 = b.get("proCode") != null ? b.get("proCode").toString() : "";
                return c1.compareTo(c2);
            };
            solveRows.sort(cmp);
            innovateRows.sort(cmp);
            // 原导出：每个类型一个sheet，横向展示全部专家（保留为注释，便于回滚）
            // buildMatrixSheet(wb, solveRows,    experts, year, "问题解决型");
            // buildMatrixSheet(wb, innovateRows, experts, year, "（创新型）");
            // 新导出：按专业组（qcGroupName）分块纵向堆叠，每组一段标题+表头+该组专家打分列+该组项目数据
            buildMatrixSheetByExpert(wb, solveRows,    experts, year, "问题解决型");
            buildMatrixSheetByExpert(wb, innovateRows, experts, year, "（创新型）");
            String fileName = URLEncoder.encode("任务打分情况_" + taskId + ".xlsx", "UTF-8").replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            wb.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

    private void buildMatrixSheet(XSSFWorkbook wb, List<Map<String, Object>> rows,
                                  List<ExpertGroupDO> experts, int year, String typeLabel) {
        String sheetName = typeLabel.contains("创新") ? "创新型" : "问题解决型";
        // 表格尾部：资料分、完成单位、申报单位、小组成员
        int nFixed = 6, nExperts = experts.size(), nTail = 4, nTotal = nFixed + nExperts + nTail;
        // XSSFWorkbook:代表整个 Excel工作簿 XSSFSheet:代表工作簿中的一个工作表（Sheet）
        XSSFSheet sheet = wb.createSheet(sheetName);

        // ---- 颜色 ----
        XSSFColor titleColor  = new XSSFColor(new byte[]{(byte)189,(byte)215,(byte)238}, null);
        XSSFColor fixColor    = new XSSFColor(new byte[]{(byte)217,(byte)225,(byte)242}, null);
        XSSFColor expColor    = new XSSFColor(new byte[]{(byte)252,(byte)228,(byte)214}, null);
        XSSFColor tailColor   = new XSSFColor(new byte[]{(byte)226,(byte)239,(byte)218}, null);
        XSSFColor avoidColor  = new XSSFColor(new byte[]{(byte)255,(byte)255,0}, null);
        XSSFColor avgColor    = new XSSFColor(new byte[]{(byte)217,(byte)240,(byte)217}, null);
        XSSFColor evenColor   = new XSSFColor(new byte[]{(byte)245,(byte)247,(byte)250}, null);

        // ---- 样式工厂 ----
        Supplier<XSSFCellStyle> newStyle = () -> {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setBorderTop(BorderStyle.THIN); cs.setBorderBottom(BorderStyle.THIN);
            cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setWrapText(true);
            return cs;
        };
        java.util.function.BiConsumer<XSSFCellStyle, XSSFColor> setFill = (cs, color) -> {
            cs.setFillForegroundColor(color); cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        };

        XSSFFont titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short)12);
        XSSFFont hdrFont   = wb.createFont(); hdrFont.setBold(true);   hdrFont.setFontHeightInPoints((short)10);
        XSSFFont dataFont  = wb.createFont(); dataFont.setFontHeightInPoints((short)9);
        XSSFFont avoidFont = wb.createFont(); avoidFont.setFontHeightInPoints((short)9); avoidFont.setBold(true);
        avoidFont.setColor(new XSSFColor(new byte[]{(byte)180,0,0}, null));
        XSSFFont avgFont   = wb.createFont(); avgFont.setFontHeightInPoints((short)9); avgFont.setBold(true);
        avgFont.setColor(new XSSFColor(new byte[]{(byte)31,(byte)107,(byte)32}, null));

        XSSFCellStyle titleStyle = newStyle.get(); titleStyle.setFont(titleFont); setFill.accept(titleStyle, titleColor); titleStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle fixStyle   = newStyle.get(); fixStyle.setFont(hdrFont);     setFill.accept(fixStyle,   fixColor);   fixStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle expStyle   = newStyle.get(); expStyle.setFont(hdrFont);     setFill.accept(expStyle,   expColor);   expStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle tailStyle  = newStyle.get(); tailStyle.setFont(hdrFont);    setFill.accept(tailStyle,  tailColor);  tailStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataCenter = newStyle.get(); dataCenter.setFont(dataFont); dataCenter.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataLeft   = newStyle.get(); dataLeft.setFont(dataFont);   dataLeft.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle dataEvenC  = newStyle.get(); dataEvenC.setFont(dataFont);  setFill.accept(dataEvenC, evenColor); dataEvenC.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataEvenL  = newStyle.get(); dataEvenL.setFont(dataFont);  setFill.accept(dataEvenL, evenColor); dataEvenL.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle avoidStyle = newStyle.get(); avoidStyle.setFont(avoidFont); setFill.accept(avoidStyle, avoidColor); avoidStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle avgStyle   = newStyle.get(); avgStyle.setFont(avgFont);     setFill.accept(avgStyle,   avgColor);   avgStyle.setAlignment(HorizontalAlignment.CENTER);

        // ---- Row 0 标题（合并到资料分/平均分列） ----
        int avgCol0 = nFixed + nExperts; // 资料分列
        Row r0 = sheet.createRow(0); r0.setHeightInPoints(26);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(year + "年石油工程建设优秀质量管理小组活动成果资料评分表——" + typeLabel);
        c0.setCellStyle(titleStyle);
        for (int c = 1; c < nTotal; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(c <= avgCol0 ? titleStyle : dataCenter); }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, avgCol0));

        // ---- Row 1 主表头 ----
        Row r1 = sheet.createRow(1); r1.setHeightInPoints(42);
        String[] fixH = {"序号","分派专业组","申报号","课题名称","小组名称","分类"};
        for (int i = 0; i < fixH.length; i++) {
            Cell c = r1.createCell(i); c.setCellValue(fixH[i]); c.setCellStyle(fixStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, i, i));
        }
        if (nExperts > 0) {
            Cell ec = r1.createCell(nFixed); ec.setCellValue("专家打分"); ec.setCellStyle(expStyle);
            if (nExperts > 1) sheet.addMergedRegion(new CellRangeAddress(1, 1, nFixed, nFixed + nExperts - 1));
            else              sheet.addMergedRegion(new CellRangeAddress(1, 2, nFixed, nFixed));
        }
        String[] tailH = {"资料分\n（小数点后两位）","完成单位","申报单位","小组成员"};
        for (int i = 0; i < tailH.length; i++) {
            int col = nFixed + nExperts + i;
            Cell c = r1.createCell(col); c.setCellValue(tailH[i]);
            c.setCellStyle(i == 0 ? avgStyle : tailStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, col, col));
        }
        for (int c = 0; c < nTotal; c++) if (r1.getCell(c) == null) { Cell cc = r1.createCell(c); cc.setCellStyle(c < nFixed ? fixStyle : (c < nFixed+nExperts ? expStyle : tailStyle)); }

        // ---- Row 2 专家姓名子表头 ----
        Row r2 = sheet.createRow(2); r2.setHeightInPoints(32);
        for (int i = 0; i < experts.size(); i++) {
            Cell c = r2.createCell(nFixed + i); c.setCellValue(experts.get(i).getExpertName() != null ? experts.get(i).getExpertName() : ""); c.setCellStyle(expStyle);
        }
        for (int c = 0; c < nTotal; c++) if (r2.getCell(c) == null) { Cell cc = r2.createCell(c); cc.setCellStyle(c < nFixed ? fixStyle : tailStyle); }

        // ---- 数据行 ----
        int avgCol = nFixed + nExperts;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> pro = rows.get(i);
            Row row = sheet.createRow(3 + i); row.setHeightInPoints(18);
            boolean isEven = (i % 2 == 1);
            @SuppressWarnings("unchecked")
            Map<String, String> es = (Map<String, String>) pro.get("expertScores");
            XSSFCellStyle cs = isEven ? dataEvenC : dataCenter;
            XSSFCellStyle lcs = isEven ? dataEvenL : dataLeft;
            setCell(row, 0, String.valueOf(i+1), cs);
            setCell(row, 1, str(pro.get("qcGroupName")), cs);
            setCell(row, 2, str(pro.get("proCode")),  cs);
            setCell(row, 3, str(pro.get("topicName")), lcs);
            setCell(row, 4, str(pro.get("groupName")), lcs);
            setCell(row, 5, str(pro.get("topicType")), cs);
            // 原代码：直接写原始分
            // for (int k = 0; k < experts.size(); k++) {
            //     String acc = experts.get(k).getLoginAccount();
            //     String sc = (es != null && acc != null) ? es.getOrDefault(acc, "") : "";
            //     setCell(row, nFixed + k, sc, "回避".equals(sc) ? avoidStyle : cs);
            // }
            // 先×0.6再求平均，确保Excel中显示值可手动验算
            List<BigDecimal> scaledScores = new ArrayList<>();
            for (int k = 0; k < experts.size(); k++) {
                String acc = experts.get(k).getLoginAccount();
                String sc = (es != null && acc != null) ? es.getOrDefault(acc, "") : "";
                if ("回避".equals(sc)) {
                    setCell(row, nFixed + k, sc, avoidStyle);
                } else {
                    String scaled = scaleScore(sc, 0.6);
                    setCell(row, nFixed + k, scaled, cs);
                    if (!scaled.isEmpty()) {
                        try { scaledScores.add(new BigDecimal(scaled)); } catch (NumberFormatException ignored) {}
                    }
                }
            }
            String avgDisplay = "";
            if (!scaledScores.isEmpty()) {
                List<BigDecimal> toAvg = new ArrayList<>(scaledScores);
                if (toAvg.size() >= 3) {
                    toAvg.sort(BigDecimal::compareTo);
                    toAvg.remove(0);
                    toAvg.remove(toAvg.size() - 1);
                }
                BigDecimal sum = BigDecimal.ZERO;
                for (BigDecimal s : toAvg) sum = sum.add(s);
                avgDisplay = sum.divide(BigDecimal.valueOf(toAvg.size()), 2, BigDecimal.ROUND_HALF_UP).toString();
            }
            setCell(row, avgCol, avgDisplay, avgStyle);
            setCell(row, avgCol+1, str(pro.get("completeUnit")), cs);
            setCell(row, avgCol+2, str(pro.get("companyName")), cs);
            setCell(row, avgCol+3, str(pro.get("groupMember")), cs);
        }

        // ---- 列宽 ----
        sheet.setColumnWidth(0, 1500); // 序号
        sheet.setColumnWidth(1, 4000); // 分派专业组
        sheet.setColumnWidth(2, 3500); // 申报号
        sheet.setColumnWidth(3, 7000); // 课题名称
        sheet.setColumnWidth(4, 5000); // 小组名称
        sheet.setColumnWidth(5, 3500); // 分类
        for (int k = 0; k < nExperts; k++) sheet.setColumnWidth(nFixed + k, 2400);
        sheet.setColumnWidth(avgCol, 2800); sheet.setColumnWidth(avgCol+1, 5500);
        sheet.setColumnWidth(avgCol+2, 3500); sheet.setColumnWidth(avgCol+3, 4500);

        // ---- 冻结：前3行 + 左侧5列 ----
        sheet.createFreezePane(nFixed, 3);
    }

    /**
     * 新导出：按专业组（qcGroupName）分块堆叠（仅用于"任务分数查询"导出Excel）
     * 每个专业组一个区块：标题含组名 + 表头 + 该组专家打分列 + 该组项目数据
     */
    private void buildMatrixSheetByExpert(XSSFWorkbook wb, List<Map<String, Object>> rows,
                                          List<ExpertGroupDO> experts, int year, String typeLabel) {
        String sheetName = (typeLabel != null && typeLabel.contains("创新")) ? "创新型" : "问题解决型";
        XSSFSheet sheet = wb.createSheet(sheetName);

        // ---- 按 qcGroupName 分组项目行（rows 已按 qcGroupName+proCode 排序） ----
        LinkedHashMap<String, List<Map<String, Object>>> groupRowsMap = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String gName = r.get("qcGroupName") != null ? r.get("qcGroupName").toString() : "";
            groupRowsMap.computeIfAbsent(gName, k -> new ArrayList<>()).add(r);
        }
        // ---- 按 groupName 分组专家 ----
        LinkedHashMap<String, List<ExpertGroupDO>> groupExpertsMap = new LinkedHashMap<>();
        for (ExpertGroupDO e : experts) {
            String gName = e.getGroupName() != null ? e.getGroupName().trim() : "";
            groupExpertsMap.computeIfAbsent(gName, k -> new ArrayList<>()).add(e);
        }

        // ---- 颜色 ----
        XSSFColor titleColor  = new XSSFColor(new byte[]{(byte)189,(byte)215,(byte)238}, null);
        XSSFColor fixColor    = new XSSFColor(new byte[]{(byte)217,(byte)225,(byte)242}, null);
        XSSFColor expColor    = new XSSFColor(new byte[]{(byte)252,(byte)228,(byte)214}, null);
        XSSFColor tailColor   = new XSSFColor(new byte[]{(byte)226,(byte)239,(byte)218}, null);
        XSSFColor avoidColor  = new XSSFColor(new byte[]{(byte)255,(byte)255,0}, null);
        XSSFColor avgColor    = new XSSFColor(new byte[]{(byte)217,(byte)240,(byte)217}, null);
        XSSFColor evenColor   = new XSSFColor(new byte[]{(byte)245,(byte)247,(byte)250}, null);

        // ---- 样式 ----
        java.util.function.Supplier<XSSFCellStyle> newStyle = () -> {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setBorderTop(BorderStyle.THIN); cs.setBorderBottom(BorderStyle.THIN);
            cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setWrapText(true);
            return cs;
        };
        java.util.function.BiConsumer<XSSFCellStyle, XSSFColor> setFill = (cs, color) -> {
            cs.setFillForegroundColor(color); cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        };

        XSSFFont titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short)12);
        XSSFFont hdrFont   = wb.createFont(); hdrFont.setBold(true);   hdrFont.setFontHeightInPoints((short)10);
        XSSFFont dataFont  = wb.createFont(); dataFont.setFontHeightInPoints((short)9);
        XSSFFont avoidFont = wb.createFont(); avoidFont.setFontHeightInPoints((short)9); avoidFont.setBold(true);
        avoidFont.setColor(new XSSFColor(new byte[]{(byte)180,0,0}, null));
        XSSFFont avgFont   = wb.createFont(); avgFont.setFontHeightInPoints((short)9); avgFont.setBold(true);
        avgFont.setColor(new XSSFColor(new byte[]{(byte)31,(byte)107,(byte)32}, null));

        XSSFCellStyle titleStyle = newStyle.get(); titleStyle.setFont(titleFont); setFill.accept(titleStyle, titleColor); titleStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle fixStyle   = newStyle.get(); fixStyle.setFont(hdrFont);     setFill.accept(fixStyle,   fixColor);   fixStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle expStyle   = newStyle.get(); expStyle.setFont(hdrFont);     setFill.accept(expStyle,   expColor);   expStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle tailStyle  = newStyle.get(); tailStyle.setFont(hdrFont);    setFill.accept(tailStyle,  tailColor);  tailStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataCenter = newStyle.get(); dataCenter.setFont(dataFont); dataCenter.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataLeft   = newStyle.get(); dataLeft.setFont(dataFont);   dataLeft.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle dataEvenC  = newStyle.get(); dataEvenC.setFont(dataFont);  setFill.accept(dataEvenC, evenColor); dataEvenC.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataEvenL  = newStyle.get(); dataEvenL.setFont(dataFont);  setFill.accept(dataEvenL, evenColor); dataEvenL.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle avoidStyle = newStyle.get(); avoidStyle.setFont(avoidFont); setFill.accept(avoidStyle, avoidColor); avoidStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle avgStyle   = newStyle.get(); avgStyle.setFont(avgFont);     setFill.accept(avgStyle,   avgColor);   avgStyle.setAlignment(HorizontalAlignment.CENTER);

        int cur = 0; // 当前写入的起始行
        boolean frozen = false; // 仅对首个区块设置冻结

        // ---- 遍历每个专业组，逐组生成区块 ----
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupRowsMap.entrySet()) {
            String groupName = entry.getKey();
            List<Map<String, Object>> groupRows = entry.getValue();
            // 该组的专家列表
            List<ExpertGroupDO> groupExperts = groupExpertsMap.getOrDefault(groupName, java.util.Collections.emptyList());

            int nFixed = 6; // 序号、分派专业组、申报号、课题名称、小组名称、分类
            int nGrpExperts = groupExperts.size(); // 该组专家数量
            int nTail = 4;  // 资料分、完成单位、申报单位、小组成员
            int nTotal = nFixed + nGrpExperts + nTail;

            // ---- Row 0 标题（合并到资料分列） ----
            int avgCol0 = nFixed + nGrpExperts;
            Row r0 = sheet.createRow(cur + 0); r0.setHeightInPoints(26);
            Cell c0 = r0.createCell(0);
            c0.setCellValue(year + "年石油工程建设优秀质量管理小组活动成果资料评分表——" + typeLabel + "  " + groupName);
            c0.setCellStyle(titleStyle);
            for (int c = 1; c < nTotal; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(c <= avgCol0 ? titleStyle : dataCenter); }
            sheet.addMergedRegion(new CellRangeAddress(cur + 0, cur + 0, 0, avgCol0));

            // ---- Row 1 主表头 ----
            Row r1 = sheet.createRow(cur + 1); r1.setHeightInPoints(42);
            String[] fixH = {"序号","分派专业组","申报号","课题名称","小组名称","分类"};
            for (int i = 0; i < fixH.length; i++) {
                Cell c = r1.createCell(i); c.setCellValue(fixH[i]); c.setCellStyle(fixStyle);
                sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 2, i, i));
            }
            if (nGrpExperts > 0) {
                Cell ec = r1.createCell(nFixed); ec.setCellValue("专家打分"); ec.setCellStyle(expStyle);
                if (nGrpExperts > 1) sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 1, nFixed, nFixed + nGrpExperts - 1));
                else                 sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 2, nFixed, nFixed));
            }
            String[] tailH = {"资料分\n（小数点后两位）","完成单位","申报单位","小组成员"};
            for (int i = 0; i < tailH.length; i++) {
                int col = nFixed + nGrpExperts + i;
                Cell c = r1.createCell(col); c.setCellValue(tailH[i]);
                c.setCellStyle(i == 0 ? avgStyle : tailStyle);
                sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 2, col, col));
            }
            for (int c = 0; c < nTotal; c++) if (r1.getCell(c) == null) { Cell cc = r1.createCell(c); cc.setCellStyle(c < nFixed ? fixStyle : (c < nFixed+nGrpExperts ? expStyle : tailStyle)); }

            // ---- Row 2 子表头：各专家姓名 ----
            Row r2 = sheet.createRow(cur + 2); r2.setHeightInPoints(32);
            for (int k = 0; k < nGrpExperts; k++) {
                String eName = groupExperts.get(k).getExpertName() != null ? groupExperts.get(k).getExpertName() : "";
                Cell c = r2.createCell(nFixed + k); c.setCellValue(eName); c.setCellStyle(expStyle);
            }
            for (int c = 0; c < nTotal; c++) if (r2.getCell(c) == null) { Cell cc = r2.createCell(c); cc.setCellStyle(c < nFixed ? fixStyle : tailStyle); }

            // ---- 数据行：仅该组的项目 ----
            int startData = cur + 3;
            int avgCol = nFixed + nGrpExperts;
            for (int i = 0; i < groupRows.size(); i++) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pro = groupRows.get(i);
                Row row = sheet.createRow(startData + i); row.setHeightInPoints(18);
                boolean isEven = (i % 2 == 1);
                @SuppressWarnings("unchecked")
                Map<String, String> es = (Map<String, String>) pro.get("expertScores");
                XSSFCellStyle cs  = isEven ? dataEvenC : dataCenter;
                XSSFCellStyle lcs = isEven ? dataEvenL : dataLeft;
                setCell(row, 0, String.valueOf(i+1), cs);
                setCell(row, 1, str(pro.get("qcGroupName")), cs);
                setCell(row, 2, str(pro.get("proCode")), cs);
                setCell(row, 3, str(pro.get("topicName")), lcs);
                setCell(row, 4, str(pro.get("groupName")), lcs);
                setCell(row, 5, str(pro.get("topicType")), cs);

                // 先×0.6再求平均，确保Excel中显示值可手动验算
                List<BigDecimal> scaledScores = new ArrayList<>();
                for (int k = 0; k < nGrpExperts; k++) {
                    String acc = groupExperts.get(k).getLoginAccount();
                    String sc = (es != null && acc != null) ? es.getOrDefault(acc, "") : "";
                    if ("回避".equals(sc)) {
                        setCell(row, nFixed + k, sc, avoidStyle);
                    } else {
                        String scaled = scaleScore(sc, 0.6);
                        setCell(row, nFixed + k, scaled, cs);
                        if (!scaled.isEmpty()) {
                            try { scaledScores.add(new BigDecimal(scaled)); } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                String avgDisplay = "";
                if (!scaledScores.isEmpty()) {
                    List<BigDecimal> toAvg = new ArrayList<>(scaledScores);
                    if (toAvg.size() >= 3) {
                        toAvg.sort(BigDecimal::compareTo);
                        toAvg.remove(0);
                        toAvg.remove(toAvg.size() - 1);
                    }
                    BigDecimal sum = BigDecimal.ZERO;
                    for (BigDecimal s : toAvg) sum = sum.add(s);
                    avgDisplay = sum.divide(BigDecimal.valueOf(toAvg.size()), 2, BigDecimal.ROUND_HALF_UP).toString();
                }
                setCell(row, avgCol, avgDisplay, avgStyle);
                setCell(row, avgCol+1, str(pro.get("completeUnit")), cs);
                setCell(row, avgCol+2, str(pro.get("companyName")), cs);
                setCell(row, avgCol+3, str(pro.get("groupMember")), cs);
            }

            // ---- 列宽 ----
            sheet.setColumnWidth(0, 1500); // 序号
            sheet.setColumnWidth(1, 4000); // 分派专业组
            sheet.setColumnWidth(2, 3500); // 申报号
            sheet.setColumnWidth(3, 7000); // 课题名称
            sheet.setColumnWidth(4, 5000); // 小组名称
            sheet.setColumnWidth(5, 3500); // 分类
            for (int k = 0; k < nGrpExperts; k++) sheet.setColumnWidth(nFixed + k, 2400);
            sheet.setColumnWidth(avgCol,  2800); // 资料分
            sheet.setColumnWidth(avgCol+1, 5500); // 完成单位
            sheet.setColumnWidth(avgCol+2, 3500); // 申报单位
            sheet.setColumnWidth(avgCol+3, 4500); // 小组成员

            if (!frozen) { sheet.createFreezePane(nFixed, 3); frozen = true; }

            // 区块间空一行
            cur = startData + groupRows.size() + 1;
        }
    }

    // ==================== 发布分（第二次打分） ====================

    /**
     * 专家提交发布分（将当前专家在当前任务下所有发布分记录标记为 scoreOver=1）
     */
    @RequestMapping("/submitPresentScore")
    @ResponseBody
    public R submitPresentScore(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(taskId)) return R.error("任务ID不能为空");

        // ===== 前置校验：所有非回避项目必须已评发布分 =====
        Map<String, Object> proQueryParams = new HashMap<>();
        proQueryParams.put("scoreSpecialistUid", String.valueOf(uid));
        proQueryParams.put("taskId", taskId);
        List<QcProDataDto> allProjects = qcAwardService.listProInfo(proQueryParams);
        if (allProjects != null && !allProjects.isEmpty()) {
            // 检查每个非回避项目是否已评发布分
            List<String> unscoredProjects = new ArrayList<>();
            for (QcProDataDto pro : allProjects) {
                if (pro.getProId() == null) continue;
                // 检查是否回避
                boolean isAvoided = avoidanceService.checkAvoidance(taskId, pro.getProId(), uid.intValue());
                if (isAvoided) continue;
                // 检查是否已评发布分
                Map<String, Object> checkScore = new HashMap<>();
                checkScore.put("optUid", uid);
                checkScore.put("proId", pro.getProId());
                checkScore.put("taskId", taskId);
                List<QcPresentScoreDO> presentCheck = qcPresentScoreService.list(checkScore);
                if (presentCheck == null || presentCheck.isEmpty()) {
                    String name = StringUtils.isNotBlank(pro.getTopicName()) ? pro.getTopicName() : ("项目ID:" + pro.getProId());
                    unscoredProjects.add(name);
                }
            }
            if (!unscoredProjects.isEmpty()) {
                return R.error("以下项目尚未评发布分，请完成所有项目发布分评分后再提交：" + String.join("、", unscoredProjects));
            }
        }
        // ===== 前置校验结束 =====

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        queryParams.put("taskId", taskId);
        List<QcPresentScoreDO> list = qcPresentScoreService.list(queryParams);
        if (list == null || list.isEmpty()) return R.error("尚未进行任何发布分打分，无法提交");

        int updated = 0;
        for (QcPresentScoreDO s : list) {
            if (Integer.valueOf(1).equals(s.getScoreOver())) continue;
            s.setScoreOver(1);
            s.setUpdated(new Date());
            updated += qcPresentScoreService.update(s);
        }
        // 更新 add_special_info.present_score_over = 1
        try {
            Map<String, Object> expertQuery = new HashMap<>();
            expertQuery.put("userId", String.valueOf(uid));
            expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            expertQuery.put("taskId", taskId);
            List<ExpertGroupDO> expertList = expertGroupService.list(expertQuery);
            if (!expertList.isEmpty()) {
                ExpertGroupDO expert = expertList.get(0);
                expert.setPresentScoreOver(1);
                expertGroupService.update(expert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok("发布分提交成功，共提交 " + updated + " 条记录");
    }

    /**
     * 按专家维度查询发布分（供 major_group_admin.html 发布分查询弹窗使用）
     */
    @GetMapping("/getExpertPresentScores")
    @ResponseBody
    public R getExpertPresentScores(@RequestParam Map<String, Object> params) {
        try {
            String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
            String expertUidStr = params.get("expertUid") != null ? params.get("expertUid").toString() : "";
            if (taskId.isEmpty() || expertUidStr.isEmpty()) return R.error("参数不完整");
            Integer expertUid = Integer.parseInt(expertUidStr);

            Map<String, Object> proQuery = new HashMap<>();
            proQuery.put("taskId", taskId);
            proQuery.put("scoreSpecialistUid", expertUid);
            List<QcProDataDto> projects = qcAwardService.listProInfo(proQuery);

            List<Integer> avoidedIds = avoidanceService.getAvoidedProIds(taskId, expertUid);
            Set<Integer> avoidedSet = new HashSet<>(avoidedIds != null ? avoidedIds : Collections.emptyList());

            List<Map<String, Object>> result = new ArrayList<>();
            for (QcProDataDto pro : projects) {
                if (pro.getProId() == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("proCode", pro.getApplyId());
                item.put("topicName", pro.getTopicName());
                item.put("groupName", pro.getGroupName());
                item.put("topicType", pro.getTopicType());
                item.put("isAvoided", avoidedSet.contains(pro.getProId()));
                if (!avoidedSet.contains(pro.getProId())) {
                    Map<String, Object> sq = new HashMap<>();
                    sq.put("optUid", expertUid);
                    sq.put("proId", pro.getProId());
                    sq.put("taskId", taskId);
                    List<QcPresentScoreDO> ps = qcPresentScoreService.list(sq);
                    if (!ps.isEmpty()) {
                        item.put("appraiseSum", ps.get(0).getAppraiseSum());
                        item.put("scoreOver", ps.get(0).getScoreOver());
                        item.put("scoreTime", ps.get(0).getUpdated() != null
                                ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(ps.get(0).getUpdated()) : "");
                    } else {
                        item.put("appraiseSum", null);
                        item.put("scoreOver", 0);
                        item.put("scoreTime", "");
                    }
                } else {
                    item.put("appraiseSum", null);
                    item.put("scoreOver", 0);
                    item.put("scoreTime", "");
                }
                result.add(item);
            }
            return R.ok().put("data", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 按任务维度查询发布分（供 qc_pro_list.html 任务发布分查询弹窗使用）
     * 返回 flat list（与 getTaskScores 结构一致），含 qcGroupName / professionalScope，按分派专业组+申报账号排序
     */
    @GetMapping("/getTaskPresentScores")
    @ResponseBody
    public R getTaskPresentScores(@RequestParam Map<String, Object> params) {
        try {
            String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
            if (taskId.isEmpty()) return R.error("参数不完整");

            // 外聘人员（角色72）只能看到绑定组的数据
            boolean isExternalExpert = false;
            Set<String> allowedQcGroups = new HashSet<>();
            UserDO currentUser = getUser();
            if (currentUser.getRoleIds().contains(ROLE_QC_EXTERNAL_EMPLOYMENT_ID)) {
                isExternalExpert = true;
                Map<String, Object> bindingQuery = new HashMap<>();
                bindingQuery.put("taskId", taskId);
                bindingQuery.put("userId", String.valueOf(getUserId()));
                bindingQuery.put("proType", "qc_view_scope");
                List<ExpertGroupDO> bindings = expertGroupService.list(bindingQuery);
                for (ExpertGroupDO b : bindings) {
                    if (b.getGroupName() != null) allowedQcGroups.add(b.getGroupName().trim());
                }
            }
            if (isExternalExpert && allowedQcGroups.isEmpty()) {
                return R.ok().put("data", new ArrayList<>());
            }

            Map<String, Object> proQuery = new HashMap<>();
            proQuery.put("taskId", taskId);
            List<QcProDataDto> projects = qcAwardService.listProInfo(proQuery);

            // 外聘人员过滤：只保留绑定组的项目
            if (isExternalExpert) {
                final Set<String> finalAllowed = allowedQcGroups;
                projects = projects.stream()
                        .filter(p -> p.getQcGroupName() != null && finalAllowed.contains(p.getQcGroupName().trim()))
                        .collect(Collectors.toList());
            }

            // 查询所有发布分
            Map<String, Object> psQuery = new HashMap<>();
            psQuery.put("taskId", taskId);
            List<QcPresentScoreDO> allScores = qcPresentScoreService.list(psQuery);

            // 按 proId 聚合有效分
            Map<Integer, List<BigDecimal>> proValidScores = new HashMap<>();
            for (QcPresentScoreDO s : allScores) {
                if (s.getProId() == null || s.getOptUid() == null) continue;
                // 查该专家是否回避
                List<Integer> avoidedProIds = avoidanceService.getAvoidedProIds(taskId, s.getOptUid());
                if (avoidedProIds != null && avoidedProIds.contains(s.getProId())) continue;
                if (s.getAppraiseSum() != null && !s.getAppraiseSum().isEmpty()) {
                    try {
                        proValidScores.computeIfAbsent(s.getProId(), k -> new ArrayList<>()).add(new BigDecimal(s.getAppraiseSum()));
                    } catch (NumberFormatException ignore) {}
                }
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (QcProDataDto pro : projects) {
                if (pro.getProId() == null) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("proId", pro.getProId());
                item.put("proCode", pro.getApplyId());
                item.put("topicName", pro.getTopicName());
                item.put("groupName", pro.getGroupName());
                item.put("qcGroupName", pro.getQcGroupName());
                item.put("topicType", pro.getTopicType());
                item.put("professionalScope", pro.getProfessionalScope());

                List<BigDecimal> validScores = proValidScores.getOrDefault(pro.getProId(), Collections.emptyList());
                int validCount = validScores.size();
                item.put("scorerCount", validCount);

                BigDecimal avgScore = null;
                if (validCount > 0) {
                    List<BigDecimal> toAvg = new ArrayList<>(validScores);
                    if (validCount >= 3) {
                        toAvg.sort(BigDecimal::compareTo);
                        toAvg.remove(0);
                        toAvg.remove(toAvg.size() - 1);
                    }
                    BigDecimal sum = BigDecimal.ZERO;
                    for (BigDecimal v : toAvg) sum = sum.add(v);
                    avgScore = sum.divide(BigDecimal.valueOf(toAvg.size()), 2, BigDecimal.ROUND_HALF_UP);
                }
                item.put("avgScore", avgScore);
                result.add(item);
            }

            // 排序：先按分派专业组，再按申报账号
            result.sort((a, b) -> {
                String g1 = a.get("qcGroupName") != null ? a.get("qcGroupName").toString() : "";
                String g2 = b.get("qcGroupName") != null ? b.get("qcGroupName").toString() : "";
                int gc = g1.compareTo(g2);
                if (gc != 0) return gc;
                String c1 = a.get("proCode") != null ? a.get("proCode").toString() : "";
                String c2 = b.get("proCode") != null ? b.get("proCode").toString() : "";
                return c1.compareTo(c2);
            });

            return R.ok().put("data", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 跳转到发布分打分页面
     */
    @RequestMapping("/toPresentScore")
    public String toPresentScore(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        Long uid = getUserId();
        String proId = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";

        if (StringUtils.isNotBlank(proId)) {
            Map<String, Object> proParams = new HashMap<>();
            proParams.put("proId", proId);
            proParams.put("taskId", taskId);
            List<QcProDataDto> proList = qcAwardService.listProInfo(proParams);
            if (proList != null && !proList.isEmpty()) {
                map.put("proInfo", proList.get(0));
            }
        }
        map.put("proId", proId);
        map.put("expertUid", uid);
        String topicType = params.get("topicType") != null ? params.get("topicType").toString() : "";
        map.put("topicType", topicType);
        return prefix + "/score/specialist_present_score";
    }

    /**
     * 获取当前专家对某项目的发布分打分数据（JSON）
     */
    @RequestMapping("/getPresentScore")
    @ResponseBody
    public R getPresentScore(@RequestParam Map<String, Object> params) {
        Long uid = getUserId();
        String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (StringUtils.isBlank(proIdStr)) {
            return R.error("项目ID不能为空");
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        queryParams.put("proId", Integer.parseInt(proIdStr));
        queryParams.put("taskId", taskId);
        List<QcPresentScoreDO> list = qcPresentScoreService.list(queryParams);
        R r = R.ok();
        r.put("score", (list != null && !list.isEmpty()) ? list.get(0) : new QcPresentScoreDO());
        return r;
    }

    /**
     * 保存或更新发布分打分
     */
    @RequestMapping("/savePresentScore")
    @ResponseBody
    public R savePresentScore(QcPresentScoreDO score) {
        Long uid = getUserId();
        score.setOptUid(uid.intValue());
        if (score.getProId() == null || StringUtils.isBlank(score.getTaskId())) {
            return R.error("项目信息缺失，无法保存");
        }
        score.setUpdated(new Date());
        if (score.getDeleted() == null) {
            score.setDeleted(0);
        }

        BigDecimal total = BigDecimal.ZERO;
        total = total.add(score.getLogicScore() == null ? BigDecimal.ZERO : score.getLogicScore());
        total = total.add(score.getProfessionalScore() == null ? BigDecimal.ZERO : score.getProfessionalScore());
        total = total.add(score.getPresentFormatScore() == null ? BigDecimal.ZERO : score.getPresentFormatScore());
        total = total.add(score.getExpressionScore() == null ? BigDecimal.ZERO : score.getExpressionScore());
        total = total.add(score.getAnswerScore() == null ? BigDecimal.ZERO : score.getAnswerScore());
        total = total.add(score.getMemberTimeScore() == null ? BigDecimal.ZERO : score.getMemberTimeScore());
        score.setAppraiseSum(total.toPlainString());

        Map<String, Object> checkParams = new HashMap<>();
        checkParams.put("optUid", uid);
        checkParams.put("proId", score.getProId());
        checkParams.put("taskId", score.getTaskId());
        List<QcPresentScoreDO> existList = qcPresentScoreService.list(checkParams);
        int tag;
        if (existList != null && !existList.isEmpty()) {
            QcPresentScoreDO existing = existList.get(0);
            if (Integer.valueOf(1).equals(existing.getScoreOver())) {
                return R.error("已提交打分，不可再修改。");
            }
            score.setId(existing.getId());
            tag = qcPresentScoreService.update(score);
        } else {
            tag = qcPresentScoreService.save(score);
        }
        if (tag > 0) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }

    /**
     * 导出专家发布分打分情况为 Excel（含签章图片，问题解决型和创新型分sheet）
     */
    @GetMapping("/exportExpertPresentScoresExcel")
    public void exportExpertPresentScoresExcel(@RequestParam Map<String, Object> params,
                                                HttpServletResponse response) throws Exception {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String expertUidStr = params.get("expertUid") != null ? params.get("expertUid").toString() : "";
        String expertName = params.get("expertName") != null ? params.get("expertName").toString() : "专家";
        String groupName = params.get("groupName") != null ? params.get("groupName").toString() : "";
        if (taskId.isEmpty() || expertUidStr.isEmpty()) {
            response.sendError(400, "参数不完整");
            return;
        }
        Integer expertUid = Integer.parseInt(expertUidStr);

        // ---- 查数据 ----
        Map<String, Object> proQuery = new HashMap<>();
        proQuery.put("taskId", taskId);
        proQuery.put("scoreSpecialistUid", expertUid);
        List<QcProDataDto> assignedProjects = qcAwardService.listProInfo(proQuery);
        List<Integer> avoidedProIdList = avoidanceService.getAvoidedProIds(taskId, expertUid);
        Set<Integer> avoidedProIds = new HashSet<>(avoidedProIdList != null ? avoidedProIdList : Collections.emptyList());

        List<Map<String, Object>> allItems = new ArrayList<>();
        for (QcProDataDto pro : assignedProjects) {
            if (pro.getProId() == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("proCode", pro.getApplyId());
            item.put("topicName", pro.getTopicName());
            item.put("groupName", pro.getGroupName());
            item.put("topicType", pro.getTopicType());
            item.put("professionalScope", pro.getProfessionalScope());
            item.put("completeUnit", pro.getCompleteUnit());
            item.put("companyName", pro.getCompanyName());
            item.put("groupMember", pro.getGroupMember());
            boolean isAvoided = avoidedProIds.contains(pro.getProId());
            item.put("isAvoided", isAvoided);
            List<Map<String, Object>> details = new ArrayList<>();
            String totalScore = null;
            if (!isAvoided) {
                Map<String, Object> sq = new HashMap<>();
                sq.put("taskId", taskId); sq.put("proId", pro.getProId()); sq.put("optUid", expertUid);
                List<QcPresentScoreDO> ps = qcPresentScoreService.list(sq);
                if (!ps.isEmpty()) {
                    QcPresentScoreDO s = ps.get(0);
                    totalScore = s.getAppraiseSum();
                    details.add(buildScoreDetail("逻辑性", s.getLogicScore()));
                    details.add(buildScoreDetail("专业性", s.getProfessionalScore()));
                    details.add(buildScoreDetail("发布形式", s.getPresentFormatScore()));
                    details.add(buildScoreDetail("表达", s.getExpressionScore()));
                    details.add(buildScoreDetail("回答问题", s.getAnswerScore()));
                    details.add(buildScoreDetail("成员/时间", s.getMemberTimeScore()));
                }
            }
            item.put("scoreDetails", details);
            item.put("totalScore", totalScore);
            allItems.add(item);
        }

        // ---- 查签章路径 ----
        String signFilePath = null;
        try {
            Map<String, Object> eq = new HashMap<>();
            eq.put("userId", expertUidStr); eq.put("taskId", taskId);
            eq.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            List<ExpertGroupDO> el = expertGroupService.list(eq);
            if (!el.isEmpty() && el.get(0).getExpertSignUrl() != null) {
                String signUrl = el.get(0).getExpertSignUrl();
                String uploadRoot = bootdoConfig.getUploadPath();
                if (uploadRoot.endsWith("/**")) uploadRoot = uploadRoot.substring(0, uploadRoot.length() - 3);
                signFilePath = uploadRoot + signUrl.replace("/files/", "/");
            }
        } catch (Exception ignore) {}

        // ---- 生成 Excel（单sheet，所有课题类型合并）----
        int year = Calendar.getInstance().get(Calendar.YEAR);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            byte[] signImgBytes = null;
            int signPicType = XSSFWorkbook.PICTURE_TYPE_PNG;
            if (signFilePath != null) {
                File sf = new File(signFilePath);
                if (sf.exists()) {
                    signImgBytes = Files.readAllBytes(sf.toPath());
                    String ext = signFilePath.substring(signFilePath.lastIndexOf('.') + 1).toLowerCase();
                    if ("jpg".equals(ext) || "jpeg".equals(ext)) signPicType = XSSFWorkbook.PICTURE_TYPE_JPEG;
                    else if ("gif".equals(ext)) signPicType = XSSFWorkbook.PICTURE_TYPE_GIF;
                }
            }
            int pictureIdx = signImgBytes != null ? wb.addPicture(signImgBytes, signPicType) : -1;

            buildPresentScoreSheet(wb, allItems, groupName, year, pictureIdx);

            String fileName = URLEncoder.encode(expertName + "_发布分打分情况.xlsx", "UTF-8").replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            wb.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

    /**
     * 构建发布分评分sheet（单sheet，所有课题类型合并，按模板样式）
     */
    private void buildPresentScoreSheet(XSSFWorkbook wb, List<Map<String, Object>> items,
                                         String groupName, int year, int pictureIdx) {
        int nFixed = 6;      // 序号,申报账号,课题名称,小组名称,课题类型,分类
        int nScore = 6;      // 逻辑性,专业性,发布形式,表达,回答问题,成员/时间
        int scoreStart = nFixed;
        int totalCol = nFixed + nScore;   // col 12 = 发布分
        int refStart = totalCol + 1;      // col 13
        int nTotal = refStart + 3;        // 16 columns (0-15)

        XSSFSheet sheet = wb.createSheet("发布分");

        // ---- 样式（与资料分 buildExpertScoreSheet 一致）----
        XSSFCellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 11);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setWrapText(true);
        setBorder(titleStyle);

        XSSFCellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true); headerFont.setFontHeightInPoints((short) 9);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)217,(byte)225,(byte)242}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(headerStyle);

        XSSFCellStyle refNoteStyle = wb.createCellStyle();
        XSSFFont refNoteFont = wb.createFont();
        refNoteFont.setBold(true); refNoteFont.setFontHeightInPoints((short) 9);
        refNoteStyle.setFont(refNoteFont);
        refNoteStyle.setAlignment(HorizontalAlignment.CENTER);
        refNoteStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        refNoteStyle.setWrapText(true);
        setBorder(refNoteStyle);

        XSSFCellStyle dataStyle = wb.createCellStyle();
        XSSFFont dataFont = wb.createFont();
        dataFont.setFontHeightInPoints((short) 9);
        dataStyle.setFont(dataFont);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setWrapText(true);
        setBorder(dataStyle);

        XSSFCellStyle avoidStyle = wb.createCellStyle();
        XSSFFont avoidFont0 = wb.createFont();
        avoidFont0.setFontHeightInPoints((short) 9); avoidFont0.setBold(true);
        avoidFont0.setColor(new XSSFColor(new byte[]{(byte)204,0,0}, null));
        avoidStyle.setFont(avoidFont0);
        avoidStyle.setAlignment(HorizontalAlignment.CENTER);
        avoidStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        avoidStyle.setWrapText(true);
        avoidStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255,(byte)255,0}, null));
        avoidStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(avoidStyle);

        XSSFCellStyle sigStyle = wb.createCellStyle();
        sigStyle.setAlignment(HorizontalAlignment.LEFT);
        sigStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(sigStyle);

        // ---- Row 0: 标题 + "(以下列不打印，供参考)" ----
        Row r0 = sheet.createRow(0); r0.setHeightInPoints(24);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(year + "年石油工程建设优秀质量管理小组活动成果现场发布评分表  " + (groupName != null ? groupName : ""));
        c0.setCellStyle(titleStyle);
        for (int c = 1; c <= totalCol; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(titleStyle); }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCol));

        Cell refNote = r0.createCell(refStart);
        refNote.setCellValue("(以下列不打印，供参考)");
        refNote.setCellStyle(refNoteStyle);
        for (int c = refStart + 1; c < nTotal; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(refNoteStyle); }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, refStart, nTotal - 1));

        // ---- Row 1: 主表头 ----
        Row r1 = sheet.createRow(1); r1.setHeightInPoints(28);
        String[] fixH = {"序号", "申报账号", "课题名称", "小组名称", "课题类型", "分类"};
        for (int i = 0; i < fixH.length; i++) {
            Cell c = r1.createCell(i); c.setCellValue(fixH[i]); c.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, i, i));
        }
        Cell scoreH = r1.createCell(scoreStart);
        scoreH.setCellValue("评审项目"); scoreH.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, scoreStart, scoreStart + nScore - 1));

        Cell totalH = r1.createCell(totalCol);
        totalH.setCellValue("发布分\n(满分28)"); totalH.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 2, totalCol, totalCol));

        String[] refH = {"完成单位", "申报单位", "小组成员"};
        for (int i = 0; i < refH.length; i++) {
            Cell c = r1.createCell(refStart + i); c.setCellValue(refH[i]); c.setCellStyle(refNoteStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 2, refStart + i, refStart + i));
        }
        for (int c = 0; c < nTotal; c++) {
            if (r1.getCell(c) == null) { Cell cc = r1.createCell(c); cc.setCellStyle(headerStyle); }
        }

        // ---- Row 2: 评审项目子表头 ----
        Row r2 = sheet.createRow(2); r2.setHeightInPoints(32);
        String[] scoreSubH = {"逻辑性\n(6)", "专业性\n(6)", "发布形式\n(4)", "表达\n(4)", "回答问题\n(4)", "成员/时间\n(4)"};
        for (int i = 0; i < scoreSubH.length; i++) {
            Cell c = r2.createCell(scoreStart + i); c.setCellValue(scoreSubH[i]); c.setCellStyle(headerStyle);
        }
        for (int c = 0; c < nTotal; c++) {
            if (r2.getCell(c) == null) { Cell cc = r2.createCell(c); cc.setCellStyle(headerStyle); }
        }

        // ---- 数据行 ----
        int seq = 1;
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            Row row = sheet.createRow(3 + i); row.setHeightInPoints(24);
            boolean isAvoided = Boolean.TRUE.equals(item.get("isAvoided"));

            setCell(row, 0, String.valueOf(seq++), dataStyle);
            setCell(row, 1, str(item.get("proCode")), dataStyle);
            setCell(row, 2, str(item.get("topicName")), dataStyle);
            setCell(row, 3, str(item.get("groupName")), dataStyle);
            setCell(row, 4, str(item.get("topicType")), dataStyle);
            setCell(row, 5, str(item.get("professionalScope")), dataStyle);

            if (isAvoided) {
                for (int c = scoreStart; c < scoreStart + nScore; c++) setCell(row, c, "", dataStyle);
                setCell(row, totalCol, "回避", avoidStyle);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> details = (List<Map<String, Object>>) item.get("scoreDetails");
                if (details != null && !details.isEmpty()) {
                    for (int di = 0; di < nScore && di < details.size(); di++) {
                        Object score = details.get(di).get("score");
                        setCell(row, scoreStart + di, score != null ? score.toString() : "", dataStyle);
                    }
                } else {
                    for (int c = scoreStart; c < scoreStart + nScore; c++) setCell(row, c, "", dataStyle);
                }
                setCell(row, totalCol, str(item.get("totalScore")), dataStyle);
            }

            setCell(row, refStart, str(item.get("completeUnit")), dataStyle);
            setCell(row, refStart + 1, str(item.get("companyName")), dataStyle);
            setCell(row, refStart + 2, str(item.get("groupMember")), dataStyle);
        }

        // ---- 空白行 + 签章行 ----
        int blankRow = 3 + items.size();
        Row rBlank = sheet.createRow(blankRow); rBlank.setHeightInPoints(20);
        for (int c = 0; c < nTotal; c++) { Cell cell = rBlank.createCell(c); cell.setCellStyle(dataStyle); }

        int sigRow = blankRow + 1;
        Row rSig = sheet.createRow(sigRow); rSig.setHeightInPoints(25);
        for (int c = 0; c < totalCol; c++) { Cell cell = rSig.createCell(c); cell.setCellStyle(dataStyle); }
        Cell sigCell = rSig.createCell(totalCol);
        sigCell.setCellValue("评委："); sigCell.setCellStyle(sigStyle);
        for (int c = totalCol + 1; c < nTotal; c++) { Cell cell = rSig.createCell(c); cell.setCellStyle(dataStyle); }

        Row rTime = sheet.createRow(sigRow + 1); rTime.setHeightInPoints(20);
        String timeStr = new SimpleDateFormat("yyyy.M.d").format(new Date());
        for (int c = 0; c < totalCol; c++) { Cell cell = rTime.createCell(c); cell.setCellStyle(dataStyle); }
        Cell timeCell = rTime.createCell(totalCol);
        timeCell.setCellValue("时间：" + timeStr); timeCell.setCellStyle(sigStyle);
        for (int c = totalCol + 1; c < nTotal; c++) { Cell cell = rTime.createCell(c); cell.setCellStyle(dataStyle); }

        // ---- 签章图片（紧跟"评委："右侧）----
        if (pictureIdx >= 0) {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = new XSSFClientAnchor(500000, 20000, 0, 0, totalCol, sigRow, totalCol + 1, sigRow + 1);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIdx);
        }

        // ---- 列宽 ----
        sheet.setColumnWidth(0, 1500);   // 序号
        sheet.setColumnWidth(1, 2500);   // 申报账号
        sheet.setColumnWidth(2, 7000);   // 课题名称
        sheet.setColumnWidth(3, 4500);   // 小组名称
        sheet.setColumnWidth(4, 3000);   // 课题类型
        sheet.setColumnWidth(5, 2500);   // 分类
        for (int c = scoreStart; c < scoreStart + nScore; c++) sheet.setColumnWidth(c, 2800);
        sheet.setColumnWidth(totalCol, 4500);  // 发布分（加宽适应评委/时间行）
        sheet.setColumnWidth(refStart, 5500);  // 完成单位
        sheet.setColumnWidth(refStart + 1, 5500); // 申报单位
        sheet.setColumnWidth(refStart + 2, 4500); // 小组成员
    }

    /**
     * 查询单个课题的发布分详细打分情况
     * @param params taskId, proId
     */
    @GetMapping("/getProjectPresentScoreDetail")
    @ResponseBody
    public R getProjectPresentScoreDetail(@RequestParam Map<String, Object> params) {
        try {
            String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
            String proIdStr = params.get("proId") != null ? params.get("proId").toString() : "";
            if (taskId.isEmpty() || proIdStr.isEmpty()) return R.error("参数不完整");
            Integer proId = Integer.parseInt(proIdStr);

            // 获取该课题的 qcGroupName（分派专业组）
            Map<String, Object> proDataParams = new HashMap<>();
            proDataParams.put("proId", proId);
            proDataParams.put("taskId", taskId);
            List<QcProDataDto> proDataList = qcAwardService.listProInfo(proDataParams);
            String qcGroupName = (proDataList != null && !proDataList.isEmpty())
                    ? proDataList.get(0).getQcGroupName() : null;

            // 仅查询该专业组的专家
            Map<String, Object> expertQuery = new HashMap<>();
            expertQuery.put("taskId", taskId);
            expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
            if (qcGroupName != null && !qcGroupName.isEmpty()) {
                expertQuery.put("groupName", qcGroupName);
            }
            List<ExpertGroupDO> experts = expertGroupService.list(expertQuery);

            List<Integer> avoidedExpertIds = avoidanceService.getAvoidedExpertIds(taskId, proId);
            Set<Integer> avoidedSet = new HashSet<>(avoidedExpertIds != null ? avoidedExpertIds : Collections.emptyList());

            List<Map<String, Object>> result = new ArrayList<>();
            for (ExpertGroupDO expert : experts) {
                if (expert.getUserId() == null) continue;
                Integer expertUid;
                try {
                    expertUid = Integer.parseInt(expert.getUserId());
                } catch (NumberFormatException e) {
                    continue;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("expertName", expert.getExpertName());
                item.put("isAvoided", avoidedSet.contains(expertUid));

                if (!avoidedSet.contains(expertUid)) {
                    Map<String, Object> sq = new HashMap<>();
                    sq.put("taskId", taskId);
                    sq.put("proId", proId);
                    sq.put("optUid", expertUid);
                    List<QcPresentScoreDO> scores = qcPresentScoreService.list(sq);
                    if (!scores.isEmpty()) {
                        QcPresentScoreDO score = scores.get(0);
                        item.put("score", score.getAppraiseSum());
                        item.put("scoreTime", score.getUpdated() != null
                                ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(score.getUpdated()) : "");
                    } else {
                        item.put("score", null);
                        item.put("scoreTime", "");
                    }
                } else {
                    item.put("score", null);
                    item.put("scoreTime", "");
                }
                result.add(item);
            }
            return R.ok().put("data", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 导出任务发布分评分矩阵 Excel（单sheet，按分派专业组堆叠）
     */
    @GetMapping("/exportTaskPresentScoreMatrixExcel")
    public void exportTaskPresentScoreMatrixExcel(@RequestParam Map<String, Object> params,
                                                   HttpServletResponse response) throws Exception {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        if (taskId.isEmpty()) { response.sendError(400, "参数不完整"); return; }

        // 查询专家列表
        Map<String, Object> expertQuery = new HashMap<>();
        expertQuery.put("taskId", taskId);
        expertQuery.put("proType", EnumProjectType.QC_PRO_GROUP.getProType());
        List<ExpertGroupDO> experts = expertGroupService.list(expertQuery);

        // 查询项目列表
        Map<String, Object> proQuery = new HashMap<>();
        proQuery.put("taskId", taskId);
        List<QcProDataDto> projects = qcAwardService.listProInfo(proQuery);

        // 查询所有发布分
        Map<String, Object> psQuery = new HashMap<>();
        psQuery.put("taskId", taskId);
        List<QcPresentScoreDO> allScores = qcPresentScoreService.list(psQuery);
        Map<String, String> scoreMap = new HashMap<>();
        for (QcPresentScoreDO s : allScores) {
            if (s.getProId() != null && s.getOptUid() != null)
                scoreMap.put(s.getProId() + "_" + s.getOptUid(), s.getAppraiseSum() != null ? s.getAppraiseSum() : "");
        }

        // 查询回避 + 分配信息
        Map<Integer, Set<Integer>> avoidMap  = new HashMap<>();
        Map<Integer, Set<Integer>> assignMap = new HashMap<>();
        for (ExpertGroupDO e : experts) {
            if (e.getUserId() == null) continue;
            try {
                Integer uid = Integer.parseInt(e.getUserId());
                List<Integer> ap = avoidanceService.getAvoidedProIds(taskId, uid);
                avoidMap.put(uid, new HashSet<>(ap != null ? ap : Collections.emptyList()));
                Map<String, Object> aq = new HashMap<>();
                aq.put("taskId", taskId); aq.put("scoreSpecialistUid", uid);
                List<QcProDataDto> al = qcAwardService.listProInfo(aq);
                Set<Integer> aids = new HashSet<>();
                if (al != null) for (QcProDataDto p : al) if (p.getProId() != null) aids.add(p.getProId());
                assignMap.put(uid, aids);
            } catch (NumberFormatException ignored) {}
        }

        // 构建行数据（所有课题类型合并，含 expertScores map）
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (QcProDataDto pro : projects) {
            if (pro.getProId() == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("proCode", pro.getApplyId()); row.put("topicName", pro.getTopicName());
            row.put("groupName", pro.getGroupName()); row.put("topicType", pro.getTopicType());
            row.put("qcGroupName", pro.getQcGroupName());
            row.put("professionalScope", pro.getProfessionalScope());
            row.put("completeUnit", pro.getCompleteUnit()); row.put("companyName", pro.getCompanyName());
            row.put("groupMember", pro.getGroupMember());
            Map<String, String> expertScores = new LinkedHashMap<>();
            for (ExpertGroupDO e : experts) {
                if (e.getUserId() == null || e.getLoginAccount() == null) continue;
                try {
                    Integer uid = Integer.parseInt(e.getUserId());
                    String key = pro.getProId() + "_" + uid;
                    Set<Integer> avoided  = avoidMap.getOrDefault(uid, Collections.emptySet());
                    Set<Integer> assigned = assignMap.getOrDefault(uid, Collections.emptySet());
                    if (!assigned.contains(pro.getProId())) {
                        expertScores.put(e.getLoginAccount(), "");
                    } else if (avoided.contains(pro.getProId())) {
                        expertScores.put(e.getLoginAccount(), "回避");
                    } else {
                        String sc = scoreMap.get(key);
                        expertScores.put(e.getLoginAccount(), sc != null ? sc : "");
                    }
                } catch (NumberFormatException ignored) { expertScores.put(e.getLoginAccount(), ""); }
            }
            row.put("expertScores", expertScores);
            allRows.add(row);
        }

        // 排序：先按分派专业组，再按申报账号
        allRows.sort((a, b) -> {
            String g1 = a.get("qcGroupName") != null ? a.get("qcGroupName").toString() : "";
            String g2 = b.get("qcGroupName") != null ? b.get("qcGroupName").toString() : "";
            int gc = g1.compareTo(g2);
            if (gc != 0) return gc;
            String c1 = a.get("proCode") != null ? a.get("proCode").toString() : "";
            String c2 = b.get("proCode") != null ? b.get("proCode").toString() : "";
            return c1.compareTo(c2);
        });

        int year = Calendar.getInstance().get(Calendar.YEAR);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            buildTaskPresentScoreSheetByGroup(wb, allRows, experts, year);
            String fileName = URLEncoder.encode(year + "年_任务发布分计算.xlsx", "UTF-8").replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            wb.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

    /**
     * 构建任务发布分评分sheet（单sheet，按分派专业组堆叠，样式与模板一致）
     */
    private void buildTaskPresentScoreSheetByGroup(XSSFWorkbook wb, List<Map<String, Object>> rows,
                                                    List<ExpertGroupDO> experts, int year) {
        XSSFSheet sheet = wb.createSheet("发布分");

        // ---- 按 qcGroupName 分组项目行（rows 已按 qcGroupName+proCode 排序） ----
        java.util.LinkedHashMap<String, List<Map<String, Object>>> groupRowsMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String gName = r.get("qcGroupName") != null ? r.get("qcGroupName").toString() : "";
            groupRowsMap.computeIfAbsent(gName, k -> new ArrayList<>()).add(r);
        }
        // 按 groupName 分组专家
        java.util.LinkedHashMap<String, List<ExpertGroupDO>> groupExpertsMap = new java.util.LinkedHashMap<>();
        for (ExpertGroupDO e : experts) {
            String gName = e.getGroupName() != null ? e.getGroupName().trim() : "";
            groupExpertsMap.computeIfAbsent(gName, k -> new ArrayList<>()).add(e);
        }

        // ---- 颜色 ----
        XSSFColor titleColor  = new XSSFColor(new byte[]{(byte)189,(byte)215,(byte)238}, null);
        XSSFColor fixColor    = new XSSFColor(new byte[]{(byte)217,(byte)225,(byte)242}, null);
        XSSFColor expColor    = new XSSFColor(new byte[]{(byte)252,(byte)228,(byte)214}, null);
        XSSFColor tailColor   = new XSSFColor(new byte[]{(byte)226,(byte)239,(byte)218}, null);
        XSSFColor avoidColor  = new XSSFColor(new byte[]{(byte)255,(byte)255,0}, null);
        XSSFColor avgColor    = new XSSFColor(new byte[]{(byte)217,(byte)240,(byte)217}, null);
        XSSFColor evenColor   = new XSSFColor(new byte[]{(byte)245,(byte)247,(byte)250}, null);

        // ---- 样式 ----
        java.util.function.Supplier<XSSFCellStyle> newStyle = () -> {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setBorderTop(BorderStyle.THIN); cs.setBorderBottom(BorderStyle.THIN);
            cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setWrapText(true);
            return cs;
        };
        java.util.function.BiConsumer<XSSFCellStyle, XSSFColor> setFill = (cs, color) -> {
            cs.setFillForegroundColor(color); cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        };

        XSSFFont titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short)12);
        XSSFFont hdrFont   = wb.createFont(); hdrFont.setBold(true);   hdrFont.setFontHeightInPoints((short)10);
        XSSFFont dataFont  = wb.createFont(); dataFont.setFontHeightInPoints((short)9);
        XSSFFont avoidFont = wb.createFont(); avoidFont.setFontHeightInPoints((short)9); avoidFont.setBold(true);
        avoidFont.setColor(new XSSFColor(new byte[]{(byte)180,0,0}, null));
        XSSFFont avgFont   = wb.createFont(); avgFont.setFontHeightInPoints((short)9); avgFont.setBold(true);
        avgFont.setColor(new XSSFColor(new byte[]{(byte)31,(byte)107,(byte)32}, null));
        XSSFFont refNoteFont = wb.createFont(); refNoteFont.setBold(true); refNoteFont.setFontHeightInPoints((short)9);

        XSSFCellStyle titleStyle  = newStyle.get(); titleStyle.setFont(titleFont);   setFill.accept(titleStyle, titleColor); titleStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle fixStyle    = newStyle.get(); fixStyle.setFont(hdrFont);       setFill.accept(fixStyle,   fixColor);   fixStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle expStyle    = newStyle.get(); expStyle.setFont(hdrFont);       setFill.accept(expStyle,   expColor);   expStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle tailStyle   = newStyle.get(); tailStyle.setFont(hdrFont);      setFill.accept(tailStyle,  tailColor);  tailStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataCenter  = newStyle.get(); dataCenter.setFont(dataFont);    dataCenter.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataLeft    = newStyle.get(); dataLeft.setFont(dataFont);      dataLeft.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle dataEvenC   = newStyle.get(); dataEvenC.setFont(dataFont);     setFill.accept(dataEvenC, evenColor); dataEvenC.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataEvenL   = newStyle.get(); dataEvenL.setFont(dataFont);     setFill.accept(dataEvenL, evenColor); dataEvenL.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle avoidStyle  = newStyle.get(); avoidStyle.setFont(avoidFont);   setFill.accept(avoidStyle, avoidColor); avoidStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle avgStyle    = newStyle.get(); avgStyle.setFont(avgFont);       setFill.accept(avgStyle,   avgColor);   avgStyle.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle refNoteStyle = newStyle.get(); refNoteStyle.setFont(refNoteFont); refNoteStyle.setAlignment(HorizontalAlignment.CENTER);

        int cur = 0;
        boolean frozen = false;

        // ---- 遍历每个专业组，逐组生成区块 ----
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupRowsMap.entrySet()) {
            String groupName = entry.getKey();
            List<Map<String, Object>> groupRows = entry.getValue();
            List<ExpertGroupDO> groupExperts = groupExpertsMap.getOrDefault(groupName, java.util.Collections.emptyList());

            int nFixed = 7; // 发布序号, 分派专业组, 申报账号, 课题名称, 小组名称, 课题类型, 分类
            int nGrpExperts = groupExperts.size();
            int nTail = 3;  // 完成单位, 申报单位, 小组成员
            int avgCol = nFixed + nGrpExperts;  // 发布分列
            int refStart = avgCol + 1;
            int nTotal = refStart + nTail;

            // ---- Row 0: 标题 + "(以下列不打印，供参考)" ----
            Row r0 = sheet.createRow(cur); r0.setHeightInPoints(26);
            Cell c0 = r0.createCell(0);
            c0.setCellValue(year + "年石油工程建设优秀质量管理小组活动成果现场发布评分表  " + groupName);
            c0.setCellStyle(titleStyle);
            for (int c = 1; c <= avgCol; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(titleStyle); }
            sheet.addMergedRegion(new CellRangeAddress(cur, cur, 0, avgCol));

            Cell refNote = r0.createCell(refStart);
            refNote.setCellValue("(以下列不打印，供参考)"); refNote.setCellStyle(refNoteStyle);
            for (int c = refStart + 1; c < nTotal; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(refNoteStyle); }
            if (nTail > 1) sheet.addMergedRegion(new CellRangeAddress(cur, cur, refStart, nTotal - 1));

            // ---- Row 1: 主表头 ----
            Row r1 = sheet.createRow(cur + 1); r1.setHeightInPoints(42);
            String[] fixH = {"发布\n序号", "分派\n专业组", "申报账号", "课题名称", "小组名称", "课题类型", "分类"};
            for (int i = 0; i < fixH.length; i++) {
                Cell c = r1.createCell(i); c.setCellValue(fixH[i]); c.setCellStyle(fixStyle);
                sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 2, i, i));
            }
            if (nGrpExperts > 0) {
                Cell ec = r1.createCell(nFixed); ec.setCellValue("专家打分"); ec.setCellStyle(expStyle);
                if (nGrpExperts > 1) sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 1, nFixed, nFixed + nGrpExperts - 1));
                else                 sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 2, nFixed, nFixed));
            }
            Cell avgH = r1.createCell(avgCol); avgH.setCellValue("发布分"); avgH.setCellStyle(avgStyle);
            sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 2, avgCol, avgCol));

            String[] refH = {"完成单位", "申报单位", "小组成员"};
            for (int i = 0; i < refH.length; i++) {
                Cell c = r1.createCell(refStart + i); c.setCellValue(refH[i]); c.setCellStyle(tailStyle);
                sheet.addMergedRegion(new CellRangeAddress(cur + 1, cur + 2, refStart + i, refStart + i));
            }
            for (int c = 0; c < nTotal; c++) if (r1.getCell(c) == null) {
                Cell cc = r1.createCell(c); cc.setCellStyle(c < nFixed ? fixStyle : (c < nFixed + nGrpExperts ? expStyle : tailStyle));
            }

            // ---- Row 2: 专家姓名子表头 ----
            Row r2 = sheet.createRow(cur + 2); r2.setHeightInPoints(32);
            for (int k = 0; k < nGrpExperts; k++) {
                String eName = groupExperts.get(k).getExpertName() != null ? groupExperts.get(k).getExpertName() : "";
                Cell c = r2.createCell(nFixed + k); c.setCellValue(eName); c.setCellStyle(expStyle);
            }
            for (int c = 0; c < nTotal; c++) if (r2.getCell(c) == null) {
                Cell cc = r2.createCell(c); cc.setCellStyle(c < nFixed ? fixStyle : tailStyle);
            }

            // ---- 数据行 ----
            int startData = cur + 3;
            for (int i = 0; i < groupRows.size(); i++) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pro = groupRows.get(i);
                Row row = sheet.createRow(startData + i); row.setHeightInPoints(18);
                boolean isEven = (i % 2 == 1);
                XSSFCellStyle cs  = isEven ? dataEvenC : dataCenter;
                XSSFCellStyle lcs = isEven ? dataEvenL : dataLeft;

                setCell(row, 0, String.valueOf(i + 1), cs);
                setCell(row, 1, str(pro.get("qcGroupName")), cs);
                setCell(row, 2, str(pro.get("proCode")), cs);
                setCell(row, 3, str(pro.get("topicName")), lcs);
                setCell(row, 4, str(pro.get("groupName")), lcs);
                setCell(row, 5, str(pro.get("topicType")), cs);
                setCell(row, 6, str(pro.get("professionalScope")), cs);

                // 专家发布分（不×0.6），同时收集有效分用于计算平均
                @SuppressWarnings("unchecked")
                Map<String, String> es = (Map<String, String>) pro.get("expertScores");
                List<BigDecimal> validScores = new ArrayList<>();
                for (int k = 0; k < nGrpExperts; k++) {
                    String acc = groupExperts.get(k).getLoginAccount();
                    String sc = (es != null && acc != null) ? es.getOrDefault(acc, "") : "";
                    if ("回避".equals(sc)) {
                        setCell(row, nFixed + k, sc, avoidStyle);
                    } else {
                        setCell(row, nFixed + k, sc, cs);
                        if (!sc.isEmpty()) {
                            try { validScores.add(new BigDecimal(sc)); } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                // 计算发布分（平均分）：≥3个有效分去最高最低
                String avgDisplay = "";
                if (!validScores.isEmpty()) {
                    List<BigDecimal> toAvg = new ArrayList<>(validScores);
                    if (toAvg.size() >= 3) {
                        toAvg.sort(BigDecimal::compareTo);
                        toAvg.remove(0);
                        toAvg.remove(toAvg.size() - 1);
                    }
                    BigDecimal sum = BigDecimal.ZERO;
                    for (BigDecimal s : toAvg) sum = sum.add(s);
                    avgDisplay = sum.divide(BigDecimal.valueOf(toAvg.size()), 2, BigDecimal.ROUND_HALF_UP).toString();
                }
                setCell(row, avgCol, avgDisplay, avgStyle);
                setCell(row, refStart,     str(pro.get("completeUnit")), cs);
                setCell(row, refStart + 1, str(pro.get("companyName")), cs);
                setCell(row, refStart + 2, str(pro.get("groupMember")), cs);
            }

            // ---- 列宽 ----
            sheet.setColumnWidth(0, 1500);  // 发布序号
            sheet.setColumnWidth(1, 4000);  // 分派专业组
            sheet.setColumnWidth(2, 3500);  // 申报账号
            sheet.setColumnWidth(3, 7000);  // 课题名称
            sheet.setColumnWidth(4, 5000);  // 小组名称
            sheet.setColumnWidth(5, 3000);  // 课题类型
            sheet.setColumnWidth(6, 2500);  // 分类
            for (int k = 0; k < nGrpExperts; k++) sheet.setColumnWidth(nFixed + k, 2400);
            sheet.setColumnWidth(avgCol, 2800);      // 发布分
            sheet.setColumnWidth(refStart, 5500);     // 完成单位
            sheet.setColumnWidth(refStart + 1, 5500); // 申报单位
            sheet.setColumnWidth(refStart + 2, 4500); // 小组成员

            if (!frozen) { sheet.createFreezePane(nFixed, 3); frozen = true; }

            // 区块间空一行
            cur = startData + groupRows.size() + 1;
        }
    }

    // ==================== 终版意见导出 ====================

    /**
     * 导出终版意见表 Excel（按分派专业组堆叠）
     * 参数: taskId (必需), groupName (可选 —— 如果提供则只导出该组)
     */
    @GetMapping("/exportFinalOpinionExcel")
    public void exportFinalOpinionExcel(@RequestParam Map<String, Object> params,
                                        HttpServletResponse response) throws Exception {
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        String filterGroup = params.get("groupName") != null ? params.get("groupName").toString().trim() : "";
        if (taskId.isEmpty()) { response.sendError(400, "参数不完整"); return; }

        // 外聘人员（角色72）只能看到绑定组的数据
        boolean isExternalExpert = false;
        Set<String> allowedQcGroups = new HashSet<>();
        UserDO currentUser = getUser();
        if (currentUser.getRoleIds().contains(ROLE_QC_EXTERNAL_EMPLOYMENT_ID)) {
            isExternalExpert = true;
            Map<String, Object> bindingQuery = new HashMap<>();
            bindingQuery.put("taskId", taskId);
            bindingQuery.put("userId", String.valueOf(getUserId()));
            bindingQuery.put("proType", "qc_view_scope");
            List<ExpertGroupDO> bindings = expertGroupService.list(bindingQuery);
            for (ExpertGroupDO b : bindings) {
                if (b.getGroupName() != null) allowedQcGroups.add(b.getGroupName().trim());
            }
        }
        if (isExternalExpert && allowedQcGroups.isEmpty()) {
            response.sendError(403, "无权限查看"); return;
        }

        // 查询项目列表（含 qcGroupName / professionalScope 等）
        Map<String, Object> proQuery = new HashMap<>();
        proQuery.put("taskId", taskId);
        List<QcProDataDto> proDataList = qcAwardService.listProInfo(proQuery);

        // 外聘人员过滤：只保留绑定组的项目
        if (isExternalExpert) {
            final Set<String> finalAllowed = allowedQcGroups;
            proDataList = proDataList.stream()
                    .filter(p -> p.getQcGroupName() != null && finalAllowed.contains(p.getQcGroupName().trim()))
                    .collect(Collectors.toList());
        }

        // 查询项目基本信息（用于 topicType 判断问题解决型/创新型）
        Map<String, Object> projQuery = new HashMap<>();
        projQuery.put("taskId", taskId);
        List<QcGroupApplyInfoDO> projList = qcGroupApplyInfoService.list(projQuery);
        Map<Integer, QcGroupApplyInfoDO> projMap = new HashMap<>();
        for (QcGroupApplyInfoDO p : projList) projMap.put(p.getProId(), p);

        // 查询所有发布分
        Map<String, Object> psQuery = new HashMap<>();
        psQuery.put("taskId", taskId);
        List<QcPresentScoreDO> allPresentScores = qcPresentScoreService.list(psQuery);

        // 按 proId 聚合有效发布分（排除回避）
        Map<Integer, List<BigDecimal>> presentMap = new HashMap<>();
        for (QcPresentScoreDO s : allPresentScores) {
            if (s.getProId() == null || s.getOptUid() == null) continue;
            List<Integer> avoidedProIds = avoidanceService.getAvoidedProIds(taskId, s.getOptUid());
            if (avoidedProIds != null && avoidedProIds.contains(s.getProId())) continue;
            if (s.getAppraiseSum() != null && !s.getAppraiseSum().isEmpty()) {
                try {
                    presentMap.computeIfAbsent(s.getProId(), k -> new ArrayList<>()).add(new BigDecimal(s.getAppraiseSum()));
                } catch (NumberFormatException ignore) {}
            }
        }

        // 构建行数据
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (QcProDataDto pro : proDataList) {
            if (pro.getProId() == null) continue;
            String qcGrp = pro.getQcGroupName() != null ? pro.getQcGroupName().trim() : "";
            if (!filterGroup.isEmpty() && !filterGroup.equals(qcGrp)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("proCode", pro.getApplyId());
            row.put("topicName", pro.getTopicName());
            row.put("groupName", pro.getGroupName());
            row.put("qcGroupName", qcGrp);
            row.put("completeUnit", pro.getCompleteUnit());
            row.put("companyName", pro.getCompanyName());
            row.put("topicType", pro.getTopicType());
            row.put("professionalScope", pro.getProfessionalScope());

            // ---- 计算资料分（去高低平均 × 0.6）----
            String topicType = pro.getTopicType();
            QcGroupApplyInfoDO proj = projMap.get(pro.getProId());
            if (proj != null && proj.getTopicType() != null) topicType = proj.getTopicType();

            List<BigDecimal> initValid = new ArrayList<>();
            String recommendLevel = null;
            Map<String, Object> sq = new HashMap<>();
            sq.put("taskId", taskId); sq.put("proId", pro.getProId()); sq.put("deleted", 0);
            List<Integer> avoidedExperts = avoidanceService.getAvoidedExpertIds(taskId, pro.getProId());
            if ("问题解决型".equals(topicType)) {
                List<QcResultSolveScoreDO> scores = qcResultSolveScoreService.list(sq);
                if (scores != null) for (QcResultSolveScoreDO s : scores) {
                    if (avoidedExperts != null && avoidedExperts.contains(s.getOptUid())) continue;
                    if (s.getAppraiseSum() != null) {
                        try { initValid.add(new BigDecimal(s.getAppraiseSum().toString())); } catch (NumberFormatException ignore) {}
                    }
                    if (recommendLevel == null && s.getRecommendLevel() != null && !s.getRecommendLevel().isEmpty()) {
                        recommendLevel = s.getRecommendLevel();
                    }
                }
            } else {
                List<QcResultInnovateScoreDO> scores = qcResultInnovateScoreService.list(sq);
                if (scores != null) for (QcResultInnovateScoreDO s : scores) {
                    if (avoidedExperts != null && avoidedExperts.contains(s.getOptUid())) continue;
                    if (s.getAppraiseSum() != null) {
                        try { initValid.add(new BigDecimal(s.getAppraiseSum().toString())); } catch (NumberFormatException ignore) {}
                    }
                    if (recommendLevel == null && s.getRecommendLevel() != null && !s.getRecommendLevel().isEmpty()) {
                        recommendLevel = s.getRecommendLevel();
                    }
                }
            }
            row.put("recommendLevel", recommendLevel);
            BigDecimal initAvg = calcTrimmedAvg(initValid);
            BigDecimal initScaled = initAvg != null ? initAvg.multiply(new BigDecimal("0.6")).setScale(2, BigDecimal.ROUND_HALF_UP) : null;
            row.put("initialScore", initScaled);

            // ---- 计算发布分（去高低平均，不缩放）----
            List<BigDecimal> presValid = presentMap.getOrDefault(pro.getProId(), Collections.emptyList());
            BigDecimal presAvg = calcTrimmedAvg(presValid);
            row.put("presentScore", presAvg);

            // ---- 总分 ----
            BigDecimal total = null;
            if (initScaled != null || presAvg != null) {
                total = (initScaled != null ? initScaled : BigDecimal.ZERO)
                        .add(presAvg != null ? presAvg : BigDecimal.ZERO)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            row.put("totalScore", total);
            allRows.add(row);
        }

        // 排序：先按分派专业组，再按申报账号
        allRows.sort((a, b) -> {
            String g1 = str(a.get("qcGroupName")), g2 = str(b.get("qcGroupName"));
            int gc = g1.compareTo(g2);
            if (gc != 0) return gc;
            return str(a.get("proCode")).compareTo(str(b.get("proCode")));
        });

        // 按组计算排名（总分降序）
        java.util.LinkedHashMap<String, List<Map<String, Object>>> grouped = new java.util.LinkedHashMap<>();
        for (Map<String, Object> r : allRows) {
            grouped.computeIfAbsent(str(r.get("qcGroupName")), k -> new ArrayList<>()).add(r);
        }
        for (List<Map<String, Object>> gRows : grouped.values()) {
            List<Map<String, Object>> ranked = new ArrayList<>(gRows);
            ranked.sort((a, b) -> {
                BigDecimal ta = a.get("totalScore") instanceof BigDecimal ? (BigDecimal) a.get("totalScore") : null;
                BigDecimal tb = b.get("totalScore") instanceof BigDecimal ? (BigDecimal) b.get("totalScore") : null;
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            });
            for (int i = 0; i < ranked.size(); i++) {
                ranked.get(i).put("rank", ranked.get(i).get("totalScore") != null ? (i + 1) : null);
            }
        }

        int year = Calendar.getInstance().get(Calendar.YEAR);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            buildFinalOpinionSheet(wb, allRows, year);
            String fileName = URLEncoder.encode(year + "年_终版意见表.xlsx", "UTF-8").replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            wb.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

    /** 去高低后求平均（≥3个去高低，否则直接平均） */
    private BigDecimal calcTrimmedAvg(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) return null;
        List<BigDecimal> toAvg = new ArrayList<>(scores);
        if (toAvg.size() >= 3) {
            toAvg.sort(BigDecimal::compareTo);
            toAvg.remove(0);
            toAvg.remove(toAvg.size() - 1);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal s : toAvg) sum = sum.add(s);
        return sum.divide(BigDecimal.valueOf(toAvg.size()), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 构建终版意见表 sheet（单sheet，按分派专业组堆叠）
     * 列：发布序号 / 分派专业组 / 申报账号 / 课题名称 / 小组名称 / 完成单位名称 / 申报单位名称 / 成果类型 / 分类
     *     / 资料分(满分72) / 发布分(满分28) / 总分 / 排名 / 建议等级
     */
    private void buildFinalOpinionSheet(XSSFWorkbook wb, List<Map<String, Object>> rows, int year) {
        XSSFSheet sheet = wb.createSheet("专家签字打印 最终");

        // 按 qcGroupName 分组（rows 已排序）
        java.util.LinkedHashMap<String, List<Map<String, Object>>> groupMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> r : rows)
            groupMap.computeIfAbsent(str(r.get("qcGroupName")), k -> new ArrayList<>()).add(r);

        // ---- 颜色 ----
        XSSFColor titleColor = new XSSFColor(new byte[]{(byte)189,(byte)215,(byte)238}, null);
        XSSFColor hdrColor   = new XSSFColor(new byte[]{(byte)217,(byte)225,(byte)242}, null);
        XSSFColor evenColor  = new XSSFColor(new byte[]{(byte)245,(byte)247,(byte)250}, null);

        // ---- 样式 ----
        java.util.function.Supplier<XSSFCellStyle> newStyle = () -> {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setBorderTop(BorderStyle.THIN); cs.setBorderBottom(BorderStyle.THIN);
            cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setWrapText(true);
            return cs;
        };

        XSSFFont titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short)12);
        XSSFFont hdrFont   = wb.createFont(); hdrFont.setBold(true);   hdrFont.setFontHeightInPoints((short)10);
        XSSFFont dataFont  = wb.createFont(); dataFont.setFontHeightInPoints((short)9);

        XSSFCellStyle titleStyle = newStyle.get(); titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(titleColor); titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle hdrStyle = newStyle.get(); hdrStyle.setFont(hdrFont);
        hdrStyle.setFillForegroundColor(hdrColor); hdrStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        hdrStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle dataC = newStyle.get(); dataC.setFont(dataFont); dataC.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle dataL = newStyle.get(); dataL.setFont(dataFont); dataL.setAlignment(HorizontalAlignment.LEFT);
        XSSFCellStyle evenC = newStyle.get(); evenC.setFont(dataFont);
        evenC.setFillForegroundColor(evenColor); evenC.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        evenC.setAlignment(HorizontalAlignment.CENTER);
        XSSFCellStyle evenL = newStyle.get(); evenL.setFont(dataFont);
        evenL.setFillForegroundColor(evenColor); evenL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        evenL.setAlignment(HorizontalAlignment.LEFT);

        int nCols = 14; // 14列
        int cur = 0;

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupMap.entrySet()) {
            String gName = entry.getKey();
            List<Map<String, Object>> gRows = entry.getValue();

            // ---- 标题行 ----
            Row r0 = sheet.createRow(cur); r0.setHeightInPoints(28);
            Cell c0 = r0.createCell(0);
            c0.setCellValue(year + "年石油工程建设优秀质量管理小组活动成果评价  专家小组意见表  " + gName);
            c0.setCellStyle(titleStyle);
            for (int c = 1; c < nCols; c++) { Cell cc = r0.createCell(c); cc.setCellStyle(titleStyle); }
            sheet.addMergedRegion(new CellRangeAddress(cur, cur, 0, nCols - 1));

            // ---- 表头行 ----
            Row r1 = sheet.createRow(cur + 1); r1.setHeightInPoints(36);
            String[] headers = {"发布\n序号", "分派\n专业组", "申报账号", "课题名称", "小组名称",
                    "完成单位名称", "申报单位名称", "成果类型", "分类",
                    "资料分\n(满分72)", "发布分", "总分", "排名", "建议等级"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = r1.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hdrStyle);
            }

            // ---- 数据行 ----
            for (int i = 0; i < gRows.size(); i++) {
                Map<String, Object> pro = gRows.get(i);
                Row row = sheet.createRow(cur + 2 + i); row.setHeightInPoints(18);
                boolean isEven = (i % 2 == 1);
                XSSFCellStyle cs  = isEven ? evenC : dataC;
                XSSFCellStyle lcs = isEven ? evenL : dataL;

                setCell(row, 0, String.valueOf(i + 1), cs);
                setCell(row, 1, str(pro.get("qcGroupName")), cs);
                setCell(row, 2, str(pro.get("proCode")), cs);
                setCell(row, 3, str(pro.get("topicName")), lcs);
                setCell(row, 4, str(pro.get("groupName")), lcs);
                setCell(row, 5, str(pro.get("completeUnit")), lcs);
                setCell(row, 6, str(pro.get("companyName")), lcs);
                setCell(row, 7, str(pro.get("topicType")), cs);
                setCell(row, 8, str(pro.get("professionalScope")), cs);
                setCell(row, 9, pro.get("initialScore") != null ? pro.get("initialScore").toString() : "", cs);
                setCell(row, 10, pro.get("presentScore") != null ? pro.get("presentScore").toString() : "", cs);
                setCell(row, 11, pro.get("totalScore") != null ? pro.get("totalScore").toString() : "", cs);
                setCell(row, 12, pro.get("rank") != null ? pro.get("rank").toString() : "", cs);
                setCell(row, 13, str(pro.get("recommendLevel")), cs); // 建议等级
            }

            // ---- 列宽 ----
            sheet.setColumnWidth(0, 1800);  // 发布序号
            sheet.setColumnWidth(1, 4000);  // 分派专业组
            sheet.setColumnWidth(2, 3500);  // 申报账号
            sheet.setColumnWidth(3, 7000);  // 课题名称
            sheet.setColumnWidth(4, 5000);  // 小组名称
            sheet.setColumnWidth(5, 5500);  // 完成单位名称
            sheet.setColumnWidth(6, 5500);  // 申报单位名称
            sheet.setColumnWidth(7, 3000);  // 成果类型
            sheet.setColumnWidth(8, 2500);  // 分类
            sheet.setColumnWidth(9, 3200);  // 资料分
            sheet.setColumnWidth(10, 3200); // 发布分
            sheet.setColumnWidth(11, 2500); // 总分
            sheet.setColumnWidth(12, 2000); // 排名
            sheet.setColumnWidth(13, 2500); // 建议等级

            cur = cur + 2 + gRows.size() + 1; // 区块间空一行
        }
    }
}
