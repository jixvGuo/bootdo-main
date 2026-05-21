var prefix = "/surverScore"
// 勘察奖专家打分页面cxq
// Phase C 新增：勘察奖专家侧"淘汰评级"接口前缀
var SURVER_ELIM_EXPERT_PREFIX = '/cpe/suverProcess/eliminate/expert';
// 与管理员侧 surverProList.js SURVER_SUBTYPE_LABEL 一致（导出/弹窗说明用）
var SURVER_SCORE_SUBTYPE_LABEL = {
    contribution: '优秀勘察奖',
    design: '优秀设计奖',
    software: '计算机软件奖',
    standard: '标准设计奖',
    consulting: '咨询奖'
};
// 全局缓存：当前专家在此任务下的"已评等级 / 回避情况 / 是否已确认提交"
var SURVER_EXPERT_GRADE_MAP = {};   // proId -> 'A|B|C|D'
var SURVER_EXPERT_REMARK_MAP = {};  // proId -> '评级理由'
var SURVER_EXPERT_AVOID_SET = {};   // proId -> true (已回避)
var SURVER_EXPERT_LOCKED   = false; // 已确认提交后锁定
// 专家审核意见 / 主评意见（ass_surver_expert_review_opinion，与淘汰活动表解耦）
var SURVER_EXPERT_AUDIT_OPINION = {}; // proId -> 'agree' | 'disagree'
var SURVER_MAIN_REVIEW_TEXT = {};     // proId -> 主评意见文本
var SURVER_MAIN_REVIEW_DONE = {};     // proId -> 是否已提交主评意见

/** 专家打分页高级筛选：独立 localStorage（cpe.surverScoreProList…），与项目列表互不覆盖 */
var SURVER_SCORE_ADV_FILTER_KEY_PREFIX = "cpe.surverScoreProList.advFilter.v1";
/** Tab「勘察设计评级」独立高级筛选 localStorage */
var SURVER_SCORE_RATING_ADV_FILTER_KEY_PREFIX = "cpe.surverScoreProList.ratingAdvFilter.v1";
var _surverScoreAdvFilterSaveTimer = null;
var _surverScoreRatingAdvFilterSaveTimer = null;

function getSurverScoreAdvFilterStorageKey() {
    var tid = ($("#taskId").val() || "") + "";
    var pst = ($("#proSubType").val() || "") + "";
    return SURVER_SCORE_ADV_FILTER_KEY_PREFIX + ":" + tid + ":" + pst;
}

