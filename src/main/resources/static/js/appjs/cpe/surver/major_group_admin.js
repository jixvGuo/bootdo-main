// 勘察奖 - 专业组管理（参考 QC 奖 major_group_admin.html）
// 仅打通：① 按任务加载专业组列表 ② 表格渲染 + 关键字搜索 ③ 新增/删除专家绑定
// 高阶功能（评分发布查询/回避/导出 等）留 TODO
// 勘察奖专业组管理页面

var SVR_PREFIX = "/cpe/suverProcess";
var CURRENT_GROUP_NAME = ""; // 空字符串 = "全部"

// 新增（v1.5，对齐 QC "添加→暂存→保存" 流程）
// PENDING_LOGINS：已点击"添加专家"但尚未落库的临时行（按 loginAccount 索引）
// EDITING_LOGINS：已存在但被切到 inline 编辑态的行（按 loginAccount 索引）
var PENDING_LOGINS = {};
var EDITING_LOGINS = {};

$(function () {
    initExpertTable();
    bindGroupListClick();
});

function getTaskId() { return $("#taskId").val(); }
function getProType() { return $("#proType").val() || "surver_pro_group"; }

function bindGroupListClick() {
    $("#majorGroupList").on("click", "li.list-group-item", function () {
        $("#majorGroupList li").removeClass("active");
        $(this).addClass("active");
        CURRENT_GROUP_NAME = $(this).data("group") || "";
        $("#currentGroupLabel").text(CURRENT_GROUP_NAME ? CURRENT_GROUP_NAME : "全部");
        reloadExperts();
    });
}

