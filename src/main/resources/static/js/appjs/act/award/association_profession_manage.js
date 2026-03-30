var selfProfession = "";

// 科学技术奖协会专业组管理页面的前端逻辑脚本
//页面初始化
$(function () {

    var allData = $("#assPros").val();
    console.log(JSON.stringify(allData));


    $('.form_date').datetimepicker({
        language: 'zh-CN',
        autoclose: true,
        minuteStep: 1,
        todayBtn: true,
        setDate: new Date()
    });

    reloadData();

    // 监听奖项选择下拉框变化
    $("select#assAwardType").change(function () {
        console.log($(this).val() + "-------");
        var str = $(this).val()
        getMajors(str); // 切换奖项时重新加载专业组
    });

});

/***
 * 刷新页面
 */
function reloadData() {
    
}
/****
 * 获取专业组列表（切换奖项时调用）
 * @param val
 */
function getMajors(val) {
    // AJAX 请求后端接口 /scienceProgressScience/getAssignGroups
    // 根据选中的奖项类型加载对应的专业分组列表
    // 更新左侧专业组列表，清空右侧专家列表

    var assAwardType = $("#assAwardType").val();  //获取选中的项
    var taskId = $("#publishTaskId").val();

    $.ajax({
        cache: true,
        url: "/scienceProgressScience/getAssignGroups",
        type: 'POST',
        data: {
            proType: assAwardType,
            taskId:taskId
        },
        async: false,
        error: function (request) {
            parent.layer.alert("Connection error");
        },
        // 切换奖项类型时触发
        success: function (data) {
            if (data.code == 0) {
                // 原始实现：仅更新左侧分组列表
                // parent.layer.msg("操作成功");
                // let groupListStr = "";
                // if(data.groupList) {
                //     for(var i=0;i<data.groupList.length;i++) {
                //         let groupName = data.groupList[i];
                //         let str = "<li>" +
                //             "<h4 style='display:inline-block' onclick='showSpeclist(\"" + groupName + "\")'>" + groupName + "</h4>" +
                //             "<a onclick=\"javascript:add('"+ groupName +"')\" style='display:inline-block'><i class='fa fa-plus'></i></a>" +
                //             "</li>";
                //         groupListStr += str;
                //     }
                // }
                // $("#proGroupList").html(groupListStr);

                // 修正：更新左侧分组列表，并清空右侧已分配列表（切换奖项后重新点击分组加载）
                let groupListStr = "";
                if(data.groupList) {
                    for(var i=0;i<data.groupList.length;i++) {
                        let groupName = data.groupList[i];
                        let str = "<li>" +
                            "<h4 style='display:inline-block; cursor:pointer;' onclick='showSpeclist(\"" + groupName + "\")'>" + groupName + "</h4>" +
                            "<a onclick=\"javascript:add('"+ groupName +"')\" style='display:inline-block; cursor:pointer; margin-left:5px;'><i class='fa fa-plus'></i></a>" +
                            "</li>";
                        groupListStr += str;
                    }
                }
                $("#proGroupList").html(groupListStr);
                // 清空右侧专家列表，切换奖项后需重新点击分组加载
                $("#list").html("");
                parent.layer.msg("已切换奖项，请点击左侧专业分组查看专家");
            } else {
                parent.layer.alert(data.msg)
            }

        }
    })

}


function getYearMonth() {
    var date = new Date;
    var year = date.getFullYear();
    var month = date.getMonth() + 1;
    month = (month < 10 ? "0" + month : month);
    return mydate = (year.toString() + month.toString());
}

/****
 * 添加专家
 * 分配（点击左侧 + 号添加新专家行）
 *   <input placeholder="开始日期" autocomplete="off" class="form-control layer-date" id="task_start_time">

 * **/