function readSurverScoreAdvFilterFromDom() {
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

function isSurverScoreAdvFilterEmpty(o) {
    return !o.filterProName && !o.filterApplyCompany && !o.filterMajor && !o.filterDeclareAccount
        && !o.filterQcGroupName && !o.filterExpertGroupName && !o.filterEliminated && !o.filterProStat
        && !o.filterReviewResult;
}

function persistSurverScoreAdvFilterFromDom() {
    try {
        var key = getSurverScoreAdvFilterStorageKey();
        var data = readSurverScoreAdvFilterFromDom();
        if (isSurverScoreAdvFilterEmpty(data)) {
            localStorage.removeItem(key);
        } else {
            localStorage.setItem(key, JSON.stringify(data));
        }
    } catch (e) { /* ignore */ }
}

function schedulePersistSurverScoreAdvFilter() {
    if (_surverScoreAdvFilterSaveTimer) {
        clearTimeout(_surverScoreAdvFilterSaveTimer);
    }
    _surverScoreAdvFilterSaveTimer = setTimeout(function () {
        _surverScoreAdvFilterSaveTimer = null;
        persistSurverScoreAdvFilterFromDom();
    }, 400);
}

function restoreSurverScoreAdvFilterFromStorage() {
    try {
        var raw = localStorage.getItem(getSurverScoreAdvFilterStorageKey());
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
        var filled = readSurverScoreAdvFilterFromDom();
        if (!isSurverScoreAdvFilterEmpty(filled)) {
            $("#scoreProFilterPanel").addClass("in");
            $('a[href="#scoreProFilterPanel"]').attr("aria-expanded", "true");
        }
    } catch (e) { /* ignore */ }
}

function clearSurverScoreAdvFilterStorage() {
    try {
        localStorage.removeItem(getSurverScoreAdvFilterStorageKey());
    } catch (e) { /* ignore */ }
}

function getSurverScoreRatingAdvFilterStorageKey() {
    var tid = ($("#taskId").val() || "") + "";
    var pst = ($("#proSubType").val() || "") + "";
    return SURVER_SCORE_RATING_ADV_FILTER_KEY_PREFIX + ":" + tid + ":" + pst;
}

function readSurverScoreRatingAdvFilterFromDom() {
    return {
        filterProName: ($("#ratingFilterProName").val() || "").trim(),
        filterApplyCompany: ($("#ratingFilterApplyCompany").val() || "").trim(),
        filterMajor: ($("#ratingFilterMajor").val() || "").trim(),
        filterDeclareAccount: ($("#ratingFilterDeclareAccount").val() || "").trim(),
        filterQcGroupName: ($("#ratingFilterQcGroupName").val() || "").trim(),
        filterExpertGroupName: ($("#ratingFilterExpertGroupName").val() || "").trim(),
        filterEliminated: ($("#ratingFilterEliminated").val() || "").trim(),
        filterProStat: ($("#ratingFilterProStat").val() || "").trim(),
        filterReviewResult: ($("#ratingFilterReviewResult").val() || "").trim()
    };
}

function isSurverScoreRatingAdvFilterEmpty(o) {
    return !o.filterProName && !o.filterApplyCompany && !o.filterMajor && !o.filterDeclareAccount
        && !o.filterQcGroupName && !o.filterExpertGroupName && !o.filterEliminated && !o.filterProStat
        && !o.filterReviewResult;
}

function persistSurverScoreRatingAdvFilterFromDom() {
    try {
        var key = getSurverScoreRatingAdvFilterStorageKey();
        var data = readSurverScoreRatingAdvFilterFromDom();
        if (isSurverScoreRatingAdvFilterEmpty(data)) {
            localStorage.removeItem(key);
        } else {
            localStorage.setItem(key, JSON.stringify(data));
        }
    } catch (e) { /* ignore */ }
}

function schedulePersistSurverScoreRatingAdvFilter() {
    if (_surverScoreRatingAdvFilterSaveTimer) {
        clearTimeout(_surverScoreRatingAdvFilterSaveTimer);
    }
    _surverScoreRatingAdvFilterSaveTimer = setTimeout(function () {
        _surverScoreRatingAdvFilterSaveTimer = null;
        persistSurverScoreRatingAdvFilterFromDom();
    }, 400);
}

function restoreSurverScoreRatingAdvFilterFromStorage() {
    try {
        var raw = localStorage.getItem(getSurverScoreRatingAdvFilterStorageKey());
        if (raw == null || raw === "") {
            return;
        }
        var o = JSON.parse(raw);
        if (!o || typeof o !== "object") {
            return;
        }
        $("#ratingFilterProName").val(o.filterProName != null ? o.filterProName : "");
        $("#ratingFilterApplyCompany").val(o.filterApplyCompany != null ? o.filterApplyCompany : "");
        $("#ratingFilterMajor").val(o.filterMajor != null ? o.filterMajor : "");
        $("#ratingFilterDeclareAccount").val(o.filterDeclareAccount != null ? o.filterDeclareAccount : "");
        $("#ratingFilterQcGroupName").val(o.filterQcGroupName != null ? o.filterQcGroupName : "");
        $("#ratingFilterExpertGroupName").val(o.filterExpertGroupName != null ? o.filterExpertGroupName : "");
        $("#ratingFilterEliminated").val(o.filterEliminated != null ? o.filterEliminated : "");
        $("#ratingFilterProStat").val(o.filterProStat != null ? o.filterProStat : "");
        $("#ratingFilterReviewResult").val(o.filterReviewResult != null ? o.filterReviewResult : "");
        var filled = readSurverScoreRatingAdvFilterFromDom();
        if (!isSurverScoreRatingAdvFilterEmpty(filled)) {
            $("#scoreRatingFilterPanel").addClass("in");
            $('a[href="#scoreRatingFilterPanel"]').attr("aria-expanded", "true");
        }
    } catch (e) { /* ignore */ }
}

function clearSurverScoreRatingAdvFilterStorage() {
    try {
        localStorage.removeItem(getSurverScoreRatingAdvFilterStorageKey());
    } catch (e) { /* ignore */ }
}

/** 形审评语/结论：去 HTML 后纯文本展示 */
function surverScoreStripHtml(html) {
    if (html == null || html === undefined) {
        return "";
    }
    var s = String(html);
    if (typeof document !== "undefined") {
        var el = document.createElement("div");
        el.innerHTML = s;
        s = el.textContent || el.innerText || s;
    } else {
        s = s.replace(/<[^>]+>/g, "");
    }
    return s.replace(/\s+/g, " ").trim();
}

/** 形式审查结论（原形审结果列逻辑，无链接） */
function formatScoreReviewConclusionText(row) {
    var text = surverScoreStripHtml(row.latestReviewResult);
    if (!text) {
        var checkStarted = row.checkStartTime && String(row.checkStartTime).trim() !== "";
        text = checkStarted ? "暂无形审结果" : "形审未开始";
    }
    return text;
}

/** 形式审查问题（原形审评语；无记录为「暂无形审结果」） */
function formatScoreReviewRemarksText(row) {
    var text = surverScoreStripHtml(row.latestReviewRemarks);
    if (!text) {
        return "暂无形审结果";
    }
    return text;
}

function buildScoreTableQueryParams(params, useRatingFilter) {
    var qp = {
        limit: params.limit,
        offset: params.offset,
        proSubType: $("#proSubType").val(),
        taskId: $("#taskId").val()
    };
    var prefix = useRatingFilter ? "ratingFilter" : "filter";
    var fmap = {
        filterProName: $("#" + prefix + "ProName").val(),
        filterApplyCompany: $("#" + prefix + "ApplyCompany").val(),
        filterMajor: $("#" + prefix + "Major").val(),
        filterDeclareAccount: $("#" + prefix + "DeclareAccount").val(),
        filterQcGroupName: $("#" + prefix + "QcGroupName").val(),
        filterExpertGroupName: $("#" + prefix + "ExpertGroupName").val(),
        filterEliminated: $("#" + prefix + "Eliminated").val(),
        filterProStat: $("#" + prefix + "ProStat").val(),
        filterReviewResult: $("#" + prefix + "ReviewResult").val()
    };
    Object.keys(fmap).forEach(function (k) {
        var v = fmap[k];
        if (v !== undefined && v !== null && (v + "").length > 0) {
            qp[k] = (v + "").trim();
        }
    });
    return qp;
}

function formatterScoreExpertGrade(row) {
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

function formatterScoreMainReview(row) {
    var pid = row.proId;
    var sub = (row.proSubType || '').replace(/'/g, "\\'");
    if (SURVER_MAIN_REVIEW_DONE[pid]) {
        var editL = !SURVER_EXPERT_LOCKED
            ? ' <a href="javascript:void(0)" onclick="openSurverMainReviewDialog(' + pid + ', \'' + sub + '\', false)">修改</a>'
            : '';
        return '<span class="label label-success" style="font-size:12px;">已提交</span> '
            + '<a href="javascript:void(0)" onclick="openSurverMainReviewDialog(' + pid + ', \'' + sub + '\', true)">查看</a>'
            + editL;
    }
    var fill = SURVER_EXPERT_LOCKED ? 'disabled' : '';
    return '<span style="color:#999;font-size:12px;margin-right:6px;">未填写</span>'
        + '<a href="javascript:void(0)" class="btn btn-primary btn-xs ' + fill + '" '
        + 'onclick="openSurverMainReviewDialog(' + pid + ', \'' + sub + '\', false)">填写</a>';
}

/** 顶部菜单栏打开「勘察设计评级」页（与「查看项目」一致，走 page/contabs） */
function openSurverRatingTab() {
    var taskId = ($('#taskId').val() || '') + '';
    var proSubType = ($('#proSubType').val() || '') + '';
    var url = '/surverScore/proRatingList?taskId=' + encodeURIComponent(taskId)
        + '&proSubType=' + encodeURIComponent(proSubType);
    // 原：页内 Bootstrap Tab（本项目加载了 semantic UI，.tab('show') 会报错）
    // $('a[href="#tab-score-rating"]').tab('show');
    if (typeof page === 'function') {
        page(url, '勘察设计评级', 20220507);
    } else {
        window.location.href = url;
    }
}

function getSurverScorePageMode() {
    var m = ($('#surverScorePageMode').val() || 'main') + '';
    return m === 'rating' ? 'rating' : 'main';
}

$(function () {
    // [DEBUG] 打印当前页面使用的 taskId，方便排查专家/管理员 taskId 不一致问题
    console.log('[DEBUG-score] 专家打分页 taskId=' + $("#taskId").val());
    restoreSurverScoreAdvFilterFromStorage();
    restoreSurverScoreRatingAdvFilterFromStorage();
    var $scoreFilterPanel = $("#scoreProFilterPanel");
    $scoreFilterPanel.on("input change", "input, select", schedulePersistSurverScoreAdvFilter);
    $scoreFilterPanel.on("keydown", "input.form-control", function (e) {
        if (e.which === 13 || e.keyCode === 13) {
            e.preventDefault();
            applyScoreProFilter();
        }
    });
    $scoreFilterPanel.on("change", "select.form-control", function () {
        applyScoreProFilter();
    });
    var $ratingFilterPanel = $("#scoreRatingFilterPanel");
    $ratingFilterPanel.on("input change", "input, select", schedulePersistSurverScoreRatingAdvFilter);
    $ratingFilterPanel.on("keydown", "input.form-control", function (e) {
        if (e.which === 13 || e.keyCode === 13) {
            e.preventDefault();
            applyScoreRatingFilter();
        }
    });
    $ratingFilterPanel.on("change", "select.form-control", function () {
        applyScoreRatingFilter();
    });
    var pageMode = getSurverScorePageMode();
    loadExpertGradeContext(function () {
        if (pageMode === 'rating') {
            loadScoreRatingTable();
            renderExpertSubmitToolbar();
        } else {
            loadScoreMainTable();
            // 原：同页加载评级 Tab 子表 loadScoreRatingTable();
        }
    });
});

/** 拉取当前专家在该任务下的评级/回避/锁定状态，并渲染顶部按钮；随后加载审核意见/主评意见 */
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
            loadExpertReviewOpinions(cb);
        },
        error: function() { loadExpertReviewOpinions(cb); }
    });
}

