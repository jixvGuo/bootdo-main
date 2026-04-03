var prefix = "/qcAward"
window.projectTypes = window.projectTypes || [];
window.classifications = window.classifications || [];
window.pageTaskStageCode = window.pageTaskStageCode || "WAIT_APPLY";
window.isAssociationLeader = (typeof window.isAssociationLeader !== 'undefined')
    ? !!window.isAssociationLeader
    : ($("#isAssociationLeader").val() === 'true');

window.isEnterpriseUser = (typeof window.isEnterpriseUser !== 'undefined')
    ? !!window.isEnterpriseUser
    : ($("#isEnterpriseUser").val() === 'true');
$(function () {
    load();
    $("#statusFilter").on("change", function () {
        // 第0步修改：setStatusFilter 内部已 reLoad，避免重复刷新
        setStatusFilter($(this).val());
        console.log("状态切换为：" + $(this).val());
        // reLoad();
    });
    // 初始化筛选下拉框 - 延迟执行确保 HTML 已渲染
    setTimeout(function() {
        initFilterDropdowns();
    }, 100);
});
// 初始化筛选下拉框
function initFilterDropdowns() {
    // 从全局变量或 Thymeleaf 模板获取数据
    var projectTypesData = window.projectTypes || [];
    var classificationsData = window.classifications || [];

    // 如果全局变量为空，尝试从页面隐藏域获取（备用方案）
    if (projectTypesData.length === 0 && $("#projectTypesData").length > 0) {
        try {
            projectTypesData = JSON.parse($("#projectTypesData").val());
        } catch(e) {
            console.log("解析 projectTypes 失败");
        }
    }
    if (classificationsData.length === 0 && $("#classificationsData").length > 0) {
        try {
            classificationsData = JSON.parse($("#classificationsData").val());
        } catch(e) {
            console.log("解析 classifications 失败");
        }
    }

    if (projectTypesData && projectTypesData.length > 0) {
        var topicTypeHtml = '<option value="">课题类型 - 全部</option>';
        for (var i = 0; i < projectTypesData.length; i++) {
            topicTypeHtml += '<option value="' + projectTypesData[i].value + '">' + projectTypesData[i].name + '</option>';
        }
        $("#filter_topicType").html(topicTypeHtml);
        console.log("课题类型下拉框已初始化，共" + projectTypesData.length + "项");
    } else {
        console.warn("未找到课题类型字典数据");
    }

    if (classificationsData && classificationsData.length > 0) {
        var professionalScopeHtml = '<option value="">分类类型 - 全部</option>';
        for (var i = 0; i < classificationsData.length; i++) {
            professionalScopeHtml += '<option value="' + classificationsData[i].value + '">' + classificationsData[i].name + '</option>';
        }
        $("#filter_professionalScope").html(professionalScopeHtml);
        console.log("分类类型下拉框已初始化，共" + classificationsData.length + "项");
    } else {
        console.warn("未找到分类类型字典数据");
    }
}

// 应用筛选
function applyFilters() {
    reLoad();
}

// 清除筛选
function clearFilters() {
    $("#filter_id").val("");
    $("#filter_proCode").val("");
    $("#filter_topicName").val("");
    $("#filter_groupName").val("");
    $("#filter_groupMember").val("");
    $("#filter_companyName").val("");
    $("#filter_topicType").val("");
    $("#filter_professionalScope").val("");
    $("#filter_qcGroupName").val("");
    $("#filter_applyStat").val("");
    reLoad();
}



// ===================== 操作权限重构（第1步）开始 =====================

/**
 * 统一标准化课题状态
 * 目标值：UNSUBMITTED(未提交) / CHECKING(审核中) / REJECTED(已驳回)
 */
function normalizeProStat(proStat) {
    var stat = (proStat || "").toLowerCase();
    // score/experts_score在项目列表页等同审核中
    if (stat === "check" || stat === "score" || stat === "experts_score") return "CHECKING";
    if (stat === "reject") return "REJECTED";
    return "UNSUBMITTED";
}

