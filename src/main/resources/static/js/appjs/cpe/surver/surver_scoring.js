// 专家打分页面JS

var currentTaskId = '';
var currentProType = '';
var isConfirmed = false; // 是否已确认打分结果

$(document).ready(function() {
    currentTaskId = $('#taskId').val() || '';
    // 默认加载勘察项目
    loadTabData('contribution');
});

/**
 * 加载指定子奖项的项目数据
 */
function loadTabData(proSubType) {
    currentProType = proSubType;
    var tbodyId = '#tbody-' + proSubType;

    $.ajax({
        url: '/surverScore/getScoringProjects',
        type: 'GET',
        data: {
            taskId: currentTaskId,
            proSubType: proSubType
        },
        success: function(res) {
            if (res.code === 0) {
                renderTable(proSubType, res.data || []);
            } else {
                layer.msg(res.msg || '加载失败');
                $(tbodyId).html('<tr><td colspan="11" class="text-center text-danger">加载失败</td></tr>');
            }
        },
        error: function() {
            layer.msg('请求失败');
            $(tbodyId).html('<tr><td colspan="11" class="text-center text-danger">请求失败</td></tr>');
        }
    });
}

/**
 * 渲染表格
 */
function renderTable(proSubType, projects) {
    var tbodyId = '#tbody-' + proSubType;
    var tbody = $(tbodyId);
    tbody.empty();

    if (!projects || projects.length === 0) {
        var colSpan = getColSpan(proSubType);
        tbody.append('<tr><td colspan="' + colSpan + '" class="text-center text-muted">暂无数据</td></tr>');
        return;
    }

    projects.forEach(function(project, index) {
        var row = buildRow(proSubType, project, index);
        tbody.append(row);
    });

    // 绑定分数输入事件
    bindScoreInputEvents(proSubType);
}

/**
 * 获取表格列数
 */
function getColSpan(proSubType) {
    switch(proSubType) {
        case 'contribution': return 11;
        case 'design': return 14;
        case 'software': return 12;
        case 'standard': return 12;
        default: return 11;
    }
}

/**
 * 构建表格行
 */
function buildRow(proSubType, project, index) {
    var proId = project.proId || '';
    var declareAccount = project.declareAccount || '';
    var proCode = project.proCode || '';
    var topicName = project.topicName || '';

    // 获取已保存的打分数据
    var scoreData = project.scoreData || {};

    var row = '<tr data-pro-id="' + proId + '">';

    // 固定列：申报账号、项目编号、项目名称
    row += '<td>' + declareAccount + '</td>';
    row += '<td>' + proCode + '</td>';
    row += '<td>' + topicName + '</td>';

    // 根据子奖项类型添加分数列
    switch(proSubType) {
        case 'contribution':
            row += buildScoreCell(proId, 'technicalLevel', 30, scoreData.technicalLevel);
            row += buildScoreCell(proId, 'technicalDifficulty', 20, scoreData.technicalDifficulty);
            row += buildScoreCell(proId, 'technicalInnovation', 20, scoreData.technicalInnovation);
            row += buildScoreCell(proId, 'economicBenefit', 20, scoreData.economicBenefit);
            row += buildScoreCell(proId, 'materialQuality', 10, scoreData.materialQuality);
            break;
        case 'design':
            row += buildScoreCell(proId, 'overallTechnicalLevel', 25, scoreData.overallTechnicalLevel);
            row += buildScoreCell(proId, 'difficultyInnovation', 15, scoreData.difficultyInnovation);
            row += buildScoreCell(proId, 'digitalDesignLevel', 15, scoreData.digitalDesignLevel);
            row += buildScoreCell(proId, 'environmentSafety', 10, scoreData.environmentSafety);
            row += buildScoreCell(proId, 'designQuality', 7, scoreData.designQuality);
            row += buildScoreCell(proId, 'energySaving', 8, scoreData.energySaving);
            row += buildScoreCell(proId, 'greenConstruction', 10, scoreData.greenConstruction);
            row += buildScoreCell(proId, 'materialQuality', 10, scoreData.materialQuality);
            break;
        case 'software':
        case 'standard':
            row += buildScoreCell(proId, 'technicalLevel', 30, scoreData.technicalLevel);
            row += buildScoreCell(proId, 'technicalDifficulty', 20, scoreData.technicalDifficulty);
            row += buildScoreCell(proId, 'technicalInnovation', 20, scoreData.technicalInnovation);
            row += buildScoreCell(proId, 'promotability', 20, scoreData.promotability);
            row += buildScoreCell(proId, 'economicBenefit', 20, scoreData.economicBenefit);
            row += buildScoreCell(proId, 'materialQuality', 10, scoreData.materialQuality);
            break;
    }

    // 总分列（自动计算）
    var totalScore = calculateTotal(proSubType, scoreData);
    row += '<td class="total-score">' + (totalScore > 0 ? totalScore : '') + '</td>';

    // 主评意见列
    var opinionGrade = scoreData.opinionGrade || '';
    var opinionText = scoreData.opinionText || '';
    row += '<td>';
    row += '<a href="javascript:void(0)" onclick="openOpinionModal(\'' + proId + '\')" class="btn btn-xs btn-info">';
    row += opinionGrade ? opinionGrade : '填写意见';
    row += '</a>';
    row += '<input type="hidden" class="opinion-grade" value="' + opinionGrade + '">';
    row += '<input type="hidden" class="opinion-text" value="' + opinionText + '">';
    row += '</td>';

    // 操作列
    row += '<td>';
    row += '<button type="button" class="btn btn-xs btn-default" onclick="viewProject(\'' + proId + '\')">查看</button> ';
    row += '<button type="button" class="btn btn-xs btn-primary" onclick="saveScore(\'' + proId + '\', \'' + proSubType + '\')">保存</button>';
    row += '</td>';

    row += '</tr>';
    return row;
}