/** 从库加载专家审核意见 + 主评意见（与淘汰确认提交锁定无关，确认后由 save 接口拒绝写入） */
function loadExpertReviewOpinions(cb) {
    var taskId = $("#taskId").val();
    if (!taskId) { if (cb) cb(); return; }
    $.ajax({
        type: 'GET',
        url: SURVER_ELIM_EXPERT_PREFIX + '/review/listMy',
        data: { taskId: taskId },
        success: function(r) {
            if (r && r.code === 0) {
                SURVER_EXPERT_AUDIT_OPINION = r.auditOpinions || {};
                SURVER_MAIN_REVIEW_TEXT = r.mainReviewTexts || {};
                SURVER_MAIN_REVIEW_DONE = {};
                var sub = r.mainReviewSubmitted || {};
                Object.keys(sub).forEach(function(k) {
                    if (sub[k] == 1 || sub[k] === true) {
                        SURVER_MAIN_REVIEW_DONE[k] = true;
                    }
                });
            }
            if (cb) cb();
        },
        error: function() { if (cb) cb(); }
    });
}

/** Tab1「勘察奖项打分」列表 */
function loadScoreMainTable() {
    if ($('#exampleTable').data('bootstrap.table')) {
        $('#exampleTable').bootstrapTable('destroy');
    }
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
                    return buildScoreTableQueryParams(params, false);
                },
                // //请求服务器数据时，你可以通过重写参数的方式添加一些额外的参数，例如 toolbar 中的参数 如果
                // queryParamsType = 'limit' ,返回参数必须包含
                // limit, offset, search, sort, order 否则, 需要包含:
                // pageSize, pageNumber, searchText, sortName,
                // sortOrder.
                // 返回false将会终止请求
                columns: [
                    /* 原主表列配置（单页含形审结果/淘汰/主评），保留备查：
                    { field: 'proCode', title: '项目编号' },
                    { field: 'proName', title: '项目名称' },
                    { field: 'extSurverNovelty', title: '是否有查新', ... },
                    { field: '_expertAuditOpinion', title: '专家审核意见', ... },
                    { field: 'declareAccount', title: '申报账号' },
                    { field: 'latestReviewResult', title: '形审结果', formatter: 链接 showReviewRecordList },
                    { field: '_elimGrade', title: '淘汰', ... openSurverGradeDialog },
                    { field: '_mainReview', title: '主评意见', ... },
                    { title: '操作', formatter: 仅「查看项目」 }
                    */
                    { field: 'declareAccount', title: '申报账号' },
                    { field: 'proCode', title: '项目编号' },
                    { field: 'proName', title: '项目名称' },
                    {
                        field: 'extSurverNovelty',
                        title: '是否有查新',
                        align: 'center',
                        formatter: function (value) {
                            var v = (value || '').toString().trim();
                            if (v === '是' || v === '否') {
                                return '<span style="font-size:12px;">' + v + '</span>';
                            }
                            return '<span style="color:#bbb;">—</span>';
                        }
                    },
                    {
                        field: 'latestReviewRemarks',
                        title: '形式审查问题',
                        formatter: function (value, row) {
                            return formatScoreReviewRemarksText(row);
                        }
                    },
                    {
                        field: 'latestReviewResult',
                        title: '形式审查结论',
                        formatter: function (value, row) {
                            return formatScoreReviewConclusionText(row);
                        }
                    },
                    {
                        field: '_expertAuditOpinion',
                        title: '专家审核意见',
                        align: 'center',
                        formatter: function (value, row) {
                            var pid = row.proId;
                            var cur = SURVER_EXPERT_AUDIT_OPINION[pid] || '';
                            var locked = SURVER_EXPERT_LOCKED ? 'disabled' : '';
                            var s0 = cur === '' ? 'selected' : '';
                            var s1 = cur === 'agree' ? 'selected' : '';
                            var s2 = cur === 'disagree' ? 'selected' : '';
                            return '<select class="form-control input-sm" style="min-width:96px;max-width:120px;display:inline-block;" '
                                + locked + ' onchange="onSurverExpertAuditOpinion(this, ' + pid + ', \'' + (row.proSubType || '') + '\')">'
                                + '<option value="" ' + s0 + '>请选择</option>'
                                + '<option value="agree" ' + s1 + '>同意</option>'
                                + '<option value="disagree" ' + s2 + '>不同意</option>'
                                + '</select>';
                        }
                    },
                    {
                        title: '操作',
                        field: 'id',
                        align: 'center',
                        formatter: function (value, row) {
                            var d = '<a class="btn btn-warning btn-sm" href="#" onclick="onwatch(\''
                                + row.proId + '\',\'' + row.proSubType + '\')">查看项目</a> ';
                            var r = '<a class="btn btn-primary btn-sm" href="#" style="margin-left:4px;" onclick="openSurverRatingTab()">评级</a> ';
                            return d + r;
                        }
                    }
                    // 原列配置（形审结果链接、淘汰、主评意见等）已迁至 Tab「勘察设计评级」
                ]
            });
}