/**
 * 判断申报是否结束（用于 CHECKING 阶段下未提交课题是否还能提交）
 */
function isApplyClosed(row) {
    var applyEnd = row.applyEndDate || "";
    if (!applyEnd) return false;
    var end = new Date(applyEnd.replace(/-/g, "/"));
    if (isNaN(end.getTime())) return false;
    return end <= new Date();
}

/**
 * 第一步核心：先算权限（不拼按钮）
 * stage: WAIT_APPLY / APPLYING / CHECKING / CHECK_END
 */
// function resolveOps(stage, proStat, row) {
//     var normStat = normalizeProStat(proStat);
//     var ops = {
//         view: true,       // 查看默认始终可见
//         edit: false,
//         remove: false,
//         print: false,
//         download: false,
//         submit: false,    // 提交审核
//         cancel: false,    // 回收
//         review: false,    // 形式审查
//         saveCode: false
//     };
//     var normStat = normalizeProStat(proStat);
//     var rawStat = (proStat || "").toLowerCase();
//     var isReviewResultStat = rawStat === "partake_award"
//         || rawStat === "improve_partake"
//         || rawStat === "no_award"
//         || rawStat === "delayed_award";

//     var hasCancelPermission = (typeof s_cancel_review_h !== 'undefined' && s_cancel_review_h !== 'hidden');

//     if ((stage === "APPLYING" || stage === "CHECKING")
//         && (normStat === "CHECKING" || isReviewResultStat)
//         && row.isCancelReview
//         && hasCancelPermission) {
//         ops.cancel = true;
//     }
//     var hasReviewPermission = (typeof s_review_h !== 'undefined' && s_review_h !== 'hidden');
//     // 审核中项目，允许回收（申报阶段 / 形审阶段都支持）
//     if ((stage === "APPLYING" || stage === "CHECKING")
//         && normStat === "CHECKING"
//         && row.isCancelReview
//         && hasCancelPermission) {
//         ops.cancel = true;
//     }
//     // ===== 按阶段 + 课题状态 先做业务矩阵 =====
//     if (stage === "WAIT_APPLY") {
//         // 申报、形审未开始：仅查看
//     } else if (stage === "APPLYING") {
//         // 申报开始、形审未开始
//         if (normStat === "UNSUBMITTED") {
//             ops.edit = true;
//             ops.remove = true;
//             ops.print = true;
//             ops.download = true;
//             ops.submit = true;
//         } else if (normStat === "CHECKING") {
//             // 审核中：仅查看（是否回收可按 row.isCancelReview 再叠加）
//         } else if (normStat === "REJECTED") {
//             ops.edit = true;
//             ops.remove = true;
//             ops.print = true;
//             ops.download = true;
//             ops.submit = true;
//         }
//     } else if (stage === "CHECKING") {
//         // 形审开始（可能与申报重叠）
//         if (normStat === "UNSUBMITTED") {
//             // 申报未结束时可提交；申报结束后仅查看
//             if (!isApplyClosed(row)) {
//                 ops.edit = true;
//                 ops.remove = true;
//                 ops.print = true;
//                 ops.download = true;
//                 ops.submit = true;
//             }
//         } else if (normStat === "CHECKING") {
//             // 审核中仅查看
//         } else if (normStat === "REJECTED") {
//             // 已驳回可重新提交
//             ops.edit = true;
//             ops.remove = true;
//             ops.print = true;
//             ops.download = true;
//             ops.submit = true;
//         }
//     } else if (stage === "CHECK_END") {
//         // 申报结束、形审结束：仅查看
//     } else {
//         // 未知阶段兜底：仅查看
//     }

//     // ===== 再叠加行级权限（后端返回）=====
//     if (!row.isEdit) {
//         ops.edit = false;
//         ops.remove = false;
//     }
//     if (!row.isDownloadProDoc) {
//         ops.download = false;
//     }
//     if (row.isSubCheck != 1) {
//         ops.submit = false;
//     }
//     if (!row.isCancelReview) {
//         ops.cancel = false;
//     }
//     // if (normalizeProStat(proStat) === "CHECKING" && row.isReview) {
//     //     ops.review = true;
//     //     ops.view = true;
//     // }
//     if (!row.isReview) {
//         ops.review = false;
//     }


