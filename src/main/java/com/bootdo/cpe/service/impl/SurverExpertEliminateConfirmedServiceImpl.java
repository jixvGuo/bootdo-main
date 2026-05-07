package com.bootdo.cpe.service.impl;

import com.bootdo.cpe.dao.SurverExpertEliminateConfirmedDao;
import com.bootdo.cpe.domain.SurverExpertEliminateConfirmedDO;
import com.bootdo.cpe.service.SurverExpertEliminateConfirmedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SurverExpertEliminateConfirmedServiceImpl implements SurverExpertEliminateConfirmedService {

    @Autowired
    private SurverExpertEliminateConfirmedDao dao;

    @Override
    public SurverExpertEliminateConfirmedDO get(Long id) { return dao.get(id); }

    @Override
    public List<SurverExpertEliminateConfirmedDO> list(Map<String, Object> map) { return dao.list(map); }

    @Override
    public int count(Map<String, Object> map) { return dao.count(map); }

    @Override
    public int save(SurverExpertEliminateConfirmedDO d) { return dao.save(d); }

    @Override
    public int remove(Long id) { return dao.remove(id); }

    @Override
    public int copyFromActiveByExpertAndTask(Long expertUid, String taskId) {
        return dao.copyFromActiveByExpertAndTask(expertUid, taskId);
    }

    @Override
    public int deleteByExpertAndTask(Long expertUid, String taskId) {
        return dao.deleteByExpertAndTask(expertUid, taskId);
    }
}
