// 原代码v2：基本列 + 打分日期列 + 查看形审结果按钮 + 提交/撤回打分功能
// 新代码v3：在v2基础上增加淘汰功能（淘汰按钮、淘汰名单tab、撤销淘汰）

var prefix = "/qcScore"
var eliminateTableLoaded = false;

$(function () {
    load();
    loadEliminateCount();
    // 初始化淘汰确认提交状态
    updateEliminateSubmitUI();
});

function load() {
    $('#exampleTable')
        .bootstrapTable(
            {
                method: 'get',
                url: prefix + "/get/expertProList",
                iconSize: 'outline',
                toolbar: '#exampleToolbar',
                striped: true,
                dataType: "json",
                pagination: true,
                singleSelect: false,
                pageSize: 100000,
                pageNumber: 1,
                showColumns: false,
                sidePagination: "server",
                // Bootstrap Tabl的回调函数
                queryParams: function (params) {
                    return {
                        limit: params.limit,
                        offset: params.offset,
                        taskId: $("#taskId").val(),
                        keyWord: $("input[name='keyWord']").val(),
                        avoidanceFilter: $("#avoidanceFilter").val(),
                        filterProCode: $("#filter_proCode").val(),
                        filterTopicName: $("#filter_topicName").val(),
                        filterGroupName: $("#filter_groupName").val(),
                        filterCompanyName: $("#filter_companyName").val(),
                        filterTopicType: $("#filter_topicType").val(),
                        filterProfessionalScope: $("#filter_professionalScope").val()
                    };
                },
                columns: [
                    {
                        // field: 'proId',
                        field: 'id',
                        title: '序号'
                    },
                    {
                        field: 'proCode',
                        title: '申报账号'
                    },
                    {
                        field: 'topicName',
                        title: '课题名称'
                    },
                    {
                        field: 'groupName',
                        title: '小组名称'
                    },
                    {
                        field: 'companyName',
                        title: '单位名称'
                    },
                    {
                        field: 'topicType',
                        title: '课题类型'
                    },
                    {
                        field: 'professionalScope',
                        title: '分类类型'
                    },
                    {
                        field: 'scoreDate',
                        title: '打分日期',
                        formatter: function (value) {
                            return value ? value : '';
                        }
                    },
                    // 在这里加一个打分日期
                    // {
                    //     field: 'applyStat',
                    //     title: '审核状态'
                    // },
                    // {
                    //     field: 'latestReviewResult',
                    //     title: '形审结论'
                    // },
                    {
                        title: '操作',
                        //field: 'id',
                        width: 320,
                        align: 'center',
                        formatter: function (value, row, index) {
                            var viewPro = '<a class="btn btn-warning btn-sm" href="#" title="查看项目" onclick="viewPro(\''
                                + row.proId
                                + '\')">查看项目</a> ';

                            var viewCheck = '<a class="btn btn-success btn-sm" href="#" title="查看形审结果" onclick="viewCheckResult(\''
                                + row.proId
                                + '\')">形式审查结果</a> ';

                            // 回避项目不显示评分按钮，但仍可淘汰
                            var score = '';
                            if (row.isAvoided) {
                                score = '<span class="label label-danger" style="font-size:12px;padding:4px 8px;border-radius:3px;"><i class="fa fa-ban"></i> 回避</span> ';
                            } else if (window.scoreIsOver == 1) {
                                score = '<a class="btn btn-default btn-sm" href="#" title="查看打分" onclick="specialistScore(\'' + row.proId + '\',\'' + row.taskId
                                    + '\',true)">查看打分</a> ';
                            } else {
                                score = '<a class="btn btn-success btn-sm" href="#" title="评分" onclick="specialistScore(\'' + row.proId + '\',\'' + row.taskId
                                    + '\')">评分</a> ';
                            }

                            // 淘汰按钮：确认提交后禁用
                            var eliminate = '';
                            if (window.eliminateIsOver == 1) {
                                eliminate = '<a class="btn btn-default btn-sm disabled" style="cursor:default;pointer-events:none;">淘汰已锁定</a>';
                            } else {
                                eliminate = '<a class="btn btn-danger btn-sm" href="#" title="淘汰" onclick="eliminateProject(\''
                                    + row.proId + '\',\'' + row.taskId
                                    + '\')">淘汰</a>';
                            }

                            var recommend = '<a class="btn btn-info btn-sm" href="#" title="主评意见" onclick="openRecommend(\''
                                + row.proId + '\',\'' + row.taskId + '\',\'' + (row.topicType || '') 
                                + '\')">主评意见</a> ';

                            return viewPro + viewCheck + score + '<br style="margin-bottom:4px;">' + recommend + eliminate;
                        }
                    }]
            });
}