//     // 回收按钮显示条件：申请中/形审中 + 项目审核中(check) + 行级允许 + 页面权限允许
//     var hasCancelPermission = (typeof s_cancel_review_h !== 'undefined' && s_cancel_review_h !== 'hidden');
//     if ((stage === "APPLYING" || stage === "CHECKING")
//         && normStat === "CHECKING"
//         && row.isCancelReview
//         && hasCancelPermission) {
//         ops.cancel = true;
//     }
//     // 若你需要“APPLYING + 审核中可回收”，可打开这个规则：
//     if (stage === "APPLYING" && normalizeProStat(proStat) === "CHECKING" && row.isCancelReview) {
//         ops.cancel = true;
//     }
//     // 第3步修复：形审人员按钮开启条件
//     // 仅当后端标记可审核(row.isReview=true) 且课题为审核中(check)时，显示“形式审查”
//     if (row.isReview && normalizeProStat(proStat) === "CHECKING" && hasReviewPermission) {
//         ops.review = true;
//         ops.view = true; // 形审人员至少可查看
//     }

//     return ops;
// }

function resolveOps(stage, proStat, row) {
    var normStat = normalizeProStat(proStat);
    var rawStat = (proStat || "").toLowerCase();

    var ops = {
        view: true,
        edit: false,
        remove: false,
        print: false,
        download: false,
        submit: false,
        cancel: false,
        review: false,
        saveCode: false,
        qycancel:false
    };

    var hasReviewPermission = (typeof s_review_h !== 'undefined' && s_review_h !== 'hidden');
    var hasCancelPermission = (typeof s_cancel_review_h !== 'undefined' && s_cancel_review_h !== 'hidden');
    var isLeader = !!window.isAssociationLeader;
    var isEnterprise = !!window.isEnterpriseUser;
    var isReviewResultStat = rawStat === "partake_award"
        || rawStat === "improve_partake"
        || rawStat === "no_award"
        || rawStat === "delayed_award";



    // 1) 基础操作矩阵
    if (stage === "APPLYING") {
        if (normStat === "UNSUBMITTED" || normStat === "REJECTED") {
            ops.edit = true;
            ops.remove = true;
            ops.print = true;
            ops.download = true;
            ops.submit = true;

        }
    } else if (stage === "CHECKING") {
        if (normStat === "UNSUBMITTED") {
            if (!isApplyClosed(row)) {
                ops.edit = true;
                ops.remove = true;
                ops.print = true;
                ops.download = true;
                ops.submit = true;
            }
        } else if (normStat === "REJECTED") {
            ops.edit = true;
            ops.remove = true;
            ops.print = true;
            ops.download = true;
            ops.submit = true;
        }
    }
    
    // 编辑按钮始终可用（需求：无论任何阶段编辑按钮都要一直存在）
    ops.edit = true;

    // 2) 行级权限叠加
    if (!row.isEdit) {
        // ops.edit = false;  // 原代码：编辑按钮随 isEdit 禁用
        // ops.edit = true;   // 新代码：编辑按钮始终可用，不再受 isEdit 影响
        ops.remove = false;
    }
    if (!row.isDownloadProDoc) {
        ops.download = false;
    }
    if (row.isSubCheck != 1) {
        ops.submit = false;
    }

    // 3) 回收按钮（统一规则：审核中+四种形审结果都可回收）
    if (!isEnterprise && (stage === "APPLYING" || stage === "CHECKING")
        && (normStat === "CHECKING" || isReviewResultStat)
        && row.isCancelReview
        && hasCancelPermission) {
        ops.cancel = true;
    }
    // 新增：企业用户回收按钮（申报开始后、形审开始前显示）
    if (isEnterprise && stage === "APPLYING"
        && normStat === "CHECKING"
        && row.isCancelReview) {
        ops.qycancel = true;
    }
    if (typeof isEnterpriseUser !== 'undefined' && !isEnterpriseUser && !window.isAssociationLeader) {
        ops.submit = false;
        // ops.edit=false;    // 原代码：非企业/非领导禁用编辑
        ops.remove=false;
    }
    // 4) 形式审查按钮
    // if (row.isReview && normStat === "CHECKING" && hasReviewPermission) {
    //     ops.review = true;
    //     ops.view = true;
    // }
    if (!isEnterprise && (normStat === "CHECKING" || isReviewResultStat)
        && hasReviewPermission) {
        ops.review = true;
        ops.view = true;
    }
    if (isLeader) {
        // ops.edit = false;  // 原代码：领导角色禁用编辑
        ops.edit = false;      // 新代码：领导角色也始终显示编辑按钮
        ops.remove = false;
        ops.print = true;
        ops.download = true;
        ops.submit = false;
        ops.cancel = false;
        ops.review = false;
    }
    if (isLeader && (normStat === "CHECKING" || isReviewResultStat)) {
        ops.cancel = true;
        ops.review = true;
    }
    return ops;
}