function add(e) {
    // 点击左侧专业组的"+"号时触发
    // 生成随机账号：202603_" + 随机 7 位字符
    // 动态生成 HTML 表格行，添加到右侧专家列表
    // 包含：专业名、账号、姓名、工作单位、银行账号、手机号等输入框

    this.selfProfession = e;

    var items = ['1', '2', '4', '5', '6', '7', '8', '9', '10', 'a', 'b', 'c', 'e', 'g', 's', '1', 'q', 'f', 'A'];

    var aindex = ($("#list").children().length + 1);

    var options = $("#assAwardType option:selected");  //获取选中的项

    var expend = options.val();


    if (expend.toString().length == 0) {
        parent.layer.msg("请选择奖项选择");
        return
    }


    console.log("----" + options.val() + "== " + e);

    // 原始实现：生成随机账号，onclick 未对 loginAccount 加引号
    // var raondom = getYearMonth() + expend + aindex + items[Math.floor(Math.random() * items.length)];
    // let loginAccount = raondom;
    // ... (原始 HTML 拼接省略，见注释上方)

    // 修正：生成随机账号时增加更多随机字符避免重复，onclick 对 loginAccount 字符串参数加引号
    var raondom = getYearMonth() + "_" + Math.random().toString(36).substr(2, 7);
    let loginAccount = raondom;

    console.log("----add loginAccount: " + loginAccount);

    // 动态生成一个新的专家信息行，添加到右侧专家列表中
    var html = " <tr id='tr_"+loginAccount+"'>" +
        "<td name='trNum' style='width:50px'>" + aindex + "</td>" +
        "<td style='width:50px'><input type='hidden' value='"+e+"' id='groupName"+loginAccount+"' /> " + e + "</td>" +
        "<td style='width:120px' id='accountId" + loginAccount + "'>" + loginAccount + "</td>" +
        "<td style='width:120px'>" +
        "  <input type='text' class='form-control' placeholder='专家名称' id='accountName" + loginAccount + "'/>" +
        "</td>" +
        "<td style='width:120px'><input id='accountCom" + loginAccount + "' class='form-control' type='text' placeholder='工作单位'></td>" +
        "<td style='width:120px'><input id='accountBank" + loginAccount + "' class='form-control' type='text' placeholder='银行账户'></td>" +
        "<td style='width:120px'><input id='accountPhone" + loginAccount + "' class='form-control' type='text' placeholder='手机号'></td>" +
        "<td id='signImg_"+loginAccount+"' style='width:120px'></td>" +
        "<td style='width:200px'>" +
        "  <button id='saveBtn_"+loginAccount+"' type='button' class='btn btn-primary btn-xs' onclick=\"save('" + loginAccount + "')\">保存</button>" +
        "  <button type='button' class='btn btn-primary btn-xs' onclick=\"onAddSign('" + loginAccount + "')\">上传签章</button>" +
        "  <button type='button' class='btn btn-primary btn-xs' onclick=\"remove('" + loginAccount + "')\">移出</button>" +
        "</td>" +
        "</tr>";
    $("#list").append(html);

    $(".i-checks").iCheck({checkboxClass: "icheckbox_square-green", radioClass: "iradio_square-green",})
}


/***
 移出专家
 * 删除某个条目（移出专家）
 * @param loginAccount 专家登录账号（字符串）
 *
 *  // 删除指定账号的专家行
 *  // 发送 AJAX 到 /scienceProgressScience/expert/remove
 *  // 删除后重新编号剩余行
 */
function remove(loginAccount) {
    // 原始实现：loginAccount未加引号传入，对于含字母的账号会导致JS错误
    // 修正：现在从HTML th:onclick 和 add() 中均传入带引号的字符串参数
    if(!loginAccount) {
        parent.layer.alert("账号为空，无法移出");
        return;
    }
    loginAccount = (loginAccount + '').trim();
    // 修正v2：layer.confirm 回调需接收 index 参数，并在操作完成后调用 layer.close(index) 关闭弹窗
    // 原始实现：回调未接收 index，导致确认弹窗无法关闭；async:false 在同步请求时也会阻塞UI
    layer.confirm('确定要移出该专家吗？', {
        btn: ['确定', '取消']
    }, function(index) {
        layer.close(index);
        $.ajax({
            cache: false,
            url: "/scienceProgressScience/expert/remove",
            type: 'POST',
            data: {
                loginAccount: loginAccount
            },
            async: true,
            error: function (request) {
                parent.layer.alert("Connection error");
            },
            success: function (data) {
                if (data.code == 0) {
                    parent.layer.msg("移出成功");
                    $("#tr_" + loginAccount).remove();
                    // 重新编号
                    $("td[name = 'trNum']").each(function (index, e) {
                        $(this).html(index + 1);
                    });
                } else {
                    parent.layer.alert(data.msg)
                }
            }
        });
    });
}


