$().ready(function() {
	validateRule();
});

$.validator.setDefaults({
	submitHandler : function() {
		save();
	}
});
function save() {
	var form = $("#signupForm").serializeArray();
	console.log("勘察奖项---->", form)
	let postData = new Object();
	$.each(form, function (i, field) {
		postData[field.name] = field.value;
	})
	$("#designTable").find("td").each(function () {
	    let isEdit = $(this).attr("contenteditable");
	    if(isEdit) {
	    	let name = $(this).attr("name");
	    	let val = $(this).text();
	    	postData[name] = val;
		}
	});
	$("designTable").find("input").each(function () {
		let name = $(this).attr("name");
		let val = $(this).val();
		postData[name] = val;
	})

	console.log("勘察奖申报表数据--->", postData)

	$.ajax({
		cache : true,
		type : "POST",
		url : "/cpe/surverDesignApplyTableInfo/save",
		data : postData,// 你的formid
		async : false,
		error : function(request) {
			parent.layer.alert("Connection error");
		},
		success : function(data) {
			if (data.code == 0) {
				let id = data.id;
				$("#id").val(id);
				parent.layer.msg("操作成功");
				if (parent && parent.$) {
					var $iframe = parent.$("#proDesc");
					if ($iframe && $iframe.length) {
						var src = $iframe.attr("src");
						$iframe.attr("src", src);
					}
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
