package com.bootdo.cpe.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.cpe.domain.EnumAwardType;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.petroleum_engineering_award.service.PetroleumEngineeringService;
import com.bootdo.cpe.service.ProjectCommonService;
import com.bootdo.oa.domain.NotifyDO;
import com.bootdo.oa.service.NotifyService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bootdo.cpe.domain.SurverReviewConsultResultDO;
import com.bootdo.cpe.service.SurverReviewConsultResultService;
import com.bootdo.cpe.utils.SurverReviewFormLoadHelper;
import com.bootdo.cpe.utils.SurverReviewNotifyHelper;
import com.bootdo.cpe.utils.SurverReviewRecordOpsFactory;
import com.bootdo.cpe.utils.SurverReviewSaveHelper;
import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.Query;
import com.bootdo.common.utils.R;

/**
 * 咨询类形式审查模板
 *
 * @author chglee
 * @email mrhouzhibin@163.com
 * @date 2022-04-13 07:07:29
 */

@Controller
@RequestMapping("/cpe/surverReviewConsultResult")
public class SurverReviewConsultResultController extends BaseSurverController {
	private String prefix = "cpe/survey/check";
	@Autowired
	private SurverReviewConsultResultService surverReviewConsultResultService;
	@Autowired
	private PetroleumEngineeringService petroleumEngineeringService;
	@Autowired
	private ProjectCommonService projectCommonService;
	@Autowired
	private NotifyService notifyService;

	@GetMapping()
	@RequiresPermissions("cpe:surverApplyInfo:review")
	String SurverReviewConsultResult(@RequestParam Map<String, Object> params, ModelMap map){
		packageAwardTaskId(map, params);
		// 原：带入历史 id，自动保存 update 与正式提交 save 重复插入
		// map.put("reviewResult", SurverReviewFormLoadHelper.loadLatest(params,
		// 		surverReviewConsultResultService::list, SurverReviewConsultResultDO::new));
		map.put("reviewResult", SurverReviewFormLoadHelper.loadLatestForFormSession(params,
				surverReviewConsultResultService::list, SurverReviewConsultResultDO::new));
	    return prefix + "/review_consulting_template";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("cpe:surverApplyInfo:review")
	public PageUtils list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);
		List<SurverReviewConsultResultDO> surverReviewConsultResultList = surverReviewConsultResultService.list(query);
		int total = surverReviewConsultResultService.count(query);
		PageUtils pageUtils = new PageUtils(surverReviewConsultResultList, total);
		return pageUtils;
	}

	@GetMapping("/add")
	@RequiresPermissions("cpe:surverReviewConsultResult:add")
	String add(){
	    return "cpe/surverReviewConsultResult/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("cpe:surverReviewConsultResult:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		SurverReviewConsultResultDO surverReviewConsultResult = surverReviewConsultResultService.get(id);
		model.addAttribute("surverReviewConsultResult", surverReviewConsultResult);
	    return "cpe/surverReviewConsultResult/edit";
	}

	/**
	 * 保存
	 */
	@ResponseBody
	@PostMapping("/save")
	@RequiresPermissions("cpe:surverReview:add")
	public R save(SurverReviewConsultResultDO surverReviewConsultResult,
	              @RequestParam(value = "formalSubmit", required = false) String formalSubmit,
	              @RequestParam(value = "originReviewResult", required = false) String originReviewResult,
	              @RequestParam(value = "originRemarks", required = false) String originRemarks) {
		Integer proId = surverReviewConsultResult.getProId();
		Long uid = getUserId();
		return SurverReviewSaveHelper.save(
				surverReviewConsultResult,
				formalSubmit,
				originReviewResult,
				originRemarks,
				proId,
				uid,
				SurverReviewRecordOpsFactory.consult(surverReviewConsultResultService),
				notifyService,
				projectCommonService);
	}
	/**
	 * 修改
	 */
	@ResponseBody
	@RequestMapping("/update")
	@RequiresPermissions(value = {"cpe:surverReview:add", "cpe:surverReviewConsultResult:edit"}, logical = Logical.OR)
	public R update(SurverReviewConsultResultDO surverReviewConsultResult,
	                @RequestParam(value = "formalSubmit", required = false) String formalSubmit) {
		// 原：仅 update，不发通知
		// surverReviewConsultResultService.update(surverReviewConsultResult);
		// return R.ok();

		Long uid = getUserId();
		surverReviewConsultResult.setOptUid(uid.intValue());
		surverReviewConsultResultService.update(surverReviewConsultResult);
		if (SurverReviewNotifyHelper.isFormalSubmit(formalSubmit)
				&& surverReviewConsultResult.getId() != null
				&& surverReviewConsultResult.getProId() != null) {
			SurverReviewNotifyHelper.sendFormalReviewNotify(
					notifyService, projectCommonService, surverReviewConsultResult.getProId(),
					surverReviewConsultResult.getProName(),
					surverReviewConsultResult.getReviewResult(),
					surverReviewConsultResult.getRemarks(),
					uid, surverReviewConsultResult.getId());
		}
		R r = R.ok();
		if (surverReviewConsultResult.getId() != null) {
			r.put("id", surverReviewConsultResult.getId());
		}
		return r;
	}

	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewConsultResult:remove")
	public R remove( Integer id){
		if(surverReviewConsultResultService.remove(id)>0){
		return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewConsultResult:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		surverReviewConsultResultService.batchRemove(ids);
		return R.ok();
	}

}
