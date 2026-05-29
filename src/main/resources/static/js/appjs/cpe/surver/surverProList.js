var prefix = "/surverPro"
/**
 * 每页条数（分页）
 * - SURVER_PRO_LIST_PAGE_SIZE_OPTIONS：下拉可选值，须包含「默认条数」
 * - SURVER_PRO_LIST_DEFAULT_PAGE_SIZE：无 localStorage、或保存值非法时的默认每页条数
 *
 * 若要默认每页 2 条：把 DEFAULT 改成 2，并在 OPTIONS 里加入 2，例如 [1, 2, 10, 25, 50, 100]
 * 曾调试「默认 1」：把下面一行改成 1 即可（并保证 1 在 OPTIONS 里）
 */
var SURVER_PRO_LIST_PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
// var SURVER_PRO_LIST_DEFAULT_PAGE_SIZE = 1; // 调试：默认每页 1（需要时取消本行注释，并注释掉下一行）
var SURVER_PRO_LIST_DEFAULT_PAGE_SIZE = 10;

var SURVER_PRO_LIST_PAGE_SIZE_KEY = "cpe.surverProList.pageSize";

function getSavedSurverProListPageSize() {
    try {
        var raw = localStorage.getItem(SURVER_PRO_LIST_PAGE_SIZE_KEY);
        if (raw == null || raw === "") {
            return SURVER_PRO_LIST_DEFAULT_PAGE_SIZE;
        }
        var n = parseInt(raw, 10);
        if (isNaN(n) || SURVER_PRO_LIST_PAGE_SIZE_OPTIONS.indexOf(n) < 0) {
            return SURVER_PRO_LIST_DEFAULT_PAGE_SIZE;
        }
        return n;
    } catch (e) {
        return SURVER_PRO_LIST_DEFAULT_PAGE_SIZE;
    }
}

function persistSurverProListPageSize(size) {
    try {
        var n = parseInt(size, 10);
        if (!isNaN(n) && SURVER_PRO_LIST_PAGE_SIZE_OPTIONS.indexOf(n) >= 0) {
            localStorage.setItem(SURVER_PRO_LIST_PAGE_SIZE_KEY, String(n));
        }
    } catch (e) { /* ignore */ }
}

/** 高级筛选：持久化到 localStorage，刷新/再次进入页面自动回填；仅「重置」清空并删存储 */
var SURVER_PRO_ADV_FILTER_KEY_PREFIX = "cpe.surverProList.advFilter.v1";
var _surverProAdvFilterSaveTimer = null;

function getSurverProAdvFilterStorageKey() {
    var tid = ($("#taskId").val() || "") + "";
    var pst = ($("#proSubType").val() || "") + "";
    return SURVER_PRO_ADV_FILTER_KEY_PREFIX + ":" + tid + ":" + pst;
}

function readSurverProAdvFilterFromDom() {
    return {
        filterProName: ($("#filterProName").val() || "").trim(),
        filterApplyCompany: ($("#filterApplyCompany").val() || "").trim(),
        filterMajor: ($("#filterMajor").val() || "").trim(),
        filterDeclareAccount: ($("#filterDeclareAccount").val() || "").trim(),
        filterQcGroupName: ($("#filterQcGroupName").val() || "").trim(),
        filterExpertGroupName: ($("#filterExpertGroupName").val() || "").trim(),
        filterEliminated: ($("#filterEliminated").val() || "").trim(),
        filterProStat: ($("#filterProStat").val() || "").trim(),
        filterReviewResult: ($("#filterReviewResult").val() || "").trim()
    };
}

function isSurverProAdvFilterEmpty(o) {
    return !o.filterProName && !o.filterApplyCompany && !o.filterMajor && !o.filterDeclareAccount
        && !o.filterQcGroupName && !o.filterExpertGroupName && !o.filterEliminated && !o.filterProStat
        && !o.filterReviewResult;
}

function persistSurverProAdvFilterFromDom() {
    try {
        var key = getSurverProAdvFilterStorageKey();
        var data = readSurverProAdvFilterFromDom();
        if (isSurverProAdvFilterEmpty(data)) {
            localStorage.removeItem(key);
        } else {
            localStorage.setItem(key, JSON.stringify(data));
        }
    } catch (e) { /* ignore */ }
}

function schedulePersistSurverProAdvFilter() {
    if (_surverProAdvFilterSaveTimer) {
        clearTimeout(_surverProAdvFilterSaveTimer);
    }
    _surverProAdvFilterSaveTimer = setTimeout(function () {
        _surverProAdvFilterSaveTimer = null;
        persistSurverProAdvFilterFromDom();
    }, 400);
}

function restoreSurverProAdvFilterFromStorage() {
    try {
        var raw = localStorage.getItem(getSurverProAdvFilterStorageKey());
        if (raw == null || raw === "") {
            return;
        }
        var o = JSON.parse(raw);
        if (!o || typeof o !== "object") {
            return;
        }
        $("#filterProName").val(o.filterProName != null ? o.filterProName : "");
        $("#filterApplyCompany").val(o.filterApplyCompany != null ? o.filterApplyCompany : "");
        $("#filterMajor").val(o.filterMajor != null ? o.filterMajor : "");
        $("#filterDeclareAccount").val(o.filterDeclareAccount != null ? o.filterDeclareAccount : "");
        $("#filterQcGroupName").val(o.filterQcGroupName != null ? o.filterQcGroupName : "");
        $("#filterExpertGroupName").val(o.filterExpertGroupName != null ? o.filterExpertGroupName : "");
        $("#filterEliminated").val(o.filterEliminated != null ? o.filterEliminated : "");
        $("#filterProStat").val(o.filterProStat != null ? o.filterProStat : "");
        $("#filterReviewResult").val(o.filterReviewResult != null ? o.filterReviewResult : "");
        var filled = readSurverProAdvFilterFromDom();
        if (!isSurverProAdvFilterEmpty(filled)) {
            $("#surverProFilterPanel").addClass("in");
            $('a[href="#surverProFilterPanel"]').attr("aria-expanded", "true");
        }
    } catch (e) { /* ignore */ }
}

function clearSurverProAdvFilterStorage() {
    try {
        localStorage.removeItem(getSurverProAdvFilterStorageKey());
    } catch (e) { /* ignore */ }
}

/**
 * 列表行定位（查看 / 形审 / 修改返回后滚回离开前那一行）
 * - 页码由 bootstrap-table 自己保持；这里只记 proId + 当时页码，刷新后滚到对应 tr
 * - 用 sessionStorage，按 taskId + proSubType 区分（与高级筛选 key 规则一致）
 */
var SURVER_PRO_LIST_ROW_ANCHOR_KEY_PREFIX = "cpe.surverProList.rowAnchor.v1";
/** 形审提交后延迟刷新列表（等列表 iframe 重新可见再 refresh，避免锚点被提前清掉） */
var SURVER_PRO_LIST_PENDING_REFRESH_PREFIX = "cpe.surverProList.pendingRefresh.v1";
/** 内存一份，避免仅 refresh、未关页时读不到 session */
var _surverProListPendingRowAnchor = null;
/** 程序正在恢复滚动时置 true，避免把恢复过程当成用户手动滚动而清锚点 */
var _surverProListAnchorRestoreInProgress = false;
/** 本轮已滚回锚点行则置 true，避免 focus / 异步回调反复把列表拉回旧行 */
var _surverProListAnchorRestoreSatisfied = false;
var _surverProListAnchorScrollClearTimer = null;
var _surverProListAnchorScrollBound = false;
var _surverProListAnchorRowClickBound = false;

function getSurverProListRowAnchorStorageKey() {
    var tid = ($("#taskId").val() || "") + "";
    var pst = ($("#proSubType").val() || "") + "";
    return SURVER_PRO_LIST_ROW_ANCHOR_KEY_PREFIX + ":" + tid + ":" + pst;
}

