var prefix = "/surverScore"
// 勘察奖专家打分页面cxq
// Phase C 新增：勘察奖专家侧"淘汰评级"接口前缀
var SURVER_ELIM_EXPERT_PREFIX = '/cpe/suverProcess/eliminate/expert';
// 全局缓存：当前专家在此任务下的"已评等级 / 回避情况 / 是否已确认提交"
var SURVER_EXPERT_GRADE_MAP = {};   // proId -> 'A|B|C|D'
var SURVER_EXPERT_REMARK_MAP = {};  // proId -> '评级理由'
var SURVER_EXPERT_AVOID_SET = {};   // proId -> true (已回避)
var SURVER_EXPERT_LOCKED   = false; // 已确认提交后锁定

$(function () {
    // [DEBUG] 打印当前页面使用的 taskId，方便排查专家/管理员 taskId 不一致问题
    console.log('[DEBUG-score] 专家打分页 taskId=' + $("#taskId").val());
    // 先拉取等级/回避/锁定状态，再加载表格（保证渲染时能正确填充下拉/按钮）
    loadExpertGradeContext(function() {
        load();
        renderExpertSubmitToolbar();
    });
});

/** 拉取当前专家在该任务下的评级/回避/锁定状态，并渲染顶部按钮 */
function loadExpertGradeContext(cb) {
    var taskId = $("#taskId").val();
    if (!taskId) { if (cb) cb(); return; }
    $.ajax({
        type: 'GET',
        url: SURVER_ELIM_EXPERT_PREFIX + '/listMyGrades',
        data: { taskId: taskId },
        success: function(r) {
            if (r && r.code === 0) {
                SURVER_EXPERT_GRADE_MAP = r.grades || {};
                SURVER_EXPERT_REMARK_MAP = r.remarks || {};
                SURVER_EXPERT_AVOID_SET = {};
                (r.avoidances || []).forEach(function(pid) { SURVER_EXPERT_AVOID_SET[pid] = true; });
                SURVER_EXPERT_LOCKED   = (r.eliminateOver == 1);
                window._surverExpertStat = {
                    gradedCount: r.gradedCount || 0,
                    avoidedCount: r.avoidedCount || 0,
                    totalCount: r.totalCount || 0,
                    eliminateOver: SURVER_EXPERT_LOCKED ? 1 : 0
                };
            }
            if (cb) cb();
        },
        error: function() { if (cb) cb(); }
    });
}

