package com.bootdo.cpe.controller.science_progress;

import com.bootdo.activiti.domain.EnterpriseProjectInfoDo;
import com.bootdo.activiti.domain.PublishAwardTaskDo;
import com.bootdo.activiti.service.AwardEnterpriseProjectService;
import com.bootdo.activiti.service.AwardFlowService;
import com.bootdo.common.config.BootdoConfig;
import com.bootdo.common.controller.BaseScienceTechnologyController;
import com.bootdo.common.service.DictService;
import com.bootdo.common.utils.*;
import com.bootdo.cpe.domain.*;
import com.bootdo.cpe.petroleum_engineering_award.domain.*;
import com.bootdo.cpe.service.EnterpriTeamLeaderInfoService;
import com.bootdo.cpe.service.ExpertGroupService;
import com.bootdo.cpe.service.ProjectCommonService;
import com.bootdo.cpe.service.ScienceProcessService;
import com.bootdo.system.domain.*;
import com.bootdo.system.service.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bootdo.common.config.Constant.*;

/**
 * 科技奖控制处理
 * （有专家组管理）
 *
 * @author houzb
 * @version 1.0
 * @date 2021-04-16 8:14
 */
@Controller
@RequestMapping("/scienceProgressScience")
public class ScienceController extends BaseScienceTechnologyController {
    private static final Logger logger = LoggerFactory.getLogger(ScienceController.class);
    
    @Autowired
    private AwardFlowService awardFlowService;
    @Autowired
    private EnterpriseChengguoBaseInfoService enterpriseChengguoBaseInfoService;
    @Autowired
    private AwardEnterpriseProjectService awardEnterpriseProjectService;
    @Autowired
    private UserService userService;
    @Autowired
    private ScienceProcessService scienceProcessService;
    @Autowired
    private ExpertGroupService expertGroupService;
    @Autowired
    private BootdoConfig bootdoConfig;
    @Autowired
    private DictService dictService;
    @Autowired
    private EnterpriTeamInfoService enterpriTeamInfoService;
    @Autowired
    private EnterpriPersonalInfoService enterpriPersonalInfoService;
    @Autowired
    private EnterpriTeamLeaderInfoService enterpriTeamLeaderInfoService; //主要成员情况带头人
    @Autowired
    private EnterpriseChengguoOtherInfoService enterpriseChengguoOtherInfoService;
    @Autowired
    private ProjectCommonService projectCommonService;

    /**
     * 更新项目信息
     * @param id
     * @param major
     * @param proType
     * @return
     */
    @RequestMapping("/updateProMajor")
    @ResponseBody
    public R upateProMajor(int id, String major,String proType) {
        EnterpriseProjectInfoDo projectInfoDo = new EnterpriseProjectInfoDo();
        projectInfoDo.setId(id);
        projectInfoDo.setMajor(major);
        int rst = awardEnterpriseProjectService.updateProjectInfo(projectInfoDo);
        if(rst > 0) {
            if("science".equals(proType)) {
                EnterpriseChengguoBaseInfoDO enterpriseChengguoBaseInfo = new EnterpriseChengguoBaseInfoDO();
                enterpriseChengguoBaseInfo.setProId(id + "");
                enterpriseChengguoBaseInfo.setMajor(major);
                enterpriseChengguoBaseInfo.setSubjectType(major);
                enterpriseChengguoBaseInfoService.update(enterpriseChengguoBaseInfo);
            }

            if("team".equals(proType)) {
                EnterpriTeamInfoDO enterpriTeamInfo = new EnterpriTeamInfoDO();
                enterpriTeamInfo.setProId(id);
                enterpriTeamInfo.setSubjectClassificationName(major);
                enterpriTeamInfo.setResearchDirection(major);
                enterpriTeamInfoService.updateMajor(enterpriTeamInfo);
            }

            if("personal".equals(proType)) {
                EnterpriPersonalInfoDO enterpriPersonalInfo = new EnterpriPersonalInfoDO();
                enterpriPersonalInfo.setProId(id);
                enterpriPersonalInfo.setWorkMajor(major);
                enterpriPersonalInfoService.updateMajor(enterpriPersonalInfo);
            }

            return R.ok();
        }
        return R.error("更新失败");
    }