function initExpertTable() {
    // 新增：读取70角色标志，用于控制签章/工作单位列可见性（参考 QC isQcAssociationContactRole70）
    var IS_ROLE_70 = String($("#isQcAssociationContactRole70").val()) === 'true';
    // 新增：读取勘察奖小组联络人角色标志，用于控制驳回按钮可见性
    var IS_GROUP_CONTACT = String($("#isSurverGroupContactRole").val()) === 'true';
    // 管理员或小组联络人可操作驳回按钮
    var CAN_REJECT = IS_ROLE_70 || IS_GROUP_CONTACT;
    $("#expertTable").bootstrapTable({
        method: "get",
        url: SVR_PREFIX + "/surver_major_group/expert/list",
        striped: true,
        dataType: "json",
        pagination: true,
        pageSize: 10,
        showColumns: false,
        sidePagination: "client",
        // 新增：以 loginAccount 作唯一键，便于 prepend / removeByUniqueId / updateByUniqueId
        uniqueId: "loginAccount",
        rowAttributes: function (row) {
            return { 'data-login-account': row.loginAccount || '' };
        },
        responseHandler: function (res) {
            // 后端返回 { code:0, data:[...] }
            var list = (res && res.data) ? res.data : [];
            var key = ($("#searchKey").val() || "").trim().toLowerCase();
            if (key) {
                list = list.filter(function (r) {
                    return (r.loginAccount && r.loginAccount.toLowerCase().indexOf(key) >= 0)
                        || (r.expertName && r.expertName.toLowerCase().indexOf(key) >= 0);
                });
            }
            return list;
        },
        queryParams: function () {
            return {
                taskId: getTaskId(),
                groupName: CURRENT_GROUP_NAME
            };
        },
        // 原列定义（保留，仅注释）
        // columns: [
        //     { field: "id", title: "ID", visible: false },
        //     { field: "groupName", title: "专业组" },
        //     { field: "loginAccount", title: "登录账号" },
        //     { field: "expertName", title: "专家姓名" },
        //     { field: "phone", title: "手机号" },
        //     { field: "company", title: "工作单位" },
        //     { field: "isGroupLeader", title: "组长", formatter: function (v) { return v == 1 ? "是" : "否"; } },
        //     { title: "操作", field: "operation", align: "center",
        //       formatter: function (v, row) {
        //           return '<a class="btn btn-warning btn-sm" href="javascript:removeExpert(' + row.id + ')">移除</a>';
        //       }
        //     }
        // ]
        // 新列定义：追加"淘汰状态"列 + 操作列内新增"标记/撤销"按钮
        // 进一步：参考 QC，将"手机号"列改为"签章"预览列；操作列追加"上传签章"按钮
        columns: [
            { field: "id", title: "ID", visible: false },
            { field: "groupName", title: "专业组" },
            { field: "loginAccount", title: "登录账号" },
            // 原列定义（v3）：
            // { field: "expertName", title: "专家名称" },
            // 新列定义（v1.5）：暂存/编辑态展示 input；常态展示文本
            {
                field: "expertName", title: "专家名称",
                formatter: function (v, row) {
                    var la = row.loginAccount;
                    if (PENDING_LOGINS[la] || EDITING_LOGINS[la]) {
                        return '<input type="text" class="form-control input-sm" id="edit_name_' + la
                            + '" value="' + _surverEscapeHtml(v || '') + '" placeholder="专家名称" style="min-width:120px;">';
                    }
                    return _surverEscapeHtml(v || '');
                }
            },
            // 原列（保留，仅注释）：手机号
            // { field: "phone", title: "手机号" },
            // 新列：签章（图片预览，参考 QC 的 expertSignUrl 字段）
            // 原列定义（无角色可见性控制）：
            // {
            //     field: "expertSignUrl", title: "签章",
            //     formatter: function (v) {
            //         if (!v) return '<span class="text-muted">未上传</span>';
            //         return '<img src="' + v + '" width="75" height="40" style="object-fit:contain;border:1px solid #eee;" />';
            //     }
            // },
            // 新列定义：管理员（70角色）不可见签章列（参考 QC isQcAssociationContactRole70）
            {
                field: "expertSignUrl", title: "签章",
                visible: !IS_ROLE_70,
                formatter: function (v) {
                    if (!v) return '<span class="text-muted">未上传</span>';
                    return '<img src="' + v + '" width="75" height="40" style="object-fit:contain;border:1px solid #eee;" />';
                }
            },
            // 原列定义（v3）：
            // { field: "company", title: "工作单位" },
            // 新列定义（v1.5）：暂存/编辑态展示 input；常态展示文本
            // 原列定义（v1.5，无角色可见性控制）：
            // {
            //     field: "company", title: "工作单位",
            //     formatter: function (v, row) {
            //         var la = row.loginAccount;
            //         if (PENDING_LOGINS[la] || EDITING_LOGINS[la]) {
            //             return '<input type="text" class="form-control input-sm" id="edit_company_' + la
            //                 + '" value="' + _surverEscapeHtml(v || '') + '" placeholder="工作单位" style="min-width:120px;">';
            //         }
            //         return _surverEscapeHtml(v || '');
            //     }
            // },
            // 新列定义：管理员（70角色）不可见工作单位列（参考 QC isQcAssociationContactRole70）
            {
                field: "company", title: "工作单位",
                visible: !IS_ROLE_70,
                formatter: function (v, row) {
                    var la = row.loginAccount;
                    if (PENDING_LOGINS[la] || EDITING_LOGINS[la]) {
                        return '<input type="text" class="form-control input-sm" id="edit_company_' + la
                            + '" value="' + _surverEscapeHtml(v || '') + '" placeholder="工作单位" style="min-width:120px;">';
                    }
                    return _surverEscapeHtml(v || '');
                }
            },
            // {
            //     field: "isGroupLeader", title: "组长",
            //     formatter: function (v) { return v == 1 ? "是" : "否"; }
            // },

            // 淘汰提交展示列
            // {
            //     field: "eliminateOver", title: "淘汰提交",
            //     formatter: function (v) {
            //         return v == 1
            //             ? '<span class="label label-danger">已提交</span>'
            //             : '<span class="label label-default">未提交</span>';
            //     }
            // },


            // 原操作列（v3）：常态下展示 标记淘汰/上传签章/移除
            // {
            //     title: "操作", field: "operation", align: "center",
            //     formatter: function (v, row) {
            //         var cur = (row.eliminateOver == 1) ? 1 : 0;
            //         var toggleLabel = cur == 1 ? "撤销淘汰" : "标记淘汰";
            //         var toggleCls = cur == 1 ? "btn-default" : "btn-danger";
            //         var btnToggle = '<a class="btn ' + toggleCls + ' btn-sm" '
            //             + 'href="javascript:toggleEliminate(' + row.id + ',' + cur + ')">'
            //             + toggleLabel + '</a> ';
            //         var btnRemove = '<a class="btn btn-warning btn-sm" '
            //             + 'href="javascript:removeExpert(' + row.id + ')">移除</a> ';
            //         var btnSign = '';
            //         if (row.loginAccount) {
            //             btnSign = '<a class="btn btn-primary btn-sm" '
            //                 + 'href="javascript:onAddSign(\'' + row.loginAccount + '\',' + row.id + ')">上传签章</a> ';
            //         }
            //         return btnToggle + btnSign + btnRemove;
            //     }
            // }
            // 新操作列（v1.5）：根据行状态切换
            //   PENDING（点了添加专家，未保存）→ 保存 / 取消
            //   EDITING（点了编辑，未保存）   → 保存 / 取消
            //   常态                          → 编辑 / 标记淘汰 / 上传签章 / 移除
            {
                title: "操作", field: "operation", align: "center",
                formatter: function (v, row) {
                    var la = row.loginAccount;
                    if (PENDING_LOGINS[la]) {
                        return '<a class="btn btn-success btn-sm" href="javascript:savePendingExpertRow(\'' + la + '\')">保存</a> '
                             + '<a class="btn btn-default btn-sm" href="javascript:cancelPendingExpertRow(\'' + la + '\')">取消</a>';
                    }
                    if (EDITING_LOGINS[la]) {
                        return '<a class="btn btn-success btn-sm" href="javascript:saveExistingExpertRow(\'' + la + '\')">保存</a> '
                             + '<a class="btn btn-default btn-sm" href="javascript:cancelEditExpertRow()">取消</a>';
                    }
                    var cur = (row.eliminateOver == 1) ? 1 : 0;
                    // var toggleLabel = cur == 1 ? "撤销淘汰" : "标记淘汰";
                    var toggleCls   = cur == 1 ? "btn-default" : "btn-danger";
                    var btnEdit   = '<a class="btn btn-info btn-sm" href="javascript:editExpertRow(\'' + la + '\')">编辑</a> ';
                    // var btnToggle = '<a class="btn ' + toggleCls + ' btn-sm" '
                    //     + 'href="javascript:toggleEliminate(' + row.id + ',' + cur + ')">' + toggleLabel + '</a> ';
                    // var btnToggle = '<a class="btn ' + toggleCls + ' btn-sm" '
                    //     + 'href="javascript:toggleEliminate(' + row.id + ',' + cur + ')">' + '</a> ';
                    var btnSign = '';
                    if (la) {
                        btnSign = '<a class="btn btn-primary btn-sm" '
                            + 'href="javascript:onAddSign(\'' + la + '\',' + row.id + ')">上传签章</a> ';
                    }
                    var btnRemove = '<a class="btn btn-warning btn-sm" '
                        + 'href="javascript:removeExpert(' + row.id + ')">移除</a> ';
                    // 勘察奖回避按钮（参考 QC 奖 manageAvoidance）
                    var btnAvoidance = '';
                    if (row.userId) {
                        btnAvoidance = '<a class="btn btn-danger btn-sm" '
                            + 'href="javascript:surverManageAvoidance(\''
                            + la + '\','
                            + row.userId
                            + ',\''
                            + _surverEscapeHtml(row.expertName || la)
                            + '\')">'
                            + '回避</a> ';
                    }
                    // 驳回淘汰按钮（管理员/小组联络人可操作，驳回专家的淘汰确认提交）
                    var btnRejectElim = '';
                    if (CAN_REJECT && row.userId) {
                        btnRejectElim = '<a class="btn btn-default btn-sm" '
                            + 'href="javascript:rejectEliminateSubmit('
                            + row.userId
                            + ',\''
                            + _surverEscapeHtml(row.expertName || la)
                            + '\')">'
                            + '驳回淘汰</a> ';
                    }
                    // 驳回打分按钮（管理员/小组联络人可操作，驳回专家的打分确认）
                    var btnRejectScore = '';
                    if (CAN_REJECT && row.userId) {
                        btnRejectScore = '<a class="btn btn-warning btn-sm" '
                            + 'href="javascript:rejectScoringConfirm('
                            + row.userId
                            + ',\''
                            + _surverEscapeHtml(row.expertName || la)
                            + '\')">'
                            + '驳回打分</a> ';
                    }
                    // return btnEdit + btnToggle + btnSign + btnRemove;
                    return btnEdit + btnSign + btnRemove + btnAvoidance + btnRejectElim + btnRejectScore;
                }
            }
        ]
    });
}

function reloadExperts() {
    $("#expertTable").bootstrapTable("refresh");
}

