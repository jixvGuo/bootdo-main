$().ready(function() {
	validateRule();
});

$.validator.setDefaults({
	submitHandler : function() {
		save();
	}
});
function save() {
	var projectId = $.trim($('#projectId').text());
	var reportingUnit = $.trim($('#reportingUnit').text());
	var awardCategory = $.trim($('#awardCategory').text());
	var projectName = $.trim($('#projectName').text());
	var recommendedGrade = $.trim($('#recommendedGrade').text());
	var preliminaryEvaluationGroup = $.trim($('#preliminaryEvaluationGroup').text());
	var projectDescription = $.trim($('#projectDescription').text());

	if (!reportingUnit) {
		parent.layer.alert('请填写申报单位');
		return;
	}
	if (!awardCategory) {
		parent.layer.alert('请填写奖项类别');
		return;
	}
	if (!projectName) {
		parent.layer.alert('请填写项目名称');
		return;
	}
	if (!recommendedGrade) {
		parent.layer.alert('请填写推荐等级');
		return;
	}

	var formData = $('#signupForm').serializeArray();
	formData.push({name: 'projectId', value: projectId});
	formData.push({name: 'reportingUnit', value: reportingUnit});
	formData.push({name: 'awardCategory', value: awardCategory});
	formData.push({name: 'projectName', value: projectName});
	formData.push({name: 'recommendedGrade', value: recommendedGrade});
	formData.push({name: 'preliminaryEvaluationGroup', value: preliminaryEvaluationGroup});
	formData.push({name: 'projectDescription', value: projectDescription});
	$.ajax({
		cache : true,
		type : "POST",
		url : "/surverStandardApply/saveProDesc",
		data : $.param(formData),// 你的formid
		async : false,
		error : function(request) {
			if (parent && parent.layer) {
				parent.layer.alert("Connection error");
			} else {
				alert("Connection error");
			}
		},
		success : function(data) {
			if (data.code == 0) {
				if (parent && parent.layer) {
					parent.layer.msg("操作成功");
				}
				if (parent && typeof parent.reLoad === 'function') {
					parent.reLoad();
				}
				if (parent && parent.layer && typeof parent.layer.getFrameIndex === 'function') {
					var index = parent.layer.getFrameIndex(window.name);
					if (index) {
						parent.layer.close(index);
						return;
					}
				}
				var proId = $('#proId').val();
				var taskId = $('#taskId').val();
				window.location.href = '/surverStandardApply/toProDesc?proId=' + encodeURIComponent(proId) + '&taskId=' + encodeURIComponent(taskId);
			} else {
				if (parent && parent.layer) {
					parent.layer.alert(data.msg)
				} else {
					alert(data.msg);
				}
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