var prefix = "/enterprise_pro";

$(function () {
    $("#expertGroupForm").validate({
        rules: {
            name: {required: true, minlength: 2, maxlength: 50}
        },
        messages: {
            name: {
                required: "请输入专家分组名称",
                minlength: "专家分组名称至少 2 个字符",
                maxlength: "专家分组名称最多 50 个字符"
            }
        },
        errorPlacement: function (error, element) {
            error.appendTo(element.parent().next());
        },
        submitHandler: function (form) {
            save();
        }
    });
});

function save() {
    var taskid = $("#taskid").val();
    var groupid = $("#groupid").val();
    var name = $("#name").val();

    var params = {
        'taskid': taskid,
        'name': name
    };
    if (groupid && groupid != '' && groupid != '0') {
        params['groupid'] = parseInt(groupid);
    }

    $.ajax({
        url: prefix + "/expert_group/save",
        type: "post",
        data: params,
        success: function (r) {
            if (r.code == 0) {
                parent.layer.msg("保存成功");
                if (typeof parent.parent.reLoad === 'function') {
                    parent.parent.reLoad();
                } else if (typeof parent.reLoad === 'function') {
                    parent.reLoad();
                }
                setTimeout(function () {
                    parent.layer.closeAll();
                    if (typeof parent.reLoad === 'function') {
                        parent.reLoad();
                    }
                }, 500);
            } else {
                layer.msg(r.msg);
            }
        }
    });
}
