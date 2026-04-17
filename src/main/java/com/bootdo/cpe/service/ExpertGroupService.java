package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.EnterpriTeamProjectInfoDO;
import com.bootdo.cpe.domain.ExpertGroupDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/***
 * 专业组管理
 */
public interface ExpertGroupService {
    // 得到专业信息
    ExpertGroupDO get(Integer id);

    List<ExpertGroupDO> list(Map<String, Object> map);

    String getSignUrl(Map<String,Object> params);

    int count(Map<String, Object> map);

    int save(ExpertGroupDO expertGroupDO);

    int update(ExpertGroupDO expertGroupDO);

    int updateExpertSignId(long signId, String taskId, long expertUid);

    int remove(Integer id);
    int removeByLoginAccount(String loginAccount);

    /**
     * 逻辑删除专家账号
     * @param loginAccount
     * @return
     */
    int delByLoginAccount(String loginAccount);

    /**
     * (得改，连接的表不对)
     * 新增：获取指定条件下的不重复专业组名称列表
     * @param params 包含taskId、proType等过滤条件
     * @return 不重复的group_name列表
     */
    List<String> getDistinctGroupNames(Map<String, Object> params);

    /**
     * 按 userId + taskId + proType 物理删除记录（用于形审专家分组绑定的清理）
     */
    int deleteByUserIdAndTaskIdAndProType(String userId, String taskId, String proType);

    /**
     * 直接插入一条记录（不做重复检查，用于形审专家分组绑定）
     */
    int directSave(ExpertGroupDO expertGroupDO);

}