/**
 * 第一步核心：再渲染按钮（只依赖 ops，不再改 hidden 变量）
 */
function renderOps(ops, row) {
    var btns = [];

    // 查看（固定）
    if (ops.view) {
        btns.push('<a class="btn btn-primary" href="#" title="查看" onclick="view(\'' + row.proId + '\')">查看</a>');
    }

    if (ops.edit) {
        btns.push('<a class="btn btn-primary" href="#" title="修改" onclick="edit(\'' + row.proId + '\')">修改</a>');
    }
    if (ops.remove) {
        btns.push('<a class="btn btn-warning" href="#" title="删除" onclick="remove(\'' + row.id + '\',\'' + row.proId + '\')">删除</a>');
    }
    // if (ops.print) {
    //     btns.push('<a class="btn btn-success" href="#" title="打印" onclick="print(\'' + row.proId + '\')">打印</a>');
    // }
    if (ops.download) {
        btns.push('<a class="btn btn-success" href="#" title="下载" onclick="downloadData(\'' + row.proId + '\')">下载</a>');
    }
    if (ops.submit) {
        btns.push('<a class="btn btn-success" href="#" title="提交审核" onclick="subCheck(' + row.proId + ')">提交审核</a>');
    }
    if (ops.cancel) {
        btns.push('<a class="btn btn-success" href="#" title="表单审核撤回" onclick="cancelCheck(' + row.proId + ')">驳回</a>');
    }
    if (ops.qycancel) {
        btns.push('<a class="btn btn-success" href="#" title="企业回收" onclick="cancelCheck(' + row.proId + ')">回收</a>');
    }
    if (ops.review) {
        btns.push('<a class="btn btn-success" href="#" title="形式审查" onclick="reviewUploadDoc(\'' + row.id + '\',\'' + row.proId + '\')">形式审查</a>');
    }
    // 领导角色显示形审记录按钮
    if (window.isAssociationLeader) {
        btns.push('<a class="btn btn-info" href="#" title="形审记录" onclick="showReviewRecordList(\'' + row.proId + '\')">形审记录</a>');
    }
    if (ops.saveCode) {
        btns.push('<a class="btn btn-success" href="#" title="保存" onclick="saveProResultCode(\'' + row.proId + '\')">保存</a>');
    }
    if (typeof isEnterpriseUser !== 'undefined' && !isEnterpriseUser && !window.isAssociationLeader) {
        ops.submit = false;
    }
    if (!window.isEnterpriseUser && !window.isAssociationLeader) {
        ops.submit = false;
    }
    if (!window.isEnterpriseUser && !window.isAssociationLeader) {
        ops.remove = false;
        // ops.edit = false; // 如果你也希望非企业不能修改，打开这行
    }
    return btns.join(' ');
}

// ===================== 操作权限重构（第1步）结束 =====================