/**
 * 原方案（v1.4，已注释保留）：
 *   "添加专家" 按钮 → 自动生成账号后立即调 /sys/user/savepro + /expert/save 直接落库。
 *   缺点：用户来不及填姓名/单位就提交，落库的 expertName 是占位值（=账号），需要再编辑。
 *
 * function openAddExpertDialog() {
 *     var taskId = getTaskId();
 *     var groupName = CURRENT_GROUP_NAME;
 *     if (!groupName) {
 *         layer.msg("请先在左侧选择具体的专业组（不能是『全部』）", { icon: 0 });
 *         return;
 *     }
 *     var loginAccount = generateSurverExpertLoginAccount();
 *     var loadIdx = layer.load(1, { shade: [0.3, "#000"] });
 *     var userFd = { username: loginAccount, password: "123456", name: loginAccount,
 *                    mobile: "", roleIds: "76", status: "1", accountCom: "" };
 *     $.ajax({ url: "/sys/user/savepro", type: "POST", data: userFd, success: function (r) { ... } });
 * }
 *
 * 新方案（v1.5，与 QC `add(major)` 完全对齐）：
 *   "添加专家" 按钮 → 仅前端 prepend 一行 "暂存"（PENDING）行：
 *     · 账号自动生成（YYYYMM_xxxxxxx，与 QC 同规则）
 *     · 专家名称 / 工作单位 渲染为 input（formatter 根据 PENDING_LOGINS 切换）
 *     · 操作列展示 [保存] [取消]
 *   用户填好点 [保存] 才真正调 /sys/user/savepro + /expert/save 落库。
 *   用户点 [取消] 则只从前端表格移除这一行，无后端副作用。
 *
 *   配套：常态行新增 [编辑] 按钮 → 切到 EDITING 态（同样的 input + [保存][取消]）。
 */
function openAddExpertDialog() {
    var groupName = CURRENT_GROUP_NAME;
    if (!groupName) {
        layer.msg("请先在左侧选择具体的专业组（不能是『全部』）", { icon: 0 });
        return;
    }
    var loginAccount = generateSurverExpertLoginAccount();
    if (PENDING_LOGINS[loginAccount]) {
        layer.msg("账号冲突，请重试", { icon: 2 });
        return;
    }
    PENDING_LOGINS[loginAccount] = true;
    var tentative = {
        id: 0,
        loginAccount: loginAccount,
        groupName: groupName,
        expertName: "",
        company: "",
        phone: "",
        isGroupLeader: 0,
        eliminateOver: 0,
        expertSignUrl: null
    };
    $("#expertTable").bootstrapTable("prepend", tentative);
    layer.msg("已生成账号：" + loginAccount + "，请填写姓名/单位后点击『保存』", { icon: 1, time: 2500 });
    setTimeout(function () {
        var $name = $("#edit_name_" + loginAccount);
        if ($name.length) { $name.focus(); }
    }, 50);
}

// ---- v1.5 暂存/编辑/保存/取消 行操作 ----

/**
 * 保存暂存中的新专家（PENDING）：先 /sys/user/savepro 创建账号，再 /expert/save 绑定专业组
 */
function savePendingExpertRow(loginAccount) {
    // 原代码（v1.5）：专家姓名必填
    // var name = ($("#edit_name_" + loginAccount).val() || "").trim();
    // if (!name) { layer.msg("请填写专家姓名", { icon: 0 }); return; }
    // 新代码（v1.6）：专家姓名可选（与 QC 一致，可以后续补充）
    var name = ($("#edit_name_" + loginAccount).val() || "").trim();
    var company = ($("#edit_company_" + loginAccount).val() || "").trim();
    var row = $("#expertTable").bootstrapTable("getRowByUniqueId", loginAccount);
    if (!row) { layer.msg("行已失效，请重新添加", { icon: 2 }); return; }
    _persistExpert({
        loginAccount:  loginAccount,
        groupName:     row.groupName,
        expertName:    name,
        company:       company,
        isGroupLeader: row.isGroupLeader || "0",
        isNew:         true
    });
}

/**
 * 取消暂存：仅前端移除这一行，无后端调用
 */
function cancelPendingExpertRow(loginAccount) {
    delete PENDING_LOGINS[loginAccount];
    $("#expertTable").bootstrapTable("removeByUniqueId", loginAccount);
}

/**
 * 把已存在的行切到 inline 编辑态（输入框 + 保存/取消）
 */
function editExpertRow(loginAccount) {
    EDITING_LOGINS[loginAccount] = true;
    // 原代码（v1.5）：updateByUniqueId 在本站用的 bootstrap-table 版本里不存在（需 v1.11+），会报 Unknown method
    // var row = $("#expertTable").bootstrapTable("getRowByUniqueId", loginAccount);
    // if (row) {
    //     $("#expertTable").bootstrapTable("updateByUniqueId", { id: loginAccount, row: row });
    // }
    // 新代码（v1.6）：取出当前内存数据再灬回去，触发整表重渲染——formatter 会重新读取 EDITING_LOGINS 状态。
    // 此举会保留 PENDING 行（它们已在 getData() 的返回中），也不会重拉服务端。
    var data = $("#expertTable").bootstrapTable("getData") || [];
    $("#expertTable").bootstrapTable("load", data);
    setTimeout(function () {
        var $name = $("#edit_name_" + loginAccount);
        if ($name.length) { $name.focus(); }
    }, 50);
}

/**
 * 保存编辑中的已存在行：upsert 路径与新增完全一致（后端两个接口都按唯一键 upsert）
 */
function saveExistingExpertRow(loginAccount) {
    // 原代码（v1.5）：专家姓名必填
    // var name = ($("#edit_name_" + loginAccount).val() || "").trim();
    // if (!name) { layer.msg("请填写专家姓名", { icon: 0 }); return; }
    // 新代码（v1.6）：专家姓名可选
    var name = ($("#edit_name_" + loginAccount).val() || "").trim();
    var company = ($("#edit_company_" + loginAccount).val() || "").trim();
    var row = $("#expertTable").bootstrapTable("getRowByUniqueId", loginAccount);
    if (!row) { layer.msg("行已失效，请刷新", { icon: 2 }); return; }
    _persistExpert({
        loginAccount:  loginAccount,
        groupName:     row.groupName,
        expertName:    name,
        company:       company,
        isGroupLeader: row.isGroupLeader || "0",
        isNew:         false
    });
}

/**
 * 取消所有编辑（清空 EDITING_LOGINS，整张表刷新；不影响 PENDING 行）
 */
function cancelEditExpertRow() {
    EDITING_LOGINS = {};
    // 原代码（v1.5）：reloadExperts() 会重拉服务端，丢掉同页未保存的 PENDING 行
    // reloadExperts();
    // 新代码（v1.6）：仅重渲染当前内存数据，保留 PENDING 行
    var data = $("#expertTable").bootstrapTable("getData") || [];
    $("#expertTable").bootstrapTable("load", data);
}

/**
 * 落库：/sys/user/savepro（按 username upsert，role=76 勘察设计奖评审专家）→ /expert/save（按三元组 upsert）
 */
