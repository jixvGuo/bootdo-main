package com.bootdo.cpe.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.cpe.domain.EnumAwardType;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.SurverReviewStandardResultDO;
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

import com.bootdo.cpe.domain.SurverReviewSurverResultDO;
import com.bootdo.cpe.service.SurverReviewSurverResultService;
import com.bootdo.cpe.utils.SurverReviewFormLoadHelper;
import com.bootdo.cpe.utils.SurverReviewNotifyHelper;
import com.bootdo.cpe.utils.SurverReviewRecordOpsFactory;
import com.bootdo.cpe.utils.SurverReviewSaveHelper;
import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.Query;
import com.bootdo.common.utils.R;

/**
 * 勘察类审查表格
 *
 * @author chglee
 * @email mrhouzhibin@163.com
 * @date 2022-04-13 07:07:29
 */

@Controller
@RequestMapping("/cpe/surverReviewSurverResult")
public class SurverReviewSurverResultController extends BaseSurverController {
	private String prefix = "cpe/survey/check";
	@Autowired
	private SurverReviewSurverResultService surverReviewSurverResultService;
	@Autowired
	private PetroleumEngineeringService petroleumEngineeringService;
	@Autowired
	private ProjectCommonService projectCommonService;
	@Autowired
	private NotifyService notifyService;

	@GetMapping()
	@RequiresPermissions("cpe:surverApplyInfo:review")
	String SurverReviewSurverResult(@RequestParam Map<String, Object> params, ModelMap map){
	    packageAwardTaskId(map, params);
		// 原：带入历史 id，自动保存 update 与正式提交 save 重复插入
		// map.put("reviewResult", SurverReviewFormLoadHelper.loadLatest(params,
		// 		surverReviewSurverResultService::list, SurverReviewSurverResultDO::new));
		map.put("reviewResult", SurverReviewFormLoadHelper.loadLatestForFormSession(params,
				surverReviewSurverResultService::list, SurverReviewSurverResultDO::new));
		return prefix + "/review_surver_template";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("cpe:surverApplyInfo:review")
	public PageUtils list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);
		List<SurverReviewSurverResultDO> surverReviewSurverResultList = surverReviewSurverResultService.list(query);
		int total = surverReviewSurverResultService.count(query);
		PageUtils pageUtils = new PageUtils(surverReviewSurverResultList, total);
		return pageUtils;
	}

	@GetMapping("/add")
	@RequiresPermissions("cpe:surverReviewSurverResult:add")
	String add(){
	    return "cpe/surverReviewSurverResult/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("cpe:surverReviewSurverResult:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		SurverReviewSurverResultDO surverReviewSurverResult = surverReviewSurverResultService.get(id);
		model.addAttribute("surverReviewSurverResult", surverReviewSurverResult);
	    return "cpe/surverReviewSurverResult/edit";
	}

	/**
	 * 保存
	 */
	@ResponseBody
	@PostMapping("/save")
	@RequiresPermissions("cpe:surverReview:add")
	public R save(SurverReviewSurverResultDO surverReviewSurverResult,
	              @RequestParam(value = "formalSubmit", required = false) String formalSubmit,
	              @RequestParam(value = "originReviewResult", required = false) String originReviewResult,
	              @RequestParam(value = "originRemarks", required = false) String originRemarks) {
		Integer proId = surverReviewSurverResult.getProId();
		Long uid = getUserId();
		return SurverReviewSaveHelper.save(
				surverReviewSurverResult,
				formalSubmit,
				originReviewResult,
				originRemarks,
				proId,
				uid,
				SurverReviewRecordOpsFactory.surver(surverReviewSurverResultService),
				notifyService,
				projectCommonService);
	}
	/**
	 * 修改
	 */
	@ResponseBody
	@RequestMapping("/update")
	@RequiresPermissions(value = {"cpe:surverReview:add", "cpe:surverReviewSurverResult:edit"}, logical = Logical.OR)
	public R update(SurverReviewSurverResultDO surverReviewSurverResult,
	                @RequestParam(value = "formalSubmit", required = false) String formalSubmit) {
		// 原：仅 update，不发通知
		// surverReviewSurverResultService.update(surverReviewSurverResult);
		// return R.ok();

		Long uid = getUserId();
		surverReviewSurverResult.setOptUid(uid.intValue());
		surverReviewSurverResultService.update(surverReviewSurverResult);
		if (SurverReviewNotifyHelper.isFormalSubmit(formalSubmit)
				&& surverReviewSurverResult.getId() != null
				&& surverReviewSurverResult.getProId() != null) {
			SurverReviewNotifyHelper.sendFormalReviewNotify(
					notifyService, projectCommonService, surverReviewSurverResult.getProId(),
					surverReviewSurverResult.getProName(),
					surverReviewSurverResult.getReviewResult(),
					surverReviewSurverResult.getRemarks(),
					uid, surverReviewSurverResult.getId());
		}
		R r = R.ok();
		if (surverReviewSurverResult.getId() != null) {
			r.put("id", surverReviewSurverResult.getId());
		}
		return r;
	}

	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewSurverResult:remove")
	public R remove( Integer id){
		if(surverReviewSurverResultService.remove(id)>0){
		return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewSurverResult:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		surverReviewSurverResultService.batchRemove(ids);
		return R.ok();
	}

}