function ensureSurverProListAnchorStyle() {
    if (document.getElementById("surver-pro-row-anchor-style")) {
        return;
    }
    var style = document.createElement("style");
    style.id = "surver-pro-row-anchor-style";
    style.textContent = ".surver-pro-row-anchor-flash td { background-color: #fff8e6 !important; }";
    document.head.appendChild(style);
}

/** 进入查看 / 形审 / 修改前调用：记下当前页码与项目 proId */
function rememberSurverProListRowAnchor(proId) {
    if (proId === undefined || proId === null || (proId + "") === "") {
        return;
    }
    var $tbl = $("#exampleTable");
    var pageNumber = 1;
    if ($tbl.length && $tbl.data("bootstrap.table")) {
        var opts = $tbl.bootstrapTable("getOptions") || {};
        pageNumber = opts.pageNumber || 1;
    }
    var anchor = {
        proId: String(proId),
        pageNumber: pageNumber,
        taskId: ($("#taskId").val() || "") + "",
        proSubType: ($("#proSubType").val() || "") + ""
    };
    _surverProListPendingRowAnchor = anchor;
    _surverProListAnchorRestoreSatisfied = false;
    try {
        sessionStorage.setItem(getSurverProListRowAnchorStorageKey(), JSON.stringify(anchor));
    } catch (e) { /* ignore */ }
}