// ==================== 淘汰功能 ====================

/**
 * 加载当前专家的淘汰数量，显示在tab徽章上
 */
function loadEliminateCount() {
    $.ajax({
        type: "GET",
        url: prefix + "/getEliminateCount",
        data: { taskId: $("#taskId").val() },
        success: function (data) {
            if (data.code == 0) {
                var count = data.count || 0;
                var badge = $("#eliminateCountBadge");
                if (count > 0) {
                    badge.text(count);
                    badge.show();
                } else {
                    badge.text('');
                    badge.hide();
                }
            }
        }
    });
}

/**
 * 淘汰项目 - 弹出理由弹窗
 */
function eliminateProject(proId, taskId) {
    // 先检查淘汰数量
    $.ajax({
        type: "GET",
        url: prefix + "/getEliminateCount",
        data: { taskId: taskId },
        async: false,
        success: function (data) {
            if (data.code == 0 && data.count >= data.max) {
                layer.msg('您最多只能淘汰' + data.max + '个项目，当前已淘汰' + data.count + '个', {
                    icon: 2,
                    time: 5000
                });
                return;
            }
            // 弹出淘汰理由弹窗
            layer.open({
                type: 1,
                title: '淘汰理由',
                area: ['450px', '280px'],
                shadeClose: false,
                content: '<div style="padding:20px;">'
                    + '<div style="margin-bottom:10px;color:#999;font-size:12px;">请填写淘汰理由（必填）：</div>'
                    + '<textarea id="eliminateReason" rows="5" class="form-control" placeholder="请输入淘汰理由..." style="resize:vertical;"></textarea>'
                    + '</div>',
                btn: ['确定淘汰', '取消'],
                yes: function (layerIndex) {
                    var reason = $.trim($("#eliminateReason").val());
                    if (!reason) {
                        layer.msg('淘汰理由不能为空', { icon: 2 });
                        return;
                    }
                    // 提交淘汰请求
                    $.ajax({
                        type: "POST",
                        url: prefix + "/eliminate",
                        data: {
                            proId: proId,
                            taskId: taskId,
                            reason: reason
                        },
                        success: function (res) {
                            if (res.code == 0) {
                                layer.close(layerIndex);
                                layer.msg('淘汰成功', { icon: 1 });
                                reLoad();
                                loadEliminateCount();
                                // 如果淘汰名单已加载，刷新它
                                if (eliminateTableLoaded) {
                                    $('#eliminateTable').bootstrapTable('refresh');
                                }
                            } else {
                                // 达到上限时提示5秒后自动消失
                                layer.msg(res.msg, { icon: 2, time: 5000 });
                            }
                        },
                        error: function () {
                            layer.msg('操作失败，请稍后重试', { icon: 2 });
                        }
                    });
                }
            });
        }
    });
}

/**
 * 切换Tab
 */
function switchTab(tab) {
    if (tab === 'eliminateList' && !eliminateTableLoaded) {
        loadEliminateTable();
        eliminateTableLoaded = true;
    }
}

/**
 * 加载淘汰名单表格
 */