function _persistExpert(opts) {
    var loadIdx = layer.load(1, { shade: [0.3, "#000"] });
    var userFd = {
        username:   opts.loginAccount,
        password:   "123456",
        name:       opts.expertName,
        mobile:     "",
        roleIds:    "76",
        status:     "1",
        accountCom: opts.company || ""
    };
    $.ajax({
        cache: true,
        url:   "/sys/user/savepro",
        type:  "POST",
        data:  userFd,
        success: function (r) {
            if (!r || r.code !== 0) {
                layer.close(loadIdx);
                layer.msg((r && r.msg) ? ("账号保存失败：" + r.msg) : "账号保存失败", { icon: 2 });
                return;
            }
            var userId = r.msg;
            var bindFd = {
                taskId:        getTaskId(),
                proType:       getProType(),
                groupName:     opts.groupName,
                loginAccount:  opts.loginAccount,
                userId:        userId,
                userName:      opts.expertName,
                expertName:    opts.expertName,
                company:       opts.company || "",
                phone:         "",
                isGroupLeader: opts.isGroupLeader || "0"
            };
            $.ajax({
                cache: true,
                url:   SVR_PREFIX + "/surver_major_group/expert/save",
                type:  "POST",
                data:  bindFd,
                success: function (r2) {
                    layer.close(loadIdx);
                    if (r2 && r2.code === 0) {
                        if (opts.isNew) {
                            delete PENDING_LOGINS[opts.loginAccount];
                            layer.msg("已新增专家：" + opts.loginAccount + "（默认密码 123456）", { icon: 1, time: 2500 });
                        } else {
                            delete EDITING_LOGINS[opts.loginAccount];
                            layer.msg("已更新：" + opts.expertName, { icon: 1 });
                        }
                        reloadExperts();
                    } else {
                        layer.msg((r2 && r2.msg) ? r2.msg : "绑定专业组失败", { icon: 2 });
                    }
                },
                error: function () {
                    layer.close(loadIdx);
                    layer.msg("绑定专业组请求失败", { icon: 2 });
                }
            });
        },
        error: function () {
            layer.close(loadIdx);
            layer.msg("账号保存请求失败", { icon: 2 });
        }
    });
}

// 改造（与 QC 旧勘察奖添加专家保持一致）：
//   1) 登录账号留空时前端自动生成（年月日 + 随机后缀，避免冲突）
//   2) 先调 /sys/user/savepro 创建 sys_user 并赋勘察评审专家角色（76），返回 userId
//   3) 再调 /cpe/suverProcess/surver_major_group/expert/save 写 add_special_info 完成绑定
//   4) 全程一次点击搞定，管理员只需填"专家姓名"，其它字段可选
function submitAddExpert() {
    var $scope = $(".layui-layer-content");
    var groupName    = $scope.find("#dlg_groupName").val();
    var loginAccount = ($scope.find("#dlg_loginAccount").val() || "").trim();
    var expertName   = ($scope.find("#dlg_expertName").val() || "").trim();
    var company      = ($scope.find("#dlg_company").val() || "").trim();
    var phone        = ($scope.find("#dlg_phone").val() || "").trim();
    var isLeader     = $scope.find("#dlg_isGroupLeader").val() || "0";

    if (!groupName)  { layer.msg("请选择专业组"); return; }
    if (!expertName) { layer.msg("请输入专家名称"); return; }
    if (!loginAccount) {
        loginAccount = generateSurverExpertLoginAccount();
    }

    // Step 1: 创建/更新 sys_user（roleId=76 勘察设计奖评审专家）
    var userFd = {
        username:   loginAccount,
        password:   "123456",
        name:       expertName,
        mobile:     phone,
        roleIds:    "76",
        status:     "1",
        accountCom: company
    };
    var loadIdx = layer.load(1, { shade: [0.3, "#000"] });
    $.ajax({
        cache: true,
        url:   "/sys/user/savepro",
        type:  "POST",
        data:  userFd,
        success: function (r) {
            if (!r || r.code !== 0) {
                layer.close(loadIdx);
                layer.msg((r && r.msg) ? ("创建账号失败：" + r.msg) : "创建账号失败", { icon: 2 });
                return;
            }
            // savepro 把 userId 放在 msg 字段里（"" + userId）
            var userId = r.msg;

            // Step 2: 写 add_special_info 绑定到专业组
            var bindFd = {
                taskId:        getTaskId(),
                proType:       getProType(),
                groupName:     groupName,
                loginAccount:  loginAccount,
                userId:        userId,
                userName:      expertName,
                expertName:    expertName,
                company:       company,
                phone:         phone,
                isGroupLeader: isLeader
            };
            $.ajax({
                cache: true,
                url:   SVR_PREFIX + "/surver_major_group/expert/save",
                type:  "POST",
                data:  bindFd,
                success: function (r2) {
                    layer.close(loadIdx);
                    if (r2 && r2.code === 0) {
                        layer.msg("已添加专家：" + loginAccount + " / " + expertName, { icon: 1 });
                        layer.closeAll();
                        reloadExperts();
                    } else {
                        layer.msg((r2 && r2.msg) ? r2.msg : "绑定专业组失败", { icon: 2 });
                    }
                },
                error: function () {
                    layer.close(loadIdx);
                    layer.msg("绑定专业组请求失败", { icon: 2 });
                }
            });
        },
        error: function () {
            layer.close(loadIdx);
            layer.msg("创建账号请求失败", { icon: 2 });
        }
    });
}

/**
 * 生成勘察奖专家登录账号
 * 与 QC 添加专家保持完全一致的生成规则（参考 cpe/qc/score/major_group_admin.html getYearMonth + 36 进制随机）
 * 形如：202604_a3xk2yp  （年月 6 位 + 下划线 + 7 位 36 进制随机）
 */
function generateSurverExpertLoginAccount() {
    return getSurverYearMonth() + "_" + Math.random().toString(36).substr(2, 7);
}

function getSurverYearMonth() {
    var date = new Date();
    var year = date.getFullYear();
    var month = date.getMonth() + 1;
    month = month < 10 ? "0" + month : month;
    return year.toString() + month.toString();
}
// 移除勘察专家
function removeExpert(id) {
    layer.confirm("确定要移除该专家吗？", { btn: ["确定", "取消"] }, function () {
        $.ajax({
            url: SVR_PREFIX + "/surver_major_group/expert/remove",
            type: "post",
            data: { id: id },
            success: function (r) {
                if (r && r.code == 0) {
                    layer.msg("已移除");
                    reloadExperts();
                } else {
                    layer.msg(r && r.msg ? r.msg : "移除失败");
                }
            }
        });
        layer.closeAll("dialog");
    });
}