function load() {
    $('#exampleTable')
        .bootstrapTable(
            {
                method: 'get', // 服务器数据的请求方式 get or post
                url: "/surverPro/get/proList", // 服务器数据的加载地址
                //	showRefresh : true,
                //	showToggle : true,
                //	showColumns : true,
                iconSize: 'outline',
                toolbar: '#exampleToolbar',
                striped: true, // 设置为true会有隔行变色效果
                dataType: "json", // 服务器返回的数据类型
                pagination: true, // 设置为true会在底部显示分页条
                // queryParamsType : "limit",
                // //设置为limit则会发送符合RESTFull格式的参数
                singleSelect: false, // 设置为true将禁止多选
                // contentType : "application/x-www-form-urlencoded",
                // //发送到服务器的数据编码类型
                pageSize: 10, // 如果设置了分页，每页数据条数
                pageNumber: 1, // 如果设置了分布，首页页码
                //search : true, // 是否显示搜索框
                showColumns: false, // 是否显示内容下拉框（选择显示的列）
                sidePagination: "server", // 设置在哪里进行分页，可选值为"client" 或者 "server"
                queryParams: function (params) {
                    // 基础参数
                    var qp = {
                        //说明：传入后台的参数包括offset开始索引，limit步长，sort排序列，order：desc或者,以及所有列的键值对
                        limit: params.limit,
                        offset: params.offset,
                        proSubType: $("#proSubType").val(),
                        taskId: $("#taskId").val(),
                    };
                    // 新增：高级筛选参数（与 surverProList.js 保持一致）
                    var fmap = {
                        filterProName:           $("#filterProName").val(),
                        filterApplyCompany:      $("#filterApplyCompany").val(),
                        filterMajor:             $("#filterMajor").val(),
                        filterDeclareAccount:    $("#filterDeclareAccount").val(),
                        filterQcGroupName:       $("#filterQcGroupName").val(),
                        filterExpertGroupName:   $("#filterExpertGroupName").val(),
                        filterEliminated:        $("#filterEliminated").val(),
                        filterProStat:           $("#filterProStat").val()
                    };
                    Object.keys(fmap).forEach(function(k) {
                        var v = fmap[k];
                        if (v !== undefined && v !== null && (v + "").length > 0) {
                            qp[k] = (v + "").trim();
                        }
                    });
                    return qp;
                },
                // //请求服务器数据时，你可以通过重写参数的方式添加一些额外的参数，例如 toolbar 中的参数 如果
                // queryParamsType = 'limit' ,返回参数必须包含
                // limit, offset, search, sort, order 否则, 需要包含:
                // pageSize, pageNumber, searchText, sortName,
                // sortOrder.
                // 返回false将会终止请求
                columns: [

                    {
                        field: 'id',
                        title: '序号'
                    },
                    {
                        field: 'proCode',

                        title: '项目编号',

                    },
                    {
                        field: 'proSubTypeStr',
                        title: '项目类别'
                    },
                    {
                        field: 'proName',
                        title: '项目名称'
                    },
                    {
                        field: 'applyCompany',
                        title: '申报单位'
                    },
                    {
                        field: 'major',
                        title: '专业'
                    },

                    // === 修复：申报账号 字段应为 declareAccount（与 surverProList.js 一致），
                    //          原 applyAccount 在数据模型里是"申报联系方式"
                    // {
                    //     field: 'applyAccount',
                    //     title: '申报账号'
                    // },
                    {
                        field: 'declareAccount',
                        title: '申报账号'
                    },
                    // 新增：申报联系方式列
                    {
                        field: 'applyAccount',
                        title: '申报联系方式'
                    },
                    // 新增：分组列（纯展示，不显示选择按钮）
                    {
                        field: 'qcGroupName',
                        title: '分组',
                        formatter: function(value, row, index) {
                            return value || '未分组';
                        }
                    },
                    // 新增：专家分组列（纯展示，不显示选择按钮）
                    {
                        field: 'expertGroupName',
                        title: '专家分组',
                        formatter: function(value, row, index) {
                            return value || '未分组';
                        }
                    },
                    // 新增：淘汰状态列
                    {
                        field: 'eliminated',
                        title: '淘汰状态',
                        formatter: function(value, row, index) {
                            if (value == 1 || value === '1') {
                                return '<span style="background:#d9534f;color:#fff;border-radius:3px;padding:2px 8px;font-size:12px;">已淘汰</span>';
                            }
                            return '<span style="color:#999;">未淘汰</span>';
                        }
                    },
                    {
                        field: 'applyStat',
                        title: '状态'
                    },
                    // 新增形审结果列
                    {
                        field: 'latestReviewResult',
                        title: '形审结果',
                        formatter: function (value, row, index) {
                            var text = value;
                            if (!text || text === '') {
                                if (!row.checkStartTime) {
                                    text = '形审未开始';
                                } else {
                                    text = '暂无形审结果';
                                }
                            }
                            return '<a href="javascript:void(0)" onclick="showReviewRecordList(' + row.proId + ',\'' + (row.proSubType || '') + '\')">' + text + '</a>';
                        }
                    },

                    // Phase C 原代码：淘汰等级（内联下拉）- 已注释
                    // {
                    //     field: '_elimGrade',
                    //     title: '淘汰等级',
                    //     align: 'center',
                    //     formatter: function (value, row, index) {
                    //         if (row.proSubType === 'consulting') {
                    //             return '<span style="color:#bbb;font-size:12px;">不参与</span>';
                    //         }
                    //         if (SURVER_EXPERT_AVOID_SET[row.proId]) {
                    //             return '<span style="color:#d9534f;font-size:12px;">已回避(无需评级)</span>';
                    //         }
                    //         var current = SURVER_EXPERT_GRADE_MAP[row.proId] || '';
                    //         var disabled = SURVER_EXPERT_LOCKED ? 'disabled' : '';
                    //         var opts = ['', 'A', 'B', 'C', 'D'].map(function(g) {
                    //             var sel = (g === current) ? 'selected' : '';
                    //             var label = g === '' ? '请选择' : g;
                    //             return '<option value="' + g + '" ' + sel + '>' + label + '</option>';
                    //         }).join('');
                    //         return '<select class="form-control input-sm" style="width:90px;display:inline-block;" '
                    //             + disabled
                    //             + ' onchange="onSurverGradeChange(this, ' + row.proId + ', \'' + row.proSubType + '\', \''
                    //             + (row.proCode || '') + '\')">'
                    //             + opts
                    //             + '</select>'
                    //             + (current ? ' <span class="label label-success" style="margin-left:4px;">' + current + '</span>' : '');
                    //     }
                    // },
                    // Phase C 改造：淘汰按钮 + 弹窗（含评级下拉 + 评级理由）
                    {
                        field: '_elimGrade',
                        title: '淘汰',
                        align: 'center',
                        formatter: function (value, row, index) {
                            if (row.proSubType === 'consulting') {
                                return '<span style="color:#bbb;font-size:12px;">不参与</span>';
                            }
                            if (SURVER_EXPERT_AVOID_SET[row.proId]) {
                                return '<span style="color:#d9534f;font-size:12px;">已回避</span>';
                            }
                            var current = SURVER_EXPERT_GRADE_MAP[row.proId] || '';
                            var btnLabel = current ? '已评级(' + current + ')' : '淘汰';
                            var btnClass = current ? 'btn-success' : 'btn-primary';
                            var disabled = SURVER_EXPERT_LOCKED ? 'disabled' : '';
                            return '<button class="btn btn-xs ' + btnClass + '" ' + disabled
                                + ' onclick="openSurverGradeDialog(' + row.proId + ', \'' + (row.proSubType || '')
                                + '\', \'' + (row.proCode || '') + '\')">'
                                + btnLabel + '</button>';
                        }
                    },
                    // Phase C 新增：回避（手动）
                    // {
                    //     field: '_elimAvoid',
                    //     title: '回避',
                    //     align: 'center',
                    //     formatter: function (value, row, index) {
                    //         if (row.proSubType === 'consulting') {
                    //             return '<span style="color:#bbb;">-</span>';
                    //         }
                    //         var locked = SURVER_EXPERT_LOCKED ? 'disabled' : '';
                    //         if (SURVER_EXPERT_AVOID_SET[row.proId]) {
                    //             return '<button class="btn btn-default btn-xs" ' + locked + ' onclick="onSurverCancelAvoid(' + row.proId + ')">取消回避</button>';
                    //         }
                    //         return '<button class="btn btn-warning btn-xs" ' + locked + ' onclick="onSurverAvoid(' + row.proId + ')">回避</button>';
                    //     }
                    // },
                    {
                        title: '操作',
                        field: 'id',
                        align: 'center',
                        formatter: function (value, row, index) {
                            // var e = '<a class="btn btn-primary btn-sm " href="#" mce_href="#" title="评分标准" onclick="openstard()">评分标准</a> ';
                            var d = '<a class="btn btn-warning btn-sm  " href="#" title=" "  mce_href="#" onclick="onwatch (\''
                            	+ row.proId
                            	+ '\',\''
                            	+ row.proSubType
                            	+ '\')"">查看项目</a> ';
                                

                            // 勘察奖专家评分
                            // var f = '<a class="btn btn-success btn-sm" href="#" title="评分"  mce_href="#" onclick="onscore(\''
                            //      + row.proId
                            //      + '\',\''
                            //      + row.taskId
                            //      + '\',\''
                            //      + row.major
                            //      + '\')">评分</a> ';
                            
                            // var f = '<a class="btn btn-success btn-sm" href="#" title="评分">评分</a>';
                            

                            // 勘察奖：回避项目显示回避标记，不显示评分按钮（参考 QC 奖 qcExpertProList.js）
                            var f = '';
                            if (SURVER_EXPERT_AVOID_SET[row.proId]) {
                                f = '<span class="label label-danger" style="font-size:12px;padding:4px 8px;border-radius:3px;"><i class="fa fa-ban"></i> 回避</span> ';
                            } else {
                                f = '<a class="btn btn-success btn-sm" href="#" title="评分">评分</a> ';
                            }
                            return d + f;
                            // return e + d + f;
                        }
                    }],

            });
    // === 修复：原逻辑会在 isViewProCode==='false' 时隐藏"项目编号"列，
    //          导致专家打分页看不到 proCode；按需求保留显示，注释隐藏逻辑。
    // if($("#isViewProCode").val() == 'false') {
    //    $('#exampleTable').bootstrapTable('hideColumn', 'proCode');
    // }
}


