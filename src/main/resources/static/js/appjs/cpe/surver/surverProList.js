var prefix = "/surverPro"
$(function () {
    load();
});

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
                        proSubType: $("#proSubType").val(),
                        taskId: $("#taskId").val(),
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

                    {
                        field: 'memberList',
                        title: '人员名单'
                    },
                    {
                        field: 'declareAccount',
                        title: '申报账号'
                    },
                    {
                        field: 'applyAccount',
                        title: '申报联系方式'
                    },
                    {
                        field: 'qcGroupName',
                        title: '分组',
                        formatter: function(value, row, index) {
                            var groupName = value || '未分组';
                            if ($("#isAssociationLeader").val() === '1') {
                                return '<div style="display:flex;align-items:center;justify-content:space-between;">' +
                                    '<span class="group-name-' + row.proId + '" style="flex:1;overflow:hidden;text-overflow:ellipsis;">' + groupName + '</span>' +
                                    '<button class="btn btn-xs btn-primary" onclick="showGroupSelect(' + row.proId + ')" style="margin-left:10px;white-space:nowrap;">选择分组</button>' +
                                    '</div>';
                            }
                            return '<span class="group-name-' + row.proId + '">' + groupName + '</span>';
                        }
                    },
                    {
                        field: 'applyStat',
                        title: '状态'
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
                            return '<a href="javascript:void(0)" onclick="showReviewRecordList(' + row.proId + ',\'' + (row.proSubType || '') + '\')">' + text + '</a>';
                        }
                    },
                    {
                        title: '操作',
                        field: 'id',
                        align: 'center',
                        formatter: function (value, row, index) {
                            let rs_edit_h = s_edit_h;
                            let rs_remove_h = s_remove_h;
                            let rs_print_h = s_print_h;
                            let rs_review_h = s_review_h;
                            let rs_cancel_review_h = s_cancel_review_h;
                            let rs_download_doc_h = s_download_doc_h;
                            let rs_download_zip_h = s_download_zip_h;
                            var isLeader = $("#isAssociationLeader").val() === '1';
                            var stat = row.proStat || '';
                            var isReviewResultStat = stat === 'partake_award' || stat === 'improve_partake' || stat === 'no_award' || stat === 'defer_score';
                            if (!row.isEdit) {
                                //不能编辑操作
                                rs_edit_h = 'hidden';
                                rs_remove_h = 'hidden';
                            }
                            if (!row.isCancelReview) {
                                rs_cancel_review_h = 'hidden';
                            }
                            // 领导同QC：审核中/已出审查结论时允许“驳回”
                            if (isLeader && (stat === 'check' || isReviewResultStat)) {
                                rs_cancel_review_h = '';
                            }
                            if (!row.isDownloadProDoc) {
                                rs_download_doc_h = 'hidden';
                                rs_download_zip_h = 'hidden';
                            }

                            var view = '<a class="btn btn-primary  " href="#" mce_href="#" title="查看" onclick="view(\''
                                + row.proId
                                + '\',\''
                                + row.proSubType
                                + '\')">查看</a> ';
                            var e = '<a class="btn btn-primary  ' + rs_edit_h + '" href="#" mce_href="#" title="修改" onclick="edit(\''
                                + row.proId
                                + '\',\''
                                + row.proSubType
                                + '\')">修改</a> ';
                            var d = '<a class="btn btn-warning  ' + rs_remove_h + '" href="#" title="删除"  mce_href="#" onclick="remove(\''
                                + row.id + '\',\''
                                + row.proId
                                + '\')">删除</a> ';
                            // var print = '<a class="btn btn-success btn-sm '+ s_print_h +'" href="#" title="打印"  mce_href="#" onclick="print(\''
                            // 		+ row.proId
                            // 		+ '\')">打印</a> ';
                            var print = '<a class="btn btn-success " href="#" title="打印"  mce_href="#" onclick="print(\''
                                + row.proId + '\',\''
                                + row.proSubType
                                + '\')">打印</a> ';

                            var downloadAppendix = '<a class="btn btn-success  ' + s_download_zip_h + '" href="#" title="下载"  mce_href="#" onclick="downloadData(\''
                                + row.proId
                                + '\',\'appendix'
                                + '\')">下载附件</a> ';
                            var downloadSupport = '<a class="btn btn-success ' + s_download_zip_h + '" href="#" title="下载"  mce_href="#" onclick="downloadData(\''
                                + row.proId
                                + '\',\'support'
                                + '\')">下载证明</a> ';
                            var download = downloadAppendix + downloadSupport;

                            var saveCode = '<a class="btn btn-success  ' + s_save_code_h + '"  href="#" title="保存"  mce_href="#" onclick="saveProResultCode(\''
                                + row.proId
                                + '\')">保存</a> ';

                            let subCheckIsHide = row.isSubCheck == 1 ? '' : 'hidden';
                            var h = '<a class="btn btn-success ' + rs_edit_h + ' ' + subCheckIsHide + '" href="#" onclick="subCheck(' + row.proId + ')" title="提交审核"  mce_href="#">提交审核</a> ';
                            var cancelBtnText = $("#isEnterpriseUser").val() === '1' ? '回收' : '驳回';
                            var cancelCheck = '<a class="btn btn-success ' + rs_cancel_review_h + '" href="#" onclick="cancelCheck(' + row.proId + ')" title="表单审核' + cancelBtnText + '"  mce_href="#">' + cancelBtnText + '</a> ';

                            var reviewHide = rs_review_h;
                            // 未提交状态不显示形式审查按钮
                            if (!row.proStat || row.proStat === '') {
                                reviewHide = 'hidden';
                            }
                            var j = '<a class="btn btn-success  ' + reviewHide + '" href="#" title="形式检查"  mce_href="#" onclick="reviewUploadDoc(\''
                                + row.id
                                + '\',\''
                                + row.proId
                                + '\',\''
                                + row.proSubType
                                + '\')">形式审查</a> ';


                            var specialistScore = '<a class="btn btn-warning btn-sm  " href="#" title="评分"  mce_href="#" onclick="specialistScore (\''
                                + row.proId
                                + '\',\''
                                + row.taskId
                                + '\')"">评分</a> ';
                            var specialistOpinion = '<a class="btn btn-success btn-sm" href="#" title="评价"  mce_href="#" onclick="specialistOpinion(\''
                                + row.proId
                                + '\',\''
                                + row.taskId
                                + '\')">评价</a> ';

                            return saveCode + view + e + d + print + download + h + cancelCheck + j /*+ specialistScore + specialistOpinion*/;
                        }
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

                    if (field === "declareAccount") {
                        // 仅有保存权限的用户允许编辑申报联系方式
                        if (s_save_code_h !== 'hidden') {
                            $element.attr('contenteditable', true);
                            $element.blur(function () {
                                let tdValue = $element.html();
                                setDeclareAccount(tdValue);
                            })
                        }
                    }

                }

            });
    if($("#isViewProCode").val() == 'false') {
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
    var taskId = $("#taskId").val();
    // 不按当前标签分类，统一导出四个奖项
    window.location.href = prefix + "/exportExcel?taskId=" + encodeURIComponent(taskId);
}

