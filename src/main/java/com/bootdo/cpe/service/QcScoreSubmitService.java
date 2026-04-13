package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.QcScoreSubmitDO;

import java.util.List;
import java.util.Map;

/**
 * QC专家打分提交快照服务
 */
public interface QcScoreSubmitService {

    QcScoreSubmitDO get(Integer id);

    List<QcScoreSubmitDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    /**
     * 幂等写入提交快照（INSERT IGNORE，重复提交不报错）
     */
    int saveIgnore(QcScoreSubmitDO submit);

    int remove(Integer id);
}