// =========================================================================
// 新增：淘汰（eliminate_over）相关功能
// - toggleEliminate(id, cur): 单条翻转/设置当前专家的"淘汰已提交"状态
// - resetAllEliminate():      本任务下所有勘察奖专业组专家的淘汰状态重置为 0
// 作用域：仅影响 pro_type=surver_pro_group 的 add_special_info 记录
// 后续如需 per-project 淘汰明细，可在此扩展（参考 QC: ass_qc_expert_eliminate）
// =========================================================================
function toggleEliminate(id, curEliminateOver) {
    var next = curEliminateOver == 1 ? 0 : 1;
    var tip = next == 1
        ? "确定要标记该专家的淘汰名单为【已提交】吗？"
        : "确定要撤销该专家的淘汰【已提交】状态吗？";
    layer.confirm(tip, { btn: ["确定", "取消"], icon: 3, title: "淘汰状态" }, function (idx) {
        layer.close(idx);
        $.ajax({
            url: SVR_PREFIX + "/surver_major_group/expert/toggleEliminateOver",
            type: "post",
            data: { id: id, eliminateOver: next },
            success: function (r) {
                if (r && r.code == 0) {
                    layer.msg(next == 1 ? "已标记为已提交" : "已撤销");
                    reloadExperts();
                } else {
                    layer.msg(r && r.msg ? r.msg : "操作失败");
                }
            }
        });
    });
}

function resetAllEliminate() {
    var taskId = getTaskId();
    if (!taskId) { layer.msg("任务 ID 为空"); return; }
    layer.confirm(
        "确定要重置【当前任务】下所有勘察奖专业组专家的淘汰提交状态吗？<br/>"
        + "（仅影响 pro_type=surver_pro_group 的记录；已物理删除的绑定不受影响）",
        { btn: ["确定", "取消"], icon: 3, title: "批量重置淘汰状态" },
        function (idx) {
            layer.close(idx);
            $.ajax({
                url: SVR_PREFIX + "/surver_major_group/expert/resetEliminateOver",
                type: "post",
                data: { taskId: taskId },
                success: function (r) {
                    if (r && r.code == 0) {
                        layer.msg("已重置 " + (r.count || 0) + " 条记录");
                        reloadExperts();
                    } else {
                        layer.msg(r && r.msg ? r.msg : "重置失败");
                    }
                }
            });
        }
    );
}

// =========================================================================
// 新增：管理员驳回专家的淘汰确认提交（删除快照表记录，保留活动表评级数据）
// =========================================================================
function rejectEliminateSubmit(expertUserId, expertName) {
    var taskId = getTaskId();
    if (!taskId) { layer.msg("任务 ID 为空"); return; }
    layer.confirm(
        "确定要驳回专家【" + (expertName || expertUserId) + "】的淘汰确认提交吗？<br/>"
        + "驳回后该专家可重新修改评级，已有评级数据不会被清空。",
        { btn: ["确定", "取消"], icon: 3, title: "驳回淘汰提交" },
        function (idx) {
            layer.close(idx);
            $.ajax({
                url: SVR_PREFIX + "/eliminate/admin/rejectConfirmSubmit",
                type: "post",
                data: { taskId: taskId, expertUid: expertUserId },
                success: function (r) {
                    if (r && r.code == 0) {
                        layer.msg(r.msg || "已驳回");
                        reloadExperts();
                    } else {
                        layer.msg(r && r.msg ? r.msg : "驳回失败");
                    }
                }
            });
        }
    );
}

// =========================================================================
// 新增：管理员驳回专家的打分确认（重置打分确认状态，让专家可重新修改打分）
// =========================================================================
function rejectScoringConfirm(expertUserId, expertName) {
    var taskId = getTaskId();
    if (!taskId) { layer.msg("任务 ID 为空"); return; }
    layer.confirm(
        "确定要驳回专家【" + (expertName || expertUserId) + "】的打分确认吗？<br/>"
        + "驳回后该专家可重新修改打分，已有打分数据不会被清空。",
        { btn: ["确定", "取消"], icon: 3, title: "驳回打分确认" },
        function (idx) {
            layer.close(idx);
            $.ajax({
                url: "/surverScore/rejectScoringConfirm",
                type: "post",
                data: { taskId: taskId, expertUid: expertUserId },
                success: function (r) {
                    if (r && r.code == 0) {
                        layer.msg(r.msg || "已驳回");
                        reloadExperts();
                    } else {
                        layer.msg(r && r.msg ? r.msg : "驳回失败");
                    }
                }
            });
        }
    );
}

// =========================================================================
// 新增：导出分数功能（管理员和小组联络人可用）
// 管理员可以选择导出全部或仅未淘汰的分数
// 小组联络人只能导出本组未淘汰的分数
// =========================================================================
function exportScore() {
    var taskId = getTaskId();
    if (!taskId) { layer.msg("任务 ID 为空"); return; }

    // 读取角色标志
    var IS_ROLE_70 = String($("#isQcAssociationContactRole70").val()) === 'true';
    var IS_GROUP_CONTACT = String($("#isSurverGroupContactRole").val()) === 'true';

    // 如果是管理员（70角色），弹出选择框让用户选择导出类型
    if (IS_ROLE_70) {
        layer.open({
            type: 1,
            title: '导出分数',
            area: ['400px', '200px'],
            content: '<div style="padding:20px;">'
                + '<p>请选择导出类型：</p>'
                + '<div style="margin-top:10px;">'
                + '<label style="margin-right:15px;"><input type="radio" name="exportType" value="all" checked> 导出全部</label>'
                + '<label><input type="radio" name="exportType" value="eliminated"> 仅导出已淘汰</label>'
                + '</div>'
                + '</div>',
            btn: ['确定', '取消'],
            yes: function(index) {
                var exportType = $('input[name="exportType"]:checked').val();
                var showEliminated = exportType === 'eliminated';
                _doExportScore(taskId, CURRENT_GROUP_NAME, showEliminated);
                layer.close(index);
            }
        });
    } else {
        // 小组联络人或其他角色，直接导出未淘汰的分数
        _doExportScore(taskId, CURRENT_GROUP_NAME, false);
    }
}

function _doExportScore(taskId, groupName, showEliminated) {
    var url = '/surverScore/exportScore?taskId=' + encodeURIComponent(taskId);
    if (groupName) {
        url += '&groupName=' + encodeURIComponent(groupName);
    }
    url += '&showEliminated=' + showEliminated;
    window.location.href = url;
}

