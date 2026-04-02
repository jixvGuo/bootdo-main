package com.bootdo.cpe.controller.qc;

import com.bootdo.common.controller.BaseQcProController;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.ExpertGroupDO;
import com.bootdo.cpe.domain.QcAppraiseActiveScoreDO;
import com.bootdo.cpe.domain.QcExpertEliminateDO;
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
import com.bootdo.cpe.service.QcGroupApplyInfoService;
import com.bootdo.cpe.service.QcResultInnovateScoreService;
import com.bootdo.cpe.service.QcResultSolveScoreService;
import com.bootdo.cpe.service.QcReviewResultRecordService;
import com.bootdo.common.service.DictService;
import com.bootdo.common.domain.DictDO;
import com.bootdo.system.domain.UserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.math.BigDecimal;

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
    private ExpertGroupService expertGroupService;

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
        // 查询当前专家是否已提交最终打分（参考科技奖 scoreIsOver 逻辑）
        int scoreIsOver = 0;
        Long uid = getUserId();
        String currentTaskId = params.get("taskId") != null ? params.get("taskId").toString() : "";
        try {
            Map<String, Object> checkParams = new HashMap<>();
            checkParams.put("optUid", uid);
            checkParams.put("scoreOver", 1);
            if (StringUtils.isNotBlank(currentTaskId)) {
                checkParams.put("taskId", currentTaskId);
            }
            scoreIsOver = qcAppraiseActiveScoreService.count(checkParams);
        } catch (Exception e) {
            // score_over列可能尚未添加到数据库，降级处理
            scoreIsOver = 0;
        }
        map.put("scoreIsOver", scoreIsOver > 0 ? 1 : 0);
        map.put("userId", uid); // 传递用户ID给前端，用于回避功能
        
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
        // 查找当前专家在当前任务下的打分记录
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("optUid", uid);
        if (StringUtils.isNotBlank(taskId)) {
            queryParams.put("taskId", taskId);
        }
        List<QcAppraiseActiveScoreDO> scoreList = qcAppraiseActiveScoreService.list(queryParams);
        if (scoreList == null || scoreList.isEmpty()) {
            return R.error("尚未进行任何打分，无法提交");
        }
        // 标记所有打分记录为已提交
        int updated = 0;
        for (QcAppraiseActiveScoreDO score : scoreList) {
            score.setScoreOver(1);
            updated += qcAppraiseActiveScoreService.update(score);
        }
        if (updated > 0) {
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
        List<QcAppraiseActiveScoreDO> scoreList = qcAppraiseActiveScoreService.list(queryParams);
        if (scoreList == null || scoreList.isEmpty()) {
            return R.error("没有已提交的打分记录");
        }
        int updated = 0;
        for (QcAppraiseActiveScoreDO score : scoreList) {
            score.setScoreOver(0);
            updated += qcAppraiseActiveScoreService.update(score);
        }
        if (updated > 0) {
            return R.ok("撤回成功");
        }
        return R.error("撤回失败");
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

        // 检查该项目是否已被该专家淘汰
        Map<String, Object> checkParams = new HashMap<>();
        checkParams.put("expertUid", uid);
        checkParams.put("proId", proId);
        checkParams.put("taskId", taskId);
        checkParams.put("deleted", 0);
        int exists = qcExpertEliminateService.count(checkParams);
        if (exists > 0) {
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

        // 保存淘汰记录
        QcExpertEliminateDO eliminate = new QcExpertEliminateDO();
        eliminate.setExpertUid(uid);
        eliminate.setProId(proId);
        eliminate.setTaskId(taskId);
        eliminate.setReason(reason);
        eliminate.setPrevProStat(prevProStat);
        eliminate.setDeleted(0);
        qcExpertEliminateService.save(eliminate);

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
    @RequestMapping("/getEliminatedProjects")
    @ResponseBody
    public R getEliminatedProjects(@RequestParam String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return R.error("任务ID不能为空");
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("deleted", 0);
        
        List<QcExpertEliminateDO> list = qcExpertEliminateService.list(params);
        
        return R.ok().put("list", list);
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
            score.setId(existList.get(0).getId());
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
            score.setId(existList.get(0).getId());
            tag = qcResultInnovateScoreService.update(score);
        } else {
            tag = qcResultInnovateScoreService.save(score);
        }
        if (tag > 0) {
            return R.ok("保存成功");
        }
        return R.error("保存失败");
    }
}