function load() {
    $('#exampleTable')
        .bootstrapTable(
            {
                method: 'get', // 服务器数据的请求方式 get or post
                url: prefix + "/get/proList", // 服务器数据的加载地址
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
                    return {
                        //说明：传入后台的参数包括offset开始索引，limit步长，sort排序列，order：desc或者,以及所有列的键值对
                        limit: params.limit,
                        offset: params.offset,
                        keyWord: $("#keyWord").val(),
                        keyType: $("#keyType").val(),
                        taskId: $("#taskId").val(),
                        statusFilter: $("#statusFilter").val(),
                        // 添加各列筛选参数
                        filterId: $("#filter_id").val(),
                        filterProCode: $("#filter_proCode").val(),
                        filterTopicName: $("#filter_topicName").val(),
                        filterGroupName: $("#filter_groupName").val(),
                        filterGroupMember: $("#filter_groupMember").val(),
                        filterCompanyName: $("#filter_companyName").val(),
                        filterTopicType: $("#filter_topicType").val(),
                        filterProfessionalScope: $("#filter_professionalScope").val(),
                        filterQcGroupName: $("#filter_qcGroupName").val(),
                        filterApplyStat: $("#filter_applyStat").val()
                    };
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
                        title: '申报账号',
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
                        field: 'groupMember',
                        title: '小组成员'
                    },
                    {
                        field: 'companyName',
                        title: '单位名称'
                    },
                    {
                        field: 'topicType',
                        title: '课题类型',
                        formatter: function (value, row, index) {
                            // 后端已经通过 CASE WHEN 转换为中文，直接返回
                            return value || '';
                        }
                    },
                    {
                        field: 'professionalScope',
                        title: '分类类型',
                        formatter: function (value, row, index) {
                            // 后端已经通过 CASE WHEN 转换为中文，直接返回
                            return value || '';
                        }
                    },
                    {
                        field: 'qcGroupName',
                        title: '分组',
                        formatter: function(value, row, index) {
                            var groupName = value || '未分组';
                            if (window.isAssociationLeader) {
                                return '<div style="display: flex; align-items: center; justify-content: space-between;">' +
                                    '<span class="group-name-' + row.proId + '" style="flex: 1; overflow: hidden; text-overflow: ellipsis;">' + groupName + '</span>' +
                                    '<button class="btn btn-xs btn-primary" onclick="showGroupSelect(' + row.proId + ')" style="margin-left:10px; white-space: nowrap;">选择分组</button>' +
                                    '</div>';
                            } else {
                                // 非领导角色只显示分组名称，不显示按钮
                                return '<span class="group-name-' + row.proId + '">' + groupName + '</span>';
                            }
                        }
                    },
                    {
                        field: 'applyStat',
                        title: '状态',
                        formatter: function (value) {
                            if (value === "申请中") {
                                return "未提交";
                            }
                            return value;
                        }
                    },
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
                            return '<a href="javascript:void(0)" onclick="showReviewComment(' + row.proId + ')">' + text + '</a>';
                        }
                    },
                    {
                        title: '操作',
                        field: 'id',
                        align: 'center',
                        formatter: function (value, row, index) {
                            // 第一步重构：formatter 只做三件事
                            // 1) 取阶段
                            var stage = row.taskStageCode || pageTaskStageCode || "WAIT_APPLY";
                        
                            // 2) 算权限
                            var ops = resolveOps(stage, row.proStat, row);
                        
                            // 3) 渲染按钮
                            return renderOps(ops, row);
                        }
                        // formatter: function (value, row, index) {
                        //     var proStat = row.proStat || ""; // ""未提交, check审核中, reject已驳回
                        //     var stage = row.taskStageCode || pageTaskStageCode || ""; // WAIT_APPLY/APPLYING/CHECKING/CHECK_END
                        //     var applyEnd = row.applyEndDate || "";
                        //     var applyClosed = !!(applyEnd && new Date(applyEnd.replace(/-/g, "/")) <= new Date());

                        //     let rs_edit_h = s_edit_h;
                        //     let rs_remove_h = s_remove_h;
                        //     let rs_print_h = s_print_h;
                        //     let rs_download_doc_h = s_download_doc_h;
                        //     let rs_review_h = s_review_h;
                        //     let rs_cancel_review_h = s_cancel_review_h;

                        //     if (!row.isEdit) {
                        //         rs_edit_h = 'hidden';
                        //         rs_remove_h = 'hidden';
                        //     }
                        //     if (!row.isDownloadProDoc) rs_download_doc_h = 'hidden';

                        //     var view = '<a class="btn btn-primary" href="#" title="查看" onclick="view(\'' + row.proId + '\')">查看</a> ';
                        //     var e = '<a class="btn btn-primary ' + rs_edit_h + '" href="#" title="修改" onclick="edit(\'' + row.proId + '\')">修改</a> ';
                        //     var d = '<a class="btn btn-warning ' + rs_remove_h + '" href="#" title="删除" onclick="remove(\'' + row.id + '\',\'' + row.proId + '\')">删除</a> ';
                        //     var print = '<a class="btn btn-success ' + rs_print_h + '" href="#" title="打印" onclick="print(\'' + row.proId + '\')">打印</a> ';
                        //     var download = '<a class="btn btn-success ' + rs_download_doc_h + '" href="#" title="下载" onclick="downloadData(\'' + row.proId + '\')">下载</a> ';

                        //     let subCheckIsHide = row.isSubCheck == 1 ? '' : 'hidden';
                        //     var h = '<a class="btn btn-success ' + rs_edit_h + ' ' + subCheckIsHide + '" href="#" onclick="subCheck(' + row.proId + ')" title="提交审核">提交审核</a> ';
                        //     var cancelCheck = '<a class="btn btn-success ' + rs_cancel_review_h + '" href="#" onclick="cancelCheck(' + row.proId + ')" title="表单审核撤回">回收</a> ';
                        //     var j = '<a class="btn btn-success ' + rs_review_h + '" href="#" title="形式审查" onclick="reviewUploadDoc(\'' + row.id + '\',\'' + row.proId + '\')">形式审查</a> ';
                        //     var specialistScore = '<a class="btn btn-warning  " href="#" title="评分"  mce_href="#" onclick="specialistScore (\''
                        //         + row.proId
                        //         + '\',\''
                        //         + row.taskId
                        //         + '\')"">评分</a> ';
                        //     var specialistOpinion = '<a class="btn btn-success " href="#" title="评价"  mce_href="#" onclick="specialistOpinion(\''
                        //         + row.proId
                        //         + '\',\''
                        //         + row.taskId
                        //         + '\')">评价</a> ';
                        //         var fullOps = view + e + d + print + download + h + (rs_cancel_review_h === 'hidden' ? '' : cancelCheck);

                        //     // 行级权限控制（恢复旧逻辑，不做阶段提前return）
                        //     if (!row.isReview) {
                        //         rs_review_h = 'hidden';
                        //     }
                        //     if (!row.isCancelReview) {
                        //         rs_cancel_review_h = 'hidden';
                        //     }

                        //     var saveCode = '<a class="btn btn-success  ' + s_save_code_h + '" href="#" title="保存" onclick="saveProResultCode(\'' + row.proId + '\')">保存</a> ';

                        //     // 注意：这里全部用 rs_*，不要再用 s_*
                        //     return saveCode + view + e + d + print + download + h + cancelCheck + j;
                            
                        // }

                    }], /**
                 * @param {点击列的 field 名称} field
                 * @param {点击列的 value 值} value
                 * @param {点击列的整行数据} row
                 * @param {td 元素} $element
                 */
                onClickCell: function (field, value, row, $element) {

                    if (field === "proCode") {
                        $element.attr('contenteditable', true);
                        $element.blur(function () {
                            let index = $element.parent().data('index');
                            let tdValue = $element.html();
                            console.log("index" + index);
                            console.log("tdValue" + tdValue);
                            setCode(tdValue);
                        })
                    }

                }

            });
    if ($("#isViewProCode").val() == 'false') {
        $('#exampleTable').bootstrapTable('hideColumn', 'proCode');
    }

}