function openstard() {
    page('/surverScore/standardTable', '评分标准', 20220505);
}

/***
 * 提交最终的打分结果
 */
function commitLast() {
    var result = {"scoreType":"personal_score"};
    var proId = $("#proId").val();
    result["proId"] = parseInt(proId);

    var centerData = {};
    console.log(JSON.stringify(result) + " ===qq== ");
    $.ajax({
        cache: true,
        type: "POST",
        url: "/specialist/score",
        data: result,// 你的formid
        async: false,
        error: function (request) {
            parent.parent.layer.alert("Connection error");
        },
        success: function (data) {
            if (data.code == 0) {
                parent.parent.layer.msg("操作成功");

            } else {
                parent.parent.layer.alert(data.msg)
            }

        }
    });



}
/****
 * 查看项目
 *  content: '/chengguo_team/apply_team/edit?proId=' + id + "&taskId=" + taskId // iframe的url
 */
function onwatch(proId, proType) {
    let url = '';
    let title = '';
    if (proType == 'design') {
        url = '/surverApply/toApplyDesign?readonly=1&proId=' + proId;
        title = '勘察设计奖';
    } else if (proType == 'software') {
        url = '/surverSoftwareApply/toApplySoftware?readonly=1&proId=' + proId;
        title = '计算机软件奖';
    } else if (proType == 'consulting') {
        url = '/surverConsultingApply/toApply?readonly=1&proId=' + proId;
        title = '咨询奖';
    } else if (proType == 'standard') {
        url = '/surverStandardApply/toApply?readonly=1&proId=' + proId;
        title = '标准设计奖';
    } else if (proType == 'contribution') {
        url = '/surverBaseExlentApply/toApply?readonly=1&proId=' + proId;
        title = '优秀勘察奖';
    }
    page(url, title, 20220505);
}


