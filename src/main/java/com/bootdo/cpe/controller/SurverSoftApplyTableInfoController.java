package com.bootdo.cpe.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.cpe.domain.AwardEnterpriseProjectDO;
import com.bootdo.cpe.service.AwardEnterpriseProjectCommonService;
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

import com.bootdo.cpe.domain.SurverSoftApplyProjectProfileDO;
import com.bootdo.cpe.domain.SurverSoftApplyTableInfoDO;
import com.bootdo.cpe.service.SurverSoftApplyProjectProfileService;
import com.bootdo.cpe.service.SurverSoftApplyTableInfoService;
import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.Query;
import com.bootdo.common.utils.R;

/**
 *  石油工程建设优秀勘察设计计算机软件奖申报表
 *
 * @author chglee
 * @email mrhouzhibin@163.com
 * @date 2022-03-29 20:51:20
 */

@Controller
@RequestMapping("/cpe/surverSoftApplyTableInfo")
public class SurverSoftApplyTableInfoController extends BaseSurverController {
	@Autowired
	private SurverSoftApplyTableInfoService surverSoftApplyTableInfoService;
	@Autowired
	private AwardEnterpriseProjectCommonService awardEnterpriseProjectCommonService;
	@Autowired
	private SurverSoftApplyProjectProfileService surverSoftApplyProjectProfileService;

	@GetMapping()
	@RequiresPermissions("cpe:surverSoftApplyTableInfo:surverSoftApplyTableInfo")
	String SurverSoftApplyTableInfo(){
	    return "cpe/surverSoftApplyTableInfo/surverSoftApplyTableInfo";
	}

	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("cpe:surverSoftApplyTableInfo:surverSoftApplyTableInfo")
	public PageUtils list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);
		List<SurverSoftApplyTableInfoDO> surverSoftApplyTableInfoList = surverSoftApplyTableInfoService.list(query);
		int total = surverSoftApplyTableInfoService.count(query);
		PageUtils pageUtils = new PageUtils(surverSoftApplyTableInfoList, total);
		return pageUtils;
	}

	@GetMapping("/add")
	@RequiresPermissions("cpe:surverSoftApplyTableInfo:add")
	String add(){
	    return "cpe/surverSoftApplyTableInfo/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("cpe:surverSoftApplyTableInfo:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		SurverSoftApplyTableInfoDO surverSoftApplyTableInfo = surverSoftApplyTableInfoService.get(id);
		model.addAttribute("surverSoftApplyTableInfo", surverSoftApplyTableInfo);
	    return "cpe/surverSoftApplyTableInfo/edit";
	}

	/**
	 * 保存
	 */
	@ResponseBody
	@PostMapping("/save")
	public R save( SurverSoftApplyTableInfoDO surverSoftApplyTableInfo){
		AwardEnterpriseProjectDO projectDO = new AwardEnterpriseProjectDO();
		projectDO.setId(surverSoftApplyTableInfo.getProId());
		projectDO.setMajor(surverSoftApplyTableInfo.getApplyMajor());
		projectDO.setProSubType(AwardSurverSubTypeEnum.SOFTWARE.getSubType());
		projectDO.setChengguo(surverSoftApplyTableInfo.getSoftName());
		awardEnterpriseProjectCommonService.update(projectDO);

		Long optUid = getUserId();
		surverSoftApplyTableInfo.setOptUid(optUid.intValue());
		Integer id = surverSoftApplyTableInfo.getId();
		if(id != null && id > 0) {
			int rst = surverSoftApplyTableInfoService.update(surverSoftApplyTableInfo);
			if (rst > 0) {
				syncSoftProDesc(surverSoftApplyTableInfo);
				return R.ok();
			}
			return R.error();
		}
		if(surverSoftApplyTableInfoService.save(surverSoftApplyTableInfo)>0){
			syncSoftProDesc(surverSoftApplyTableInfo);
			R r = R.ok();
			r.put("id", surverSoftApplyTableInfo.getId());
			return r;
		}
		return R.error();
	}
	/**
	 * 修改
	 */
	@ResponseBody
	@RequestMapping("/update")
	@RequiresPermissions("cpe:surverSoftApplyTableInfo:edit")
	public R update( SurverSoftApplyTableInfoDO surverSoftApplyTableInfo){
		surverSoftApplyTableInfoService.update(surverSoftApplyTableInfo);
		return R.ok();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("cpe:surverSoftApplyTableInfo:remove")
	public R remove( Integer id){
		if(surverSoftApplyTableInfoService.remove(id)>0){
		return R.ok();
		}
		return R.error();
	}

	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("cpe:surverSoftApplyTableInfo:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		surverSoftApplyTableInfoService.batchRemove(ids);
		return R.ok();
	}

	private void syncSoftProDesc(SurverSoftApplyTableInfoDO tableInfo) {
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
		List<SurverSoftApplyProjectProfileDO> list = surverSoftApplyProjectProfileService.list(query);
		SurverSoftApplyProjectProfileDO proDesc = list.size() > 0 ? list.get(0) : new SurverSoftApplyProjectProfileDO();
		proDesc.setProId(tableInfo.getProId());
		proDesc.setTaskId(tableInfo.getTaskId());
		proDesc.setOptUid(getUserId().intValue());
		proDesc.setDeleted(0);
		proDesc.setReportingUnit(joinUnit(tableInfo.getEditorChief(), tableInfo.getCooperationUnit()));
		proDesc.setAwardCategory("计算机软件奖");
		proDesc.setProjectName(tableInfo.getSoftName());
		if (proDesc.getId() != null && proDesc.getId() > 0) {
			surverSoftApplyProjectProfileService.update(proDesc);
		} else {
			surverSoftApplyProjectProfileService.save(proDesc);
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