function loadEliminateTable() {
    $('#eliminateTable')
        .bootstrapTable({
            method: 'get',
            url: prefix + "/getEliminateList",
            striped: true,
            dataType: "json",
            pagination: true,
            pageSize: 100000,
            pageNumber: 1,
            showColumns: false,
            sidePagination: "server",
            queryParams: function (params) {
                return {
                    limit: params.limit,
                    offset: params.offset,
                    taskId: $("#taskId").val()
                };
            },
            columns: [
                {
                    field: 'id',
                    title: '序号',
                    formatter: function (value, row, index) {
                        return index + 1;
                    }
                },
                {
                    field: 'proCode',
                    title: '申报账号'
                },
                {
                    field: 'topicName',
                    title: '课题名称'
                },
                // {
                //     field: 'groupName',
                //     title: '小组名称'
                // },
                // {
                //     field: 'companyName',
                //     title: '单位名称'
                // },
                {
                    field: 'reason',
                    title: '淘汰理由'
                },
                {
                    field: 'created',
                    title: '淘汰时间',
                    formatter: function (value) {
                        if (!value) return '';
                        var d = new Date(value);
                        return d.getFullYear() + '-' + ('0'+(d.getMonth()+1)).slice(-2) + '-' + ('0'+d.getDate()).slice(-2)
                            + ' ' + ('0'+d.getHours()).slice(-2) + ':' + ('0'+d.getMinutes()).slice(-2);
                    }
                },
                {
                    title: '操作',
                    field: 'id',
                    align: 'center',
                    formatter: function (value, row, index) {
                        if (window.eliminateIsOver == 1) {
                            return '<a class="btn btn-default btn-sm disabled" style="cursor:default;pointer-events:none;">已锁定</a>';
                        }
                        return '<a class="btn btn-warning btn-sm" href="#" onclick="cancelEliminate(\'' + row.id + '\')">撤销淘汰</a>';
                    }
                }
            ]
        });
}

/**
 * 撤销淘汰
 */
function cancelEliminate(id) {
    layer.confirm('确定要撤销该项目的淘汰吗？撤销后项目将恢复到淘汰前的状态。', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            type: "POST",
            url: prefix + "/cancelEliminate",
            data: { id: id },
            success: function (data) {
                if (data.code == 0) {
                    layer.msg('撤销淘汰成功', { icon: 1 });
                    $('#eliminateTable').bootstrapTable('refresh');
                    reLoad();
                    loadEliminateCount();
                } else {
                    layer.alert(data.msg);
                }
            },
            error: function () {
                layer.msg('操作失败，请稍后重试', { icon: 2 });
            }
        });
    });
}

// ==================== 原有功能 ====================

function specialistScore(proId, taskId, viewOnly) {
    var url = prefix + '/toScore?proId=' + proId + '&taskId=' + taskId;
    if (viewOnly) {
        url += '&viewOnly=1';
    }
    var index = layer.open({
        type: 2,
        title: viewOnly ? 'QC专家评分（只读）' : 'QC专家评分',
        maxmin: true,
        shadeClose: false,
        area: ['800px', '600px'],
        content: url
    });
    layer.full(index);
}

// function specialistOpinion(proId, taskId) {
//     var index = layer.open({
//         type: 2,
//         title: 'QC奖专家评价',
//         maxmin: true,
//         shadeClose: false,
//         area: ['800px', '600px'],
//         content: prefix + '/toOpinion?proId=' + proId + '&taskId=' + taskId
//     });
//     layer.full(index);
// }

function viewPro(proId) {
    var index = layer.open({
        type: 2,
        title: 'QC奖项目查看',
        maxmin: true,
        shadeClose: false,
        area: ['800px', '600px'],
        content: '/qcAward/view/apply?readonly=1&proId=' + proId
    });
    layer.full(index);
}

/**
 * 查看形式审查结果（参考科技奖 viewCheckResult）
 */
function viewCheckResult(proId) {
    // 原代码：content: '/qcProcess/toReivew?readonly=1&proId=' + proId
    // 问题：/qcProcess/toReivew 有 @RequiresPermissions 注解，专家角色85无权访问
    // 新代码：使用 /qcScore/viewCheckResult 端点，无权限注解
    layer.open({
        type: 2,
        title: 'QC奖形式审查结果',
        maxmin: true,
        shadeClose: false,
        area: ['800px', '520px'],
        content: prefix + '/viewCheckResult?proId=' + proId
    });
}

/**
 * 提交最终打分结果（参考科技奖 add 函数 → /specialist/scoreOver）
 */
