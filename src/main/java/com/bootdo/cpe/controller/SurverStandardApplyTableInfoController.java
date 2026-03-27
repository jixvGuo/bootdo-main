package com.bootdo.cpe.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.cpe.domain.AwardEnterpriseProjectDO;
import com.bootdo.cpe.domain.SurverStandardApplyProjectProfileDO;
import com.bootdo.cpe.domain.SurverStandardApplyTableInfoDO;
import com.bootdo.cpe.service.AwardEnterpriseProjectCommonService;
import com.bootdo.cpe.service.SurverStandardApplyProjectProfileService;
import com.bootdo.cpe.service.SurverStandardApplyTableInfoService;
import com.bootdo.cpe.utils.AwardSurverSubTypeEnum;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.Query;
import com.bootdo.common.utils.R;

/**
 *
 *
 * @author chglee
 * @email mrhouzhibin@163.com
 * @date 2022-03-27 21:44:17
 */

@Controller
@RequestMapping("/cpe/surverStandardApplyTableInfo")
public class SurverStandardApplyTableInfoController extends BaseSurverController {
	@Autowired
	private SurverStandardApplyTableInfoService surverStandardApplyTableInfoService;
	@Autowired
	private AwardEnterpriseProjectCommonService awardEnterpriseProjectCommonService;
	@Autowired
	private SurverStandardApplyProjectProfileService surverStandardApplyProjectProfileService;

	@GetMapping()
	@RequiresPermissions("cpe:surverStandardApplyTableInfo:surverStandardApplyTableInfo")
	String SurverStandardApplyTableInfo(){
	    return "system/surverStandardApplyTableInfo/surverStandardApplyTableInfo";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("cpe:surverStandardApplyTableInfo:surverStandardApplyTableInfo")
	public PageUtils list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);
		List<SurverStandardApplyTableInfoDO> surverStandardApplyTableInfoList = surverStandardApplyTableInfoService.list(query);
		int total = surverStandardApplyTableInfoService.count(query);
		PageUtils pageUtils = new PageUtils(surverStandardApplyTableInfoList, total);
		return pageUtils;
	}

	@GetMapping("/add")
	@RequiresPermissions("cpe:surverStandardApplyTableInfo:add")
	String add(){
	    return "system/surverStandardApplyTableInfo/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("cpe:surverStandardApplyTableInfo:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		SurverStandardApplyTableInfoDO surverStandardApplyTableInfo = surverStandardApplyTableInfoService.get(id);
		model.addAttribute("surverStandardApplyTableInfo", surverStandardApplyTableInfo);
	    return "system/surverStandardApplyTableInfo/edit";
	}

	/**
	 * 保存
	 */
	@ResponseBody
	@PostMapping("/save")
	public R save( SurverStandardApplyTableInfoDO surverStandardApplyTableInfo){
		AwardEnterpriseProjectDO projectDO = new AwardEnterpriseProjectDO();
		projectDO.setId(surverStandardApplyTableInfo.getProId());
		projectDO.setMajor(surverStandardApplyTableInfo.getApplyMajor());
		projectDO.setProSubType(AwardSurverSubTypeEnum.STANDARD.getSubType());
		projectDO.setChengguo(surverStandardApplyTableInfo.getGalleryName());
		awardEnterpriseProjectCommonService.update(projectDO);

		Long optUid = getUserId();
		surverStandardApplyTableInfo.setOptUid(optUid.intValue());
		Integer id = surverStandardApplyTableInfo.getId();
		if(id != null && id > 0) {
			int rst = surverStandardApplyTableInfoService.update(surverStandardApplyTableInfo);
			if (rst > 0) {
				syncStandardProDesc(surverStandardApplyTableInfo);
				return R.ok();
			}
			return R.error();
		}
		if(surverStandardApplyTableInfoService.save(surverStandardApplyTableInfo)>0){
			syncStandardProDesc(surverStandardApplyTableInfo);
			R r = R.ok();
			r.put("id", surverStandardApplyTableInfo.getId());
			return r;
		}
		return R.error();
	}
	/**
	 * 修改
	 */
	@ResponseBody
	@RequestMapping("/update")
	@RequiresPermissions("cpe:surverStandardApplyTableInfo:edit")
	public R update( SurverStandardApplyTableInfoDO surverStandardApplyTableInfo){
		surverStandardApplyTableInfoService.update(surverStandardApplyTableInfo);
		return R.ok();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("cpe:surverStandardApplyTableInfo:remove")
	public R remove( Integer id){
		if(surverStandardApplyTableInfoService.remove(id)>0){
		return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("cpe:surverStandardApplyTableInfo:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		surverStandardApplyTableInfoService.batchRemove(ids);
		return R.ok();
	}

	private void syncStandardProDesc(SurverStandardApplyTableInfoDO tableInfo) {
		if (tableInfo == null || tableInfo.getProId() == null || tableInfo.getTaskId() == null) {
			return;
		}
		Map<String, Object> query = new HashMap<>();
		query.put("proId", tableInfo.getProId());
		query.put("taskId", tableInfo.getTaskId());
		query.put("sort", "id");
		query.put("order", "desc");
		query.put("offset", 0);
		query.put("limit", 1);
		List<SurverStandardApplyProjectProfileDO> list = surverStandardApplyProjectProfileService.list(query);
		SurverStandardApplyProjectProfileDO proDesc = list.size() > 0 ? list.get(0) : new SurverStandardApplyProjectProfileDO();
		proDesc.setProId(tableInfo.getProId());
		proDesc.setTaskId(tableInfo.getTaskId());
		proDesc.setOptUid(getUserId().intValue());
		proDesc.setDeleted(0);
		proDesc.setReportingUnit(joinUnit(tableInfo.getDditorChief(), tableInfo.getCooperationUnit()));
		proDesc.setAwardCategory("标准设计奖");
		proDesc.setProjectName(tableInfo.getGalleryName());
		if (proDesc.getId() != null && proDesc.getId() > 0) {
			surverStandardApplyProjectProfileService.update(proDesc);
		} else {
			surverStandardApplyProjectProfileService.save(proDesc);
		}
	}

	private String joinUnit(String mainUnit, String cooperationUnit) {
		StringBuilder sb = new StringBuilder();
		if (mainUnit != null && mainUnit.trim().length() > 0) {
			sb.append(mainUnit.trim());
		}
		if (cooperationUnit != null && cooperationUnit.trim().length() > 0) {
			sb.append(cooperationUnit.trim());
		}
		return sb.toString();
	}

}
