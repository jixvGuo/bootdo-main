// 勘察奖专家分组新增、编辑、删除-cxq
var prefix = "/enterprise_pro";

$(function () {
    load();
});

function load() {
    var taskId = $("#taskId").val();
    $('#exampleTable').bootstrapTable({
        method: 'get',
        url: prefix + "/expert_group/list",
        striped: true,
        dataType: "json",
        pagination: true,
        singleSelect: false,
        pageSize: 10,
        pageNumber: 1,
        showColumns: false,
        sidePagination: "server",
        queryParams: function (params) {
            return {
                limit: params.limit,
                offset: params.offset,
                taskId: taskId
            };
        },
        columns: [
            {field: 'groupid', title: '专家分组 ID'},
            {field: 'name', title: '专家分组名称'},
            {
                title: '操作',
                field: 'operation',
                align: 'center',
                formatter: function (value, row, index) {
                    var e = '<a class="btn btn-primary btn-sm" href="#" onclick="edit(' + row.groupid + ')">编辑</a> ';
                    var d = '<a class="btn btn-danger btn-sm" href="#" onclick="remove(' + row.groupid + ')">删除</a>';
                    return e + d;
                }
            }
        ]
    });
}

function reLoad() {
    $('#exampleTable').bootstrapTable('refresh');
}

function add() {
    var taskId = $("#taskId").val();
    layer.open({
        type: 2,
        title: '新增专家分组',
        maxmin: true,
        shadeClose: false,
        area: ['600px', '400px'],
        content: prefix + "/expert_group/add/" + taskId
    });
}

function edit(groupid) {
    var taskId = $("#taskId").val();
    layer.open({
        type: 2,
        title: '编辑专家分组',
        maxmin: true,
        shadeClose: false,
        area: ['600px', '400px'],
        content: prefix + "/expert_group/edit/" + taskId + "/" + groupid
    });
}

function remove(groupid) {
    layer.confirm('确定要删除选中的专家分组？', {btn: ['确定', '取消']}, function () {
        $.ajax({
            url: prefix + "/expert_group/check_and_remove",
            type: "post",
            data: {
                'groupid': groupid,
                'taskId': $("#taskId").val()
            },
            success: function (r) {
                layer.msg(r.msg);
                if (r.code == 0) {
                    reLoad();
                }
            }
        });
    });
}

// ============================================================
// 上传专家分组（Excel 导入）
// ============================================================
function openImportExpertGroup() {
    var html = '<div style="padding:20px;">' +
        '<div style="margin-bottom:15px;">' +
        '  <label style="display:inline-block;width:80px;">表格文件：</label>' +
        '  <input type="text" id="importExpertGroupFileName" class="form-control" style="display:inline-block;width:250px;" placeholder="请输入...." readonly />' +
        '  <button class="btn btn-default" onclick="$(\'#importExpertGroupFile\').click()">选择</button>' +
        '  <input type="file" id="importExpertGroupFile" accept=".xls,.xlsx" style="display:none;" onchange="onExpertGroupFileSelected(this)" />' +
        '</div>' +
        '<div style="margin-bottom:15px;text-align:center;">' +
        '  <a href="javascript:void(0);" onclick="exportExpertGroupTemplate()" style="color:#1890ff;">导出模板</a>' +
        '</div>' +
        '</div>';
    layer.open({
        type: 1,
        title: '上传表格',
        area: ['480px', '250px'],
        btn: ['确认', '取消'],
        content: html,
        yes: function (index) {
            submitImportExpertGroup(index);
        }
    });
}

function onExpertGroupFileSelected(input) {
    if (input.files && input.files.length > 0) {
        $("#importExpertGroupFileName").val(input.files[0].name);
    }
}

function exportExpertGroupTemplate() {
    var taskId = $("#taskId").val();
    window.location.href = prefix + "/expert_group/exportTemplate?taskId=" + encodeURIComponent(taskId);
}

function submitImportExpertGroup(layerIndex) {
    var fileInput = document.getElementById('importExpertGroupFile');
    if (!fileInput.files || fileInput.files.length === 0) {
        layer.msg('请先选择表格文件');
        return;
    }
    var formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('taskId', $("#taskId").val());
    $.ajax({
        url: prefix + "/expert_group/importExcel",
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function (r) {
            if (r.code == 0) {
                layer.msg(r.msg || '导入成功');
                layer.close(layerIndex);
                reLoad();
            } else {
                layer.msg(r.msg || '导入失败');
            }
        },
        error: function () {
            layer.msg('上传失败，请重试');
        }
    });
}

function batchRemove() {
    var rows = $('#exampleTable').bootstrapTable('getSelections');
    if (rows.length == 0) {
        layer.msg("请选择要删除的数据");
        return;
    }
    layer.confirm("确认要删除选中的'" + rows.length + "'条专家分组吗?", {btn: ['确定', '取消']}, function () {
        var ids = [];
        $.each(rows, function (i, row) {
            ids[i] = row['groupid'];
        });
        $.ajax({
            type: 'POST',
            data: {
                "groupids": ids,
                "taskId": $("#taskId").val()
            },
            url: prefix + '/expert_group/batch_check_and_remove',
            success: function (r) {
                layer.msg(r.msg);
                if (r.code == 0) {
                    reLoad();
                }
            }
        });
    });
}
