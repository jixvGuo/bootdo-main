$().ready(function() {
	// validateRule();

	// 强制拦截原生提交，避免页面刷新
    $("#signupForm").on("submit", function (e) {
        e.preventDefault();

        // 如果有 validate 插件，走校验
        if ($.fn.validate) {
            var v = $("#signupForm").data("validator");
            if (!v) {
                $("#signupForm").validate();
            }
            if (!$("#signupForm").valid()) {
                return false;
            }
        }

        save();
        return false;
    });
});

if ($.validator) {
	$.validator.setDefaults({
		submitHandler : function() {
			save();
		}
	});
}


function save() {
	$("#id").val("");
	$.ajax({
		cache : true,
		type : "POST",
		url : "/cpe/qcReviewResultRecord/save",
		
		data : $('#signupForm').serialize(),// 你的formid
		async : false,
		error : function(request) {
			parent.layer.alert("Connection error");
		},
		success : function(data) {
			if (data.code == 0) {
				$("#id").val(data.id);
				parent.layer.msg("操作成功");
				reloadProList();
			} else {
				parent.layer.alert(data.msg)
			}

		}
	});

}

// function reloadProList() {
// 	var docs = [];
// 	try { docs.push(window.parent.document); } catch (e) {}
// 	try { docs.push(window.parent.parent.document); } catch (e) {}
// 	try { docs.push(window.top.document); } catch (e) {}
//
// 	$.each(docs, function (idx, doc) {
// 		if (!doc) return;
// 		var navArr = $("iframe", doc);
// 		$.each(navArr, function (i, val) {
// 			if (val.src && val.src.indexOf('/qcAward/toProListMain') != -1) {
// 				val.contentWindow.location.reload(true);
// 			}
// 		});
// 	});
// }

function reloadProList() {
	console.log("==== reloadProList start ====");
	var docs = [];
	try {
		docs.push({ doc: window.parent.document, level: 'parent' });
	} catch (e) {}
	try {
		docs.push({ doc: window.parent.parent.document, level: 'parent.parent' });
	} catch (e) {}

	var foundAndReloaded = false;
	$.each(docs, function (idx, item) {
		if (!item.doc || foundAndReloaded) return;

		console.log("Checking level:", item.level);
		var navArr = $("iframe", item.doc);
		console.log("Found iframes count:", navArr.length);

		$.each(navArr, function (i, val) {
			if (foundAndReloaded) return;

			var srcUrl = val.src || $(val).attr('src');
			console.log("Iframe src:", srcUrl);

			// 只刷新项目列表页面，排除审查页面和首页
			if (srcUrl) {
				// 清理 URL，去掉查询参数，并将双斜杠替换为单斜杠以便匹配
				var normalizedUrl = srcUrl.split('?')[0].replace(/\/+/g, '/');
				console.log("Normalized URL:", normalizedUrl);

				// 排除审查页面和首页
				if (normalizedUrl.includes('/qcProcess/toReivew') ||
					normalizedUrl.includes('/index_v3')) {
					console.log("Skip review/home page");
					return; // continue to next iframe
				}

				// 匹配项目列表页面 (支持多种路径格式)
				// 形式审查角色：/qcAward/view/proList
				// 领导角色：/qcAward/toProListMain (外层) 或内部的 /qcAward/view/proList
				if (normalizedUrl.includes('/qcAward/view/proList') ||
					normalizedUrl.includes('/qcAward/toProListMain') ||
					normalizedUrl.includes('/qc/qc_pro_list') ||
					normalizedUrl.includes('/qc_pro_list_main')) {
					console.log("✓ Found project list page! Reloading...");

					// 如果是 toProListMain 页面，尝试刷新其内部的 iframe
					if (val.contentWindow && val.contentWindow.document) {
						try {
							// 检查是否是 toProListMain 页面（内部包含另一个 iframe）
							var innerIframe = val.contentWindow.document.getElementById('qc');
							if (innerIframe && innerIframe.src && innerIframe.src.includes('/qcAward/view/proList')) {
								console.log("Refresh inner iframe of toProListMain");
								innerIframe.contentWindow.location.reload(true);
								foundAndReloaded = true;
								return;
							}
						} catch(e) {
							console.log("Cannot access inner iframe, reload parent");
						}
					}

					// 直接刷新当前 iframe
					val.contentWindow.location.reload(true);
					foundAndReloaded = true;
				}
			}
		});
	});

	if (!foundAndReloaded) {
		console.warn("Warning: No matching project list page found!");
	}
	console.log("==== reloadProList end ====");
}

// ... existing code ...

function rejectPro() {
	// proId 来自页面隐藏域
	var proId = $("#proId").val();
	if (!proId) {
		parent.layer.alert("缺少项目ID");
		return;
	}

	layer.confirm('确定要驳回该课题吗？', {
		btn: ['确定', '取消']
	}, function () {
		$.ajax({
			type: "POST",
			url: "/qcProcess/reject",
			data: { proId: proId },
			success: function (r) {
				if (r.code == 0) {
					parent.layer.msg("驳回成功");
					reloadProList();
				} else {
					parent.layer.alert(r.msg || "驳回失败");
				}
			},
			error: function () {
				parent.layer.alert("Connection error");
			}
		});
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