// =========================================================================
// 新增：上传专家签章（参考 QC onAddSign / qc_expert_sign_upload.html）
// 用法：行内"上传签章"按钮 → 打开 layer.iframe → 上传成功后回调 onSignUploaded
// =========================================================================
function onAddSign(loginAccount, rowId) {
    var taskId = getTaskId();
    if (!loginAccount) {
        layer.msg("登录账号为空");
        return;
    }
    layer.open({
        title: '上传专家签章',
        maxmin: true,
        type: 2,
        shadeClose: false,
        area: ['800px', '520px'],
        // 注意：新增 trIndex 使用行 id，便于 onSignUploaded 找回
        content: '/cpe/suverProcess/toUploadExpertSign?taskId=' + taskId
            + '&loginAccount=' + encodeURIComponent(loginAccount)
            + '&trIndex=' + (rowId || '')
    });
}

// 子页面 (surver_expert_sign_upload.html) 上传成功后回调本页面，刷新表格使签章预览出现
function onSignUploaded(/*trIndex*/ _ti, /*fileUrl*/ _url) {
    reloadExperts();
}

// =========================================================================
// 新增：勘察奖"小组联络人绑定"弹窗（参考 QC openExpertBindingModal）
// 仅当协会领导/勘察奖协会联系人能看到入口按钮（HTML 处已用 th:if 控制）
// 数据存放：add_special_info, proType=surver_view_scope
// =========================================================================
function _getSurverGroupNames() {
    var names = [];
    $('#majorGroupList li').each(function () {
        var g = $(this).data('group');
        if (g) names.push(String(g).trim());
    });
    return names;
}

