/**
 * 勘察奖形式审查页共用逻辑（各子奖项 add.js 之前配置 SURVER_REVIEW_FORM_CFG）
 * - 自动保存：有 id 走 update，无 id 首次走 save（避免重复刷列表、避免隐藏 iframe 下丢滚动锚点）
 * - 提交成功：只标记「待刷新」，等关 Tab 回到列表可见后再 refresh + 恢复行位置
 */
var _surverReviewAutoSaveTimer = null;

function getSurverReviewProSubTypeFromUrl() {
    var m = (window.location.search || "").match(/[?&]proSubType=([^&]+)/);
    return m ? decodeURIComponent(m[1]) : "";
}

/** 与 surverProList.js 使用同一 pendingRefresh 键规则 */
function markSurverProListRefreshDeferred() {
    var taskId = ($("#taskId").val() || "") + "";
    var proSubType = getSurverReviewProSubTypeFromUrl();
    try {
        sessionStorage.setItem("cpe.surverProList.pendingRefresh.v1:" + taskId + ":" + proSubType, "1");
    } catch (e) { /* ignore */ }
    // 原：立即 reloadProList / refresh 列表（形审 Tab 仍在前台时列表 iframe 是隐藏的，滚动恢复会失败并清掉锚点）
}

function postSurverReviewForm(url, options) {
    var cfg = window.SURVER_REVIEW_FORM_CFG || {};
    var saveUrl = url || cfg.saveUrl;
    if (!saveUrl) {
        return;
    }
    $.ajax({
        cache: true,
        type: "POST",
        url: saveUrl,
        data: $("#signupForm").serialize(),
        async: options && options.async !== false,
        error: function () {
            if (options && options.silent) {
                return;
            }
            parent.layer.alert("Connection error");
        },
        success: function (data) {
            if (data && data.code === 0) {
                if (data.id) {
                    $("#id").val(data.id);
                }
                if (options && options.onSuccess) {
                    options.onSuccess(data);
                }
            } else if (!options || !options.silent) {
                parent.layer.alert((data && data.msg) ? data.msg : "保存失败");
            }
        }
    });
}

/** 防抖自动保存：仅写入形审表，不刷新项目列表 */
function scheduleSurverReviewAutoSave() {
    var cfg = window.SURVER_REVIEW_FORM_CFG || {};
    if (!cfg.saveUrl) {
        return;
    }
    if ($("#isReadonly").val() === "1") {
        return;
    }
    if (_surverReviewAutoSaveTimer) {
        clearTimeout(_surverReviewAutoSaveTimer);
    }
    _surverReviewAutoSaveTimer = setTimeout(function () {
        _surverReviewAutoSaveTimer = null;
        autoSaveSurverReviewDraft();
    }, 900);
}

function autoSaveSurverReviewDraft() {
    var cfg = window.SURVER_REVIEW_FORM_CFG || {};
    var id = ($("#id").val() || "") + "";
    var url = (id.length > 0 && cfg.updateUrl) ? cfg.updateUrl : cfg.saveUrl;
    postSurverReviewForm(url, {
        silent: true,
        async: true,
        onSuccess: function () {
            // 可选：轻提示，避免打扰
            // parent.layer.msg("已自动保存", { time: 800 });
        }
    });
}

function initSurverReviewFormEnhancements() {
    var $form = $("#signupForm");
    if (!$form.length) {
        return;
    }
    $form.on("change input", "input, textarea, select", function () {
        scheduleSurverReviewAutoSave();
    });
    $(".ui.radio.checkbox").on("change", function () {
        scheduleSurverReviewAutoSave();
    });
    $(".ui.submit.button").on("click", function (e) {
        e.preventDefault();
        if (typeof $form.valid === "function") {
            if ($form.valid()) {
                save();
            }
        } else {
            save();
        }
    });
}

$(function () {
    initSurverReviewFormEnhancements();
});
