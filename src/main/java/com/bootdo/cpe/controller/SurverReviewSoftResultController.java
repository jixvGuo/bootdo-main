package com.bootdo.cpe.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.cpe.domain.EnumAwardType;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.SurverReviewDesignResultDO;
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

import com.bootdo.cpe.domain.SurverReviewSoftResultDO;
import com.bootdo.cpe.service.SurverReviewSoftResultService;
import com.bootdo.cpe.utils.SurverReviewFormLoadHelper;
import com.bootdo.cpe.utils.SurverReviewNotifyHelper;
import com.bootdo.cpe.utils.SurverReviewRecordOpsFactory;
import com.bootdo.cpe.utils.SurverReviewSaveHelper;
import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.Query;
import com.bootdo.common.utils.R;

/**
 * 软件类审查表格
 *
 * @author chglee
 * @email mrhouzhibin@163.com
 * @date 2022-04-13 07:07:29
 */

@Controller
@RequestMapping("/cpe/surverReviewSoftResult")
public class SurverReviewSoftResultController  extends BaseSurverController {
	private String prefix = "cpe/survey/check";

	@Autowired
	private SurverReviewSoftResultService surverReviewSoftResultService;
	@Autowired
	private PetroleumEngineeringService petroleumEngineeringService;
	@Autowired
	private ProjectCommonService projectCommonService;
	@Autowired
	private NotifyService notifyService;

	@GetMapping()
	@RequiresPermissions("cpe:surverApplyInfo:review")
	String SurverReviewSoftResult(@RequestParam Map<String, Object> params, ModelMap map){
		packageAwardTaskId(map, params);
		// 原：带入历史 id，自动保存 update 与正式提交 save 重复插入
		// map.put("reviewResult", SurverReviewFormLoadHelper.loadLatest(params,
		// 		surverReviewSoftResultService::list, SurverReviewSoftResultDO::new));
		map.put("reviewResult", SurverReviewFormLoadHelper.loadLatestForFormSession(params,
				surverReviewSoftResultService::list, SurverReviewSoftResultDO::new));
		return prefix + "/review_soft_template";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("cpe:surverApplyInfo:review")
	public PageUtils list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);
		List<SurverReviewSoftResultDO> surverReviewSoftResultList = surverReviewSoftResultService.list(query);
		int total = surverReviewSoftResultService.count(query);
		PageUtils pageUtils = new PageUtils(surverReviewSoftResultList, total);
		return pageUtils;
	}

	@GetMapping("/add")
	@RequiresPermissions("cpe:surverReviewSoftResult:add")
	String add(){
	    return "cpe/surverReviewSoftResult/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("cpe:surverReviewSoftResult:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		SurverReviewSoftResultDO surverReviewSoftResult = surverReviewSoftResultService.get(id);
		model.addAttribute("surverReviewSoftResult", surverReviewSoftResult);
	    return "cpe/surverReviewSoftResult/edit";
	}

	/**
	 * 保存
	 */
	@ResponseBody
	@PostMapping("/save")
	@RequiresPermissions("cpe:surverReview:add")
	public R save(SurverReviewSoftResultDO surverReviewSoftResult,
	              @RequestParam(value = "formalSubmit", required = false) String formalSubmit,
	              @RequestParam(value = "originReviewResult", required = false) String originReviewResult,
	              @RequestParam(value = "originRemarks", required = false) String originRemarks) {
		Integer proId = surverReviewSoftResult.getProId();
		Long uid = getUserId();
		return SurverReviewSaveHelper.save(
				surverReviewSoftResult,
				formalSubmit,
				originReviewResult,
				originRemarks,
				proId,
				uid,
				SurverReviewRecordOpsFactory.soft(surverReviewSoftResultService),
				notifyService,
				projectCommonService);
	}
	/**
	 * 修改
	 */
	@ResponseBody
	@RequestMapping("/update")
	@RequiresPermissions(value = {"cpe:surverReview:add", "cpe:surverReviewSoftResult:edit"}, logical = Logical.OR)
	public R update(SurverReviewSoftResultDO surverReviewSoftResult,
	                @RequestParam(value = "formalSubmit", required = false) String formalSubmit) {
		// 原：仅 update，不发通知
		// surverReviewSoftResultService.update(surverReviewSoftResult);
		// return R.ok();

		Long uid = getUserId();
		surverReviewSoftResult.setOptUid(uid.intValue());
		surverReviewSoftResultService.update(surverReviewSoftResult);
		if (SurverReviewNotifyHelper.isFormalSubmit(formalSubmit)
				&& surverReviewSoftResult.getId() != null
				&& surverReviewSoftResult.getProId() != null) {
			SurverReviewNotifyHelper.sendFormalReviewNotify(
					notifyService, projectCommonService, surverReviewSoftResult.getProId(),
					surverReviewSoftResult.getProName(),
					surverReviewSoftResult.getReviewResult(),
					surverReviewSoftResult.getRemarks(),
					uid, surverReviewSoftResult.getId());
		}
		R r = R.ok();
		if (surverReviewSoftResult.getId() != null) {
			r.put("id", surverReviewSoftResult.getId());
		}
		return r;
	}

	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewSoftResult:remove")
	public R remove( Integer id){
		if(surverReviewSoftResultService.remove(id)>0){
		return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewSoftResult:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		surverReviewSoftResultService.batchRemove(ids);
		return R.ok();
	}

}