/***
 * 上传签章
 * 上传签章按钮点击事件
 * @param a 值为loginAccount（字符串）
 *
 * // 如果未保存，先调用 save() 保存
 * // 然后打开弹窗 /scienceProgressScience/toUploadExpertSign
 * // 上传专家电子签名图片
 */

function onAddSign(a) {
    // 原始实现：从 #accountId + a 获取 loginAccount，但 html() 可能带空格
    // 修正：trim() 处理，兼容服务端渲染和动态添加的行
    let taskId = $("#publishTaskId").val();
    var loginAccountEl = $("#accountId" + a);
    var loginAccount = loginAccountEl.length > 0 ? loginAccountEl.text().trim() : (a + '').trim();
    let saveBtnHtml = $("#saveBtn_" + a).text().trim();

    if(saveBtnHtml != "保存") {
        toUploadSignImg(taskId, loginAccount, a);
        return;
    }

    save(a, function () {
        toUploadSignImg(taskId, loginAccount, a);
    });

}
function toUploadSignImg(taskId, loginAccount, a) {
    // 原始实现保留，仅 trim loginAccount
    loginAccount = (loginAccount + '').trim();
    layer.open({
        title: '上传专家签名',
        maxmin: true,
        type: 2,
        shadeClose: false, // 点击遮罩关闭层
        area: ['800px', '520px'],
        content: '/scienceProgressScience/toUploadExpertSign?taskId='+taskId+'&loginAccount=' + encodeURIComponent(loginAccount) + "&trIndex="+ encodeURIComponent(a)
    });
}


/***
 * 保存专家（新增时先创建用户，再保存专家分组信息）
 * @param a loginAccount字符串
 * @param callback 保存成功后的回调
 *
 * // 第一步：创建用户账号
 * // POST 到 /sys/user/savepro
 * // 默认密码：123456，角色 ID: 62
 *
 * // 第二步：判断是否组长（第一行默认为组长）
 * // 第三步：调用 saveExpertInfo() 保存专家分组信息
 *
 *
 */
function save(a, callback) {
    // 原始实现：username 从 #accountId + a 的 html() 获取，可能带空格；
    //           isLeader 使用 .get(0).find() 导致报错（get返回DOM不是jQuery对象）
    // 修正：trim处理，isLeader 使用 jQuery .eq(0).find() 替代 .get(0).find()
    var accountIdEl = $("#accountId" + a);
    var username = accountIdEl.length > 0 ? accountIdEl.text().trim() : (a + '').trim();
    var fd = {
        "username": username,
        "password": "123456",
        "name": $("#accountName" + a).val(),
        "bankCard": $("#accountBank" + a).val(),
        "mobile": $("#accountPhone" + a).val(),
        "roleIds": "62",
        "status": "1",
        "accountCom": $("#accountCom" + a).val()
    };

    console.log("保存专家数据" + JSON.stringify(fd));

    $.ajax({
        cache: true,
        url: "/sys/user/savepro",
        type: 'POST',
        data: fd,
        async: false,
        error: function (request) {
            parent.layer.alert("Connection error");
        },
        success: function (data) {
            if (data.code == 0) {
                console.log(data.msg + " userid");
                // 原始实现：var isLeader = a == $("#list").find("tr").get(0).find("td").get(2).html();
                // BUG: .get(0) 返回 DOM 元素，没有 .find() 方法
                // 修正：使用 jQuery .eq(0) 替代 .get(0)
                var isLeader = false;
                try {
                    var firstTr = $("#list").find("tr").eq(0);
                    var firstAccount = firstTr.find("td").eq(2).text().trim();
                    isLeader = (username == firstAccount);
                } catch(e) {
                    isLeader = false;
                }
                var groupName = $("#groupName" + a).val();
                saveExpertInfo(data.msg, isLeader, groupName, fd, a, callback);
            } else {
                parent.layer.alert(data.msg)
            }
        }
    })
}

/***
 * 更新专家信息
 * @param a loginAccount字符串
 * @param id 专家记录ID
 *
 * // 修改已保存的专家信息
 * // POST 到 /scienceProgressScience/expert/add
 * // 更新：姓名、工作单位、银行账号、手机号等
 *
 */