function readSurverProListRowAnchor() {
    if (_surverProListPendingRowAnchor) {
        return _surverProListPendingRowAnchor;
    }
    try {
        var raw = sessionStorage.getItem(getSurverProListRowAnchorStorageKey());
        if (raw == null || raw === "") {
            return null;
        }
        return JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

function clearSurverProListRowAnchor() {
    _surverProListPendingRowAnchor = null;
    _surverProListAnchorRestoreSatisfied = true;
    try {
        sessionStorage.removeItem(getSurverProListRowAnchorStorageKey());
    } catch (e) { /* ignore */ }
}

/** 是否仍需自动滚回锚点（待刷新、刚离开详情页时尚未滚到位） */
function shouldAutoRestoreSurverProListRowAnchor() {
    if (!readSurverProListRowAnchor()) {
        return false;
    }
    if (hasSurverProListPendingRefresh()) {
        return true;
    }
    return !_surverProListAnchorRestoreSatisfied;
}

/** 用户手动滚动表格后清除锚点 */
function scheduleClearSurverProListRowAnchorOnUserScroll() {
    if (_surverProListAnchorRestoreInProgress) {
        return;
    }
    if (_surverProListAnchorScrollClearTimer) {
        clearTimeout(_surverProListAnchorScrollClearTimer);
    }
    _surverProListAnchorScrollClearTimer = setTimeout(function () {
        _surverProListAnchorScrollClearTimer = null;
        if (_surverProListAnchorRestoreInProgress) {
            return;
        }
        clearSurverProListRowAnchor();
    }, 200);
}

function bindSurverProListAnchorUserScrollClear() {
    if (_surverProListAnchorScrollBound) {
        return;
    }
    var $tbl = $("#exampleTable");
    if (!$tbl.length) {
        return;
    }
    var $wrap = $tbl.closest(".bootstrap-table");
    var $body = $wrap.find(".fixed-table-body");
    if (!$body.length) {
        $body = $wrap.find(".fixed-table-container");
    }
    if (!$body.length) {
        return;
    }
    _surverProListAnchorScrollBound = true;
    $body.on("scroll.surverProListAnchor", function () {
        if (!readSurverProListRowAnchor()) {
            return;
        }
        scheduleClearSurverProListRowAnchorOnUserScroll();
    });
}

/** 用户点击其他项目行时清除锚点，避免一直跳回离开前那一行 */
function bindSurverProListAnchorUserRowClear() {
    if (_surverProListAnchorRowClickBound) {
        return;
    }
    var $tbl = $("#exampleTable");
    if (!$tbl.length) {
        return;
    }
    var $wrap = $tbl.closest(".bootstrap-table");
    var $body = $wrap.find(".fixed-table-body");
    if (!$body.length) {
        $body = $wrap.find(".fixed-table-container");
    }
    if (!$body.length) {
        return;
    }
    _surverProListAnchorRowClickBound = true;
    $body.find("tbody").on("click.surverProListAnchorRow", "tr", function () {
        if (_surverProListAnchorRestoreInProgress) {
            return;
        }
        var anchor = readSurverProListRowAnchor();
        if (!anchor) {
            return;
        }
        var $tr = $(this);
        var pid = ($tr.attr("data-surver-pro-id") || "") + "";
        if (!pid) {
            var idx = $tr.data("index");
            if (idx !== undefined && idx !== null) {
                var data = $tbl.bootstrapTable("getData") || [];
                if (data[idx] && data[idx].proId != null) {
                    pid = String(data[idx].proId);
                }
            }
        }
        if (pid && pid !== String(anchor.proId)) {
            clearSurverProListRowAnchor();
        }
    });
}

function getSurverProListPendingRefreshStorageKey() {
    var tid = ($("#taskId").val() || "") + "";
    var pst = ($("#proSubType").val() || "") + "";
    return SURVER_PRO_LIST_PENDING_REFRESH_PREFIX + ":" + tid + ":" + pst;
}

function markSurverProListPendingRefresh() {
    try {
        sessionStorage.setItem(getSurverProListPendingRefreshStorageKey(), "1");
    } catch (e) { /* ignore */ }
}

function clearSurverProListPendingRefresh() {
    try {
        sessionStorage.removeItem(getSurverProListPendingRefreshStorageKey());
    } catch (e) { /* ignore */ }
}

function hasSurverProListPendingRefresh() {
    try {
        return sessionStorage.getItem(getSurverProListPendingRefreshStorageKey()) === "1";
    } catch (e) {
        return false;
    }
}

function isSurverProListRowAnchorForCurrentTab(anchor) {
    if (!anchor) {
        return false;
    }
    var tid = ($("#taskId").val() || "") + "";
    var pst = ($("#proSubType").val() || "") + "";
    return String(anchor.taskId) === tid && String(anchor.proSubType) === pst;
}

/** 将表格滚动到指定 proId 所在行，并短暂高亮 */
function scrollSurverProListToProId(proId) {
    ensureSurverProListAnchorStyle();
    var $tbl = $("#exampleTable");
    if (!$tbl.length || !$tbl.data("bootstrap.table")) {
        return false;
    }
    var pid = String(proId);
    var $wrap = $tbl.closest(".bootstrap-table");
    var $body = $wrap.find(".fixed-table-body");
    if (!$body.length) {
        $body = $wrap.find(".fixed-table-container");
    }
    if (!$body.length) {
        $body = $tbl.parent();
    }

    // 优先：行上的 data-surver-pro-id（load 里 rowAttributes 写入）
    var $tr = $body.find('tbody tr[data-surver-pro-id="' + pid + '"]');
    if (!$tr.length) {
        var data = $tbl.bootstrapTable("getData") || [];
        var rowIndex = -1;
        for (var i = 0; i < data.length; i++) {
            if (String(data[i].proId) === pid) {
                rowIndex = i;
                break;
            }
        }
        if (rowIndex < 0) {
            return false;
        }
        // 原：仅用 offsetTop，在部分浏览器/嵌套 iframe 下不准
        // $tr = $body.find('tbody tr[data-index="' + rowIndex + '"]');
        $tr = $body.find('tbody tr[data-index="' + rowIndex + '"]');
        if (!$tr.length) {
            $tr = $body.find("tbody tr").eq(rowIndex);
        }
    }
    if (!$tr.length) {
        return false;
    }

    var trEl = $tr[0];
    // 使用 scrollIntoView，在 Tab 切换、iframe 重新显示时更可靠
    try {
        trEl.scrollIntoView({ block: "center", inline: "nearest" });
    } catch (e1) {
        try {
            trEl.scrollIntoView(true);
        } catch (e2) { /* ignore */ }
    }
    var bodyEl = $body[0];
    if (bodyEl && trEl) {
        var trRect = trEl.getBoundingClientRect();
        var bodyRect = bodyEl.getBoundingClientRect();
        var delta = (trRect.top - bodyRect.top) - (bodyEl.clientHeight / 2) + (trRect.height / 2);
        bodyEl.scrollTop = Math.max(0, bodyEl.scrollTop + delta);
    }

    $tr.addClass("surver-pro-row-anchor-flash");
    setTimeout(function () {
        $tr.removeClass("surver-pro-row-anchor-flash");
    }, 2500);
    return true;
}

/**
 * 表格数据加载完成后：若存在「离开前记录的行」，则切回对应页并滚动到该行
 */
function restoreSurverProListRowAnchor() {
    var anchor = readSurverProListRowAnchor();
    if (!isSurverProListRowAnchorForCurrentTab(anchor)) {
        clearSurverProListRowAnchor();
        return;
    }
    var $tbl = $("#exampleTable");
    if (!$tbl.length || !$tbl.data("bootstrap.table")) {
        return;
    }
    var opts = $tbl.bootstrapTable("getOptions") || {};
    var curPage = opts.pageNumber || 1;
    if (anchor.pageNumber && anchor.pageNumber !== curPage) {
        _surverProListAnchorRestoreInProgress = true;
        $tbl.bootstrapTable("selectPage", anchor.pageNumber);
        setTimeout(function () {
            _surverProListAnchorRestoreInProgress = false;
        }, 600);
        return;
    }
    _surverProListAnchorRestoreInProgress = true;
    if (scrollSurverProListToProId(anchor.proId)) {
        _surverProListAnchorRestoreSatisfied = true;
    }
    setTimeout(function () {
        _surverProListAnchorRestoreInProgress = false;
    }, 400);
}

var _surverProListAnchorRestoreAttempts = 0;
var SURVER_PRO_LIST_ANCHOR_RESTORE_MAX = 12;

function scheduleRestoreSurverProListRowAnchor() {
    if (!shouldAutoRestoreSurverProListRowAnchor()) {
        return;
    }
    _surverProListAnchorRestoreAttempts = 0;
    var delays = [0, 100, 300, 500, 800, 1200, 1800, 2500];
    for (var i = 0; i < delays.length; i++) {
        (function (ms) {
            setTimeout(function () {
                if (!shouldAutoRestoreSurverProListRowAnchor()) {
                    return;
                }
                if (_surverProListAnchorRestoreAttempts >= SURVER_PRO_LIST_ANCHOR_RESTORE_MAX) {
                    // 原：可见且超次数则清锚点；现保留锚点，关 Tab / 再切回列表仍可继续尝试恢复
                    // if (!document.hidden) { clearSurverProListRowAnchor(); }
                    return;
                }
                _surverProListAnchorRestoreAttempts++;
                restoreSurverProListRowAnchor();
            }, ms);
        })(delays[i]);
    }
}

/**
 * 供父页 / 形审页调用：只 refresh 列表并恢复行位置（不 location.reload，避免滚回表顶）
 */
function refreshSurverProListWithAnchor() {
    if (hasSurverProListPendingRefresh()) {
        clearSurverProListPendingRefresh();
    }
    _surverProListAnchorRestoreSatisfied = false;
    var $tbl = $("#exampleTable");
    if ($tbl.length && $tbl.data("bootstrap.table")) {
        $tbl.bootstrapTable("refresh");
        if ($("#isViewProCode").val() == "false") {
            $tbl.bootstrapTable("hideColumn", "proCode");
        }
        scheduleRestoreSurverProListRowAnchor();
        return;
    }
    load();
}

/** 从父页/顶层 Tab 切回列表时：iframe 重新显示后滚动条常被重置，需主动恢复 */
function onSurverProListBecameVisible() {
    if (hasSurverProListPendingRefresh()) {
        clearSurverProListPendingRefresh();
        refreshSurverProListWithAnchor();
        return;
    }
    if (!shouldAutoRestoreSurverProListRowAnchor()) {
        return;
    }
    scheduleRestoreSurverProListRowAnchor();
}

$(function () {
    restoreSurverProAdvFilterFromStorage();
    load();
    // 浏览器 Tab / 顶层菜单 Tab 切回可见时尝试恢复行位置
    $(document).on("visibilitychange.surverProListAnchor", function () {
        if (!document.hidden) {
            onSurverProListBecameVisible();
        }
    });
    $(window).on("focus.surverProListAnchor", function () {
        onSurverProListBecameVisible();
    });
    // 原先：仅绑定持久化，刷新列表需手动点「筛选」
    // $("#surverProFilterPanel").on("input change", "input, select", schedulePersistSurverProAdvFilter);
    var $panel = $("#surverProFilterPanel");
    $panel.on("input change", "input, select", schedulePersistSurverProAdvFilter);
    // 高级筛选：输入框回车、下拉框选中即触发筛选（等同点「筛选」）
    $panel.on("keydown", "input.form-control", function (e) {
        if (e.which === 13 || e.keyCode === 13) {
            e.preventDefault();
            applySurverProFilter();
        }
    });
    $panel.on("change", "select.form-control", function () {
        applySurverProFilter();
    });
});
// 勘察奖奖项的项目列表展示页面，那个有点不好找，没直接引入的table
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
                // 关闭智能隐藏：否则总条数较少时 pageList 里大于 total 的项不显示（例如共 8 条时只看到 1、10）
                smartDisplay: false,
                // queryParamsType : "limit",
                // //设置为limit则会发送符合RESTFull格式的参数
                singleSelect: false, // 设置为true将禁止多选
                // contentType : "application/x-www-form-urlencoded",
                // //发送到服务器的数据编码类型
                pageSize: getSavedSurverProListPageSize(),
                pageList: SURVER_PRO_LIST_PAGE_SIZE_OPTIONS,
                pageNumber: 1, // 如果设置了分布，首页页码
                //search : true, // 是否显示搜索框
                showColumns: false, // 是否显示内容下拉框（选择显示的列）
                sidePagination: "server", // 设置在哪里进行分页，可选值为"client" 或者 "server"
                // 为每行标记 proId，便于返回列表时 scrollIntoView 精确定位
                rowAttributes: function (row) {
                    return { "data-surver-pro-id": row.proId };
                },
                queryParams: function (params) {
                    // 基础参数
                    var qp = {
                        //说明：传入后台的参数包括offset开始索引，limit步长，sort排序列，order：desc或者,以及所有列的键值对
                        limit: params.limit,
                        offset: params.offset,
                        proSubType: $("#proSubType").val(),
                        taskId: $("#taskId").val(),
                    };
                    // ===== 新增：8 项高级筛选参数（空值不传，避免 SQL <if> 误命中） =====
                    var fmap = {
                        filterProName:           $("#filterProName").val(),
                        filterApplyCompany:      $("#filterApplyCompany").val(),
                        filterMajor:             $("#filterMajor").val(),
                        filterDeclareAccount:    $("#filterDeclareAccount").val(),
                        filterQcGroupName:       $("#filterQcGroupName").val(),
                        filterExpertGroupName:   $("#filterExpertGroupName").val(),
                        filterEliminated:        $("#filterEliminated").val(),
                        filterProStat:           $("#filterProStat").val(),
                        filterReviewResult:      $("#filterReviewResult").val()
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
                    // 新增列：专家分组（与上方 qcGroupName/分组 不是同一功能点）
                    // 由于专家分组是任务级（taskid 维度），四个子奖项可共用同一个专家组
                    {
                        field: 'expertGroupName',
                        title: '专家分组',
                        formatter: function(value, row, index) {
                            var cached = (typeof EXPERT_GROUP_ASSIGN_MAP !== 'undefined') ? EXPERT_GROUP_ASSIGN_MAP[row.proId] : null;
                            var name = (cached && cached.name) ? cached.name : '未分配';
                            if ($("#isAssociationLeader").val() === '1') {
                                return '<div style="display:flex;align-items:center;justify-content:space-between;">' +
                                    '<span class="expert-group-name-' + row.proId + '" style="flex:1;overflow:hidden;text-overflow:ellipsis;">' + name + '</span>' +
                                    '<button class="btn btn-xs btn-info" onclick="showExpertGroupSelect(' + row.proId + ')" style="margin-left:10px;white-space:nowrap;">选择专家分组</button>' +
                                    '</div>';
                            }
                            return '<span class="expert-group-name-' + row.proId + '">' + name + '</span>';
                        }
                    },
                    // Phase B 新增：淘汰状态列（来源 SurverProjectInfo.eliminated + eliminateType）
                    {
                        field: 'eliminated',
                        title: '淘汰状态',
                        width: 100,
                        align: 'center',
                        formatter: function (value, row, index) {
                            // 原：二值 已淘汰/未淘汰
                            // if (value == 1 || value === '1') {
                            //     return '<span style="background:#d9534f;...">已淘汰</span>';
                            // }
                            // return '<span style="color:#999;">未淘汰</span>';
                            return _surverElimStatusHtml(value, row.eliminateType);
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
                            if (!text || String(text).trim() === '') {
                                var checkStarted = row.checkStartTime && String(row.checkStartTime).trim() !== '';
                                if (!checkStarted) {
                                    text = '形审未开始';
                                } else {
                                    text = '暂无形审结果';
                                }
                            }
                            return '<a href="javascript:void(0)" onclick="showReviewRecordList(' + row.proId + ',\'' + (row.proSubType || '') + '\')">' + text + '</a>';
                        }
                    },
                    {
                        field: 'extSurverNovelty',
                        title: '是否有查新',
                        align: 'center',
                        formatter: function (value, row, index) {
                            var v = (value || '').toString().trim();
                            if (v === '是' || v === '否') {
                                return '<span style="font-size:13px;">' + v + '</span>';
                            }
                            return '<span style="color:#bbb;">—</span>';
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
                            // --- 最初实现：同一按钮按企业用户显示「回收」否则「驳回」---
                            // var cancelBtnText = $("#isEnterpriseUser").val() === '1' ? '回收' : '驳回';
                            // var cancelCheck = '<a class="btn btn-success ' + rs_cancel_review_h + '" href="#" onclick="cancelCheck(' + row.proId + ')" title="表单审核' + cancelBtnText + '"  mce_href="#">' + cancelBtnText + '</a> ';
                            // --- 中间改动：回收与驳回整颗隐藏 ---
                            // // 隐藏「回收 / 驳回」按钮（原实现保留在注释中，恢复时取消下行 var cancelCheck = '' 并解开下方两行）
                            // var cancelCheck = '';
                            // // var cancelBtnText = $("#isEnterpriseUser").val() === '1' ? '回收' : '驳回';
                            // // var cancelCheck = '<a class="btn btn-success ' + rs_cancel_review_h + '" href="#" onclick="cancelCheck(' + row.proId + ')" title="表单审核' + cancelBtnText + '"  mce_href="#">' + cancelBtnText + '</a> ';
                            // --- 当前：仅隐藏「回收」（企业用户不展示）；非企业用户仍显示「驳回」（仍受 rs_cancel_review_h 等控制）---
                            var cancelCheck = '';
                            if ($("#isEnterpriseUser").val() !== '1') {
                                var cancelBtnText = '驳回';
                                cancelCheck = '<a class="btn btn-success ' + rs_cancel_review_h + '" href="#" onclick="cancelCheck(' + row.proId + ')" title="表单审核' + cancelBtnText + '"  mce_href="#">' + cancelBtnText + '</a> ';
                            }

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
                onPageChange: function (number, size) {
                    persistSurverProListPageSize(size);
                    if (!_surverProListAnchorRestoreInProgress && readSurverProListRowAnchor()) {
                        clearSurverProListRowAnchor();
                    }
                },
                // 新增：表格数据加载完成后，批量拉取并填充专家分组归属
                onLoadSuccess: function (data) {
                    if (typeof loadExpertGroupAssignments === 'function') {
                        loadExpertGroupAssignments();
                    }
                    // 新增：从查看 / 形审 / 修改返回后，滚回离开前所在行（页码仍由表格保持）
                    bindSurverProListAnchorUserScrollClear();
                    bindSurverProListAnchorUserRowClear();
                    scheduleRestoreSurverProListRowAnchor();
                },
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
    window.location.href = prefix + "/exportExcel?taskId=" + encodeURIComponent(taskId) + "&_t=" + Date.now();
}

// ========== 原版 printDetailExcelPro（已注释，保留参考） ==========
// function printDetailExcelPro() {
//     var taskId = $("#taskId").val();
//     var proSubType = $("#proSubType").val();
//     if (!proSubType) { layer.msg("请在具体奖项页导出详情"); return; }
//     var allowSubType = ['contribution', 'design', 'software', 'standard'];
//     if (allowSubType.indexOf(proSubType) < 0) {
//         layer.msg("仅支持优秀勘察奖、优秀设计奖、计算机软件奖、标准设计奖导出详情");
//         return;
//     }
//     window.location.href = prefix + "/exportDetailExcel?taskId=" + encodeURIComponent(taskId)
//         + "&proSubType=" + encodeURIComponent(proSubType);
// }
// ========== 原版 END ==========

// ========== 导出/导入确认淘汰名单 ==========

/** 导出确认淘汰名单 Excel（模板分块：项目行 A/B/C/D/回避 计数 + KC 专家评级 + 块底统计；可编辑 O 列淘汰状态后导入） */
function exportEliminateConfirmedExcel() {
    var taskId = $("#taskId").val();
    window.location.href = prefix + "/exportEliminateExcel?taskId=" + encodeURIComponent(taskId);
}

/** 打开"导入确认淘汰名单"弹窗 */
function triggerImportEliminateExcel() {
    var modalHtml = [
        '<div style="padding:20px 30px;">',
        '  <div style="display:flex;align-items:center;margin-bottom:16px;">',
        '    <label style="width:80px;flex-shrink:0;">表格文件：</label>',
        '    <input id="elimImportDisplayName" type="text" class="form-control" style="flex:1;margin-right:8px;" placeholder="请输入..." readonly>',
        '    <button type="button" class="btn btn-default" id="elimImportChooseBtn">选择</button>',
        '    <input id="elimImportFileInput" type="file" accept=".xls,.xlsx" style="display:none;">',
        '  </div>',
        '  <div style="padding-left:80px;">',
        '    <a href="javascript:void(0)" id="elimImportTplLink" style="color:#2196f3;cursor:pointer;">导出模板</a>',
        '  </div>',
        '</div>'
    ].join("");

    var layerIdx = layer.open({
        type: 1,
        title: "上传表格",
        area: ["480px", "240px"],
        content: modalHtml,
        btn: ["确认", "取消"],
        success: function (layero) {
            // 绑定"选择"按钮
            layero.find("#elimImportChooseBtn").on("click", function () {
                layero.find("#elimImportFileInput").trigger("click");
            });
            // 文件选中后显示文件名
            layero.find("#elimImportFileInput").on("change", function () {
                var f = this.files[0];
                layero.find("#elimImportDisplayName").val(f ? f.name : "");
            });
            // 导出模板链接
            layero.find("#elimImportTplLink").on("click", function () {
                exportEliminateConfirmedExcel();
            });
        },
        yes: function (index, layero) {
            var fileInput = layero.find("#elimImportFileInput")[0];
            if (!fileInput || !fileInput.files || !fileInput.files[0]) {
                layer.msg("请先选择文件"); return;
            }
            var taskId = $("#taskId").val();
            var formData = new FormData();
            formData.append("file", fileInput.files[0]);
            formData.append("taskId", taskId);
            $.ajax({
                url: prefix + "/importEliminateExcel",
                type: "POST",
                data: formData,
                processData: false,
                contentType: false,
                success: function (r) {
                    layer.close(index);
                    if (r.code === 0) {
                        layer.msg(r.msg || "导入成功", {icon: 1});
                        // 刷新主表格（淘汰状态列同步更新）
                        $('#exampleTable').bootstrapTable('refresh');
                        if (typeof loadEliminateCandidates === "function") loadEliminateCandidates();
                    } else {
                        layer.msg(r.msg || "导入失败", {icon: 2});
                    }
                },
                error: function () { layer.msg("请求失败，请重试", {icon: 2}); }
            });
        }
    });
}

// ========== END 导入导出确认淘汰名单 ==========

/** 新版：导出"申报项目基本信息一览表"（4 Sheet，沿用模板样式） */
function printDetailExcelPro() {
    var taskId = $("#taskId").val();
    if (!taskId) {
        layer.msg("缺少任务ID");
        return;
    }
    window.location.href = prefix + "/exportDetailExcel?taskId=" + encodeURIComponent(taskId);
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

// 新增：专家分组管理，跳转至对四个子tab课题进行专家分组的页面
function toExpertGroupManage() {
    var taskId = $("#taskId").val();
    if (!taskId) {
        layer.msg("缺少任务ID");
        return;
    }
    page('/enterprise_pro/to_expert_group_manage/'
        + taskId, '专家分组管理', 20220602, true);
}

// 新增：勘察奖小组联络人(86)专用——跳转到专业组管理页面（只能看到绑定的专业组）
function toSurverMajorGroupAdminForContact() {
    var taskId = $("#taskId").val();
    if (!taskId) {
        layer.msg("缺少任务ID");
        return;
    }
    var url = '/cpe/suverProcess/toSurverMajorGroupAdmin?taskId=' + taskId + '&proType=surver_pro_group';
    // 使用 layer 弹窗或 page() 跳转；若当前页在 iframe 内，直接跳转
    if (typeof page === 'function') {
        page(url, '专业组管理', 20250429, true);
    } else {
        window.location.href = url;
    }
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
    // 原：仅 refresh，数据更新后滚动条会回到当前页顶部，看不到刚处理的那一行
    // $('#exampleTable').bootstrapTable('refresh');
    // if($("#isViewProCode").val() == 'false') {
    //    $('#exampleTable').bootstrapTable('hideColumn', 'proCode');
    // }
    refreshSurverProListWithAnchor();
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
    rememberSurverProListRowAnchor(proId);
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
    rememberSurverProListRowAnchor(proId);
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


// 除了查看、编辑，就连形式审查都做了分开跳转

function reviewUploadDoc(id, proId, proSubType) {
    rememberSurverProListRowAnchor(proId);
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
                html += '<p><b>形审时间:</b> ' + (record.reviewTime || '') + '</p>';
                html += '<p><b>形审人员:</b> ' + (record.reviewerName || '未知') + '</p>';
                html += '<p><b>形审评语:</b></p>';
                html += '<div style="border:1px solid #eee;padding:8px;min-height:80px;background:#f9f9f9;">' + (record.remarks || '暂无评语') + '</div>';
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

// ============================================================
// 新增：专家分组（任务级，四个子奖项共用同一个专家组）
// ============================================================

// proId -> { groupId, name }
var EXPERT_GROUP_ASSIGN_MAP = {};

/**
 * 拉取当前任务下所有课题的专家分组归属，填充到 EXPERT_GROUP_ASSIGN_MAP，
 * 并直接更新表格中各 .expert-group-name-<proId> 的展示文本。
 */
function loadExpertGroupAssignments() {
    var taskId = $("#taskId").val();
    if (!taskId) {
        return;
    }
    $.ajax({
        url: "/enterprise_pro/expert_group/pro_assignments",
        type: "get",
        data: { taskId: taskId },
        success: function (r) {
            if (!r || r.code != 0) {
                return;
            }
            EXPERT_GROUP_ASSIGN_MAP = {};
            var list = (r.data && r.data.data) ? r.data.data : (r.data || []);
            for (var i = 0; i < list.length; i++) {
                var item = list[i] || {};
                var pid = item.proid != null ? item.proid : item.proId;
                if (pid == null) continue;
                EXPERT_GROUP_ASSIGN_MAP[pid] = {
                    groupId: item.groupid != null ? item.groupid : item.groupId,
                    name: item.name || ''
                };
            }
            // 更新当前已渲染的单元格
            $("[class^='expert-group-name-'], [class*=' expert-group-name-']").each(function () {
                var cls = $(this).attr('class') || '';
                var m = cls.match(/expert-group-name-(\d+)/);
                if (!m) return;
                var pid = m[1];
                var info = EXPERT_GROUP_ASSIGN_MAP[pid];
                $(this).text(info && info.name ? info.name : '未分配');
            });
            // 专家分组异步改 DOM 后，若本轮尚未恢复到位再滚一次
            if (shouldAutoRestoreSurverProListRowAnchor()) {
                scheduleRestoreSurverProListRowAnchor();
            }
        }
    });
}

/**
 * 弹出"选择专家分组"下拉。任务级共享，所以列表来源是
 * /enterprise_pro/expert_group/list?taskId=...，与子 tab 无关。
 */
function showExpertGroupSelect(proId) {
    var taskId = $("#taskId").val();
    if (!taskId) {
        layer.msg("缺少任务ID");
        return;
    }
    $.ajax({
        url: "/enterprise_pro/expert_group/list",
        type: "get",
        data: {
            taskId: taskId,
            limit: 1000,
            offset: 0
        },
        success: function (data) {
            var rows = (data && data.rows) ? data.rows : [];
            if (!rows.length) {
                layer.msg('暂无专家分组，请先在"专家分组管理"中创建');
                return;
            }
            var current = EXPERT_GROUP_ASSIGN_MAP[proId];
            var currentId = current ? String(current.groupId) : '';
            var optionsHtml = '<option value="">请选择专家分组</option>';
            rows.forEach(function (g) {
                var sel = (String(g.groupid) === currentId) ? ' selected' : '';
                optionsHtml += '<option value="' + g.groupid + '"' + sel + '>' + g.name + '</option>';
            });
            layer.open({
                type: 1,
                title: '选择专家分组',
                area: ['400px', '300px'],
                content: '<div style="padding:20px;">' +
                    '<select id="expertGroupSelect" class="form-control">' + optionsHtml + '</select>' +
                    '<div style="margin-top:20px;text-align:center;">' +
                    '<button class="btn btn-primary" onclick="confirmExpertGroupSelect(' + proId + ')">确定</button> ' +
                    '<button class="btn btn-default" onclick="layer.closeAll()">取消</button>' +
                    '</div></div>'
            });
        }
    });
}

function confirmExpertGroupSelect(proId) {
    var groupId = $("#expertGroupSelect").val();
    if (!groupId) {
        layer.msg('请选择专家分组');
        return;
    }
    var taskId = $("#taskId").val();
    $.ajax({
        url: "/enterprise_pro/expert_group/assign",
        type: "post",
        data: {
            taskId: taskId,
            proId: proId,
            groupId: groupId
        },
        success: function (r) {
            if (r.code == 0) {
                layer.msg('分配成功');
                layer.closeAll();
                // 重新拉取并刷新单元格（避免整表 refresh 引发跳页）
                loadExpertGroupAssignments();
            } else {
                layer.msg(r.msg || '分配失败');
            }
        }
    });
}

// =====================================================================
// Phase B 新增：勘察奖"淘汰管理"弹窗逻辑（管理员侧）
// 服务端接口前缀：/cpe/suverProcess/eliminate/*
// 设计要点：
//   - 候选池数据来自专家评级活动表(ass_surver_expert_eliminate)的聚合
//   - "确认淘汰" / "取消淘汰" 仅写 4 张申报子表的 eliminated 字段
//   - 导入: xlsx 第 1 列=申报编号(proCode)，前端按行调 setEliminated
//   - 导出: 客户端 XLSX.js (与 QC 风格一致)
// =====================================================================

var SURVER_ELIM_PREFIX = '/cpe/suverProcess/eliminate';
var SURVER_SUBTYPE_LABEL = {
    contribution: '优秀勘察奖',
    design:       '优秀设计奖',
    software:     '计算机软件奖',
    standard:     '标准设计奖',
    consulting:   '咨询奖'
};
var _surverElimCandidates = [];   // 缓存最新一次候选数据，用于导出
var _surverElimConfirmed  = [];   // 缓存最新一次已确认数据，用于导出

/** 淘汰状态展示：未淘汰 / 评级淘汰 / 打分淘汰（历史 eliminated=1 且 type 空视为评级淘汰） */
function _surverElimDisplayKind(eliminated, eliminateType) {
    if (eliminated == 1 || eliminated === '1') {
        var t = ((eliminateType || '') + '').trim().toLowerCase();
        return t === 'score' ? 'score' : 'rating';
    }
    return 'none';
}

function _surverElimStatusHtml(eliminated, eliminateType) {
    var kind = _surverElimDisplayKind(eliminated, eliminateType);
    // 列宽与徽章样式在此控制（_surverElimDisplayKind 仅返回 none/rating/score）
    var badgeStyle = 'display:inline-block;white-space:nowrap;min-width:68px;text-align:center;border-radius:3px;padding:2px 10px;font-size:12px;';
    if (kind === 'score') {
        return '<span style="' + badgeStyle + 'background:#c9302c;color:#fff;">打分淘汰</span>';
    }
    if (kind === 'rating') {
        return '<span style="' + badgeStyle + 'background:#d9534f;color:#fff;">评级淘汰</span>';
    }
    return '<span style="display:inline-block;white-space:nowrap;color:#999;">未淘汰</span>';
}

function _surverElimStatusText(eliminated, eliminateType) {
    var kind = _surverElimDisplayKind(eliminated, eliminateType);
    if (kind === 'score') return '打分淘汰';
    if (kind === 'rating') return '评级淘汰';
    return '未淘汰';
}

function _surverElimEscape(s) {
    if (s == null) return '';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/** 入口：打开淘汰管理弹窗 */
function openEliminateManage() {
    var taskId = $("#taskId").val();
    if (!taskId) { layer.msg('缺少任务ID'); return; }
    var isContactOnly = $("#isSurverGroupContact").val() === '1' && $("#isAssociationLeader").val() !== '1';
    var winTitle = isContactOnly ? '勘察奖 - 淘汰信息（本专家组范围）' : '勘察奖 - 淘汰管理';
    layer.open({
        type: 1,
        title: winTitle,
        area: ['1240px', '720px'],
        shadeClose: false,
        content: $('#surverEliminateModal'),
        success: function() {
            // 默认进入候选池 Tab
            loadEliminateCandidates();
            // 项目类别：选中后立即筛选（原需点「刷新」）
            $('#surverElimSubTypeFilter').off('change.surverElim').on('change.surverElim', function() {
                loadEliminateCandidates();
            });
            // 切换到"已确认"Tab 时再懒加载
            $('#surverElimTabs a[href="#surverElimTabConfirmed"]').off('shown.bs.tab.surverElim').on('shown.bs.tab.surverElim', function() {
                loadEliminateConfirmed();
            });
        },
        end: function() {
            // 关闭弹窗后刷新主列表，让"淘汰状态"列同步更新
            try { reLoad(); } catch (e) {}
        }
    });
}

/** 拉取候选淘汰池数据 */
function loadEliminateCandidates() {
    var taskId = $("#taskId").val();
    var proSubType = $("#surverElimSubTypeFilter").val() || '';
    // [DEBUG] 临时调试
    console.log('[DEBUG-elim] loadEliminateCandidates taskId=' + taskId + ', proSubType=' + proSubType);
    $("#surverElimCandidatesBody").html('<tr><td colspan="12" style="text-align:center;color:#999;">加载中...</td></tr>');
    $.ajax({
        type: 'GET',
        url: SURVER_ELIM_PREFIX + '/listCandidates',
        data: { taskId: taskId, proSubType: proSubType },
        success: function(r) {
            // [DEBUG] 临时调试
            console.log('[DEBUG-elim] response:', r);
            if (r.code !== 0) { layer.msg(r.msg || '加载失败', { icon: 2 }); return; }
            _surverElimCandidates = r.list || [];
            _renderEliminateCandidates(_surverElimCandidates);
        },
        error: function(xhr, status, err) {
            console.log('[DEBUG-elim] error:', status, err);
            layer.msg('加载候选池失败', { icon: 2 });
        }
    });
}

function _renderEliminateCandidates(list) {
    var tbody = $("#surverElimCandidatesBody");
    if (!list || list.length === 0) {
        tbody.html('<tr><td colspan="12" style="text-align:center;color:#999;">暂无候选数据（当前任务下尚无专家评级）</td></tr>');
        return;
    }
    var html = '';
    for (var i = 0; i < list.length; i++) {
        var it = list[i];
        var subTypeLabel = SURVER_SUBTYPE_LABEL[it.proSubType] || (it.proSubType || '-');
        var statusHtml = _surverElimStatusHtml(it.eliminated, it.eliminateType);
        // 原：单按钮「确认淘汰」
        // var actionHtml = (it.eliminated == 1)
        //     ? '...取消淘汰...'
        //     : '...确认淘汰...';
        var actionHtml;
        if (it.eliminated == 1) {
            actionHtml = '<button class="btn btn-xs btn-default" onclick="onCancelEliminate(\'' + _surverElimEscape(it.proSubType) + '\',' + it.proId + ')">取消淘汰</button>';
        } else {
            actionHtml = '<button class="btn btn-xs btn-danger" style="margin-bottom:3px;" onclick="onConfirmRatingEliminate(\'' + _surverElimEscape(it.proSubType) + '\',' + it.proId + ')">评级淘汰</button>'
                + ' <button class="btn btn-xs btn-warning" onclick="onConfirmScoreEliminate(\'' + _surverElimEscape(it.proSubType) + '\',' + it.proId + ')">打分淘汰</button>';
        }
        // 专家名称列 + 专家评级列：从 "张三:A|李四:D" 中提取
        var gradeItems = (it.expertGrades || '').split('|').filter(function(s){ return s; });
        var expertNames = gradeItems.map(function(s) { return s.split(':')[0] || ''; }).filter(function(n){ return n; }).join('、');
        var grades = gradeItems.map(function(s) {
            var parts = s.split(':'); var name = parts[0] || ''; var g = parts[1] || '-';
            var color = (g === 'D') ? '#d9534f' : (g === 'C' ? '#f0ad4e' : (g === 'A' ? '#5cb85c':'#5bc0de'));
            return '<span title="' + _surverElimEscape(name) + '" style="display:inline-block;background:' 
            + color 
            + ';color:#fff;border-radius:3px;padding:1px 5px;margin:1px 2px;font-size:11px;cursor:default;">'
            + _surverElimEscape(g) + '</span>';
        }).join('');
        html += '<tr>'
            + '<td>' + (i + 1) + '</td>'
            + '<td>' + _surverElimEscape(subTypeLabel) + '</td>'
            + '<td>' + _surverElimEscape(it.proCode || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.topicName || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.companyName || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.declareAccount || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.groupName || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.expertGroupName || '-') + '</td>'
            // + '<td>' + (it.gradeA || 0) + '</td>'
            // + '<td>' + (it.gradeB || 0) + '</td>'
            // + '<td>' + (it.gradeC || 0) + '</td>'
            // + '<td><b style="color:#d9534f;">' + (it.gradeD || 0) + '</b></td>'
            + '<td>' + _surverElimEscape(expertNames || '-') + '</td>'
            + '<td>' + grades + ' <a href="javascript:;" style="font-size:11px;margin-left:4px;" onclick="showExpertGradeDetails(' + it.proId + ')">查看</a></td>'
            + '<td>' + statusHtml + '</td>'
            + '<td>' + actionHtml + '</td>'
            + '</tr>';
    }
    tbody.html(html);
}

/** 查看某项目下所有专家的评级详情（姓名、等级、评级理由） */
function showExpertGradeDetails(proId) {
    var taskId = $("#taskId").val();
    $.ajax({
        type: 'GET',
        url: SURVER_ELIM_PREFIX + '/expertGradeDetails',
        data: { taskId: taskId, proId: proId },
        success: function(r) {
            if (r.code !== 0) { layer.msg(r.msg || '获取详情失败', { icon: 2 }); return; }
            var list = r.data || [];
            if (list.length === 0) { layer.msg('暂无专家评级数据'); return; }
            var html = '<div style="padding:16px;max-height:450px;overflow-y:auto;">';
            for (var i = 0; i < list.length; i++) {
                var item = list[i];
                var gradeColor = (item.grade === 'D') ? '#d9534f' : (item.grade === 'C' ? '#f0ad4e' : (item.grade === 'A' ? '#5cb85c' : '#5bc0de'));
                html += '<div style="border:1px solid #ddd;margin-bottom:10px;padding:12px;border-radius:4px;background:#fff;">';
                html += '<p><b>专家姓名：</b>' + _surverElimEscape(item.expertName || '未知') + '</p>';
                html += '<p><b>评级等级：</b><span style="display:inline-block;background:' + gradeColor + ';color:#fff;border-radius:3px;padding:2px 8px;font-size:12px;">' + _surverElimEscape(item.grade || '-') + '</span></p>';
                html += '<p><b>评级理由：</b></p>';
                html += '<div style="border:1px solid #eee;padding:8px;min-height:60px;background:#f9f9f9;white-space:pre-wrap;word-break:break-all;">' + _surverElimEscape(item.remark || '未填写') + '</div>';
                html += '</div>';
            }
            html += '</div>';
            layer.open({
                type: 1,
                title: '专家评级详情',
                area: ['560px', '500px'],
                content: html
            });
        },
        error: function() { layer.msg('请求失败', { icon: 2 }); }
    });
}

/** 设置/取消淘汰（带 eliminateType：rating | score） */
function _postSetEliminated(proSubType, proId, eliminated, eliminateType, confirmTitle) {
    layer.confirm(confirmTitle, { btn: ['确定', '取消'] }, function(idx) {
        layer.close(idx);
        var payload = { proSubType: proSubType, proId: proId, eliminated: eliminated };
        if (eliminated === 1 && eliminateType) {
            payload.eliminateType = eliminateType;
        }
        $.ajax({
            type: 'POST',
            url: SURVER_ELIM_PREFIX + '/setEliminated',
            data: payload,
            success: function(r) {
                if (r.code === 0) {
                    layer.msg(r.msg || '操作成功', { icon: 1 });
                    loadEliminateCandidates();
                    try { reLoad(); } catch (e) {}
                } else {
                    layer.msg(r.msg || '操作失败', { icon: 2 });
                }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

/** 评级淘汰 */
function onConfirmRatingEliminate(proSubType, proId) {
    _postSetEliminated(proSubType, proId, 1, 'rating', '确认将该项目的「评级淘汰」？');
}

/** 打分淘汰 */
function onConfirmScoreEliminate(proSubType, proId) {
    _postSetEliminated(proSubType, proId, 1, 'score', '确认将该项目的「打分淘汰」？');
}

// 原：确认淘汰（无 eliminate_type，保留参考）
// function onConfirmEliminate(proSubType, proId) {
//     layer.confirm('确认将该项目设置为"已淘汰"？', { btn: ['确定', '取消'] }, function(idx) {
//         layer.close(idx);
//         $.ajax({
//             type: 'POST',
//             url: SURVER_ELIM_PREFIX + '/setEliminated',
//             data: { proSubType: proSubType, proId: proId, eliminated: 1 },
//             success: function(r) {
//                 if (r.code === 0) {
//                     layer.msg(r.msg || '已确认淘汰', { icon: 1 });
//                     loadEliminateCandidates();
//                 } else {
//                     layer.msg(r.msg || '操作失败', { icon: 2 });
//                 }
//             },
//             error: function() { layer.msg('请求失败', { icon: 2 }); }
//         });
//     });
// }

/** 取消淘汰 - 写子表 eliminated=0 */
function onCancelEliminate(proSubType, proId) {
    layer.confirm('确认取消该项目的淘汰标记？', { btn: ['确定', '取消'] }, function(idx) {
        layer.close(idx);
        $.ajax({
            type: 'POST',
            url: SURVER_ELIM_PREFIX + '/setEliminated',
            data: { proSubType: proSubType, proId: proId, eliminated: 0 },
            success: function(r) {
                if (r.code === 0) {
                    layer.msg(r.msg || '已取消淘汰', { icon: 1 });
                    loadEliminateCandidates();
                    try { reLoad(); } catch (e) {}
                } else {
                    layer.msg(r.msg || '操作失败', { icon: 2 });
                }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

/** 拉取已确认淘汰列表 */
function loadEliminateConfirmed() {
    var taskId = $("#taskId").val();
    $("#surverElimConfirmedBody").html('<tr><td colspan="11" style="text-align:center;color:#999;">加载中...</td></tr>');
    $.ajax({
        type: 'GET',
        url: SURVER_ELIM_PREFIX + '/listConfirmed',
        data: { taskId: taskId },
        success: function(r) {
            if (r.code !== 0) { layer.msg(r.msg || '加载失败', { icon: 2 }); return; }
            _surverElimConfirmed = r.list || [];
            _renderEliminateConfirmed(_surverElimConfirmed);
        },
        error: function() { layer.msg('加载已确认列表失败', { icon: 2 }); }
    });
}

function _renderEliminateConfirmed(list) {
    var tbody = $("#surverElimConfirmedBody");
    if (!list || list.length === 0) {
        tbody.html('<tr><td colspan="11" style="text-align:center;color:#999;">暂无已确认淘汰项目</td></tr>');
        return;
    }
    var html = '';
    for (var i = 0; i < list.length; i++) {
        var it = list[i];
        var subTypeLabel = SURVER_SUBTYPE_LABEL[it.proSubType] || (it.proSubType || '-');
        var grades = (it.expertGrades || '').split('|').filter(function(s){ return s; }).map(function(s) {
            var parts = s.split(':'); var name = parts[0] || ''; var g = parts[1] || '-';
            var color = (g === 'D') ? '#d9534f' : (g === 'C' ? '#f0ad4e' : (g === 'A' ? '#5cb85c':'#5bc0de'));
            return '<span title="' + _surverElimEscape(name) + '" style="display:inline-block;background:'
            + color
            + ';color:#fff;border-radius:3px;padding:1px 5px;margin:1px 2px;font-size:11px;cursor:default;">'
            + _surverElimEscape(g) + '</span>';
        }).join('');
        html += '<tr>'
            + '<td>' + (i + 1) + '</td>'
            + '<td>' + _surverElimEscape(subTypeLabel) + '</td>'
            + '<td>' + _surverElimEscape(it.proCode || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.topicName || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.companyName || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.declareAccount || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.groupName || '-') + '</td>'
            + '<td>' + _surverElimEscape(it.expertGroupName || '-') + '</td>'
            + '<td>' + grades + '</td>'
            + '<td>' + _surverElimStatusHtml(1, it.eliminateType) + '</td>'
            + '<td><button class="btn btn-xs btn-default" onclick="onCancelEliminate(\'' + _surverElimEscape(it.proSubType) + '\',' + it.proId + ')">取消淘汰</button></td>'
            + '</tr>';
    }
    tbody.html(html);
}

// ---------------- 导出（客户端 XLSX）----------------

function _surverElimEnsureXlsx(cb) {
    if (typeof XLSX !== 'undefined') { cb(); return; }
    var s = document.createElement('script');
    s.src = 'https://cdn.sheetjs.com/xlsx-0.20.3/package/dist/xlsx.full.min.js';
    s.onload = function() { cb(); };
    s.onerror = function() { layer.msg('Excel 库加载失败，请检查网络', { icon: 2 }); };
    document.head.appendChild(s);
}

function exportEliminateCandidatesExcel() {
    if (!_surverElimCandidates || _surverElimCandidates.length === 0) {
        layer.msg('暂无数据可导出', { icon: 0 }); return;
    }
    _surverElimEnsureXlsx(function() {
        var header = ['序号', '类别', '申报编号', '项目名称', '申报单位', 'A', 'B', 'C', 'D', '专家评级', '淘汰状态'];
        var aoa = [header];
        _surverElimCandidates.forEach(function(it, idx) {
            aoa.push([
                idx + 1,
                SURVER_SUBTYPE_LABEL[it.proSubType] || (it.proSubType || ''),
                it.proCode || '',
                it.topicName || '',
                it.companyName || '',
                it.gradeA || 0, it.gradeB || 0, it.gradeC || 0, it.gradeD || 0,
                (it.expertGrades || '').replace(/\|/g, ', '),
                _surverElimStatusText(it.eliminated, it.eliminateType)
            ]);
        });
        var ws = XLSX.utils.aoa_to_sheet(aoa);
        ws['!cols'] = [{wch:6},{wch:14},{wch:14},{wch:30},{wch:24},{wch:5},{wch:5},{wch:5},{wch:5},{wch:36},{wch:10}];
        var wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '候选淘汰池');
        XLSX.writeFile(wb, '勘察奖_候选淘汰池.xlsx');
    });
}

// ========== 原版 exportEliminateConfirmedExcel（已注释，保留参考）==========
// 原版从 /listConfirmed 拉"已确认淘汰"列表后用 XLSX.js 前端生成，已被新版后端导出替代
// function exportEliminateConfirmedExcel() {
//     var taskId = $("#taskId").val();
//     var loadIdx = layer.load(1, { shade: [0.3, '#000'] });
//     $.ajax({
//         type: 'GET',
//         url: SURVER_ELIM_PREFIX + '/listConfirmed',
//         data: { taskId: taskId },
//         success: function(r) {
//             layer.close(loadIdx);
//             if (r.code !== 0) { layer.msg(r.msg || '加载失败', { icon: 2 }); return; }
//             _surverElimConfirmed = r.list || [];
//             if (!_surverElimConfirmed.length) {
//                 layer.msg('暂无数据可导出', { icon: 0 }); return;
//             }
//             _surverElimEnsureXlsx(function() {
//                 var header = ['序号', '类别', '申报编号', '项目名称', '申报单位', '申报账号', '分组', '专家分组'];
//                 var aoa = [header];
//                 _surverElimConfirmed.forEach(function(it, idx) {
//                     aoa.push([
//                         idx + 1,
//                         SURVER_SUBTYPE_LABEL[it.proSubType] || (it.proSubType || ''),
//                         it.proCode || '',
//                         it.topicName || '',
//                         it.companyName || '',
//                         it.declareAccount || '',
//                         it.groupName || '',
//                         it.expertGroupName || ''
//                     ]);
//                 });
//                 var ws = XLSX.utils.aoa_to_sheet(aoa);
//                 ws['!cols'] = [{wch:6},{wch:14},{wch:14},{wch:30},{wch:24},{wch:16},{wch:14},{wch:16}];
//                 var wb = XLSX.utils.book_new();
//                 XLSX.utils.book_append_sheet(wb, ws, '已确认淘汰');
//                 XLSX.writeFile(wb, '勘察奖_已确认淘汰名单.xlsx');
//             });
//         },
//         error: function() { layer.close(loadIdx); layer.msg('加载失败', { icon: 2 }); }
//     });
// }
// ========== 原版 END ==========

// ---------------- 导入（客户端解析 + 串行调 setEliminated）原版已注释，替换为后端导入方式 ----------------

// ========== 原版 triggerImportEliminateExcel（已注释，保留参考）==========
// 原版通过前端 XLSX.js 解析 + 串行调 setEliminated，已被新版弹窗+后端批量导入替代
// function triggerImportEliminateExcel() {
//     $("#surverElimImportFile").val('');
//     $("#surverElimImportFile").trigger('click');
// }
// ========== 原版 END ==========

// ========== 原版 onImportEliminateExcel + _surverElimDoImport（已注释，保留参考）==========
// 原版通过前端 XLSX.js 解析 proCode 列后串行调 setEliminated，已被后端批量导入替代
// function onImportEliminateExcel(evt) {
//     var file = evt.target.files && evt.target.files[0];
//     if (!file) return;
//     _surverElimEnsureXlsx(function() {
//         var reader = new FileReader();
//         reader.onload = function(e) {
//             try {
//                 var wb = XLSX.read(e.target.result, { type: 'array' });
//                 var ws = wb.Sheets[wb.SheetNames[0]];
//                 var rows = XLSX.utils.sheet_to_json(ws, { header: 1, defval: '' });
//                 var startIdx = 0;
//                 if (rows.length && rows[0] && /(申报编号|proCode|编号|code)/i.test(String(rows[0][0]))) {
//                     startIdx = 1;
//                 }
//                 var proCodes = [];
//                 for (var i = startIdx; i < rows.length; i++) {
//                     var c = rows[i] && rows[i][0];
//                     if (c == null) continue;
//                     c = String(c).trim();
//                     if (c) proCodes.push(c);
//                 }
//                 if (proCodes.length === 0) { layer.msg('未读取到任何申报编号', { icon: 2 }); return; }
//                 _surverElimDoImport(proCodes);
//             } catch (err) {
//                 console.error(err);
//                 layer.msg('解析 Excel 失败：' + err.message, { icon: 2 });
//             }
//         };
//         reader.readAsArrayBuffer(file);
//     });
// }
//
// function _surverElimDoImport(proCodes) {
//     var taskId = $("#taskId").val();
//     var loadIdx = layer.load(1, { shade: [0.3, '#000'] });
//     var success = 0, fail = 0, notFound = [];
//     var i = 0;
//     function next() {
//         if (i >= proCodes.length) {
//             layer.close(loadIdx);
//             var msg = '导入完成：成功 ' + success + ' 条，失败 ' + fail + ' 条';
//             if (notFound.length) msg += '；未找到：' + notFound.slice(0, 5).join(',') + (notFound.length > 5 ? '...' : '');
//             layer.alert(msg, { icon: success > 0 ? 1 : 2 });
//             loadEliminateCandidates();
//             return;
//         }
//         var code = proCodes[i++];
//         $.ajax({
//             type: 'GET', url: SURVER_ELIM_PREFIX + '/findByProCode',
//             data: { taskId: taskId, proCode: code },
//             success: function(r) {
//                 if (r.code !== 0 || !r.data) { fail++; notFound.push(code); next(); return; }
//                 var info = r.data;
//                 $.ajax({
//                     type: 'POST', url: SURVER_ELIM_PREFIX + '/setEliminated',
//                     data: { proSubType: info.proSubType, proId: info.proId, eliminated: 1 },
//                     success: function(rr) { if (rr.code === 0) success++; else fail++; next(); },
//                     error: function() { fail++; next(); }
//                 });
//             },
//             error: function() { fail++; next(); }
//         });
//     }
//     next();
// }
// ========== 原版 END ==========

/* ============================================================
 * 高级筛选 - 应用 / 重置
 *   - 应用：跳到第 1 页并刷新；queryParams 会自动读取所有 #filterXxx 输入
 *   - 条件持久化到 localStorage（按 taskId+proSubType），刷新/再进页面不丢
 *   - 重置：清空输入、删除存储，再刷新到第 1 页
 * ============================================================ */
function applySurverProFilter() {
    clearSurverProListRowAnchor();
    persistSurverProAdvFilterFromDom();
    var $tbl = $('#exampleTable');
    if ($tbl.data('bootstrap.table')) {
        $tbl.bootstrapTable('selectPage', 1);   // 回到第一页
        $tbl.bootstrapTable('refresh');
    }
}

function resetSurverProFilter() {
    $("#filterProName").val('');
    $("#filterApplyCompany").val('');
    $("#filterMajor").val('');
    $("#filterDeclareAccount").val('');
    $("#filterQcGroupName").val('');
    $("#filterExpertGroupName").val('');
    $("#filterEliminated").val('');
    $("#filterProStat").val('');
    $("#filterReviewResult").val('');
    clearSurverProAdvFilterStorage();
    applySurverProFilter();
}
