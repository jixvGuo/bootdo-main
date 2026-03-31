package com.bootdo.cpe.controller;

import com.bootdo.common.utils.PageUtils;
import com.bootdo.common.utils.Query;
import com.bootdo.common.utils.R;
import com.bootdo.common.utils.ShiroUtils;
import com.bootdo.cpe.domain.QcExpertAvoidanceDO;
import com.bootdo.cpe.service.QcExpertAvoidanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QC专家回避管理控制层
 * 
 * @author system
 * @date 2026-03-27
 */
@Controller
@RequestMapping("/cpe/qcAvoidance")
public class QcExpertAvoidanceController {

    @Autowired
    private QcExpertAvoidanceService avoidanceService;

    /**
     * 查询回避记录列表
     */
    @ResponseBody
    @GetMapping("/list")
    public PageUtils list(@RequestParam Map<String, Object> params) {
        Query query = new Query(params);
        List<QcExpertAvoidanceDO> list = avoidanceService.list(query);
        int total = avoidanceService.count(query);
        return new PageUtils(list, total);
    }

    /**
     * 检查专家是否回避某项目
     */
    @ResponseBody
    @GetMapping("/check")
    public R checkAvoidance(@RequestParam String taskId,
                           @RequestParam Integer proId,
                           @RequestParam Integer expertUserId) {
        boolean isAvoided = avoidanceService.checkAvoidance(taskId, proId, expertUserId);
        return R.ok().put("isAvoided", isAvoided);
    }

    /**
     * 获取专家的回避项目ID列表
     */
    @ResponseBody
    @GetMapping("/avoidedProIds")
    public R getAvoidedProIds(@RequestParam String taskId,
                             @RequestParam Integer expertUserId) {
        List<Integer> proIds = avoidanceService.getAvoidedProIds(taskId, expertUserId);
        return R.ok().put("proIds", proIds);
    }

    /**
     * 手动回避
     */
    @ResponseBody
    @PostMapping("/manualAvoid")
    public R manualAvoid(@RequestParam String taskId,
                        @RequestParam Integer proId,
                        @RequestParam Integer expertUserId,
                        @RequestParam(required = false) String reason) {
        Long currentUserId = ShiroUtils.getUserId();
        boolean success = avoidanceService.manualAvoid(
            taskId, 
            proId, 
            expertUserId, 
            currentUserId.intValue(), 
            reason
        );
        
        if (success) {
            return R.ok("回避设置成功");
        } else {
            return R.error("回避设置失败，可能已存在回避记录");
        }
    }

    /**
     * 取消回避
     */
    @ResponseBody
    @PostMapping("/cancelAvoid")
    public R cancelAvoidance(@RequestParam String taskId,
                            @RequestParam Integer proId,
                            @RequestParam Integer expertUserId) {
        boolean success = avoidanceService.cancelAvoidance(taskId, proId, expertUserId);
        
        if (success) {
            return R.ok("已取消回避");
        } else {
            return R.error("取消回避失败");
        }
    }

    /**
     * 批量删除回避记录
     */
    @ResponseBody
    @PostMapping("/remove")
    public R remove(@RequestParam("ids[]") Integer[] ids) {
        if (avoidanceService.batchRemove(ids) > 0) {
            return R.ok();
        }
        return R.error();
    }
}