/***
 * 打印
 * @param id
 */
function onscore(proId, taskId, major) {
    page('/surverScore/toScore?proId=' + proId + '&taskId=' + taskId + '&major=' + major, '专业组评审打分', 20220505);
}

function add() {
	layer.confirm('提交后不可再次进行分数修改,是否确定提交?', {
		btn: ['确定','取消'] //按钮
	}, function(){
		$.ajax({
			cache: true,
			type: "POST",
			url: "/specialist/scoreOver",
			data: {
				scoreType:$("input[name='scoreType']").val()
			},// 你的formid
			async: false,
			error: function (request) {
				parent.parent.layer.alert("Connection error");
			},
			success: function (data) {
				if (data.code == 0) {
					parent.parent.layer.msg("操作成功");
					window.location.reload();
				} else {
					parent.parent.layer.alert(data.msg)
				}

			}
		});
	}, function(){
	});
}

function cancelSubmit(){
	layer.confirm('是否撤销提交?', {
		btn: ['确定','取消'] //按钮
	}, function(){
		$.ajax({
			cache: true,
			type: "POST",
			url: "/specialist/scoreCancel",
			data: {
				scoreType:$("input[name='scoreType']").val()
			},// 你的formid
			async: false,
			error: function (request) {
				parent.parent.layer.alert("Connection error");
			},
			success: function (data) {
				if (data.code == 0) {
					parent.parent.layer.msg("操作成功");
					window.location.reload();
				} else {
					parent.parent.layer.alert(data.msg)
				}
			}
		});
	}, function(){
	});
}

function reLoad() {
    $('#exampleTable').bootstrapTable('refresh');
    // === 修复：刷新时同样不再隐藏"项目编号"列
    // if($("#isViewProCode").val() == 'false') {
    //    $('#exampleTable').bootstrapTable('hideColumn', 'proCode');
    // }
}

