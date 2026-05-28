$().ready(function() {
	validateRule();
});

$.validator.setDefaults({
	submitHandler : function() {
		save();
	}
});
function save() {
	if (typeof cancelSurverReviewAutoSaveTimer === "function") {
		cancelSurverReviewAutoSaveTimer();
	}
	_surverReviewFormalSubmitting = true;
	var cfg = window.SURVER_REVIEW_FORM_CFG || {};
	var url = cfg.saveUrl || "/cpe/surverReviewSurverResult/save";
	$.ajax({
		cache : true,
		type : "POST",
		url : url,
		data : $('#signupForm').serialize() + '&formalSubmit=1',
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
				if (typeof refreshSurverReviewOriginSnapshot === "function") {
					refreshSurverReviewOriginSnapshot();
				}
				if (typeof markSurverProListRefreshDeferred === "function") {
					markSurverProListRefreshDeferred();
				}
			} else {
				parent.layer.alert(data.msg)
			}

		},
		complete: function () {
			_surverReviewFormalSubmitting = false;
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

/** 形审保存后刷新列表并恢复离开前的行位置（见 surverProList.js refreshSurverProListWithAnchor） */
function reloadProList() {
	var docs = [];
	try { docs.push(window.parent.document); } catch (e) {}
	try { docs.push(window.parent.parent.document); } catch (e) {}
	try { docs.push(window.top.document); } catch (e) {}

	// 原：val.contentWindow.location.reload(true);
	$.each(docs, function (idx, doc) {
		if (!doc) return;
		refreshSurverProListIframesInDocument(doc, 0);
	});
}

function refreshSurverProListIframesInDocument(doc, depth) {
	if (!doc || depth > 5) return false;
	var refreshed = false;
	$("iframe", doc).each(function () {
		if (refreshed) return false;
		var srcUrl = this.src || $(this).attr('src') || '';
		var isMain = srcUrl.indexOf('/surverPro/toProListMain') !== -1;
		var isList = srcUrl.indexOf('/surverPro/toProList') !== -1;
		if (!isMain && !isList) return;
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
