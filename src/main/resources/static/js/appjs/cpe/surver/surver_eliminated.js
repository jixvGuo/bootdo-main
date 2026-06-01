// 淘汰评语页面JS

var currentTaskId = '';

$(document).ready(function() {
    currentTaskId = $('#taskId').val() || '';
    loadEliminatedData();
});

/**
 * 加载淘汰评语数据
 */
function loadEliminatedData() {
    var tbody = $('#tbody-eliminated');
    tbody.html('<tr><td colspan="5" class="text-center text-muted">加载中...</td></tr>');

    $.ajax({
        url: '/surverScore/getEliminatedProjects',
        type: 'GET',
        data: { taskId: currentTaskId },
        success: function(res) {
            if (res.code === 0) {
                renderEliminatedTable(res.data || []);
            } else {
                layer.msg(res.msg || '加载失败');
                tbody.html('<tr><td colspan="5" class="text-center text-danger">加载失败</td></tr>');
            }
        },
        error: function() {
            layer.msg('请求失败');
            tbody.html('<tr><td colspan="5" class="text-center text-danger">请求失败</td></tr>');
        }
    });
}

/**
 * 渲染淘汰评语表格
 */
function renderEliminatedTable(projects) {
    var tbody = $('#tbody-eliminated');
    tbody.empty();

    if (!projects || projects.length === 0) {
        tbody.append('<tr><td colspan="5" class="text-center text-muted">暂无评级淘汰的项目</td></tr>');
        return;
    }

    projects.forEach(function(project, index) {
        var proId = project.proId || '';
        var proSubType = project.proSubType || '';
        var declareAccount = project.declareAccount || '';
        var proCode = project.proCode || '';
        var topicName = project.topicName || '';
        var opinionGrade = project.opinionGrade || '';
        var opinionText = project.opinionText || '';

        var row = '<tr data-pro-id="' + proId + '">';
        row += '<td>' + declareAccount + '</td>';
        row += '<td>' + proCode + '</td>';
        row += '<td>' + topicName + '</td>';
        row += '<td>';
        row += '<a href="javascript:void(0)" onclick="openOpinionModal(\'' + proId + '\')" class="btn btn-xs btn-info">';
        row += opinionGrade ? opinionGrade : '填写意见';
        row += '</a>';
        row += '<input type="hidden" class="opinion-grade" value="' + opinionGrade + '">';
        row += '<input type="hidden" class="opinion-text" value="' + opinionText + '">';
        row += '</td>';
        row += '<td>';
        row += '<button type="button" class="btn btn-xs btn-warning" onclick="viewProject(\'' + proId + '\', \'' + proSubType + '\')">查看</button>';
        row += '</td>';
        row += '</tr>';

        tbody.append(row);
    });
}

/**
 * 打开主评意见弹窗
 */
function openOpinionModal(proId) {
    var $row = $('tr[data-pro-id="' + proId + '"]');
    var currentGrade = $row.find('.opinion-grade').val() || '';
    var currentText = $row.find('.opinion-text').val() || '';

    $('#opinionGrade').val(currentGrade);
    $('#opinionText').val(currentText);

    layer.open({
        type: 1,
        title: '主评意见',
        area: ['500px', '350px'],
        content: $('#opinionModal'),
        btn: ['确定', '取消'],
        yes: function(index) {
            var grade = $('#opinionGrade').val();
            var text = $('#opinionText').val();

            if (!grade) {
                layer.msg('请选择等级');
                return;
            }

            // 保存主评意见
            saveOpinion(proId, grade, text, function() {
                $row.find('.opinion-grade').val(grade);
                $row.find('.opinion-text').val(text);
                $row.find('a.btn-info').text(grade);
                layer.close(index);
            });
        }
    });
}

/**
 * 保存主评意见
 */
function saveOpinion(proId, grade, text, callback) {
    $.ajax({
        url: '/surverScore/saveEliminatedOpinion',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            taskId: currentTaskId,
            proId: proId,
            opinionGrade: grade,
            opinionText: text
        }),
        success: function(res) {
            if (res.code === 0) {
                layer.msg('保存成功');
                if (callback) callback();
            } else {
                layer.msg(res.msg || '保存失败');
            }
        },
        error: function() {
            layer.msg('请求失败');
        }
    });
}

/**
 * 在新标签页中查看项目
 */
function viewProject(proId, proSubType) {
    var url = '';
    if (proSubType === 'design') {
        url = '/surverApply/toApplyDesign?readonly=1&proId=' + proId;
    } else if (proSubType === 'software') {
        url = '/surverSoftwareApply/toApplySoftware?readonly=1&proId=' + proId;
    } else if (proSubType === 'standard') {
        url = '/surverStandardApply/toApply?readonly=1&proId=' + proId;
    } else if (proSubType === 'contribution') {
        url = '/surverBaseExlentApply/toApply?readonly=1&proId=' + proId;
    }

    if (url) {
        window.open(url, '_blank');
    } else {
        layer.msg('未知的项目类型', { icon: 2 });
    }
}