// =====================================================================
// Phase C 新增：勘察奖专家"淘汰评级"操作（等级 / 回避 / 提交 / 撤回）
// 后端接口前缀：/cpe/suverProcess/eliminate/expert/*
// =====================================================================

/** 表格头部下方渲染"提交淘汰评级名单 / 撤回提交 / 已评统计"按钮组 */
function renderExpertSubmitToolbar() {
    var stat = window._surverExpertStat || { gradedCount: 0, avoidedCount: 0, eliminateOver: 0 };
    var locked = SURVER_EXPERT_LOCKED;
    var totalCount = stat.totalCount || 0;
    var remaining = totalCount - stat.gradedCount - stat.avoidedCount;
    if (remaining < 0) remaining = 0;
    var html = '<div id="surverElimExpertToolbar" style="margin:8px 0;padding:8px;border:1px solid #e5e5e5;border-radius:4px;background:#f9fafc;">'
        + '<span style="margin-right:16px;color:#333;"><b>淘汰评级状态：</b></span>'
        + '<span style="margin-right:16px;">共 <b>' + totalCount + '</b> 项</span>'
        + '<span style="margin-right:16px;">已评 <b style="color:#5cb85c;">' + stat.gradedCount + '</b> 项</span>'
        + '<span style="margin-right:16px;">已回避 <b style="color:#d9534f;">' + stat.avoidedCount + '</b> 项</span>'
        + (remaining > 0 ? '<span style="margin-right:16px;">剩余 <b style="color:#f0ad4e;">' + remaining + '</b> 项未处理</span>' : '')
        + '<span style="margin-right:16px;">';
    if (!locked) {
        html += '<button class="btn btn-danger btn-sm" style="font-size:13px;padding:4px 10px;vertical-align:middle;" onclick="onSurverConfirmSubmitElim()">'
            + '<i class="fa fa-check"></i> 确认提交淘汰评级名单</button>';
    } else {
        html += '<span style="display:inline-block;padding:4px 10px;font-size:13px;border-radius:3px;background:#5cb85c;color:#fff;vertical-align:middle;">'
            + '<i class="fa fa-lock"></i> 已确认提交（不可撤回）</span>';
    }
    html += '</div>';
    var $existing = $('#surverElimExpertToolbar');
    if ($existing.length) { $existing.replaceWith(html); }
    else { $('#exampleTable').before(html); }
}

/** 原代码：等级下拉切换 - 已注释 */
// function onSurverGradeChange(sel, proId, proSubType, proCode) {
//     var grade = $(sel).val();
//     if (SURVER_EXPERT_LOCKED) { layer.msg('已确认提交，无法再修改', { icon: 2 }); return; }
//     var taskId = $("#taskId").val();
//     $.ajax({
//         type: 'POST', url: SURVER_ELIM_EXPERT_PREFIX + '/saveGrade',
//         data: { taskId: taskId, proId: proId, proSubType: proSubType, grade: grade, proCode: proCode },
//         success: function(r) {
//             if (r.code === 0) {
//                 if (grade) { layer.msg('已保存：' + grade, { icon: 1, time: 800 }); SURVER_EXPERT_GRADE_MAP[proId] = grade; }
//                 else { layer.msg('已清除评级', { icon: 1, time: 800 }); delete SURVER_EXPERT_GRADE_MAP[proId]; }
//                 loadExpertGradeContext(function() { renderExpertSubmitToolbar(); $('#exampleTable').bootstrapTable('refresh'); });
//             } else { layer.msg(r.msg || '保存失败', { icon: 2 }); }
//         },
//         error: function() { layer.msg('请求失败', { icon: 2 }); }
//     });
// }

