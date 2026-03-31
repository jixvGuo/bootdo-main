package com.bootdo.system.dao;

import com.bootdo.system.domain.EnterpriPersonalInfoDO;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * 企业报奖个人信息
 * @author chglee
 * @email mrhouzhibin@163.com
 * @date 2020-06-30 04:48:14
 */
@Mapper
public interface EnterpriPersonalInfoDao {

	EnterpriPersonalInfoDO get(Integer id);

	/**
	 * 查询先进个人奖申报项目列表的 SQL，主要用于协会工作人员、专家组长等角色查看和管理先进个人奖的申报信息
	 * @param map
	 * @return
	 */
	List<EnterpriPersonalInfoDO> list(Map<String,Object> map);
	
	int count(Map<String,Object> map);
	
	int save(EnterpriPersonalInfoDO enterpriPersonalInfo);
	
	int update(EnterpriPersonalInfoDO enterpriPersonalInfo);

	int updateMajor(EnterpriPersonalInfoDO enterpriPersonalInfo);

	int remove(Integer id);
	
	int batchRemove(Integer[] ids);
}