    @RequiresPermissions("technologyaward:to:prolist")
    @RequestMapping("/toProListMain")
    public String toProListMain(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        return "cpe/science/technology_pro_list_main";
    }
    /**
     * 查看项目列表
     * @param map
     * @param params
     * @return
     */
    @RequiresPermissions("technologyaward:to:prolist")
    @RequestMapping("/toProList")
    public String toProList(ModelMap map, @RequestParam Map<String, Object> params) {
        packageAwardTaskId(map, params);
        map.put("proSubType", params.get("proSubType"));

        return "cpe/science/technology_pro_list";
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

        if (roleIdList.contains(ROLE_SCIENCE_ASSOCIATION_ID)) {
            //todo 临时使用协会联系人的用户id
            params.put("associationUserId", roleIdList.contains(ROLE_SCIENCE_OFFLINE_VIEW_ID) ? 101 : user.getUserId());
        } else if (roleIdList.contains(ROLE_ENTERPRISE_SCIENCE_ID)) {
            //企业用户查看自己创建项目
            params.put("enterpriseUid", uid);
        } else if(roleIdList.contains(ROLE_SPECIALIST_ID)) {
            //评审专家
            params.put("scoreSpecialistUid", uid);
        }else {
            //分派给自己的项目
            params.put("ass_assign_uid", uid);
        }
        getProListParamsByRole(params);

        params.put("proStatStr", "");
        Object keyWordObj = params.get("keyWord");
        if (keyWordObj != null && StringUtils.isNotBlank(keyWordObj.toString())) {
            List<String> proStatList = QcProStatEnum.getStatValByKey(keyWordObj.toString());
            if (proStatList.size() > 0) {
                String str = new String();
                for (String s : proStatList) {
                    str +=  s + ",";
                }
                params.put("proStatStr", str.substring(0, str.length() - 1));
            }
            params.put("keyWord", "%" + keyWordObj + "%");
        }
        this.newParams = params;
        Query query = new Query(params);

        List<TechnologyProjectInfo> proDataDtoList = scienceProcessService.listProInfo(query);
        int total = scienceProcessService.countProInfo(query);
        //权限校验仅lidan协会领导,协会联系人,外聘人员可以查看编号,其余不可查看
        if(!CommonUtils.isViewProCode(roleIdList)) {
             proDataDtoList.stream().forEach(pd->{
                 pd.setProCode("");
             });
        }
        PageUtils pageUtils = new PageUtils(proDataDtoList, total);
        return pageUtils;
    }

    /**
     * 跳转到要申报的项目列表
     *
     * @param map
     * @return
     */
    @RequestMapping("/toApplyPros")
    @RequiresPermissions("act:award:apply_pros")
    public String toApplyProList(@RequestParam Map<String, Object> params, ModelMap map) {
        UserDO user = getUser();
        List<Long> roleIdList = user.getRoleIds();
        isTaskIsApply(map, params, roleIdList, user);
        packageAwardTaskId(map, params);
        List<PublishAwardTaskDo> taskDoList = awardFlowService.getAwardLastTasksByAwardType(20, EnumAwardType.SCIENCE.getAwrdType());
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
        map.put("taskList", taskDoList);
        map.put("apply_pro_type", "science");
        return "act/award/enterprise_doc_award_pro_list";
    }