function specialistScore(proId, taskId) {
    page('/qcScore/toScore?proId=' + proId + '&taskId=' + taskId, 'QC奖申报查看', 20220205);
}

function specialistOpinion(proId, taskId) {
    page('/qcScore/toOpinion?proId=' + proId + '&taskId=' + taskId, 'QC奖申报查看', 20220205);

}


function subCheck(proId) {
    layer.confirm('确定要提交审核吗？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            url: "/qcProcess/subCheck",
            type: "post",
            data: {
                'proId': proId
            },
            success: function (r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });
    })
}


// 状态点击事件
function printExcelPro() {
    layer.open({
        type: 2,
        title: '打印QC项目申报奖',
        maxmin: true,
        shadeClose: false, // 点击遮罩关闭层
        area: ['800px', '520px'],
        content: prefix + "/qc/printExcel" // iframe的url
    });
}
function setStatusFilter(statusText) {
    $("#statusFilter").val(statusText || "");
    reLoad();
}

function cancelCheck(proId) {
    layer.confirm('确定要撤回,重新修改吗？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            url: "/qcProcess/cancelCheck",
            type: "post",
            data: {
                'proId': proId
            },
            success: function (r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });
    })
}

function reLoad() {
    $('#exampleTable').bootstrapTable('refresh');
    if ($("#isViewProCode").val() == 'false') {
        $('#exampleTable').bootstrapTable('hideColumn', 'proCode');
    }
}

