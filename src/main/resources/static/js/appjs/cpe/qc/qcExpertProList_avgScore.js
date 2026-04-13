/**
 * 查看项目平均分
 */
function viewAverageScore(proId, taskId) {
    $.ajax({
        type: 'GET',
        url: '/qcScore/getAverageScore',
        data: { proId: proId, taskId: taskId },
        success: function (r) {
            if (r && r.code == 0) {
                if (r.averageScore != null) {
                    layer.alert('平均分：<strong style="font-size:20px;color:#e74c3c;">' + r.averageScore + '</strong> 分<br><span style="color:#999;font-size:12px;">（已排除回避专家，≥3个有效分时去最高最低分）</span>', {
                        title: '项目平均分',
                        icon: 1
                    });
                } else {
                    layer.msg(r.message || '暂无有效评分', { icon: 0 });
                }
            } else {
                layer.alert(r.msg || '查询失败');
            }
        },
        error: function () {
            layer.alert('查询失败，请稍后重试');
        }
    });
}