function submitFinalScore() {
    layer.confirm('提交后不可再次进行分数修改,是否确定提交?', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            cache: true,
            type: "POST",
            url: prefix + "/submitScore",
            data: {
                taskId: $("#taskId").val()
            },
            async: false,
            error: function (request) {
                parent.layer.alert("Connection error");
            },
            success: function (data) {
                if (data.code == 0) {
                    parent.layer.msg("操作成功");
                    window.location.reload();
                } else {
                    parent.layer.alert(data.msg)
                }
            }
        });
    }, function () {
    });
}

/**
 * 撤回打分提交（参考科技奖 cancelSubmit 函数 → /specialist/scoreCancel）
 */
function cancelSubmitScore() {
    layer.confirm('是否撤销提交?', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            cache: true,
            type: "POST",
            url: prefix + "/cancelSubmitScore",
            data: {
                taskId: $("#taskId").val()
            },
            async: false,
            error: function (request) {
                parent.layer.alert("Connection error");
            },
            success: function (data) {
                if (data.code == 0) {
                    parent.layer.msg("操作成功");
                    window.location.reload();
                } else {
                    parent.layer.alert(data.msg)
                }
            }
        });
    }, function () {
    });
}

function reLoad() {
    $('#exampleTable').bootstrapTable('refresh');
}

// ==================== 筛选功能 ====================

/**
 * 应用筛选条件
 */
function applyFilters() {
    reLoad();
}

/**
 * 清空筛选条件
 */
function clearFilters() {
    $("#filter_proCode").val('');
    $("#filter_topicName").val('');
    $("#filter_groupName").val('');
    $("#filter_companyName").val('');
    $("#filter_topicType").val('');
    $("#filter_professionalScope").val('');
    $("#avoidanceFilter").val('');
    $("input[name='keyWord']").val('');
    reLoad();
}

/**
 * 初始化字典数据下拉框
 */
function initDictOptions() {
    // 课题类型 - 优先使用字典数据，否则使用硬编码
    var topicTypeSelect = $("#filter_topicType");
    if (window.projectTypes && window.projectTypes.length > 0) {
        window.projectTypes.forEach(function(item) {
            topicTypeSelect.append('<option value="' + item.value + '">' + item.name + '</option>');
        });
    } else {
        // 备选：硬编码选项（数据库存储的是英文编码）
        topicTypeSelect.append('<option value="solving">问题解决型</option>');
        topicTypeSelect.append('<option value="innovate">创新型</option>');
    }
    
    // 分类类型 - 优先使用字典数据，否则使用硬编码
    var professionalScopeSelect = $("#filter_professionalScope");
    if (window.classifications && window.classifications.length > 0) {
        window.classifications.forEach(function(item) {
            professionalScopeSelect.append('<option value="' + item.value + '">' + item.name + '</option>');
        });
    } else {
        // 备选：硬编码选项（数据库存储的是英文编码）
        professionalScopeSelect.append('<option value="design">勘察设计</option>');
        professionalScopeSelect.append('<option value="supervision">监理与管理</option>');
        professionalScopeSelect.append('<option value="construction">施工</option>');
        professionalScopeSelect.append('<option value="other">其它</option>');
    }
}

// 页面加载完成后初始化字典数据
$(document).ready(function() {
    initDictOptions();
});

// 注：回避功能由管理员在专业组管理页面操作，专家列表不显示回避按钮

// ==================== 淘汰确认提交功能 ====================

/**
 * 更新淘汰提交按钮的UI状态
 */
function updateEliminateSubmitUI() {
    if (window.eliminateIsOver == 1) {
        $("#btnSubmitEliminate").prop('disabled', true).removeClass('btn-primary').addClass('btn-default').html('<i class="fa fa-check"></i> 已提交');
        $("#eliminateSubmittedTip").show();
    }
}

/**
 * 确认提交淘汰名单
 */
function submitEliminate() {
    layer.confirm('提交后将无法再撤销淘汰名单中的项目，是否确认提交？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            type: "POST",
            url: prefix + "/submitEliminate",
            data: { taskId: $("#taskId").val() },
            success: function (data) {
                if (data.code == 0) {
                    layer.msg('淘汰名单确认提交成功', { icon: 1 });
                    window.eliminateIsOver = 1;
                    updateEliminateSubmitUI();
                    // 刷新淘汰表格，禁用撤销按钮
                    if (eliminateTableLoaded) {
                        $('#eliminateTable').bootstrapTable('refresh');
                    }
                } else {
                    layer.alert(data.msg);
                }
            },
            error: function () {
                layer.msg('操作失败，请稍后重试', { icon: 2 });
            }
        });
    });
}


