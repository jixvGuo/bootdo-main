package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.SurverExpertReviewOpinionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 勘察奖-专家审核意见/主评意见
 */
@Mapper
public interface SurverExpertReviewOpinionDao {

    SurverExpertReviewOpinionDO getByUnique(@Param("taskId") String taskId,
                                            @Param("proId") Integer proId,
                                            @Param("expertUid") Long expertUid);

    List<SurverExpertReviewOpinionDO> listByTaskAndExpert(@Param("taskId") String taskId,
                                                        @Param("expertUid") Long expertUid);

    int insert(SurverExpertReviewOpinionDO row);

    int updateAudit(@Param("id") Long id, @Param("auditOpinion") String auditOpinion);

    int updateMain(@Param("id") Long id,
                   @Param("mainReviewText") String mainReviewText,
                   @Param("mainReviewSubmitted") Integer mainReviewSubmitted);

    /**
     * 与 listProInfo 专家可见范围一致：专家绑定 surver_pro_group 且项目在该任务下
     */
    int countExpertProAssigned(@Param("taskId") String taskId,
                               @Param("expertUid") Long expertUid,
                               @Param("proId") Integer proId);
}