function printDetailExcelPro() {
    var taskId = $("#taskId").val();
    var proSubType = $("#proSubType").val();
    if (!proSubType) {
        layer.msg("请在具体奖项页导出详情");
        return;
    }
    var allowSubType = ['contribution', 'design', 'software', 'standard'];
    if (allowSubType.indexOf(proSubType) < 0) {
        layer.msg("仅支持优秀勘察奖、优秀设计奖、计算机软件奖、标准设计奖导出详情");
        return;
    }
    window.location.href = prefix + "/exportDetailExcel?taskId=" + encodeURIComponent(taskId)
        + "&proSubType=" + encodeURIComponent(proSubType);
}

function importCheckResult() {
    var taskId = $("#taskId").val();
    if (!taskId) {
        layer.msg("缺少任务ID");
        return;
    }
    parent.layer.open({
        zIndex: 110,
        type: 2,
        title: '上传形式审查结果',
        maxmin: true,
        shadeClose: false,
        area: ['800px', '520px'],
        content: '/award_flow/to_uploadsmall?proId=0&fileType=import_check_result_qc&taskId=' + taskId
    });
}

function toGroupManage() {
    var taskId = $("#taskId").val();
    if (!taskId) {
        layer.msg("缺少任务ID");
        return;
    }
    page('/enterprise_pro/to_group_manage/' + taskId, '分组管理', 20220601, true);
}

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