/** Tab2「勘察设计评级」列表 */
function loadScoreRatingTable() {
    if ($('#ratingTable').data('bootstrap.table')) {
        $('#ratingTable').bootstrapTable('destroy');
    }
    $('#ratingTable').bootstrapTable({
        method: 'get',
        url: "/surverPro/get/proList",
        iconSize: 'outline',
        striped: true,
        dataType: "json",
        pagination: true,
        singleSelect: false,
        pageSize: 10,
        pageNumber: 1,
        showColumns: false,
        sidePagination: "server",
        queryParams: function (params) {
            return buildScoreTableQueryParams(params, true);
        },
        columns: [
            { field: 'declareAccount', title: '申报账号' },
            { field: 'proCode', title: '项目编号' },
            { field: 'proName', title: '项目名称' },
            {
                field: '_elimGrade',
                title: '专家评级',
                align: 'center',
                formatter: function (value, row) {
                    return formatterScoreExpertGrade(row);
                }
            },
            // 它又不要这个主评意见了，说要搬到别处去
            // {
            //     field: '_mainReview',
            //     title: '主评意见',
            //     align: 'center',
            //     formatter: function (value, row) {
            //         return formatterScoreMainReview(row);
            //     }
            // },
            {
                title: '操作',
                field: 'id',
                align: 'center',
                formatter: function (value, row) {
                    return '<a class="btn btn-warning btn-sm" href="#" onclick="onwatch(\''
                        + row.proId + '\',\'' + row.proSubType + '\')">查看</a> ';
                }
            }
        ]
    });
}