/**
 * 构建分数输入单元格
 */
function buildScoreCell(proId, fieldName, maxScore, value) {
    var val = value || '';
    return '<td>' +
        '<input type="number" class="form-control input-sm score-input" ' +
        'data-field="' + fieldName + '" ' +
        'data-max="' + maxScore + '" ' +
        'value="' + val + '" ' +
        'min="0" max="' + maxScore + '" ' +
        'placeholder="0-' + maxScore + '" ' +
        'style="width: 80px;">' +
        '</td>';
}

/**
 * 绑定分数输入事件
 */
function bindScoreInputEvents(proSubType) {
    $('#tbody-' + proSubType).find('.score-input').on('input', function() {
        var $input = $(this);
        var max = parseInt($input.data('max'));
        var val = parseInt($input.val());

        if (val > max) {
            $input.val(max);
            layer.tips('不能超过' + max + '分', $input, {tips: 2, time: 1000});
        }
        if (val < 0) {
            $input.val(0);
        }

        // 自动计算总分
        var $row = $input.closest('tr');
        calculateRowTotal($row, proSubType);
    });
}

/**
 * 计算行总分
 */
function calculateRowTotal($row, proSubType) {
    var total = 0;
    $row.find('.score-input').each(function() {
        var val = parseInt($(this).val()) || 0;
        total += val;
    });
    $row.find('.total-score').text(total > 0 ? total : '');
}

/**
 * 计算总分
 */
function calculateTotal(proSubType, scoreData) {
    var total = 0;
    switch(proSubType) {
        case 'contribution':
            total = (parseInt(scoreData.technicalLevel) || 0) +
                    (parseInt(scoreData.technicalDifficulty) || 0) +
                    (parseInt(scoreData.technicalInnovation) || 0) +
                    (parseInt(scoreData.economicBenefit) || 0) +
                    (parseInt(scoreData.materialQuality) || 0);
            break;
        case 'design':
            total = (parseInt(scoreData.overallTechnicalLevel) || 0) +
                    (parseInt(scoreData.difficultyInnovation) || 0) +
                    (parseInt(scoreData.digitalDesignLevel) || 0) +
                    (parseInt(scoreData.environmentSafety) || 0) +
                    (parseInt(scoreData.designQuality) || 0) +
                    (parseInt(scoreData.energySaving) || 0) +
                    (parseInt(scoreData.greenConstruction) || 0) +
                    (parseInt(scoreData.materialQuality) || 0);
            break;
        case 'software':
        case 'standard':
            total = (parseInt(scoreData.technicalLevel) || 0) +
                    (parseInt(scoreData.technicalDifficulty) || 0) +
                    (parseInt(scoreData.technicalInnovation) || 0) +
                    (parseInt(scoreData.promotability) || 0) +
                    (parseInt(scoreData.economicBenefit) || 0) +
                    (parseInt(scoreData.materialQuality) || 0);
            break;
    }
    return total;
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

            $row.find('.opinion-grade').val(grade);
            $row.find('.opinion-text').val(text);
            $row.find('a.btn-info').text(grade);

            layer.close(index);
        }
    });
}

/**
 * 保存单个项目打分
 */
function saveScore(proId, proSubType) {
    var $row = $('tr[data-pro-id="' + proId + '"]');
    var scoreData = {};

    // 收集分数
    $row.find('.score-input').each(function() {
        var field = $(this).data('field');
        var val = $(this).val();
        scoreData[field] = val ? parseInt(val) : 0;
    });

    // 收集主评意见
    scoreData.opinionGrade = $row.find('.opinion-grade').val();
    scoreData.opinionText = $row.find('.opinion-text').val();

    // 计算总分
    scoreData.totalScore = calculateTotal(proSubType, scoreData);

    $.ajax({
        url: '/surverScore/saveScoring',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            taskId: currentTaskId,
            proId: proId,
            proSubType: proSubType,
            scoreData: scoreData
        }),
        success: function(res) {
            if (res.code === 0) {
                layer.msg('保存成功');
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
 * 确认打分结果
 */
function confirmScoreResult() {
    if (isConfirmed) {
        layer.msg('已确认打分结果，无法重复确认');
        return;
    }

    layer.confirm('确认打分结果后将无法修改，是否继续？', {
        btn: ['确认', '取消']
    }, function() {
        $.ajax({
            url: '/surverScore/confirmScoring',
            type: 'POST',
            data: { taskId: currentTaskId },
            success: function(res) {
                if (res.code === 0) {
                    layer.msg('确认成功');
                    isConfirmed = true;
                    $('#btnConfirmScore').text('已确认打分结果').prop('disabled', true);
                } else {
                    layer.msg(res.msg || '确认失败');
                }
            },
            error: function() {
                layer.msg('请求失败');
            }
        });
    });
}

/**
 * 下载打分结果
 */
function downloadScoreResult() {
    window.location.href = '/surverScore/downloadScoringResult?taskId=' + currentTaskId;
}

/**
 * 打开淘汰评语标签页（与专家评级、专家打分同级）
 */
function openEliminatedTab() {
    var url = '/surverScore/proEliminatedList?taskId=' + encodeURIComponent(currentTaskId);
    if (typeof page === 'function') {
        page(url, '淘汰评语', 20220508);
    } else {
        window.open(url, '_blank');
    }
}

/**
 * 查看项目详情（在新标签页打开）
 */
function viewProject(proId) {
    // 根据当前子奖项类型确定URL
    var proSubType = currentProType || 'contribution';
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