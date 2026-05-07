package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.ExpertGroupDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/***
 * 专业组
 */
public interface ExpertGroupDao {

    // 得到专业信息
    ExpertGroupDO get(Integer id);

    List<ExpertGroupDO> list(Map<String, Object> map);

    /**
     * 获取签名地址
     * @param params
     * @return
     */
    String getSignUrl(Map<String,Object> params);

    int count(Map<String, Object> map);

    int save(ExpertGroupDO expertGroupDO);

    int update(ExpertGroupDO expertGroupDO);

    int updateByLoginAccount(ExpertGroupDO expertGroupDO);

    int updateExpertSignId(@Param("signId") long signId, @Param("taskId") String taskId, @Param("expertUid") long expertUid);

    int remove(Integer id);

    public int removeByLoginAccount(String loginAccount);

    public int delByLoginAccount(String loginAccount);

    /**
     * （得改或者删，连接的表不对）
     * 新增：获取指定条件下的不重复专业组名称列表
     * @param params 包含taskId、proType等过滤条件
     * @return 不重复的group_name列表
     */
    List<String> getDistinctGroupNames(Map<String, Object> params);

    /**
     * 按 userId + taskId + proType 物理删除记录（用于形审专家分组绑定的清理）
     */
    int deleteByUserIdAndTaskIdAndProType(@Param("userId") String userId, @Param("taskId") String taskId, @Param("proType") String proType);

    /**
     * 获取指定用户在指定 proType 下绑定的所有不重复 task_id
     */
    List<String> getDistinctTaskIdsByUserAndProType(@Param("userId") String userId, @Param("proType") String proType);
}