function _surverEscapeHtml(str) {
    if (str == null) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

function openContactBindingModal() {
    var taskId = getTaskId();
    if (!taskId) { layer.msg('任务ID为空', { icon: 2 }); return; }
    var loadIdx = layer.load(1);
    $.ajax({
        type: 'GET',
        url: '/cpe/suverProcess/getSurverContactGroupBindings',
        data: { taskId: taskId },
        success: function (data) {
            layer.close(loadIdx);
            if (data && data.code == 0) {
                _renderContactBindingModal(data.data || [], taskId, data.groups || []);
            } else {
                layer.msg((data && data.msg) || '加载失败', { icon: 2 });
            }
        },
        error: function () {
            layer.close(loadIdx);
            layer.msg('加载失败，请稍后重试', { icon: 2 });
        }
    });
}

function _renderContactBindingModal(contacts, taskId, groups) {
    // groups 改为从后端实时返回（不再从 DOM 快照读取，避免数据不一致）
    if (!groups || groups.length === 0) groups = _getSurverGroupNames();
    var html = '<div style="padding:10px;min-width:600px;">';
    html += '<p style="color:#888;margin-bottom:12px;">为每位「勘察奖小组联络人」选择可查看的专业组（不选则该联络人当前任务下无可见专家组）</p>';
    if (!contacts || contacts.length == 0) {
        html += '<p style="color:#999;">暂无「勘察奖小组联络人」用户。请先在系统-用户管理中给用户分配该角色。</p>';
    } else {
        html += '<table class="table table-bordered table-striped" style="margin-bottom:0;">';
        html += '<thead><tr><th style="width:80px;">姓名</th><th style="width:120px;">账号</th><th>绑定专业组（多选）</th><th style="width:70px;">操作</th></tr></thead><tbody>';
        for (var i = 0; i < contacts.length; i++) {
            var u = contacts[i];
            html += '<tr>';
            html += '<td>' + _surverEscapeHtml(u.name || '-') + '</td>';
            html += '<td>' + _surverEscapeHtml(u.username || '-') + '</td>';
            html += '<td style="padding:6px 8px;">';
            for (var j = 0; j < groups.length; j++) {
                var g = groups[j];
                var checked = (u.boundGroups && u.boundGroups.indexOf(g) >= 0) ? 'checked' : '';
                html += '<label style="margin-right:12px;font-weight:normal;">'
                    + '<input type="checkbox" class="surver-grp-chk" data-uid="' + u.userId + '" '
                    + 'value="' + _surverEscapeHtml(g) + '" ' + checked + '> '
                    + _surverEscapeHtml(g) + '</label>';
            }
            if (groups.length == 0) {
                html += '<span style="color:#999;">（当前任务暂无专业组，请先维护"专家分组管理"）</span>';
            }
            html += '</td>';
            html += '<td><button class="btn btn-primary btn-xs" onclick="_saveContactBinding(\'' + u.userId + '\', \'' + taskId + '\')">保存</button></td>';
            html += '</tr>';
        }
        html += '</tbody></table>';
    }
    html += '</div>';

    layer.open({
        type: 1,
        title: '勘察奖小组联络人 - 专业组绑定',
        area: ['820px', '520px'],
        content: html,
        btn: ['关闭']
    });
}

function _saveContactBinding(expertUserId, taskId) {
    var checked = [];
    $('input.surver-grp-chk[data-uid="' + expertUserId + '"]:checked').each(function () {
        checked.push($(this).val());
    });
    var groupNames = checked.join(',');
    $.ajax({
        type: 'POST',
        url: '/cpe/suverProcess/saveSurverContactGroupBinding',
        data: { taskId: taskId, expertUserId: expertUserId, groupNames: groupNames },
        success: function (data) {
            if (data && data.code == 0) {
                layer.msg('绑定保存成功', { icon: 1 });
            } else {
                layer.msg((data && data.msg) || '保存失败', { icon: 2 });
            }
        },
        error: function () { layer.msg('保存失败，请稍后重试', { icon: 2 }); }
    });
}

// =========================================================================
// 勘察奖回避管理（参考 QC 奖 major_group_admin.html 的 manageAvoidance）
// =========================================================================
var _surverAvoidCurrentExpertUserId = null;
var _surverAvoidCurrentLoginAccount = null;
var _surverAvoidAllProjects = [];
var _surverAvoidFilteredProjects = [];
var _surverAvoidCurrentPage = 1;
var _surverAvoidPageSize = 10;

/**
 * 打开回避管理弹窗
 */
function surverManageAvoidance(loginAccount, expertUserId, expertName) {
    var taskId = getTaskId();
    if (!expertUserId || expertUserId === 'null' || expertUserId === '') {
        layer.alert("无法获取专家用户ID，请先保存专家信息");
        return;
    }
    _surverAvoidCurrentExpertUserId = expertUserId;
    _surverAvoidCurrentLoginAccount = loginAccount;

    var $modal = $('#surverAvoidanceModal');
    var modalHtml = $modal.html();
    $modal.empty();

    layer.open({
        type: 1,
        title: '专家回避管理 - ' + (expertName || loginAccount),
        area: ['1100px', '600px'],
        content: '<div style="padding: 20px;">' + modalHtml + '</div>',
        success: function () {
            _surverAvoidRefresh(taskId);
        },
        end: function () {
            $modal.html(modalHtml);
        }
    });
}

function _surverAvoidRefresh(taskId) {
    _surverAvoidLoadProjects(taskId, _surverAvoidCurrentExpertUserId);
}

/**
 * 加载专家已分配的项目列表（含回避状态）
 */
function _surverAvoidLoadProjects(taskId, expertUserId) {
    console.log('[勘察回避] 加载项目: taskId=' + taskId + ', expertUserId=' + expertUserId);
    $("#surverAvoidanceTableBody").html('<tr><td colspan="8" class="text-center">加载中...</td></tr>');
    $.ajax({
        type: "GET",
        url: SVR_PREFIX + "/avoidance/expertProjects",
        data: { taskId: taskId, expertUserId: expertUserId },
        timeout: 30000,
        success: function (data) {
            console.log('[勘察回避] 返回数据:', data);
            if (data && data.code == 0) {
                _surverAvoidAllProjects = data.rows || [];
                // _surverAvoidRenderTable(_surverAvoidAllProjects);
                _surverAvoidFilteredProjects = data.rows || [];
                _surverAvoidCurrentPage = 1;
                _surverAvoidRenderTableWithPagination();
            } else {
                _surverAvoidAllProjects = [];
                $("#surverAvoidanceTableBody").html('<tr><td colspan="7" class="text-center">' + (data && data.msg ? data.msg : '加载失败') + '</td></tr>');
            }
        },
        error: function (xhr, status, err) {
            console.error('[勘察回避] 请求失败:', status, err);
            $("#surverAvoidanceTableBody").html('<tr><td colspan="7" class="text-center text-danger">请求失败: ' + (err || status) + '</td></tr>');
        }
    });
}

/**
 * 渲染回避表格
 */
function _surverAvoidRenderTable(projects) {
    var tbody = $("#surverAvoidanceTableBody");
    tbody.empty();
    if (!projects || projects.length === 0) {
        tbody.html('<tr><td colspan="8" class="text-center">暂无分配项目</td></tr>');
        return;
    }
    projects.forEach(function (p) {
        var statusText = _surverAvoidGetStatText(p.proStat);
        var isAvoided = p.isAvoided || false;
        var avoidanceStatus = isAvoided
            ? '<span class="label label-warning">已回避</span>'
            : '<span class="label label-success">正常</span>';
        var operationBtn = '';
        if (isAvoided) {
            operationBtn = '<button class="btn btn-sm btn-success" onclick="surverCancelAvoidance(' + p.proId + ')">取消回避</button>';
        } else {
            operationBtn = '<button class="btn btn-sm btn-warning" onclick="surverSetAvoidance(' + p.proId + ')">设为回避</button>';
        }
        var row = '<tr>'
            + '<td>' + _surverEscapeHtml(p.topicName || '-') + '</td>'
            + '<td>' + _surverEscapeHtml(p.proCode || '-') + '</td>'
            + '<td>' + _surverEscapeHtml(p.applyId || '-') + '</td>'
            + '<td>' + _surverEscapeHtml(p.unitName || '-') + '</td>'
            + '<td>' + _surverEscapeHtml(p.groupDesc || '-') + '</td>'
            + '<td>' + statusText + '</td>'
            + '<td>' + avoidanceStatus + '</td>'
            + '<td>' + operationBtn + '</td>'
            + '</tr>';
        tbody.append(row);
    });
}
/**
 * 分页版渲染：先切片再渲染表格，然后更新分页控件
 */
function _surverAvoidRenderTableWithPagination() {
    var totalRecords = _surverAvoidFilteredProjects.length;
    var totalPages = Math.ceil(totalRecords / _surverAvoidPageSize);
    if (_surverAvoidCurrentPage > totalPages && totalPages > 0) {
        _surverAvoidCurrentPage = totalPages;
    }
    var startIndex = (_surverAvoidCurrentPage - 1) * _surverAvoidPageSize;
    var endIndex = Math.min(startIndex + _surverAvoidPageSize, totalRecords);
    var currentPageData = _surverAvoidFilteredProjects.slice(startIndex, endIndex);
    _surverAvoidRenderTable(currentPageData);
    _surverAvoidUpdatePagination(totalRecords);
}
function _surverAvoidUpdatePagination(totalRecords) {
    var totalPages = Math.ceil(totalRecords / _surverAvoidPageSize);
    var startIndex = totalRecords > 0 ? (_surverAvoidCurrentPage - 1) * _surverAvoidPageSize + 1 : 0;
    var endIndex = Math.min(_surverAvoidCurrentPage * _surverAvoidPageSize, totalRecords);
    $("#surverAvoidPageStart").text(startIndex);
    $("#surverAvoidPageEnd").text(endIndex);
    $("#surverAvoidPageTotal").text(totalRecords);
    var paginationHtml = '';
    paginationHtml += '<li class="' + (_surverAvoidCurrentPage <= 1 ? 'disabled' : '') + '">';
    if (_surverAvoidCurrentPage > 1) {
        paginationHtml += '<a href="javascript:void(0)" onclick="_surverAvoidChangePage(' + (_surverAvoidCurrentPage - 1) + ')">«</a>';
    } else {
        paginationHtml += '<a href="javascript:void(0)">«</a>';
    }
    paginationHtml += '</li>';
    var startPage = Math.max(1, _surverAvoidCurrentPage - 2);
    var endPage = Math.min(totalPages, startPage + 4);
    if (endPage - startPage < 4) {
        startPage = Math.max(1, endPage - 4);
    }
    for (var i = startPage; i <= endPage; i++) {
        paginationHtml += '<li class="' + (i === _surverAvoidCurrentPage ? 'active' : '') + '">';
        paginationHtml += '<a href="javascript:void(0)" ' + (i !== _surverAvoidCurrentPage ? 'onclick="_surverAvoidChangePage(' + i + ')"' : '') + '>' + i + '</a>';
        paginationHtml += '</li>';
    }
    paginationHtml += '<li class="' + (_surverAvoidCurrentPage >= totalPages ? 'disabled' : '') + '">';
    if (_surverAvoidCurrentPage < totalPages) {
        paginationHtml += '<a href="javascript:void(0)" onclick="_surverAvoidChangePage(' + (_surverAvoidCurrentPage + 1) + ')">»</a>';
    } else {
        paginationHtml += '<a href="javascript:void(0)">»</a>';
    }
    paginationHtml += '</li>';
    $("#surverAvoidPagination ul").html(paginationHtml);
}

function _surverAvoidChangePage(page) {
    _surverAvoidCurrentPage = page;
    _surverAvoidRenderTableWithPagination();
}

function _surverAvoidGetStatText(proStat) {
    var statMap = {
        '': '未提交', 'check': '审核中', 'partake_award': '参评',
        'no_award': '不评', 'delayed_award': '缓评', 'reject': '已驳回',
        'improve_partake': '完善后参评', 'eliminated': '已淘汰',
        'to_validate': '审核中', 'to_assign_experts': '分派专家',
        'experts_score': '专家打分', 'score': '专家打分'
    };
    return statMap[proStat] || proStat || '-';
}

/**
 * 设为回避
 */
function surverSetAvoidance(proId) {
    var taskId = getTaskId();
    layer.confirm('确认将此项目设为回避？', { btn: ['确认', '取消'] }, function (index) {
        $.ajax({
            type: "POST",
            url: SVR_PREFIX + "/avoidance/manualAvoid",
            data: {
                taskId: taskId,
                proId: proId,
                expertUserId: _surverAvoidCurrentExpertUserId,
                reason: "管理员手动回避"
            },
            success: function (data) {
                if (data.code == 0) {
                    layer.msg("设置成功");
                    // _surverAvoidRefresh(taskId);
                    _surverAvoidReloadData(taskId);
                } else {
                    layer.alert(data.msg || "设置失败");
                }
            },
            error: function () { layer.alert("设置回避失败"); }
        });
        layer.close(index);
    });
}

/**
 * 取消回避
 */
function surverCancelAvoidance(proId) {
    var taskId = getTaskId();
    layer.confirm('确认取消此项目的回避状态？', { btn: ['确认', '取消'] }, function (index) {
        $.ajax({
            type: "POST",
            url: SVR_PREFIX + "/avoidance/cancelAvoid",
            data: {
                taskId: taskId,
                proId: proId,
                expertUserId: _surverAvoidCurrentExpertUserId
            },
            success: function (data) {
                if (data.code == 0) {
                    layer.msg("取消成功");
                    // _surverAvoidRefresh(taskId);
                    _surverAvoidReloadData(taskId);
                } else {
                    layer.alert(data.msg || "取消失败");
                }
            },
            error: function () { layer.alert("取消回避失败"); }
        });
        layer.close(index);
    });
}
/**
 * 刷新回避数据并保留当前筛选条件与分页
 */
function _surverAvoidReloadData(taskId) {
    $("#surverAvoidanceTableBody").html('<tr><td colspan="8" class="text-center">加载中...</td></tr>');
    $.ajax({
        type: "GET",
        url: SVR_PREFIX + "/avoidance/expertProjects",
        data: { taskId: taskId, expertUserId: _surverAvoidCurrentExpertUserId },
        timeout: 30000,
        success: function (data) {
            if (data && data.code == 0) {
                _surverAvoidAllProjects = data.rows || [];
                // 保留筛选条件
                var filterTopicName = $("#surver_avoid_filter_topicName").val().toLowerCase();
                var filterProCode = $("#surver_avoid_filter_proCode").val().toLowerCase();
                var filterUnitName = $("#surver_avoid_filter_unitName").val().toLowerCase();
                var filterGroupDesc = $("#surver_avoid_filter_groupDesc").val().toLowerCase();
                var filterStatus = $("#surver_avoid_filter_status").val();
                _surverAvoidFilteredProjects = _surverAvoidAllProjects.filter(function (p) {
                    if (filterTopicName && (!p.topicName || p.topicName.toLowerCase().indexOf(filterTopicName) === -1)) return false;
                    if (filterProCode && (!p.applyId || p.applyId.toLowerCase().indexOf(filterProCode) === -1)) return false;
                    if (filterUnitName && (!p.unitName || p.unitName.toLowerCase().indexOf(filterUnitName) === -1)) return false;
                    if (filterGroupDesc && (!p.groupDesc || p.groupDesc.toLowerCase().indexOf(filterGroupDesc) === -1)) return false;
                    if (filterStatus !== "") {
                        var isAvoided = p.isAvoided || false;
                        if (filterStatus === "1" && !isAvoided) return false;
                        if (filterStatus === "0" && isAvoided) return false;
                    }
                    return true;
                });
                // 保持当前分页（如果当前页超出总页数则自动回退）
                var totalPages = Math.ceil(_surverAvoidFilteredProjects.length / _surverAvoidPageSize);
                if (_surverAvoidCurrentPage > totalPages && totalPages > 0) {
                    _surverAvoidCurrentPage = totalPages;
                }
                _surverAvoidRenderTableWithPagination();
            }
        },
        error: function () { layer.alert("重新加载数据失败"); }
    });
}

/**
 * 回避弹窗筛选
 */
function surverApplyAvoidanceFilters() {
    var filterTopicName = $("#surver_avoid_filter_topicName").val().toLowerCase();
    var filterProCode = $("#surver_avoid_filter_proCode").val().toLowerCase();
    var filterUnitName = $("#surver_avoid_filter_unitName").val().toLowerCase();
    var filterGroupDesc = $("#surver_avoid_filter_groupDesc").val().toLowerCase();
    var filterStatus = $("#surver_avoid_filter_status").val();

    // var filtered = _surverAvoidAllProjects.filter(function (p) {
    _surverAvoidFilteredProjects = _surverAvoidAllProjects.filter(function (p) {
        if (filterTopicName && (!p.topicName || p.topicName.toLowerCase().indexOf(filterTopicName) === -1)) return false;
        if (filterProCode && (!p.applyId || p.applyId.toLowerCase().indexOf(filterProCode) === -1)) return false;
        if (filterUnitName && (!p.unitName || p.unitName.toLowerCase().indexOf(filterUnitName) === -1)) return false;
        if (filterGroupDesc && (!p.groupDesc || p.groupDesc.toLowerCase().indexOf(filterGroupDesc) === -1)) return false;
        if (filterStatus !== "") {
            var isAvoided = p.isAvoided || false;
            if (filterStatus === "1" && !isAvoided) return false;
            if (filterStatus === "0" && isAvoided) return false;
        }
        return true;
    });
    // _surverAvoidRenderTable(filtered);
    _surverAvoidCurrentPage = 1;
    _surverAvoidRenderTableWithPagination();
}

function surverClearAvoidanceFilters() {
    $("#surver_avoid_filter_topicName").val('');
    $("#surver_avoid_filter_proCode").val('');
    $("#surver_avoid_filter_unitName").val('');
    $("#surver_avoid_filter_groupDesc").val('');
    $("#surver_avoid_filter_status").val('');
    // _surverAvoidRenderTable(_surverAvoidAllProjects);
    _surverAvoidFilteredProjects = _surverAvoidAllProjects;
    _surverAvoidCurrentPage = 1;
    _surverAvoidRenderTableWithPagination();
}
