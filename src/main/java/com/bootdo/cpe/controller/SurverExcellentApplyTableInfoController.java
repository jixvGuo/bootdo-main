package com.bootdo.cpe.controller;

import java.util.List;
import java.util.Map;

import com.bootdo.common.controller.BaseSurverController;
import com.bootdo.cpe.domain.AwardEnterpriseProjectDO;
import com.bootdo.system.domain.SurverExcellentApplyTableInfoDO;
import com.bootdo.cpe.service.AwardEnterpriseProjectCommonService;
import com.bootdo.cpe.service.SurverExcellentApplyTableInfoService;
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
@RequestMapping("/cpe/surverExcellentApplyTableInfo")
public class SurverExcellentApplyTableInfoController extends BaseSurverController {
	@Autowired
	private SurverExcellentApplyTableInfoService surverExcellentApplyTableInfoService;
	@Autowired
	private AwardEnterpriseProjectCommonService awardEnterpriseProjectCommonService;
	
	@GetMapping()
	@RequiresPermissions("cpe:surverExcellentApplyTableInfo:surverExcellentApplyTableInfo")
	String SurverExcellentApplyTableInfo(){
	    return "cpe/surverExcellentApplyTableInfo/surverExcellentApplyTableInfo";
	}
	
	@ResponseBody
	@GetMapping("/list")
	@RequiresPermissions("cpe:surverExcellentApplyTableInfo:surverExcellentApplyTableInfo")
	public PageUtils list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);
		List<SurverExcellentApplyTableInfoDO> surverExcellentApplyTableInfoList = surverExcellentApplyTableInfoService.list(query);
		int total = surverExcellentApplyTableInfoService.count(query);
		PageUtils pageUtils = new PageUtils(surverExcellentApplyTableInfoList, total);
		return pageUtils;
	}
	
	@GetMapping("/add")
	@RequiresPermissions("cpe:surverExcellentApplyTableInfo:add")
	String add(){
	    return "cpe/surverExcellentApplyTableInfo/add";
	}

	@GetMapping("/edit/{id}")
	@RequiresPermissions("cpe:surverExcellentApplyTableInfo:edit")
	String edit(@PathVariable("id") Integer id,Model model){
		SurverExcellentApplyTableInfoDO surverExcellentApplyTableInfo = surverExcellentApplyTableInfoService.get(id);
		model.addAttribute("surverExcellentApplyTableInfo", surverExcellentApplyTableInfo);
	    return "cpe/surverExcellentApplyTableInfo/edit";
	}
	
	/**
	 * 保存
	 */
	@ResponseBody
	@PostMapping("/save")
	public R save( SurverExcellentApplyTableInfoDO surverExcellentApplyTableInfo){
		AwardEnterpriseProjectDO projectDO = new AwardEnterpriseProjectDO();
		projectDO.setId(surverExcellentApplyTableInfo.getProId());
		projectDO.setMajor(surverExcellentApplyTableInfo.getApplyMajor());
		projectDO.setProSubType(AwardSurverSubTypeEnum.CONTRIBUTION.getSubType());
		projectDO.setChengguo(surverExcellentApplyTableInfo.getProName());
		awardEnterpriseProjectCommonService.update(projectDO);

		Long optUid = getUserId();
		surverExcellentApplyTableInfo.setOptUid(optUid.intValue());
		Integer id = surverExcellentApplyTableInfo.getId();
		if(id != null && id > 0) {
			int rst = surverExcellentApplyTableInfoService.update(surverExcellentApplyTableInfo);
			return rst > 0 ? R.ok() : R.error();
		}
		if(surverExcellentApplyTableInfoService.save(surverExcellentApplyTableInfo)>0){
			R r = R.ok();
			r.put("id", surverExcellentApplyTableInfo.getId());
			return r;
		}
		return R.error();
	}
	/**
	 * 修改
	 */
	@ResponseBody
	@RequestMapping("/update")
	@RequiresPermissions("cpe:surverExcellentApplyTableInfo:edit")
	public R update( SurverExcellentApplyTableInfoDO surverExcellentApplyTableInfo){
		surverExcellentApplyTableInfoService.update(surverExcellentApplyTableInfo);
		return R.ok();
	}
	
	/**
	 * 删除
	 */
	@PostMapping( "/remove")
	@ResponseBody
	@RequiresPermissions("cpe:surverExcellentApplyTableInfo:remove")
	public R remove( Integer id){
		if(surverExcellentApplyTableInfoService.remove(id)>0){
		return R.ok();
		}
		return R.error();
	}
	
	/**
	 * 删除
	 */
	@PostMapping( "/batchRemove")
	@ResponseBody
	@RequiresPermissions("cpe:surverExcellentApplyTableInfo:batchRemove")
	public R remove(@RequestParam("ids[]") Integer[] ids){
		surverExcellentApplyTableInfoService.batchRemove(ids);
		return R.ok();
	}
	
}