/** 兼容旧调用 */
function load() {
    loadScoreMainTable();
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
    if ($('#exampleTable').data('bootstrap.table')) {
        // $('#exampleTable').bootstrapTable('refresh');
        $('#exampleTable').bootstrapTable('refresh');
    }
    if ($('#ratingTable').data('bootstrap.table')) {
        $('#ratingTable').bootstrapTable('refresh');
    }
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
    // 原：单行工具条，无右侧「下载评分标准」
    // var html =
    //     '<div id="surverElimExpertToolbar" style="margin:8px 0;padding:8px;...">'
    //     + '<span style="margin-right:16px;">';
    // 评级页 surverExpertScorePage=1；原判断 === 'true' 导致按钮永不显示
    // var isExpertScorePage = $('#surverExpertScorePage').val() === 'true' || $('#surverExpertScorePage').val() === true;
    var isExpertScorePage = getSurverScorePageMode() === 'rating'
        || $('#surverExpertScorePage').val() === '1'
        || $('#surverExpertScorePage').val() === 'true';
    var html =
        '<div id="surverElimExpertToolbar" style="margin:8px 0;padding:8px;border:1px solid #e5e5e5;border-radius:4px;background:#f9fafc;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;">'
        // + '<span style="margin-right:16px;color:#333;"><b>淘汰评级状态：</b></span>'
        // + '<span style="margin-right:16px;">共 <b>' + totalCount + '</b> 项</span>'
        // + '<span style="margin-right:16px;">已评 <b style="color:#5cb85c;">' + stat.gradedCount + '</b> 项</span>'
        // + '<span style="margin-right:16px;">已回避 <b style="color:#d9534f;">' + stat.avoidedCount + '</b> 项</span>'
        // + (remaining > 0 ? '<span style="margin-right:16px;">剩余 <b style="color:#f0ad4e;">' + remaining + '</b> 项未处理</span>' : '')
        + '<span style="margin-right:16px;flex:1 1 auto;">';
    if (!locked) {
        // html += '确认提交淘汰评级名单';
        html += '<button class="btn btn-danger btn-sm" style="font-size:13px;padding:4px 10px;vertical-align:middle;" '
            // + 'onclick="onSurverConfirmSubmitElim()">确认淘汰结果</button>';
            // 按要求非要改名字
            + 'onclick="onSurverConfirmSubmitElim()">确认评级结果</button>';
        html += '<button class="btn btn-primary btn-sm" style="font-size:13px;padding:4px 10px;vertical-align:middle;margin-left:8px;" '
            // + 'onclick="openSurverDownloadElimRemarks()"><i class="fa fa-download"></i> 下载淘汰评语</button>';
            // 按要求非要改名字
            + 'onclick="openSurverDownloadElimRemarks()"><i class="fa fa-download"></i> 下载评级结果</button>';
    } else {
        html += '<span style="display:inline-block;padding:4px 10px;font-size:13px;border-radius:3px;background:#5cb85c;color:#fff;vertical-align:middle;">'
            + '已确认提交（不可撤回）</span>';
        html += '<button class="btn btn-default btn-sm" style="font-size:13px;padding:4px 10px;vertical-align:middle;margin-left:8px;" '
            // + 'onclick="openSurverDownloadElimRemarks()"><i class="fa fa-download"></i> 下载淘汰评语</button>';
            // 按要求非要改名字
            + 'onclick="openSurverDownloadElimRemarks()"><i class="fa fa-download"></i> 下载评级结果</button>';
    }
    // html += '<button class="btn btn-success btn-sm" style="font-size:13px;padding:4px 10px;vertical-align:middle;margin-left:8px;" '
    //     + 'onclick="downloadSurverMainReviewZip()"><i class="fa fa-download"></i> 下载主评意见</button>';
    html += '</span>';
    if (isExpertScorePage) {
        html += '<span style="flex:0 0 auto;margin-left:12px;">'
            + '<button type="button" class="btn btn-info btn-sm" style="font-size:13px;padding:4px 10px;" '
            + 'onclick="downloadSurverScoreStandardFile()"><i class="fa fa-download"></i> 下载评分标准</button>'
            + '</span>';
    }
    html += '</div>';
    var $mount = $('#surverRatingToolbar');
    if ($mount.length) {
        $mount.html(html);
    }
    // 原：插入主表上方 $('#exampleTable').before(html);
}

/** 专家：下载协会上传的勘察奖任务评分标准 */
function downloadSurverScoreStandardFile() {
    var taskId = $('#taskId').val();
    if (!taskId) {
        layer.msg('缺少任务ID', { icon: 2 });
        return;
    }
    // 原：直接跳转下载，无文件时浏览器页内显示纯文本「暂未上传评分标准文件」
    // window.location.href = '/surverScore/downloadScoreStandardFile?taskId=' + encodeURIComponent(taskId);
    $.ajax({
        type: 'GET',
        url: '/surverScore/checkScoreStandardFile',
        data: { taskId: taskId },
        success: function (r) {
            if (r && r.code === 0) {
                window.location.href = '/surverScore/downloadScoreStandardFile?taskId=' + encodeURIComponent(taskId);
                return;
            }
            layer.alert((r && r.msg) ? r.msg : '暂未上传评分标准文件', { icon: 2, title: '提示' });
        },
        error: function () {
            layer.alert('检查评分标准文件失败，请稍后重试', { icon: 2, title: '提示' });
        }
    });
}

function downloadSurverMainReviewZip() {
    var taskId = $('#taskId').val();
    if (!taskId) {
        layer.msg('缺少任务ID', { icon: 2 });
        return;
    }
    var qp = buildScoreTableQueryParams({ limit: 1, offset: 0 }, true);
    var parts = [];
    Object.keys(qp).forEach(function (k) {
        if (qp[k] !== undefined && qp[k] !== null && (qp[k] + '').length > 0) {
            parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(qp[k]));
        }
    });
    window.location.href = SURVER_ELIM_EXPERT_PREFIX + '/review/downloadMainReviewZip?' + parts.join('&');
}