/** 新增：打开淘汰评级弹窗（含评级下拉 + 评级理由） */
function openSurverGradeDialog(proId, proSubType, proCode) {
    if (SURVER_EXPERT_LOCKED) { layer.msg('已确认提交，无法再修改', { icon: 2 }); return; }
    var currentGrade = SURVER_EXPERT_GRADE_MAP[proId] || '';
    var currentRemark = SURVER_EXPERT_REMARK_MAP[proId] || '';
    var maxLen = 2000;
    var gradeOpts = ['', 'A', 'B', 'C', 'D'].map(function(g) {
        var sel = (g === currentGrade) ? 'selected' : '';
        var label = g === '' ? '请选择评级' : g;
        return '<option value="' + g + '" ' + sel + '>' + label + '</option>';
    }).join('');
    var escapedRemark = (currentRemark || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    var dlgHtml = '<div style="padding:20px 30px;">'
        + '<div style="margin-bottom:16px;"><span style="color:red;">*</span> '
        + '<label style="font-weight:bold;margin-right:10px;">专家评级：</label>'
        + '<select id="_surverGradeDlgSelect" class="form-control" style="display:inline-block;width:220px;">' + gradeOpts + '</select>'
        + '</div>'
        + '<div style="margin-bottom:8px;">'
        + '<label style="font-weight:bold;">评级理由：</label>'
        + '<span id="_surverGradeDlgCount" style="float:right;color:#999;font-size:12px;">' + (currentRemark.length) + '/' + maxLen + '</span>'
        + '</div>'
        + '<textarea id="_surverGradeDlgRemark" class="form-control" rows="8" maxlength="' + maxLen + '" '
        + 'placeholder="请输入..." style="resize:vertical;">' + escapedRemark + '</textarea>'
        + '</div>';
    layer.open({
        type: 1, title: '淘汰', area: ['480px', '420px'], shadeClose: false,
        content: dlgHtml, btn: ['确定', '取消'],
        success: function() {
            $('#_surverGradeDlgRemark').on('input', function() {
                var len = $(this).val().length;
                $('#_surverGradeDlgCount').text(len + '/' + maxLen);
                $('#_surverGradeDlgCount').css('color', len > maxLen ? '#d9534f' : '#999');
            });
        },
        yes: function(idx) {
            var grade = $('#_surverGradeDlgSelect').val();
            var remark = $('#_surverGradeDlgRemark').val() || '';
            if (!grade) { layer.msg('请选择评级', { icon: 2 }); return; }
            if (remark.length > maxLen) { layer.msg('评级理由超过' + maxLen + '字符限制', { icon: 2 }); return; }
            var taskId = $("#taskId").val();
            console.log('[DEBUG-score] saveGrade taskId=' + taskId + ', proId=' + proId + ', grade=' + grade);
            $.ajax({
                type: 'POST',
                url: SURVER_ELIM_EXPERT_PREFIX + '/saveGrade',
                data: { taskId: taskId, proId: proId, proSubType: proSubType, grade: grade, proCode: proCode, remark: remark },
                success: function(r) {
                    if (r.code === 0) {
                        layer.close(idx);
                        layer.msg('已保存：' + grade, { icon: 1, time: 800 });
                        SURVER_EXPERT_GRADE_MAP[proId] = grade;
                        SURVER_EXPERT_REMARK_MAP[proId] = remark;
                        loadExpertGradeContext(function() {
                            renderExpertSubmitToolbar();
                            $('#exampleTable').bootstrapTable('refresh');
                        });
                    } else { layer.msg(r.msg || '保存失败', { icon: 2 }); }
                },
                error: function() { layer.msg('请求失败', { icon: 2 }); }
            });
        }
    });
}

function onSurverAvoid(proId) {
    if (SURVER_EXPERT_LOCKED) { layer.msg('已锁定，无法操作', { icon: 2 }); return; }
    layer.prompt({ title: '请输入回避原因（可空）', formType: 2 }, function(reason, idx) {
        layer.close(idx);
        var taskId = $("#taskId").val();
        $.ajax({
            type: 'POST',
            url: SURVER_ELIM_EXPERT_PREFIX + '/avoid',
            data: { taskId: taskId, proId: proId, reason: reason || '' },
            success: function(r) {
                if (r.code === 0) {
                    layer.msg(r.msg || '已回避', { icon: 1 });
                    loadExpertGradeContext(function() {
                        renderExpertSubmitToolbar();
                        $('#exampleTable').bootstrapTable('refresh');
                    });
                } else { layer.msg(r.msg || '操作失败', { icon: 2 }); }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

function onSurverCancelAvoid(proId) {
    if (SURVER_EXPERT_LOCKED) { layer.msg('已锁定，无法操作', { icon: 2 }); return; }
    var taskId = $("#taskId").val();
    layer.confirm('确认取消该项目的回避？', { btn: ['确定', '取消'] }, function(idx) {
        layer.close(idx);
        $.ajax({
            type: 'POST',
            url: SURVER_ELIM_EXPERT_PREFIX + '/cancelAvoid',
            data: { taskId: taskId, proId: proId },
            success: function(r) {
                if (r.code === 0) {
                    layer.msg(r.msg || '已取消回避', { icon: 1 });
                    loadExpertGradeContext(function() {
                        renderExpertSubmitToolbar();
                        $('#exampleTable').bootstrapTable('refresh');
                    });
                } else { layer.msg(r.msg || '操作失败', { icon: 2 }); }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

function onSurverConfirmSubmitElim() {
    layer.confirm('确认提交淘汰评级名单？提交后不可撤回，请确保所有项目已评级或已回避。', { btn: ['确定', '取消'] }, function(idx) {
        layer.close(idx);
        var taskId = $("#taskId").val();
        $.ajax({
            type: 'POST',
            url: SURVER_ELIM_EXPERT_PREFIX + '/confirmSubmit',
            data: { taskId: taskId },
            success: function(r) {
                if (r.code === 0) {
                    layer.msg(r.msg || '已提交', { icon: 1 });
                    loadExpertGradeContext(function() {
                        renderExpertSubmitToolbar();
                        $('#exampleTable').bootstrapTable('refresh');
                    });
                } else { layer.msg(r.msg || '提交失败', { icon: 2 }); }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

function onSurverCancelSubmitElim() {
    layer.confirm('确认撤回淘汰评级提交？撤回后可重新修改评级。', { btn: ['确定', '取消'] }, function(idx) {
        layer.close(idx);
        var taskId = $("#taskId").val();
        $.ajax({
            type: 'POST',
            url: SURVER_ELIM_EXPERT_PREFIX + '/cancelConfirmSubmit',
            data: { taskId: taskId },
            success: function(r) {
                if (r.code === 0) {
                    layer.msg(r.msg || '已撤回', { icon: 1 });
                    loadExpertGradeContext(function() {
                        renderExpertSubmitToolbar();
                        $('#exampleTable').bootstrapTable('refresh');
                    });
                } else { layer.msg(r.msg || '撤回失败', { icon: 2 }); }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

function applyScoreProFilter() {
    $('#exampleTable').bootstrapTable('refresh');
}

function resetScoreProFilter() {
    $("#filterProName").val('');
    $("#filterApplyCompany").val('');
    $("#filterMajor").val('');
    $("#filterDeclareAccount").val('');
    $("#filterQcGroupName").val('');
    $("#filterExpertGroupName").val('');
    $("#filterEliminated").val('');
    $("#filterProStat").val('');
    applyScoreProFilter();
}

function showReviewRecordList(proId, proSubType) {
    $.ajax({
        url: "/cpe/suverProcess/list/reviewRecords",
        type: "get",
        data: {proId: proId, proSubType: proSubType},
        success: function(r) {
            if (r.code !== 0) {
                layer.msg(r.msg || "获取形审记录失败");
                return;
            }
            var records = r.data || [];
            if (!records || records.length === 0) {
                layer.msg("暂无形审记录");
                return;
            }
            var html = '<div style="padding:16px;max-height:400px;overflow-y:auto;">';
            for (var i = 0; i < records.length; i++) {
                var record = records[i];
                html += '<div style="border:1px solid #ddd;margin-bottom:10px;padding:10px;border-radius:4px;background:#fff;">';
                html += '<p><b>形审结果:</b> ' + (record.reviewResult || '无') + '</p>';
                html += '<p><b>形审时间:</b> ' + (record.created || record.reviewTime || '') + '</p>';
                html += '<p><b>形审人员:</b> ' + (record.reviewerName || '未知') + '</p>';
                html += '<p><b>形审评语:</b></p>';
                html += '<div style="border:1px solid #eee;padding:8px;min-height:80px;background:#f9f9f9;">' + (record.remarks || record.opinionDesc || '暂无评语') + '</div>';
                html += '</div>';
            }
            html += '</div>';
            layer.open({
                type: 1,
                title: "形审记录列表",
                area: ['600px', '500px'],
                content: html
            });
        }
    });
}