function cancelCheck(proId) {
    layer.confirm('确定要撤回,重新修改吗？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            url: "/cpe/suverProcess/cancelCheck",
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
    if($("#isViewProCode").val() == 'false') {
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

function view(proId, proType) {
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
    page(url, title, 20220328, true);
}

// function edit(proId, proType) {
//     let url = '';
//     let title = '';
//     if (proType == 'design') {
//         url = '/surverApply/toApplyDesign?proId=' + proId;
//         title = '勘察设计奖编辑';
//     } else if (proType == 'software') {
//         url = '/surverSoftwareApply/toApplySoftware?proId=' + proId;
//         title = '计算机软件奖编辑';
//     } else if (proType == 'consulting') {
//         url = '/surverConsultingApply/toApply?proId=' + proId;
//         title = '咨询奖编辑';
//     } else if (proType == 'standard') {
//         url = '/surverStandardApply/toApply?proId=' + proId;
//         title = '标准设计奖编辑';
//     } else if (proType == 'contribution') {
//         url = '/surverBaseExlentApply/toApply?proId=' + proId;
//         title = '优秀勘察奖';
//     }
//     page(url, title, 20220328, true);
// }

// ... existing code ...

function edit(proId, proType) {
    let url = '';
    let title = '';
    if (proType == 'design') {
        url = '/surverApply/toApplyDesign?proId=' + proId;
        title = '勘察设计奖编辑';
    } else if (proType == 'software') {
        url = '/surverSoftwareApply/toApplySoftware?proId=' + proId;
        title = '计算机软件奖编辑';
    } else if (proType == 'consulting') {
        url = '/surverConsultingApply/toApply?proId=' + proId;
        title = '咨询奖编辑';
    } else if (proType == 'standard') {
        url = '/surverStandardApply/toApply?proId=' + proId;
        title = '标准设计奖编辑';
    } else if (proType == 'contribution') {
        url = '/surverBaseExlentApply/toApply?proId=' + proId;
        title = '优秀勘察奖';
    }
    page(url, title, 20220328, true);
}




function reviewUploadDoc(id, proId, proSubType) {
    let url = '';
    let title = '';
    if (proSubType == 'design') {
        url = '/cpe/surverReviewDesignResult?';
        title = '勘察设计奖形式审查';
    } else if (proSubType == 'software') {
        url = '/cpe/surverReviewSoftResult?';
        title = '计算机软件奖形式审查';
    } else if (proSubType == 'consulting') {
        url = '/cpe/surverReviewConsultResult?';
        title = '咨询奖形式审查';
    } else if (proSubType == 'standard') {
        url = '/cpe/surverReviewStandardResult?';
        title = '标准设计奖形式审查';
    } else if (proSubType == 'contribution') {
        url = '/cpe/surverReviewSurverResult?';
        title = '优秀勘察奖形式审查';
    }
    page(url + "proId=" + proId + "&proSubType=" + proSubType, title, 20220414, true);
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
                html += '<p><b>形审人员:</b> ' + (record.optUid || record.reviewerName || '未知') + '</p>';
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

function print(proId, protype) {

    console.log("ddd" + proId);
    console.log("ddd" + protype);


    if (protype == 'design') {
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
                content: '/surverApply/print/proinfo?id=' + proId  // iframe的url
            })

        })
    } else if (protype == 'contribution') {
        //石油工程建设优秀勘察奖
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
                content: '/surverExlentApply/print/proinfo?id=' + proId  // iframe的url
            })

        })
    } else if (protype == 'software') {
        // 石油工程建设优秀勘察设计计算机软件奖
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
                content: '/surverSoftwareApply/print/proinfo?id=' + proId  // iframe的url
            })

        })
    } else if (protype == 'standard') {
// 石油工程建设优秀标准设计奖
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
                content: '/surverStandardApply/print/proinfo?id=' + proId  // iframe的url
            })

        })
    } else if (protype == 'consulting') {
// 石油工程建设优秀咨询奖
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
                content: '/surverConsultingApply/print/proinfo?id=' + proId  // iframe的url
            })

        })
    }


}

let saveValue = "";
let declareAccountSaveValue = "";

/**
 * 设置成果编码
 * @param code
 */
function setCode(code) {
    saveValue = code;
}

function setDeclareAccount(account) {
    declareAccountSaveValue = account;
}

function downloadData(proId, fileType) {
    layer.confirm('确定要下载选中的记录？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            url: prefix + "/downloadProDocFiles",
            type: "post",
            data: {
                'proId': proId,
                'fileType': fileType
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
    })
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
    if (saveValue === "" && declareAccountSaveValue === "") {
        return;
    }
    layer.confirm('确定要更新成果编号/申报账号？', {
        btn: ['确定', '取消']
    }, function () {
        $.ajax({
            url: prefix + "/saveCode",
            type: "post",
            data: {
                'resultCode': saveValue,
                'declareAccount': declareAccountSaveValue,
                'proId': proId
            },
            success: function (r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    saveValue = "";
                    declareAccountSaveValue = "";
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