/** 淘汰评语：加载 SheetJS（与管理员淘汰导出一致） */
function _surverScoreEnsureXlsx(cb) {
    if (typeof XLSX !== 'undefined') { cb(); return; }
    var s = document.createElement('script');
    s.src = 'https://cdn.sheetjs.com/xlsx-0.20.3/package/dist/xlsx.full.min.js';
    s.onload = function () { cb(); };
    s.onerror = function () { layer.msg('Excel 库加载失败，请检查网络', { icon: 2 }); };
    document.head.appendChild(s);
}

/** 淘汰评语导出：ExcelJS（支持表头底色；SheetJS 社区版写 xlsx 不保留样式） */
function _surverScoreEnsureExceljs(cb) {
    if (typeof ExcelJS !== 'undefined') { cb(); return; }
    var s = document.createElement('script');
    s.src = 'https://cdn.jsdelivr.net/npm/exceljs@4.4.0/dist/exceljs.min.js';
    s.onload = function () { cb(); };
    s.onerror = function () { layer.msg('Excel 导出库加载失败，请检查网络', { icon: 2 }); };
    document.head.appendChild(s);
}

function _surverScoreEsc(s) {
    if (s == null || s === undefined) return '';
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function _surverScoreGradeBadgeHtml(g) {
    var gv = (g == null || g === '') ? '-' : String(g);
    var color = (gv === 'D') ? '#d9534f' : (gv === 'C' ? '#f0ad4e' : (gv === 'A' ? '#5cb85c' : '#5bc0de'));
    return '<span style="display:inline-block;background:' + color + ';color:#fff;border-radius:3px;padding:2px 8px;font-size:11px;">'
        + _surverScoreEsc(gv) + '</span>';
}

function _surverScoreElimStatusHtml(elim) {
    if (elim == 1 || elim === '1') {
        return '<span style="background:#d9534f;color:#fff;border-radius:3px;padding:2px 8px;font-size:11px;">已淘汰</span>';
    }
    return '<span style="color:#999;">未淘汰</span>';
}

/**
 * 专家侧：仅本人淘汰评级及评语（下载/导出）
 */
function openSurverDownloadElimRemarks() {
    var taskId = $('#taskId').val();
    var proSubType = $('#proSubType').val() || '';
    if (!taskId) {
        layer.msg('缺少任务ID', { icon: 2 });
        return;
    }
    var loadIdx = layer.load(1, { shade: [0.2, '#000'] });
    $.ajax({
        type: 'GET',
        url: SURVER_ELIM_EXPERT_PREFIX + '/listGroupEliminateDetail',
        data: { taskId: taskId, proSubType: proSubType },
        success: function (r) {
            layer.close(loadIdx);
            if (r.code !== 0) {
                layer.msg(r.msg || '加载失败', { icon: 2 });
                return;
            }
            var list = r.list || [];
            window._surverGroupElimDetailLast = list;
            var rowsHtml = '';
            if (!list.length) {
                rowsHtml = '<tr><td colspan="12" style="text-align:center;color:#999;">暂无评级数据</td></tr>';
            } else {
                for (var i = 0; i < list.length; i++) {
                    var it = list[i];
                    var st = it.proSubType || '';
                    rowsHtml += '<tr>'
                        + '<td style="text-align:center;">' + (i + 1) + '</td>'
                        + '<td>' + _surverScoreEsc(SURVER_SCORE_SUBTYPE_LABEL[st] || st || '-') + '</td>'
                        + '<td>' + _surverScoreEsc(it.proCode || '-') + '</td>'
                        + '<td>' + _surverScoreEsc(it.topicName || '-') + '</td>'
                        + '<td>' + _surverScoreEsc(it.companyName || '-') + '</td>'
                        + '<td>' + _surverScoreEsc(it.declareAccount || '-') + '</td>'
                        + '<td>' + _surverScoreEsc(it.groupName || '-') + '</td>'
                        + '<td>' + _surverScoreEsc(it.expertGroupName || '-') + '</td>'
                        + '<td>' + _surverScoreEsc(it.expertName || '-') + '</td>'
                        + '<td style="text-align:center;">' + _surverScoreGradeBadgeHtml(it.grade) + '</td>'
                        + '<td style="text-align:center;">' + _surverScoreElimStatusHtml(it.eliminated) + '</td>'
                        + '<td style="max-width:220px;white-space:pre-wrap;word-break:break-all;">'
                        + _surverScoreEsc(it.remark || '') + '</td>'
                        + '</tr>';
                }
            }
            var inner = ''
                + '<div style="padding:12px 16px;">'
                + '<div style="max-height:440px;overflow:auto;border:1px solid #e5e5e5;border-radius:4px;">'
                + '<table class="table table-bordered table-condensed" style="font-size:12px;margin-bottom:0;">'
                + '<thead><tr>'
                + '<th style="width:48px;">序号</th>'
                + '<th style="min-width:88px;">类别</th>'
                + '<th style="min-width:88px;">项目编号</th>'
                + '<th style="min-width:140px;">项目名称</th>'
                + '<th style="min-width:80px;">申报单位</th>'
                + '<th style="min-width:88px;">申报账号</th>'
                + '<th style="min-width:72px;">分组</th>'
                + '<th style="min-width:100px;">专家分组</th>'
                + '<th style="min-width:80px;">专家名称</th>'
                + '<th style="min-width:72px;">专家评级</th>'
                + '<th style="min-width:72px;">淘汰状态</th>'
                + '<th style="min-width:180px;">评级理由</th>'
                + '</tr></thead><tbody>' + rowsHtml + '</tbody></table>'
                + '</div>'
                + '<div style="margin-top:12px;text-align:right;">'
                + '<button type="button" class="btn btn-success btn-sm" onclick="exportSurverGroupElimDetailExcel()">'
                + '<i class="fa fa-file-excel-o"></i> 导出 Excel</button>'
                + '</div>'
                + '</div>';
            layer.open({
                type: 1,
                title: '下载淘汰评语详情',
                area: ['1320px', '680px'],
                shadeClose: true,
                content: inner
            });
        },
        error: function () {
            layer.close(loadIdx);
            layer.msg('请求失败', { icon: 2 });
        }
    });
}

/** 导出上一步弹窗中已加载的淘汰评语明细（表头黄色底，使用 ExcelJS） */
function exportSurverGroupElimDetailExcel() {
    var list = window._surverGroupElimDetailLast;
    if (!list || !list.length) {
        layer.msg('暂无数据可导出', { icon: 0 });
        return;
    }
    _surverScoreEnsureExceljs(function () {
        var fname = '勘察奖_淘汰评语_本人.xlsx';
        try {
            var pst = $('#proSubType').val() || '';
            if (pst) fname = '勘察奖_淘汰评语_' + pst + '_本人.xlsx';
        } catch (e2) { /* ignore */ }

        var thin = { style: 'thin', color: { argb: 'FFB4B4B4' } };
        var wb = new ExcelJS.Workbook();
        var ws = wb.addWorksheet('淘汰评语', { views: [{ state: 'frozen', ySplit: 1 }] });

        var headers = ['序号', '类别', '项目编号', '项目名称', '申报单位', '申报账号', '分组', '专家分组', '专家名称', '专家评级', '淘汰状态', '评级理由'];
        var hr = ws.addRow(headers);
        hr.height = 22;
        hr.eachCell({ includeEmpty: true }, function (cell) {
            cell.font = { bold: true, size: 11, name: 'Microsoft YaHei' };
            cell.fill = {
                type: 'pattern',
                pattern: 'solid',
                fgColor: { argb: 'FFFFFF00' }
            };
            cell.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true };
            cell.border = { top: thin, left: thin, bottom: thin, right: thin };
        });

        list.forEach(function (it, idx) {
            var st = it.proSubType || '';
            var elim = (it.eliminated == 1 || it.eliminated === '1') ? '已淘汰' : '未淘汰';
            var dr = ws.addRow([
                idx + 1,
                SURVER_SCORE_SUBTYPE_LABEL[st] || st || '',
                it.proCode || '',
                it.topicName || '',
                it.companyName || '',
                it.declareAccount || '',
                it.groupName || '',
                it.expertGroupName || '',
                it.expertName || '',
                it.grade || '',
                elim,
                it.remark || ''
            ]);
            dr.eachCell({ includeEmpty: true }, function (cell) {
                cell.border = { top: thin, left: thin, bottom: thin, right: thin };
                cell.alignment = { vertical: 'top', wrapText: true };
            });
            dr.getCell(1).alignment = { vertical: 'middle', horizontal: 'center', wrapText: false };
        });

        ws.columns = [
            { width: 6 }, { width: 14 }, { width: 12 }, { width: 32 }, { width: 16 },
            { width: 14 }, { width: 12 }, { width: 18 }, { width: 12 }, { width: 8 },
            { width: 10 }, { width: 42 }
        ];

        wb.xlsx.writeBuffer().then(function (buffer) {
            var blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
            if (window.navigator && window.navigator.msSaveOrOpenBlob) {
                window.navigator.msSaveOrOpenBlob(blob, fname);
            } else {
                var a = document.createElement('a');
                a.href = window.URL.createObjectURL(blob);
                a.download = fname;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(a.href);
            }
            layer.msg('导出成功', { icon: 1, time: 1000 });
        }).catch(function (err) {
            console.error(err);
            layer.msg('导出失败', { icon: 2 });
        });
    });
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

/** 专家审核意见 → ass_surver_expert_review_opinion.audit_opinion */
function onSurverExpertAuditOpinion(sel, proId, proSubType) {
    if (SURVER_EXPERT_LOCKED) {
        layer.msg('已确认提交，无法再修改', { icon: 2 });
        // loadExpertReviewOpinions(function () { $('#exampleTable').bootstrapTable('refresh'); });
        loadExpertReviewOpinions(function () { reLoad(); });
        return;
    }
    var v = $(sel).val();
    var taskId = $("#taskId").val();
    $.ajax({
        type: 'POST',
        url: SURVER_ELIM_EXPERT_PREFIX + '/review/saveAudit',
        data: { taskId: taskId, proId: proId, proSubType: proSubType || '', auditOpinion: v },
        success: function (r) {
            if (r && r.code === 0) {
                if (!v) {
                    delete SURVER_EXPERT_AUDIT_OPINION[proId];
                } else {
                    SURVER_EXPERT_AUDIT_OPINION[proId] = v;
                }
                layer.msg('已保存', { icon: 1, time: 600 });
            } else {
                layer.msg(r.msg || '保存失败', { icon: 2 });
                // loadExpertReviewOpinions(function () { $('#exampleTable').bootstrapTable('refresh'); });
                loadExpertReviewOpinions(function () { reLoad(); });
            }
        },
        error: function () {
            layer.msg('请求失败', { icon: 2 });
            // loadExpertReviewOpinions(function () { $('#exampleTable').bootstrapTable('refresh'); });
            loadExpertReviewOpinions(function () { reLoad(); });
        }
    });
}

/**
 * 主评意见
 * @param viewOnly true=仅查看；false=编辑（未提交=首次提交 submitMain=true；已提交且未锁定=修改 submitMain=false）
 */
function openSurverMainReviewDialog(proId, proSubType, viewOnly) {
    if (!viewOnly && SURVER_EXPERT_LOCKED) {
        layer.msg('已确认提交，无法再修改', { icon: 2 });
        return;
    }
    var done = !!SURVER_MAIN_REVIEW_DONE[proId];
    var ro = !!viewOnly || (done && SURVER_EXPERT_LOCKED);
    var amend = done && !viewOnly && !SURVER_EXPERT_LOCKED;
    var existing = SURVER_MAIN_REVIEW_TEXT[proId] || '';
    var maxLen = 2000;
    var escaped = (existing || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
    var dlgHtml = '<div style="padding:16px 20px;">'
        + '<p style="color:#888;font-size:12px;margin:0 0 8px 0;">' + (ro ? '主评意见' : (amend ? '修改主评意见' : '请填写主评意见')) + '</p>'
        + '<textarea id="_surverMainReviewTa" class="form-control" rows="10" maxlength="' + maxLen + '" '
        + (ro ? 'readonly ' : '')
        + 'style="resize:vertical;">' + escaped + '</textarea>'
        + '</div>';
    if (ro) {
        layer.open({
            type: 1,
            title: '主评意见',
            area: ['520px', '420px'],
            shadeClose: true,
            content: dlgHtml,
            btn: ['关闭'],
            yes: function (idx) { layer.close(idx); }
        });
        return;
    }
    layer.open({
        type: 1,
        title: '主评意见',
        area: ['520px', '420px'],
        shadeClose: false,
        content: dlgHtml,
        btn: [amend ? '保存' : '提交', '取消'],
        yes: function (idx) {
            var text = ($('#_surverMainReviewTa').val() || '').trim();
            if (!text) {
                layer.msg('请填写内容后再提交', { icon: 2 });
                return;
            }
            var taskId = $("#taskId").val();
            var submitMain = !done;
            $.ajax({
                type: 'POST',
                url: SURVER_ELIM_EXPERT_PREFIX + '/review/saveMain',
                data: {
                    taskId: taskId,
                    proId: proId,
                    proSubType: proSubType || '',
                    mainReviewText: text,
                    submitMain: submitMain ? '1' : '0'
                },
                success: function (r) {
                    if (r && r.code === 0) {
                        SURVER_MAIN_REVIEW_TEXT[proId] = text;
                        SURVER_MAIN_REVIEW_DONE[proId] = true;
                        layer.close(idx);
                        layer.msg(submitMain ? '已提交' : '已保存', { icon: 1, time: 800 });
                        // $('#exampleTable').bootstrapTable('refresh');
                        reLoad();
                    } else {
                        layer.msg(r.msg || '保存失败', { icon: 2 });
                    }
                },
                error: function () { layer.msg('请求失败', { icon: 2 }); }
            });
        },
        btn2: function (idx) {
            layer.close(idx);
        }
    });
}

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
                            // $('#exampleTable').bootstrapTable('refresh');
                        reLoad();
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
                        // $('#exampleTable').bootstrapTable('refresh');
                        reLoad();
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
                        // $('#exampleTable').bootstrapTable('refresh');
                        reLoad();
                    });
                } else { layer.msg(r.msg || '操作失败', { icon: 2 }); }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