/**
 * 导出淘汰名单为Excel（CSV格式，Excel可直接打开）
 */
function exportEliminateExcel() {
    var taskId = $("#taskId").val();
    $.ajax({
        type: "GET",
        url: prefix + "/getEliminateList",
        data: { taskId: taskId, limit: 100000, offset: 0 },
        success: function (data) {
            if (!data || !data.rows || data.rows.length === 0) {
                layer.msg('暂无淘汰数据', { icon: 2 });
                return;
            }
            var rows = data.rows;
            var sheetData = [['序号', '申报账号', '课题名称', '淘汰理由', '淘汰时间']];
            rows.forEach(function (row, index) {
                var created = '';
                if (row.created) {
                    var d = new Date(row.created);
                    created = d.getFullYear() + '-'
                        + ('0' + (d.getMonth() + 1)).slice(-2) + '-'
                        + ('0' + d.getDate()).slice(-2) + ' '
                        + ('0' + d.getHours()).slice(-2) + ':'
                        + ('0' + d.getMinutes()).slice(-2);
                }
                sheetData.push([
                    index + 1,
                    row.proCode || '',
                    row.topicName || '',
                    row.reason || '',
                    created
                ]);
            });
            var ws = XLSX.utils.aoa_to_sheet(sheetData);
            var wb = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(wb, ws, '淘汰名单');
            XLSX.writeFile(wb, '淘汰名单.xlsx');
        },
        error: function () {
            layer.msg('导出失败，请稍后重试', { icon: 2 });
        }
    });
}

// ==================== 主评意见功能 ====================

/**
 * 打开主评意见弹窗
 */
function openRecommend(proId, taskId, topicType) {
    var html = '<div style="padding:20px;">'
        + '<div style="margin-bottom:15px;">'
        + '<label style="display:block;margin-bottom:5px;font-weight:bold;">推荐意见等级</label>'
        + '<select id="rec_recommendLevel" class="form-control">'
        + '<option value="">请选择</option>'
        + '<option value="优秀">优秀</option>'
        + '<option value="良好">良好</option>'
        + '<option value="合格">合格</option>'
        + '<option value="不合格">不合格</option>'
        + '</select>'
        + '</div>'
        + '<div style="margin-bottom:15px;">'
        + '<label style="display:block;margin-bottom:5px;font-weight:bold;">评价意见</label>'
        + '<textarea id="rec_sumRecommend" class="form-control" rows="6" placeholder="请输入评价意见..."></textarea>'
        + '</div>'
        + '</div>';

    var layerIndex = layer.open({
        type: 1,
        title: '主评意见',
        area: ['500px', '400px'],
        content: html,
        btn: ['保存', '取消'],
        success: function () {
            // 加载已有的主评意见
            $.ajax({
                type: 'GET',
                url: prefix + '/getRecommend',
                data: { proId: proId, taskId: taskId, topicType: topicType },
                success: function (r) {
                    if (r && r.code == 0) {
                        if (r.recommendLevel) {
                            $('#rec_recommendLevel').val(r.recommendLevel);
                        }
                        if (r.sumRecommend) {
                            $('#rec_sumRecommend').val(r.sumRecommend);
                        }
                    }
                }
            });
        },
        yes: function (index) {
            var recommendLevel = $('#rec_recommendLevel').val();
            var sumRecommend = $('#rec_sumRecommend').val();
            $.ajax({
                type: 'POST',
                url: prefix + '/saveRecommend',
                data: {
                    proId: proId,
                    taskId: taskId,
                    topicType: topicType,
                    recommendLevel: recommendLevel,
                    sumRecommend: sumRecommend
                },
                success: function (r) {
                    if (r && r.code == 0) {
                        layer.msg('保存成功', { icon: 1 });
                        layer.close(index);
                    } else {
                        layer.alert(r.msg || '保存失败');
                    }
                },
                error: function () {
                    layer.alert('保存失败，请稍后重试');
                }
            });
        }
    });
}
