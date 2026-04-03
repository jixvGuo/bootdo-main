$().ready(function() {
	validateRule();
});

$.validator.setDefaults({
	submitHandler : function() {
		save();
	}
});
function save() {
	$.ajax({
		cache : true,
		type : "POST",
		url : "/cpe/surverReviewSurverResult/save",
		data : $('#signupForm').serialize(),// 你的formid
		async : false,
		error : function(request) {
			parent.layer.alert("Connection error");
		},
		success : function(data) {
			if (data.code == 0) {
				parent.layer.msg("操作成功");
				$("#id").val(data.id);
				reloadProList();
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

function reloadProList() {
	var docs = [];
	try { docs.push(window.parent.document); } catch (e) {}
	try { docs.push(window.parent.parent.document); } catch (e) {}
	try { docs.push(window.top.document); } catch (e) {}

	$.each(docs, function (idx, doc) {
		if (!doc) return;
		var navArr = $("iframe", doc);
		$.each(navArr, function (i, val) {
			var srcUrl = val.src || $(val).attr('src') || '';
			if (srcUrl.indexOf('/surverPro/toProListMain') !== -1 || srcUrl.indexOf('/surverPro/toProList') !== -1) {
				try {
					val.contentWindow.location.reload(true);
				} catch (e) {}
			}
		});
	});
}
