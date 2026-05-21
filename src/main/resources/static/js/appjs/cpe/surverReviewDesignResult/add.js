$().ready(function() {
	validateRule();
});

$.validator.setDefaults({
	submitHandler : function() {
		save();
	}
});
function save() {
	var cfg = window.SURVER_REVIEW_FORM_CFG || {};
	// 正式提交始终走 save（后端会 setId(null) 新增形审记录并发通知）；自动保存才走 update
	var url = cfg.saveUrl || "/cpe/surverReviewDesignResult/save";
	$.ajax({
		cache : true,
		type : "POST",
		url : url,
		data : $('#signupForm').serialize(),// 你的formid
		async : false,
		error : function(request) {
			parent.layer.alert("Connection error");
		},
		success : function(data) {
			if (data.code == 0) {
				parent.layer.msg("操作成功");
				if (data.id) {
					$("#id").val(data.id);
				}
				// 原：立即 reloadProList()，在形审 Tab 仍打开时刷新隐藏中的列表，锚点恢复失败
				// reloadProList();
				if (typeof markSurverProListRefreshDeferred === "function") {
					markSurverProListRefreshDeferred();
				}
			} else {
				parent.layer.alert(data.msg)
			}

		}
	});

}
function validateRule() {
	var icon = "<i class='fa fa-times-circle'></i> ";
	$("#signupForm").validate({
		rules : {
			name : {
				required : true
			}
		},
		messages : {
			name : {
				required : icon + "请输入姓名"
			}
		}
	})
}

/**
 * 形审保存成功后刷新勘察项目列表：不整页 reload（会丢滚动位置），改为调用列表页的 refreshSurverProListWithAnchor
 */
function reloadProList() {
	var docs = [];
	try { docs.push(window.parent.document); } catch (e) {}
	try { docs.push(window.parent.parent.document); } catch (e) {}
	try { docs.push(window.top.document); } catch (e) {}

	// 原：location.reload 整页重载，表格回到第 1 页顶部
	// $.each(docs, function (idx, doc) {
	// 	if (!doc) return;
	// 	var navArr = $("iframe", doc);
	// 	$.each(navArr, function (i, val) {
	// 		var srcUrl = val.src || $(val).attr('src') || '';
	// 		if (srcUrl.indexOf('/surverPro/toProListMain') !== -1 || srcUrl.indexOf('/surverPro/toProList') !== -1) {
	// 			try {
	// 				val.contentWindow.location.reload(true);
	// 			} catch (e) {}
	// 		}
	// 	});
	// });

	$.each(docs, function (idx, doc) {
		if (!doc) return;
		refreshSurverProListIframesInDocument(doc, 0);
	});
}

/** 递归查找勘察项目列表 iframe（含 toProListMain 外壳 + 内层 toProList） */
function refreshSurverProListIframesInDocument(doc, depth) {
	if (!doc || depth > 5) {
		return false;
	}
	var refreshed = false;
	$("iframe", doc).each(function () {
		if (refreshed) {
			return false;
		}
		var srcUrl = this.src || $(this).attr('src') || '';
		var isMain = srcUrl.indexOf('/surverPro/toProListMain') !== -1;
		var isList = srcUrl.indexOf('/surverPro/toProList') !== -1;
		if (!isMain && !isList) {
			return;
		}
		try {
			var win = this.contentWindow;
			if (isList && win && typeof win.refreshSurverProListWithAnchor === 'function') {
				win.refreshSurverProListWithAnchor();
				refreshed = true;
				return false;
			}
			if (isMain && win && typeof win.reLoad === 'function') {
				win.reLoad();
				refreshed = true;
				return false;
			}
			if (win && win.document && refreshSurverProListIframesInDocument(win.document, depth + 1)) {
				refreshed = true;
				return false;
			}
		} catch (e) { /* ignore */ }
	});
	return refreshed;
}
