package com.bootdo.cpe.service.impl;

import com.bootdo.common.utils.StringUtils;
import com.bootdo.cpe.dao.ExpertGroupDao;
import com.bootdo.cpe.domain.ExpertGroupDO;
import com.bootdo.cpe.service.ExpertGroupService;
import com.bootdo.cpe.service.QcExpertAvoidanceService;
import com.bootdo.system.dao.UserDao;
import com.bootdo.system.domain.UserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpertGroupServiceImpl implements ExpertGroupService {


    @Autowired
    private ExpertGroupDao expertGroupDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private QcExpertAvoidanceService avoidanceService;

    @Override
    public ExpertGroupDO get(Integer id) {
       return expertGroupDao.get(id);

    }

    @Override
    public List<ExpertGroupDO> list(Map<String, Object> map) {
        return expertGroupDao.list(map);
    }

    @Override
    public String getSignUrl(Map<String, Object> params) {
        return expertGroupDao.getSignUrl(params);
    }

    @Override
    public int count(Map<String, Object> map) {
        return expertGroupDao.count(map);
    }

    @Override
    public int save(ExpertGroupDO expertGroupDO) {
        Map<String,Object> params = new HashMap<>();
        String loginAccount = expertGroupDO.getLoginAccount();
        if(StringUtils.isBlank(loginAccount)) {
            return 0;
        }
        loginAccount = loginAccount.trim();
        params.put("loginAccount", loginAccount);
        List<ExpertGroupDO> list = expertGroupDao.list(params);
        int result = 0;
        if(list.size() > 0) {
            Map<String,Object> userParams = new HashMap<>();
            userParams.put("name", expertGroupDO.getExpertName());
            List<UserDO> userList = userDao.list(userParams);
            if(userList.size() > 0) {
                return -100;
            }

            UserDO userDO = new UserDO();
            userDO.setUserId(Long.parseLong(expertGroupDO.getUserId()));
            userDO.setName(expertGroupDO.getExpertName());
            userDao.update(userDO);

            ExpertGroupDO existsExpert = list.get(0);
            if(loginAccount.equals(existsExpert.getLoginAccount())) {
                expertGroupDO.setId(existsExpert.getId());
                result = expertGroupDao.update(expertGroupDO);
            }
        } else {
            result = expertGroupDao.save(expertGroupDO);
        }
        
        return result;
    }

    @Override
    public int update(ExpertGroupDO expertGroupDO) {
        return expertGroupDao.update(expertGroupDO);
    }

    @Override
    public int updateExpertSignId(long signId, String taskId, long expertUid) {
        return expertGroupDao.updateExpertSignId(signId, taskId, expertUid);
    }

    @Override
    public int remove(Integer id) {
        return expertGroupDao.remove(id);
    }

    @Override
    public int removeByLoginAccount(String loginAccount) {
        return expertGroupDao.removeByLoginAccount(loginAccount);
    }

    /**
     * 逻辑删除专家账号
     *
     * @param loginAccount
     * @return
     */
    @Override
    public int delByLoginAccount(String loginAccount) {
        return expertGroupDao.delByLoginAccount(loginAccount);
    }

    /**
     * 新增：获取指定条件下的不重复专业组名称列表(得删或者得改)
     */
    @Override
    public List<String> getDistinctGroupNames(Map<String, Object> params) {
        return expertGroupDao.getDistinctGroupNames(params);
    }

    @Override
    public int deleteByUserIdAndTaskIdAndProType(String userId, String taskId, String proType) {
        return expertGroupDao.deleteByUserIdAndTaskIdAndProType(userId, taskId, proType);
    }

    /**
     * 直接将专家分组数据保存到数据库，不做任何业务逻辑校验,用于形审专家绑定功能
     * @param expertGroupDO
     * @return
     */
    @Override
    public int directSave(ExpertGroupDO expertGroupDO) {
        return expertGroupDao.save(expertGroupDO);
    }

    @Override
    public List<String> getDistinctTaskIdsByUserAndProType(String userId, String proType) {
        return expertGroupDao.getDistinctTaskIdsByUserAndProType(userId, proType);
    }
}
