// 专家打分页面JS

var currentTaskId = '';
var currentProType = '';
var isConfirmed = false; // 是否已确认打分结果
var scoredDataCache = {}; // 缓存各子奖项的项目数据
var SCORING_FILTER_KEY = "cpe.surverScoring.advFilter.v1"; // localStorage键名

$(document).ready(function() {
    currentTaskId = $('#taskId').val() || '';
    // 默认加载勘察项目
    loadTabData('contribution');

    // 绑定筛选面板回车事件
    $('#scoringFilterPanel').on('keydown', 'input.form-control', function(e) {
        if (e.which === 13 || e.keyCode === 13) {
            e.preventDefault();
            applyScoringFilter();
        }
    });

    // 恢复保存的筛选条件
    restoreScoringFilterFromStorage();

    // 禁用浏览器中number输入框的滚轮改变数值行为
    $(document).on('wheel', "input[type='number'], .score-input", function (e) {
        e.preventDefault();
        $(this).blur();
    });
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
                // 缓存原始数据
                var data = res.data || [];
                // 设计项目按项目编号升序排序（数字格式，如10.1, 4.1, 5.1）
                if (proSubType === 'design') {
                    data.sort(function(a, b) {
                        var numA = parseFloat(a.proCode) || 0;
                        var numB = parseFloat(b.proCode) || 0;
                        return numA - numB;
                    });
                }
                scoredDataCache[proSubType] = data;
                // 应用筛选
                var filtered = getFilteredData(proSubType);
                renderTable(proSubType, filtered);
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
    var isAvoided = project.isAvoided == 1; // 是否已回避

    // 获取已保存的打分数据
    var scoreData = project.scoreData || {};

    var row = '<tr data-pro-id="' + proId + '"' + (isAvoided ? ' class="avoided-row"' : '') + '>';

    // 固定列：申报账号、项目编号、项目名称
    row += '<td>' + declareAccount + '</td>';
    row += '<td>' + proCode + '</td>';
    row += '<td>' + topicName + (isAvoided ? ' <span class="label label-warning">已回避</span>' : '') + '</td>';

    // 根据子奖项类型添加分数列
    switch(proSubType) {
        case 'contribution':
            row += buildScoreCell(proId, 'technicalLevel', 30, scoreData.technicalLevel, isAvoided);
            row += buildScoreCell(proId, 'technicalDifficulty', 20, scoreData.technicalDifficulty, isAvoided);
            row += buildScoreCell(proId, 'technicalInnovation', 20, scoreData.technicalInnovation, isAvoided);
            row += buildScoreCell(proId, 'economicBenefit', 20, scoreData.economicBenefit, isAvoided);
            row += buildScoreCell(proId, 'materialQuality', 10, scoreData.materialQuality, isAvoided);
            break;
        case 'design':
            row += buildScoreCell(proId, 'overallTechnicalLevel', 25, scoreData.overallTechnicalLevel, isAvoided);
            row += buildScoreCell(proId, 'difficultyInnovation', 15, scoreData.difficultyInnovation, isAvoided);
            row += buildScoreCell(proId, 'digitalDesignLevel', 15, scoreData.digitalDesignLevel, isAvoided);
            row += buildScoreCell(proId, 'environmentSafety', 10, scoreData.environmentSafety, isAvoided);
            row += buildScoreCell(proId, 'designQuality', 7, scoreData.designQuality, isAvoided);
            row += buildScoreCell(proId, 'energySaving', 8, scoreData.energySaving, isAvoided);
            row += buildScoreCell(proId, 'greenConstruction', 10, scoreData.greenConstruction, isAvoided);
            row += buildScoreCell(proId, 'materialQuality', 10, scoreData.materialQuality, isAvoided);
            break;
        case 'software':
        case 'standard':
            row += buildScoreCell(proId, 'technicalLevel', 30, scoreData.technicalLevel, isAvoided);
            row += buildScoreCell(proId, 'technicalDifficulty', 20, scoreData.technicalDifficulty, isAvoided);
            row += buildScoreCell(proId, 'technicalInnovation', 20, scoreData.technicalInnovation, isAvoided);
            row += buildScoreCell(proId, 'promotability', 20, scoreData.promotability, isAvoided);
            row += buildScoreCell(proId, 'economicBenefit', 20, scoreData.economicBenefit, isAvoided);
            row += buildScoreCell(proId, 'materialQuality', 10, scoreData.materialQuality, isAvoided);
            break;
    }

    // 总分列（自动计算）
    var totalScore = calculateTotal(proSubType, scoreData);
    row += '<td class="total-score">' + (totalScore > 0 ? totalScore : '') + '</td>';

    // 主评意见列
    var opinionGrade = scoreData.opinionGrade || '';
    var opinionText = scoreData.opinionText || '';
    row += '<td>';
    if (isAvoided) {
        row += '<span class="text-muted">' + (opinionGrade || '-') + '</span>';
    } else {
        row += '<a href="javascript:void(0)" onclick="openOpinionModal(\'' + proId + '\')" class="btn btn-xs btn-info">';
        row += opinionGrade ? opinionGrade : '填写意见';
        row += '</a>';
    }
    row += '<input type="hidden" class="opinion-grade" value="' + opinionGrade + '">';
    row += '<input type="hidden" class="opinion-text" value="' + opinionText + '">';
    row += '</td>';

    // 操作列
    row += '<td>';
    row += '<button type="button" class="btn btn-xs btn-default" onclick="viewProject(\'' + proId + '\')">查看</button> ';
    if (!isAvoided) {
        row += '<button type="button" class="btn btn-xs btn-primary" onclick="saveScore(\'' + proId + '\', \'' + proSubType + '\')">保存</button>';
    }
    row += '</td>';

    row += '</tr>';
    return row;
}

/**
 * 构建分数输入单元格
 */
function buildScoreCell(proId, fieldName, maxScore, value, isAvoided) {
    var val = value || '';
    if (isAvoided) {
        // 回避项目：显示分数但不可编辑
        return '<td>' +
            '<span class="form-control input-sm" style="width: 80px; background-color: #f5f5f5; color: #999;">' + (val || '-') + '</span>' +
            '</td>';
    } else {
        // 正常项目：可编辑输入框（使用text类型，通过正则限制只能输入数字）
        return '<td>' +
            '<input type="text" class="form-control input-sm score-input" ' +
            'data-field="' + fieldName + '" ' +
            'data-max="' + maxScore + '" ' +
            'value="' + val + '" ' +
            'placeholder="0-' + maxScore + '" ' +
            'oninput="onScoreInput(this)" ' +
            'style="width: 80px;">' +
            '</td>';
    }
}

/**
 * 分数输入实时过滤（oninput触发）
 * 只允许输入数字，自动去除非法字符
 */
function onScoreInput(el) {
    var $input = $(el);
    var raw = $input.val();
    // 只保留数字
    var filtered = raw.replace(/[^0-9]/g, '');
    // 去除前导零（单独的0保留）
    if (filtered.length > 1 && filtered.charAt(0) === '0') {
        filtered = filtered.replace(/^0+/, '');
    }
    if (raw !== filtered) {
        $input.val(filtered);
    }
}

/**
 * 校验分数输入是否为整数
 * @returns {boolean} 是否合法
 */
function validateScoreInput($input) {
    var raw = $input.val();
    // 空值不校验（由范围校验处理）
    if (raw === '' || raw === null || raw === undefined) {
        $input.removeClass('score-input-invalid');
        return true;
    }

    // 检测是否包含小数点
    if (raw.indexOf('.') > -1) {
        layer.tips('请输入整数，不支持小数', $input, {tips: 2, time: 1500});
        // 截取整数部分
        var intVal = parseInt(raw);
        $input.val(isNaN(intVal) ? '' : intVal);
        $input.addClass('score-input-invalid');
        setTimeout(function() { $input.removeClass('score-input-invalid'); }, 1500);
        return false;
    }

    // 检测前导零（如01、007等）
    if (raw.length > 1 && raw.charAt(0) === '0') {
        layer.tips('不支持前导零，请直接输入数字', $input, {tips: 2, time: 1500});
        // 去除前导零
        var intVal = parseInt(raw, 10);
        $input.val(isNaN(intVal) ? '' : intVal);
        $input.addClass('score-input-invalid');
        setTimeout(function() { $input.removeClass('score-input-invalid'); }, 1500);
        return false;
    }

    // 检测科学计数法（如1e5）及其他非法字符
    if (/[eE\+\-]/.test(raw)) {
        layer.tips('请输入数字，不支持科学计数法', $input, {tips: 2, time: 1500});
        $input.val('');
        $input.addClass('score-input-invalid');
        setTimeout(function() { $input.removeClass('score-input-invalid'); }, 1500);
        return false;
    }

    // 检测是否为非数字
    if (isNaN(parseInt(raw))) {
        layer.tips('请输入数字', $input, {tips: 2, time: 1500});
        $input.val('');
        $input.addClass('score-input-invalid');
        setTimeout(function() { $input.removeClass('score-input-invalid'); }, 1500);
        return false;
    }

    $input.removeClass('score-input-invalid');
    return true;
}

/**
 * 绑定分数输入事件
 */
function bindScoreInputEvents(proSubType) {
    var $container = $('#tbody-' + proSubType);
    $container.find('.score-input').on('input', function() {
        var $input = $(this);

        // 整数格式校验

        validateScoreInput($input);

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

    // 失焦时也进行校验（处理粘贴等场景）
    $container.find('.score-input').on('blur', function() {
        var $input = $(this);
        validateScoreInput($input);

        var max = parseInt($input.data('max'));
        var val = parseInt($input.val());
        if (val > max) {
            $input.val(max);
        }
        if (val < 0) {
            $input.val(0);
        }

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
                    // 多行错误信息用弹窗显示
                    if (res.msg && res.msg.indexOf('\n') > -1) {
                        layer.open({
                            type: 1,
                            title: '提示',
                            area: ['500px', '400px'],
                            content: '<div style="padding:15px;white-space:pre-wrap;">' + res.msg + '</div>',
                            btn: ['确定']
                        });
                    } else {
                        layer.msg(res.msg || '确认失败');
                    }
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

// ==================== 高级筛选相关函数 ====================

/**
 * 获取筛选后的数据
 */
function getFilteredData(proSubType) {
    var allData = scoredDataCache[proSubType] || [];
    var declareAccount = ($('#scoringFilterDeclareAccount').val() || '').trim().toLowerCase();
    var proCode = ($('#scoringFilterProCode').val() || '').trim().toLowerCase();
    var proName = ($('#scoringFilterProName').val() || '').trim().toLowerCase();

    // 如果没有筛选条件，返回全部数据
    if (!declareAccount && !proCode && !proName) {
        return allData;
    }

    return allData.filter(function(project) {
        var matchAccount = !declareAccount || (project.declareAccount || '').toLowerCase().indexOf(declareAccount) > -1;
        var matchCode = !proCode || (project.proCode || '').toLowerCase().indexOf(proCode) > -1;
        var matchName = !proName || (project.topicName || '').toLowerCase().indexOf(proName) > -1;
        return matchAccount && matchCode && matchName;
    });
}

/**
 * 应用筛选
 */
function applyScoringFilter() {
    // 持久化筛选条件
    persistScoringFilterFromDom();

    // 对所有已加载的子奖项重新渲染
    $.each(scoredDataCache, function(proSubType) {
        var filtered = getFilteredData(proSubType);
        renderTable(proSubType, filtered);
    });
}

/**
 * 重置筛选
 */
function resetScoringFilter() {
    $('#scoringFilterDeclareAccount').val('');
    $('#scoringFilterProCode').val('');
    $('#scoringFilterProName').val('');

    // 清除localStorage
    clearScoringFilterStorage();

    // 对所有已加载的子奖项重新渲染（显示全部数据）
    $.each(scoredDataCache, function(proSubType) {
        renderTable(proSubType, scoredDataCache[proSubType]);
    });
}

/**
 * 从DOM读取筛选条件并保存到localStorage
 */
function persistScoringFilterFromDom() {
    try {
        var data = {
            declareAccount: $('#scoringFilterDeclareAccount').val() || '',
            proCode: $('#scoringFilterProCode').val() || '',
            proName: $('#scoringFilterProName').val() || ''
        };
        // 如果所有字段都为空，则清除存储
        if (!data.declareAccount && !data.proCode && !data.proName) {
            localStorage.removeItem(SCORING_FILTER_KEY);
        } else {
            localStorage.setItem(SCORING_FILTER_KEY, JSON.stringify(data));
        }
    } catch (e) { /* ignore */ }
}

/**
 * 从localStorage恢复筛选条件到DOM
 */
function restoreScoringFilterFromStorage() {
    try {
        var raw = localStorage.getItem(SCORING_FILTER_KEY);
        if (raw == null || raw === '') {
            return;
        }
        var data = JSON.parse(raw);
        if (!data || typeof data !== 'object') {
            return;
        }
        $('#scoringFilterDeclareAccount').val(data.declareAccount || '');
        $('#scoringFilterProCode').val(data.proCode || '');
        $('#scoringFilterProName').val(data.proName || '');
    } catch (e) { /* ignore */ }
}

/**
 * 清除localStorage中的筛选条件
 */
function clearScoringFilterStorage() {
    try {
        localStorage.removeItem(SCORING_FILTER_KEY);
    } catch (e) { /* ignore */ }
}