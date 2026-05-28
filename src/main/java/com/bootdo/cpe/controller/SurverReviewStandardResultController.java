package com.bootdo.cpe.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.cpe.domain.EnumAwardType;
import com.bootdo.cpe.domain.EnumProjectType;
import com.bootdo.cpe.domain.SurverReviewSoftResultDO;
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

import com.bootdo.cpe.domain.SurverReviewStandardResultDO;
import com.bootdo.cpe.service.SurverReviewStandardResultService;
import com.bootdo.cpe.utils.SurverReviewFormLoadHelper;
import com.bootdo.cpe.utils.SurverReviewNotifyHelper;
import com.bootdo.cpe.utils.SurverReviewRecordOpsFactory;
import com.bootdo.cpe.utils.SurverReviewSaveHelper;
import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.Query;
import com.bootdo.common.utils.R;

/**
 * 标准设计类审查表格
 *
 * @author chglee
 * @email mrhouzhibin@163.com
 * @date 2022-04-13 07:07:29
 */

@Controller
@RequestMapping("/cpe/surverReviewStandardResult")
public class SurverReviewStandardResultController extends BaseSurverController {
	private String prefix = "cpe/survey/check";
	@Autowired
	private SurverReviewStandardResultService surverReviewStandardResultService;
	@Autowired
	private PetroleumEngineeringService petroleumEngineeringService;
	@Autowired
	private ProjectCommonService projectCommonService;
	@Autowired
	private NotifyService notifyService;

	@GetMapping()
	@RequiresPermissions("cpe:surverApplyInfo:review")
	String SurverReviewStandardResult(@RequestParam Map<String, Object> params, ModelMap map){
		packageAwardTaskId(map, params);
		// 原：带入历史 id，自动保存 update 与正式提交 save 重复插入
		// map.put("reviewResult", SurverReviewFormLoadHelper.loadLatest(params,
		// 		surverReviewStandardResultService::list, SurverReviewStandardResultDO::new));
		map.put("reviewResult", SurverReviewFormLoadHelper.loadLatestForFormSession(params,
				surverReviewStandardResultService::list, SurverReviewStandardResultDO::new));
		return prefix + "/review_standard_template";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("cpe:surverApplyInfo:review")
	public PageUtils list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);
		List<SurverReviewStandardResultDO> surverReviewStandardResultList = surverReviewStandardResultService.list(query);
		int total = surverReviewStandardResultService.count(query);
		PageUtils pageUtils = new PageUtils(surverReviewStandardResultList, total);
		return pageUtils;
	}

	@GetMapping("/add")
	@RequiresPermissions("cpe:surverReviewStandardResult:add")
	String add(){
	    return "cpe/surverReviewStandardResult/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("cpe:surverReviewStandardResult:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		SurverReviewStandardResultDO surverReviewStandardResult = surverReviewStandardResultService.get(id);
		model.addAttribute("surverReviewStandardResult", surverReviewStandardResult);
	    return "cpe/surverReviewStandardResult/edit";
	}

	/**
	 * 保存
	 */
	@ResponseBody
	@PostMapping("/save")
	@RequiresPermissions("cpe:surverReview:add")
	public R save(SurverReviewStandardResultDO surverReviewStandardResult,
	              @RequestParam(value = "formalSubmit", required = false) String formalSubmit,
	              @RequestParam(value = "originReviewResult", required = false) String originReviewResult,
	              @RequestParam(value = "originRemarks", required = false) String originRemarks) {
		Integer proId = surverReviewStandardResult.getProId();
		Long uid = getUserId();
		return SurverReviewSaveHelper.save(
				surverReviewStandardResult,
				formalSubmit,
				originReviewResult,
				originRemarks,
				proId,
				uid,
				SurverReviewRecordOpsFactory.standard(surverReviewStandardResultService),
				notifyService,
				projectCommonService);
	}
	/**
	 * 修改
	 */
	@ResponseBody
	@RequestMapping("/update")
	@RequiresPermissions(value = {"cpe:surverReview:add", "cpe:surverReviewStandardResult:edit"}, logical = Logical.OR)
	public R update(SurverReviewStandardResultDO surverReviewStandardResult,
	                @RequestParam(value = "formalSubmit", required = false) String formalSubmit) {
		// 原：仅 update，不发通知
		// surverReviewStandardResultService.update(surverReviewStandardResult);
		// return R.ok();

		Long uid = getUserId();
		surverReviewStandardResult.setOptUid(uid.intValue());
		surverReviewStandardResultService.update(surverReviewStandardResult);
		if (SurverReviewNotifyHelper.isFormalSubmit(formalSubmit)
				&& surverReviewStandardResult.getId() != null
				&& surverReviewStandardResult.getProId() != null) {
			SurverReviewNotifyHelper.sendFormalReviewNotify(
					notifyService, projectCommonService, surverReviewStandardResult.getProId(),
					surverReviewStandardResult.getProName(),
					surverReviewStandardResult.getReviewResult(),
					surverReviewStandardResult.getRemarks(),
					uid, surverReviewStandardResult.getId());
		}
		R r = R.ok();
		if (surverReviewStandardResult.getId() != null) {
			r.put("id", surverReviewStandardResult.getId());
		}
		return r;
	}

	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewStandardResult:remove")
	public R remove( Integer id){
		if(surverReviewStandardResultService.remove(id)>0){
		return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("cpe:surverReviewStandardResult:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		surverReviewStandardResultService.batchRemove(ids);
		return R.ok();
	}

}
