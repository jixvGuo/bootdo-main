var prefix = "/enterprise_pro";
$(function() {
    load();
});

function load() {
    var taskId = $("#taskId").val();
    $('#exampleTable')
        .bootstrapTable({
            method: 'get',
            url: prefix + "/group/list",
            striped: true,
            dataType: "json",
            pagination: true,
            singleSelect: false,
            pageSize: 10,
            pageNumber: 1,
            showColumns: false,
            sidePagination: "server",
            queryParams: function(params) {
                return {
                    limit: params.limit,
                    offset: params.offset,
                    taskId: taskId
                };
            },
            columns: [
                // {
                //     checkbox: true
                // },
                {
                    field: 'groupid',
                    title: '分组 ID'
                },
                {
                    field: 'name',
                    title: '分组名称'
                },
                {
                    title: '操作',
                    field: 'operation',
                    align: 'center',
                    formatter: function(value, row, index) {
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
        title: '新增分组',
        maxmin: true,
        shadeClose: false,
        area: ['600px', '400px'],
        content: prefix + "/group/add/" + taskId
    });
}

function edit(groupid) {
    var taskId = $("#taskId").val();
    layer.open({
        type: 2,
        title: '编辑分组',
        maxmin: true,
        shadeClose: false,
        area: ['600px', '400px'],
        content: prefix + "/group/edit/" + taskId + "/" + groupid
    });
}

function remove(groupid) {
    layer.confirm('确定要删除选中的记录？', {
        btn: ['确定', '取消']
    }, function() {
        $.ajax({
            url: prefix + "/group/check_and_remove",
            type: "post",
            data: {
                'groupid': groupid,
                'taskId': $("#taskId").val()
            },
            success: function(r) {
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

function batchRemove() {
    var rows = $('#exampleTable').bootstrapTable('getSelections');
    if (rows.length == 0) {
        layer.msg("请选择要删除的数据");
        return;
    }
    layer.confirm("确认要删除选中的'" + rows.length + "'条数据吗?", {
        btn: ['确定', '取消']
    }, function() {
        var ids = new Array();
        $.each(rows, function(i, row) {
            ids[i] = row['groupid'];
        });
        $.ajax({
            type: 'POST',
            data: {
                "groupids": ids,
                "taskId": $("#taskId").val()
            },
            url: prefix + '/group/batch_check_and_remove',
            success: function(r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });
    }, function() {});
}
