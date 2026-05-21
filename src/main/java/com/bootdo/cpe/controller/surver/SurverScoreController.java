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
import com.bootdo.cpe.service.ExpertGroupService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bootdo.common.config.Constant.ROLE_SPECIALIST_ID;
import static com.bootdo.common.config.Constant.ROLE_SURVER_SPECALIST_ID;

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

}