    private Map<String, Object> newParams;

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
            //todo 临时使用协会联系人的用户id
            params.put("associationUserId", roleIdList.contains(ROLE_SCIENCE_OFFLINE_VIEW_ID) ? 101 : user.getUserId());
        } else if (roleIdList.contains(ROLE_ENTERPRISE_SCIENCE_ID)) {
            //企业用户查看自己创建项目
            params.put("enterpriseUid", uid);
        } else {
            //分派给自己的项目
            params.put("ass_assign_uid", uid);
        }
        getProListParamsByRole(params);
        this.newParams = params;

        params.put("proStatStr", "");
        Object keyWordObj = params.get("keyWord");
        if (keyWordObj != null) {
            List<String> proStatList = OilProStatEnum.getStatValByKey(keyWordObj.toString());
            if (proStatList.size() > 0) {
                String str = new String();
                for (String s : proStatList) {
                    str +=  s + ",";
                }
                params.put("proStatStr", str.substring(0, str.length() - 1));
            }
        }

        Query query = new Query(params);
        List<EnterpriseProjectInfoDo> proList = awardEnterpriseProjectService.list(query);
        int total = awardEnterpriseProjectService.count(query);
        PageUtils pageUtils = new PageUtils(proList, total);
        return pageUtils;
    }


    @RequestMapping("/printExcel")
    @ResponseBody
    public String printExcel(HttpServletResponse response, ModelMap map, @RequestParam Map<String, Object> params) {

        this.newParams.put("limit", "100000");
        Query query = new Query(this.newParams);


        List<EnterpriseProjectInfoDo> enterpriTeamInfoList = awardEnterpriseProjectService.list(query);
        System.out.println(enterpriTeamInfoList.size());

        Map<String, Object> proParamsMap =new HashMap<String, Object>();
        proParamsMap.putAll(this.newParams);
        proParamsMap.remove("proSubType");
        proParamsMap.remove("keyWord");
        proParamsMap.remove("proStatStr");
        List<TechnologyProjectInfo> proDataDtoList = scienceProcessService.listProInfo(new Query(proParamsMap));
        Map<Integer, String> proCodeMap = new HashMap<>();
        proDataDtoList.stream().forEach(p->{
            proCodeMap.put(p.getProId(), p.getProCode());
        });


        for (EnterpriseProjectInfoDo   enterpriseProjectInfoDo :enterpriTeamInfoList   ) {
            Map<String, Object> teamParams = new HashMap<>();
            teamParams.put("taskId", this.newParams.get("taskId"));
            teamParams.put("proId", enterpriseProjectInfoDo.getId());


            List<EnterpriseChengguoOtherInfoDO> otherInfoDOList = enterpriseChengguoOtherInfoService.list(teamParams);
            StringBuilder stringBuilder = new StringBuilder();

            for (EnterpriseChengguoOtherInfoDO   user :otherInfoDOList   ) {
                stringBuilder.append(user.getMainEnterpriseCompleter() +",");
            }
            enterpriseProjectInfoDo.setMainCompleteNames(stringBuilder.toString());
            enterpriseProjectInfoDo.setProCode(proCodeMap.get(enterpriseProjectInfoDo.getId()));

            enterpriseProjectInfoDo.initShowProName();
        }

        String[] title = {"编号", "项目编号", "项目类别", "申报单位", "专业", "申报日期", "成果", "状态",  "主要贡献人"};
        try {
            PoiWordUtils.downScienceExcel(title, enterpriTeamInfoList, response);
        } catch (Exception e) {
        }
        map.addAttribute("result", "下载成功");

        return "";
    }


    /**
     * 去编辑成果信息
     * 企业可以在一个任务下创建多个项目
     *
     * @return
     */
    @GetMapping("/toEditChengguo")
    @RequiresPermissions("system:enterpriseChengguoBaseInfo:editView")
    public String toUpateChengguoInfo(ModelMap map, @RequestParam Map<String, Object> params) {
        packageAwardTaskId(map, params);
        //获取项目成果相关信息
        List<EnterpriseChengguoBaseInfoDO> baseInfoDOList = enterpriseChengguoBaseInfoService.list(params);
        map.put("enterpriseChengguoBaseInfo", baseInfoDOList.size() > 0 ? baseInfoDOList.get(0) : null);
        return "act/award/chengguo/index";
    }


    /**
     * 科学形式审查的列表 科学技术奖成果审查 Formal review
     *
     * @param map
     * @return
     */
    @RequestMapping("/toReviewSciencePros")
    @RequiresPermissions("act:award:apply_pros")
    public String toListScienceFormalReview(@RequestParam Map<String, Object> params, ModelMap map) {
        map.put("apply_type", "science");
        packageAwardTaskId(map, params);
        List<PublishAwardTaskDo> taskDoList = awardFlowService.getAwardLastTasksByAwardType(20, EnumAwardType.SCIENCE.getAwrdType());
        map.put("taskList", taskDoList);
        return "act/award/formal/enterprise_formal_science_award_pro_list";
    }


    /**
     * 专业组管理 筛选专业
     *
     * @return
     */
    @RequiresPermissions("act:award:project_professional")
    @RequestMapping("/toAssignExperts")
    public String toManageProfession(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        // 原始实现：仅从项目表获取分组列表
        // List<String> groupList = awardEnterpriseProjectService.getProGroupList(params);

        // 修正v2：同时从项目表和专家组表获取分组，合并去重，确保左侧分类能正确显示
        // List<String> groupListFromProject = awardEnterpriseProjectService.getProGroupList(params);
        // Map<String, Object> expertGroupParams = new HashMap<>();
        // expertGroupParams.put("taskId", params.get("taskId"));
        // expertGroupParams.put("proType", params.get("proType"));
        // List<String> groupListFromExpert = expertGroupService.getDistinctGroupNames(expertGroupParams);
        // List<String> groupList = new ArrayList<>(groupListFromProject);
        // for (String g : groupListFromExpert) {
        //     if (g != null && !groupList.contains(g)) {
        //         groupList.add(g);
        //     }
        // }

        // 修正v3（已废弃，major_team_ids 可能为空导致左侧无数据）：
        // String taskId = (String) params.get("taskId");
        // List<com.bootdo.activiti.domain.MajorInfo> majorInfoList = awardFlowService.getAwardTaskMajorInfosById(taskId);
        // List<String> groupList = new ArrayList<>();
        // if (majorInfoList != null) {
        //     for (com.bootdo.activiti.domain.MajorInfo mi : majorInfoList) {
        //         String name = mi.getMajorName();
        //         if (name != null && !name.trim().isEmpty() && !groupList.contains(name)) {
        //             groupList.add(name);
        //         }
        //     }
        // }

        // 从项目表获取专业列表，同时从 ass_award_publish_task.major_team_ids 补充，合并去重。
        String taskId = (String) params.get("taskId");
        // 1) 从项目表获取有项目的专业列表
        List<String> groupListFromProject = awardEnterpriseProjectService.getProMajorList(params);
        List<String> groupList = new ArrayList<>(groupListFromProject);
        // 2) 从 ass_award_publish_task.major_team_ids 补充（如果该字段有配置）
        List<com.bootdo.activiti.domain.MajorInfo> majorInfoList = awardFlowService.getAwardTaskMajorInfosById(taskId);
        if (majorInfoList != null) {
            for (com.bootdo.activiti.domain.MajorInfo mi : majorInfoList) {
                String name = mi.getMajorName();
                if (name != null && !name.trim().isEmpty() && !groupList.contains(name)) {
                    groupList.add(name);
                }
            }
        }

        // 查询并获取所有企业项目信息列表
        List<EnterpriseProjectInfoDo> list = awardEnterpriseProjectService.getAllProList(params);
        boolean isAssign = true;

        // 判断是否已分配任务或权限
        map.put("isAssign", isAssign);
        params.put("roleId", "62");
        if (isAssign) {
            //如果是分派阶段则可查询进行分派
            // 根据 params（包含角色 ID）查询可被分派的用户列表。
            List<UserDO> assWorkers = userService.list(params);
            map.put("assWorkers", assWorkers);
        }
        // 企业申请的数据

        /**
         * 将企业项目列表 list 存入模型 map，键为 assPros，供前端展示待分派的项目
         */
        map.put("assPros", list);

        /**
         * 将专业组列表 groupList 存入模型，键为 profession，用于页面显示或筛选专业组选项
         */
        map.put("profession", groupList);

        /**
         * 1. **创建查询参数**：新建一个 `HashMap` 用于构造查询条件。
         * 2. **封装任务ID**：从原参数中获取 `taskId` 放入新参数Map。
         * 3. **封装专业组**：将 `major`（专业组名称）作为 `groupName` 存入。
         * 4. **封装项目类型**：将 `proType` 存入，用于区分不同类型的奖项或项目。
         *
         * 该参数Map通常用于后续查询专家分组或相关专业信息。
         */
        Map<String, Object> selParams = new HashMap<>();
        selParams.put("taskId", params.get("taskId"));
        selParams.put("groupName", params.get("major"));
        selParams.put("proType", params.get("proType"));

        /**
         * 1. **查询专家分组**：调用 `expertGroupService.list()` 方法，
         * 根据 `selParams` 参数（包含任务ID、专业组名称、项目类型）查询匹配的专家分组列表。
         * 2. **存入模型**：将查询结果 `selList` 以键名 `selInfoList` 放入模型，供前端页面渲染专家分配信息。
         * 3. **传递项目类型**：将 `proType` 参数也存入模型，用于页面标识或区分不同的奖项类型。
         */
        List<ExpertGroupDO> selList = expertGroupService.list(selParams);
        map.put("selInfoList", selList);
        map.put("proType", params.get("proType"));
        return "act/award/association_profession_manage";
    }

    /**
     * 获取项目分组(奖项选择)
     * @param params
     * @param map
     * @return
     */
    @RequiresPermissions("act:award:project_professional")
    @RequestMapping("/getAssignGroups")
    @ResponseBody
    public R getProGroupList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        // 原始实现：仅从项目表获取分组
        // List<EnterpriseProjectInfoDo> list = awardEnterpriseProjectService.getAllProList(params);
        // String selProType = (String) params.get("proType");
        // List<String> majoList = new ArrayList<>();
        // for (EnterpriseProjectInfoDo workerUid : list) {
        //     String wMajor = workerUid.getProGroupName();
        //     String proType = workerUid.getProType();
        //     if (StringUtils.isNotBlank(wMajor) && proType.equals(selProType))
        //         if (!majoList.contains(wMajor)) {
        //             majoList.add(wMajor);
        //         }
        // }


        // 从项目表获取专业列表，同时从 ass_award_publish_task.major_team_ids 补充，合并去重。
        String taskId = (String) params.get("taskId");
        // 1) 从项目表获取有项目的专业列表
        List<String> majoList = new ArrayList<>(awardEnterpriseProjectService.getProMajorList(params));
        // 2) 从 ass_award_publish_task.major_team_ids 补充
        List<com.bootdo.activiti.domain.MajorInfo> majorInfoList = awardFlowService.getAwardTaskMajorInfosById(taskId);
        if (majorInfoList != null) {
            for (com.bootdo.activiti.domain.MajorInfo mi : majorInfoList) {
                String name = mi.getMajorName();
                if (name != null && !name.trim().isEmpty() && !majoList.contains(name)) {
                    majoList.add(name);
                }
            }
        }

        R result = R.ok();
        result.put("groupList", majoList);
        return result;
    }



    @RequiresPermissions("act:award:project_professional")
    @RequestMapping("/getAssignMajors")
    @ResponseBody
    public R getProMajorList(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        List<EnterpriseProjectInfoDo> list = awardEnterpriseProjectService.getAllProList(params);
        String selProType = (String) params.get("proType");
        List<String> majoList = new ArrayList<>();
        for (EnterpriseProjectInfoDo workerUid : list) {
            String wMajor = workerUid.getMajor();
            String proType = workerUid.getProType();
            if (StringUtils.isNotBlank(wMajor) && proType.equals(selProType))
                if (!majoList.contains(wMajor)) {
                    majoList.add(wMajor);
                }
        }
        R result = R.ok();
        result.put("majorList", majoList);
        return result;
    }

    /**
     * 专业组管理 添加专家
     * 保存或更新专家分组信息
     *
     * @return
     */
    @ResponseBody
    @PostMapping("/expert/add")
    public R toAddExpert(ExpertGroupDO expertGroupDO) {
        String loginAccountObj = expertGroupDO.getLoginAccount();
        if(loginAccountObj != null) {
            String loginAccount = loginAccountObj.trim();
            expertGroupDO.setLoginAccount(loginAccount);
        }

        Integer id = expertGroupDO.getId();
        if(id == null) {
            Map<String, Object> params = new HashMap<>();
            params.put("loginAccount", expertGroupDO.getLoginAccount());
            params.put("taskId", expertGroupDO.getTaskId());
            params.put("proType", expertGroupDO.getProType());
            List<ExpertGroupDO> expertGroupDOList = expertGroupService.list(params);
            if(expertGroupDOList.size() > 0) {
                id = expertGroupDOList.get(0).getId();
                if(id > 0) {
                    expertGroupDO.setId(id);
                }
            }
        }
        int tag = 0;
        if(id != null) {
           tag = expertGroupService.update(expertGroupDO);
        }else {
           tag = expertGroupService.save(expertGroupDO);
        }
        if (tag > 0) {
            // 原代码：保存专家分组信息后直接返回
            // R r = R.ok();
            // r.put("id", expertGroupDO.getId());
            // return r;
            
            // 新增：保存专家分组信息后，更新该专业组下所有项目的 pro_group_name 字段
            // 这样专家登录后才能通过 JOIN 查询到分配给他的项目
            String groupName = expertGroupDO.getGroupName();
            String taskId = expertGroupDO.getTaskId();
            String proType = expertGroupDO.getProType();
            
            if (StringUtils.isNotBlank(groupName) && StringUtils.isNotBlank(taskId) && StringUtils.isNotBlank(proType)) {
                try {
                    // 查询该任务下该奖项类型下该专业组的所有项目
                    Map<String, Object> projectParams = new HashMap<>();
                    projectParams.put("taskId", taskId);
                    projectParams.put("proType", proType);
                    projectParams.put("major", groupName);  // 使用 major 字段匹配专业
                    
                    List<EnterpriseProjectInfoDo> projectList = awardEnterpriseProjectService.getAllProList(projectParams);
                    
                    // 为每个项目更新 pro_group_name 字段
                    for (EnterpriseProjectInfoDo project : projectList) {
                        if (project.getId()> 0) {
                            try {
                                int proId = project.getId();
                                projectCommonService.updateProGroupName(proId, groupName);
                            } catch (NumberFormatException e) {
                                logger.error("更新项目 pro_group_name 失败，项目ID格式错误: " + project.getId(), e);
                            }
                        }
                    }
                    
                    logger.info("已为专业组 [" + groupName + "] 更新 " + projectList.size() + " 个项目的 pro_group_name 字段");
                } catch (Exception e) {
                    logger.error("更新项目 pro_group_name 字段时出错", e);
                    // 不影响专家保存，仅记录日志
                }
            }
            
            R r = R.ok();
            r.put("id", expertGroupDO.getId());
            return r;
        } else {
            if(tag == -100) {
                return R.error("修改的用户已存在,请删除后再操作");
            }
            return R.error("添加数据出错");
        }
    }

    /**
     * 专业组管理 添加专家
     * 其实是移除专家吧
     *
     * @return
     */
    @ResponseBody
    @PostMapping("/expert/remove")
    public R toRemoveExpert(String loginAccount) {
        if (StringUtils.isBlank(loginAccount)) {
            return R.error("账号为空，无法移除");
        }
        loginAccount = loginAccount.trim();
        Map<String,Object> params = new HashMap<>();
        params.put("loginAccount",loginAccount);
        int count = expertGroupService.count(params);
        if(count == 0) {
            return R.ok();
        }
        //逻辑删除
        int tag = expertGroupService.delByLoginAccount(loginAccount);
        if (tag > 0) {
            //逻辑删除
            userService.delUserByAccount(loginAccount);
            return R.ok();
        } else {
            return R.error("移除数据出错");
        }
    }

    /**
     * 上传分派的专家签名
     *
     * @param params
     * @param map
     * @return
     */
    @RequestMapping("/toUploadExpertSign")
    public String toUploadConfirmFilePage(@RequestParam Map<String, Object> params, ModelMap map) {
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
        return "cpe/science/science_expert_sign_upload";
    }

     /**
     * 上传分派的专家签名
     *
     * @param params
     * @param map
     * @return
     */
    @RequestMapping("/toUploadHeadImg")
    public String toUploadHeadImgFilePage(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        return "cpe/science/personal_head_upload";
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
        if (proId != null && proId > 0 && awardEnterpriseProjectService.updateProCheck(proId) > 0) {
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
    @RequiresPermissions("system:enterpriseChengguoBaseInfo:cancelReview")
    public R cancelCheckPro(Integer proId) {
        if (proId != null && proId > 0 && awardEnterpriseProjectService.updateProApply(proId) > 0) {
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
    @RequestMapping("/teamCancelCheck")
    @ResponseBody
    @RequiresPermissions("system:enterpriTeamInfo:cancelReview")
    public R teamCancelCheckPro(Integer proId) {
        return cancelCheckPro(proId);
    }

    /**
     * 撤回提交审核项目
     *
     * @param proId
     * @return
     */
    @RequestMapping("/personalCancelCheck")
    @ResponseBody
    @RequiresPermissions("system:enterpriPersonalInfo:cancelReview")
    public R personalCancelCheckPro(Integer proId) {
        return cancelCheckPro(proId);
    }

    /**
     * 下载项目中的文件列表
     */
    @RequestMapping("/downloadProDocFiles")
    @RequiresPermissions("system:scienceProcess:downloadProDocFiles")
    @ResponseBody
    public R downloadUpDocFiles(Integer proId) {
        if (proId == null) {
            return R.error("选择要下载的项目");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("proId", proId);
        List<String> fileUrlList = scienceProcessService.getUploadFileUrlList(proId);
        String uploadPath = bootdoConfig.getUploadPath();

        Set<String> filePathList = new HashSet<>();
        fileUrlList.stream().forEach(url -> {
            String filePath = uploadPath + "/" + url.replaceAll("/files/", "");
            filePathList.add(filePath);
        });

        //打包数据下发
        String curDate = DateUtils.getCurDate();
        String[] dateArr = curDate.split("-");
        String userFolderPath = dateArr[0] + "/" + dateArr[1] + "/u_" + getUserId() + "/zip";
        String storeZipFolder = uploadPath + userFolderPath;
        File zipFolder = new File(storeZipFolder);
        if (!zipFolder.exists()) {
            zipFolder.mkdirs();
        }

        String zipFileName = System.currentTimeMillis() + "_" + proId;

        List<String> fList = new ArrayList<>();
        filePathList.stream().forEach(f -> {
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

    @RequestMapping("/removePro")
    @ResponseBody
    @RequiresPermissions("system:enterpriseChengguoBaseInfo:remove")
    public R removeProject(int proId) {
        int rst = awardEnterpriseProjectService.remove(proId + "");
        if (rst == 0) {
            return R.error("删除失败");
        }
        return R.ok();
    }

    /**
     * 新增：跳转到专家-项目分派页面
     * 功能：在专业组管理后，为每位专家分配具体要评审的项目
     * 
     * @param params
     * @param map
     * @return
     */
    @RequiresPermissions("act:award:project_professional")
    @RequestMapping("/toAssignProjectsToExperts")
    public String toAssignProjectsToExperts(@RequestParam Map<String, Object> params, ModelMap map) {
        packageAwardTaskId(map, params);
        
        String taskId = (String) params.get("taskId");
        String proType = (String) params.get("proType");
        
        // 1. 获取该任务下该奖项类型的所有专家
        Map<String, Object> expertParams = new HashMap<>();
        expertParams.put("taskId", taskId);
        expertParams.put("proType", proType);
        List<ExpertGroupDO> expertList = expertGroupService.list(expertParams);
        
        // 2. 为每个专家查询已分派的项目ID列表
        for (ExpertGroupDO expert : expertList) {
            Map<String, Object> assignedParams = new HashMap<>();
            assignedParams.put("taskId", taskId);
            assignedParams.put("userId", expert.getUserId());
            assignedParams.put("proType", proType);
            List<ExpertGroupDO> assignedList = expertGroupService.list(assignedParams);
            
            List<Integer> assignedProjectIds = new ArrayList<>();
            for (ExpertGroupDO assigned : assignedList) {
                if (assigned.getProId() != null && !"0".equals(assigned.getProId())) {
                    try {
                        assignedProjectIds.add(Integer.parseInt(assigned.getProId()));
                    } catch (NumberFormatException e) {
                        // 忽略非数字的proId
                    }
                }
            }
            expert.setAssignedProjectIds(assignedProjectIds);
        }
        
        // 3. 获取该任务下该奖项类型的所有项目
        Map<String, Object> projectParams = new HashMap<>();
        projectParams.put("taskId", taskId);
        projectParams.put("proType", proType);
        List<EnterpriseProjectInfoDo> projectList = awardEnterpriseProjectService.getAllProList(projectParams);
        
        map.put("expertList", expertList);
        map.put("projectList", projectList);
        map.put("taskId", taskId);
        map.put("proType", proType);
        
        return "act/award/assign_projects_to_experts";
    }

    /**
     * 新增：保存专家-项目分派关系
     * 功能：批量保存每位专家要评审的项目
     * 
     * @param requestBody
     * @return
     */
    @RequiresPermissions("act:award:project_professional")
    @RequestMapping("/saveExpertProjectAssignments")
    @ResponseBody
    public R saveExpertProjectAssignments(@RequestBody Map<String, Object> requestBody) {
        try {
            String taskId = (String) requestBody.get("taskId");
            String proType = (String) requestBody.get("proType");
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) requestBody.get("assignments");
            
            if (StringUtils.isBlank(taskId) || StringUtils.isBlank(proType) || assignments == null || assignments.isEmpty()) {
                return R.error("参数不完整");
            }
            
            int successCount = 0;
            int errorCount = 0;
            
            // 遍历每个专家的分派数据
            for (Map<String, Object> assignment : assignments) {
                String expertId = (String) assignment.get("expertId");
                List<String> projectIds = (List<String>) assignment.get("projectIds");
                
                if (StringUtils.isBlank(expertId) || projectIds == null || projectIds.isEmpty()) {
                    continue;
                }
                
                // 先删除该专家在此任务下的所有旧分派记录（proId != "0"的记录）
                Map<String, Object> deleteParams = new HashMap<>();
                deleteParams.put("taskId", taskId);
                deleteParams.put("userId", expertId);
                deleteParams.put("proType", proType);
                List<ExpertGroupDO> oldAssignments = expertGroupService.list(deleteParams);
                for (ExpertGroupDO old : oldAssignments) {
                    if (old.getProId() != null && !"0".equals(old.getProId())) {
                        expertGroupService.remove(old.getId());
                    }
                }
                
                // 为每个项目创建一条专家-项目关联记录
                for (String projectId : projectIds) {
                    // 查询该专家的基础信息（proId=0的记录）
                    Map<String, Object> baseParams = new HashMap<>();
                    baseParams.put("taskId", taskId);
                    baseParams.put("userId", expertId);
                    baseParams.put("proType", proType);
                    baseParams.put("proId", "0");
                    List<ExpertGroupDO> baseList = expertGroupService.list(baseParams);
                    
                    if (baseList.isEmpty()) {
                        errorCount++;
                        continue;
                    }
                    
                    ExpertGroupDO baseExpert = baseList.get(0);
                    
                    // 创建新的分派记录
                    ExpertGroupDO newAssignment = new ExpertGroupDO();
                    newAssignment.setTaskId(taskId);
                    newAssignment.setUserId(expertId);
                    newAssignment.setProId(projectId);
                    newAssignment.setProType(proType);
                    newAssignment.setGroupName(baseExpert.getGroupName());
                    newAssignment.setIsGroupLeader(baseExpert.getIsGroupLeader());
                    newAssignment.setExpertName(baseExpert.getExpertName());
                    newAssignment.setCompany(baseExpert.getCompany());
                    newAssignment.setBankAccount(baseExpert.getBankAccount());
                    newAssignment.setPhone(baseExpert.getPhone());
                    newAssignment.setLoginAccount(baseExpert.getLoginAccount());
                    newAssignment.setExpertSignUrl(baseExpert.getExpertSignUrl());
                    
                    int result = expertGroupService.save(newAssignment);
                    if (result > 0) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                }
            }
            
            if (errorCount > 0) {
                return R.ok("分派完成，成功 " + successCount + " 条，失败 " + errorCount + " 条");
            }
            return R.ok("项目分派成功，共分派 " + successCount + " 条记录");
            
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("分派失败：" + e.getMessage());
        }
    }

}