function add() {
    layer.open({
        type: 2,
        title: '增加',
        maxmin: true,
        shadeClose: false, // 点击遮罩关闭层
        area: ['800px', '520px'],
        content: prefix + '/add' // iframe的url
    });
}

function view(proId) {
    page('/qcAward/view/apply?readonly=1&proId=' + proId, 'QC奖申报查看', 20220205,true);
}

function edit(proId) {
    page('/qcAward/view/apply?proId=' + proId, 'QC奖申报编辑', 20220206);
}

function reviewUploadDoc(id, proId) {
    page('/qcProcess/toReivew?groupInfoId=' + id + "&proId=" + proId, '形式检查', 20220211,true);
}

/***
 * 打印
 */
// function print(id){
// 	layer.open({
// 		type: 2,
// 		title: '状态',
// 		maxmin: true,
// 		shadeClose: false, // 点击遮罩关闭层
// 		area: ['800px', '520px'],
// 		content: prefix+'/print/proinfo?id=' + id  // iframe的url
// 	});
// }

function print(proId) {
    layer.confirm('确定要打印选中的记录？', {
        btn: ['确定', '取消']
    }, function (index) {
        console.log("ddd" + index);
        layer.close(index);
        layer.open({
            type: 2,
            title: '打印文档',
            maxmin: true,
            shadeClose: false, // 点击遮罩关闭层
            area: ['800px', '520px'],
            content: prefix + '/print/proinfo?id=' + proId  // iframe的url
        })

    })
}

let saveValue = "";

/**
 * 设置成果编码
 * @param code
 */
function setCode(code) {
    saveValue = code;
}
function downloadData(proId) {
    $.ajax({
        url: "/qcAward/downloadProDocFiles",
        type: "post",
        data: {
            'proId': proId
        },
        success: function (r) {
            if (r.code == 0) {
                layer.msg(r.msg);
                layer.open({
                    type: 2,
                    title: "下载文件",
                    maxmin: true,
                    shadeClose: false, // 点击遮罩关闭层
                    area: ['800px', '520px'],
                    content: "/common/sysFile/toDownload?url=" + r.zipUrl // iframe的url
                });
            } else {
                layer.msg(r.msg);
            }
        }
    });
}


function remove(id, proId) {
    layer.confirm('确定要删除选中的记录？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            url: prefix + "/remove/groupInfo",
            type: "post",
            data: {
                'id': id,
                'proId': proId
            },
            success: function (r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });
    })
}

function saveProResultCode(proId) {
    if (saveValue == "") {
        return;
    }
    layer.confirm('确定要更新成果编号？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            url: prefix + "/saveCode",
            type: "post",
            data: {
                'resultCode': saveValue,
                'proId': proId
            },
            success: function (r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });
    })
}