function onSurverConfirmSubmitElim() {
    // layer.confirm('确认提交淘汰评级名单？提交后不可撤回，请确保所有项目已评级或已回避。', ...
    layer.confirm('确认淘汰结果？提交后不可撤回，请确保所有项目已评级或已回避。', { btn: ['确定', '取消'] }, function(idx) {
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
                        // $('#exampleTable').bootstrapTable('refresh');
                        reLoad();
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
                        // $('#exampleTable').bootstrapTable('refresh');
                        reLoad();
                    });
                } else { layer.msg(r.msg || '撤回失败', { icon: 2 }); }
            },
            error: function() { layer.msg('请求失败', { icon: 2 }); }
        });
    });
}

/**
 * 专家打分页：高级筛选
 * 行为与 surver_pro_list 一致：localStorage 按 taskId+子类型；文本框回车、下拉选中即筛；重置清空存储；
 * 「淘汰状态」仅作服务端筛选用，主表仍不展示淘汰列（仅管理员侧展示）。
 */
function applyScoreProFilter() {
    persistSurverScoreAdvFilterFromDom();
    var $tbl = $('#exampleTable');
    if ($tbl.data('bootstrap.table')) {
        $tbl.bootstrapTable('selectPage', 1);
        $tbl.bootstrapTable('refresh');
    }
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
    $("#filterReviewResult").val('');
    clearSurverScoreAdvFilterStorage();
    applyScoreProFilter();
}

function applyScoreRatingFilter() {
    persistSurverScoreRatingAdvFilterFromDom();
    var $tbl = $('#ratingTable');
    if ($tbl.data('bootstrap.table')) {
        $tbl.bootstrapTable('selectPage', 1);
        $tbl.bootstrapTable('refresh');
    }
}

function resetScoreRatingFilter() {
    $("#ratingFilterProName").val('');
    $("#ratingFilterApplyCompany").val('');
    $("#ratingFilterMajor").val('');
    $("#ratingFilterDeclareAccount").val('');
    $("#ratingFilterQcGroupName").val('');
    $("#ratingFilterExpertGroupName").val('');
    $("#ratingFilterEliminated").val('');
    $("#ratingFilterProStat").val('');
    $("#ratingFilterReviewResult").val('');
    clearSurverScoreRatingAdvFilterStorage();
    applyScoreRatingFilter();
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