function update(a, id) {
    // 原始实现：groupName 使用 .val() 但服务端渲染的是 <td> 而非 <input>，取值为 undefined
    // 修正：groupName <td> 中已添加 hidden input，.val() 现在可以正确取值
    //       同时添加 loginAccount 和 proType 参数，确保后端能正确定位记录
    var groupNameEl = $("#groupName" + a);
    // 兼容：如果是 hidden input 则用 val()，如果是 td 则用 text()
    var groupName = groupNameEl.is('input') ? groupNameEl.val() : groupNameEl.find('input[type=hidden]').val() || groupNameEl.text().trim();
    var accountName = $("#accountName" + a).val();
    var accountCom = $("#accountCom" + a).val();
    var accountBank = $("#accountBank" + a).val();
    var accountPhone = $("#accountPhone" + a).val();
    var loginAccount = $("#accountId" + a).length > 0 ? $("#accountId" + a).text().trim() : (a + '').trim();
    var assAwardType = $("#assAwardType").val();
    var taskId = $("#publishTaskId").val();

    var fd = {
        "id": id,
        "groupName": groupName,
        "company": accountCom,
        "bankAccount": accountBank,
        "phone": accountPhone,
        "expertName": accountName,
        "loginAccount": loginAccount,
        "proType": assAwardType,
        "taskId": taskId
    };
    console.log("更新专家数据：" + JSON.stringify(fd));

    $.ajax({
        cache: true,
        url: "/scienceProgressScience/expert/add",
        type: 'POST',
        data: fd,
        async: false,
        error: function (request) {
            parent.layer.alert("Connection error");
        },
        success: function (data) {
            if (data.code == 0) {
                parent.layer.msg("更新成功");
            } else {
                parent.layer.alert(data.msg)
            }
        }
    })
}

/***
 保存专家分组信息
 * 保存对应的专业信息/专业组
 * @param uid 用户ID
 * @param isLeader 是否组长
 * @param groupName 专业组名称
 * @param userFd 用户表单数据
 * @param trIndex 行标识（loginAccount）
 * @param callback 回调函数
 *
 * // 将专家与专业组绑定
 * // POST 到 /scienceProgressScience/expert/add
 * // 保存后将"保存"按钮改为"更新"按钮
 */
function saveExpertInfo(uid, isLeader, groupName, userFd, trIndex, callback) {
    console.log("== saveExpertInfo uid=" + uid + ", groupName=" + groupName);
    var assAwardType = $("#assAwardType").val();  //获取选中的项
    var loginAccountVal = userFd.username ? userFd.username.trim() : '';
    var fd = {
        "userName": groupName,
        "taskId": $("#publishTaskId").val(),
        "userId": uid,
        "proId": "0",
        "isGroupLeader": isLeader ? 1 : 0,
        "groupName": groupName,
        "company": userFd.accountCom,
        "bankAccount": userFd.bankCard,
        "phone": userFd.mobile,
        "expertName": userFd.name,
        "loginAccount": loginAccountVal,
        "proType": assAwardType
    };
    console.log("保存专家分组信息：" + JSON.stringify(fd));

    $.ajax({
        cache: true,
        url: "/scienceProgressScience/expert/add",
        type: 'POST',
        data: fd,
        async: false,
        error: function (request) {
            parent.layer.alert("Connection error");
        },
        success: function (data) {
            if (data.code == 0) {
                // 原始实现：使用 .onclick 绑定（对jQuery对象无效）
                // $("#saveBtn_" + trIndex).onclick = function(){ update(trIndex, data.id) };
                // 修正：使用 jQuery .attr('onclick') 替换按钮行为
                $("#saveBtn_" + trIndex).text("更新");
                $("#saveBtn_" + trIndex).attr('onclick', "update('" + trIndex + "'," + data.id + ")");
                if(callback) {
                    callback();
                } else {
                    parent.layer.msg("保存成功");
                }
            } else {
                parent.layer.alert(data.msg)
            }
        }
    })
}


/***
 * 查看专业组详情
 * @param major 专业名称
 *
 * // 点击左侧专业组名称时跳转
 * // 跳转到该专业组的专家分配详情页面
 */
function showSpeclist(major) {
    var assAwardType = $("#assAwardType").val();  //获取选中的项
    let taskId = $("#publishTaskId").val();
    window.location.href= "/scienceProgressScience/toAssignExperts?taskId=" + taskId + "&major=" + major + "&proType=" + assAwardType;
}