function resetPwd(id) {
}
function showReviewComment(proId) {
    $.get("/qcAward/get/latestReviewComment", {proId: proId}, function (r) {
        if (r.code !== 0) {
            layer.msg(r.msg || "获取形审评语失败");
            return;
        }
        var data = r.data || {};
        var result = data.latestReviewResult || "暂无形审结果";
        var comment = data.latestReviewComment || "暂无评语";
        var time = data.latestReviewTime || "";

        layer.open({
            type: 1,
            title: "形审详情",
            area: ['520px', '320px'],
            content:
                '<div style="padding:16px;">'
                + '<p><b>形审结果：</b>' + result + '</p>'
                + '<p><b>形审时间：</b>' + time + '</p>'
                + '<p><b>形审评语：</b></p>'
                + '<div style="border:1px solid #eee;padding:8px;min-height:120px;">' + comment + '</div>'
                + '</div>'
        });
    });
}

function batchRemove() {
    var rows = $('#exampleTable').bootstrapTable('getSelections'); // 返回所有选择的行，当没有选择的记录时，返回一个空数组
    if (rows.length == 0) {
        layer.msg("请选择要删除的数据");
        return;
    }
    layer.confirm("确认要删除选中的'" + rows.length + "'条数据吗?", {
        btn: ['确定', '取消']
        // 按钮
    }, function () {
        var ids = new Array();
        // 遍历所有选择的行数据，取每条数据对应的ID
        $.each(rows, function (i, row) {
            ids[i] = row['id'];
        });
        $.ajax({
            type: 'POST',
            data: {
                "ids": ids
            },
            url: prefix + '/batchRemove',
            success: function (r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });
    }, function () {

    });
}
    /**
     * 显示分组选择对话框
     */
    function showGroupSelect(proId) {
        var taskId = $("#taskId").val();
        $.ajax({
            url: "/enterprise_pro/group/list",
            type: "get",
            data: {
                taskId: taskId,
                limit: 100,
                offset: 0
            },
            success: function(data) {
                if (data.rows && data.rows.length > 0) {
                    var optionsHtml = '<option value="">请选择分组</option>';
                    data.rows.forEach(function(group) {
                        optionsHtml += '<option value="' + group.groupid + '">' + group.name + '</option>';
                    });

                    layer.open({
                        type: 1,
                        title: '选择分组',
                        area: ['400px', '300px'],
                        content: '<div style="padding:20px;">' +
                            '<select id="groupSelect" class="form-control">' + optionsHtml + '</select>' +
                            '<div style="margin-top:20px;text-align:center;">' +
                            '<button class="btn btn-primary" onclick="confirmGroupSelect(' + proId + ')">确定</button> ' +
                            '<button class="btn btn-default" onclick="layer.closeAll()">取消</button>' +
                            '</div></div>'
                    });
                } else {
                    layer.msg('暂无分组，请先在分组管理中创建分组');
                }
            }
        });
    }

    /**
     * 显示形审记录列表 (所有历史记录)
     */
    function showReviewRecordList(proId) {
        $.ajax({
            url: "/qcAward/list/reviewRecords",
            type: "get",
            data: {proId: proId},
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
                    html += '<p><b>形审时间:</b> ' + (record.reviewTime || '') + '</p>';
                    html += '<p><b>形审人员:</b> ' + (record.reviewerName || '未知') + '</p>';
                    html += '<p><b>形审评语:</b></p>';
                    html += '<div style="border:1px solid #eee;padding:8px;min-height:80px;background:#f9f9f9;">' + (record.opinionDesc || '暂无评语') + '</div>';
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

    /**
     * 确认选择分组
     */
    function confirmGroupSelect(proId) {
        var groupId = $("#groupSelect").val();
        if (!groupId) {
            layer.msg('请选择分组');
            return;
        }

        $.ajax({
            url: "/enterprise_pro/assign_to_group",
            type: "post",
            data: {
                proId: proId,
                groupId: groupId
            },
            success: function(r) {
                if (r.code == 0) {
                    layer.msg('分配成功');
                    layer.closeAll();
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });

}
