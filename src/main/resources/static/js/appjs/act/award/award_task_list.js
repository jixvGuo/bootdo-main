/* ============================================================================
 * 文件：award_task_list.js
 * 作用：申报任务管理列表页（左侧菜单 → "申报任务管理"）的前端动态交互
 *
 * ★ 多奖项共用同一个页面 ★
 *   本列表页通过 localStorage.enterType（即 awardId）区分当前进入的奖项：
 *     awardId == 1  科技进步奖
 *     awardId == 2  勘察设计奖（勘察奖）  ← 本项目重点维护对象
 *     awardId == 3  QC 质量管理奖
 *     awardId == 4  优质工程奖
 *     awardId == 5  工法奖
 *
 *   ⚠ 因此操作列 formatter 中大量 if(row.awardId == X) 分支并不是冗余，
 *     而是同一行数据按当前奖项类型展示不同按钮、跳转不同后端 URL。
 *
 * 主要结构：
 *   - 工具函数：QC 任务阶段判断、申报按钮状态、协会角色矩阵等
 *   - load()：初始化 bootstrapTable，定义列与数据请求
 *   - 操作列 formatter：按奖项 + 角色生成按钮 HTML（详见函数内分支注释）
 *   - 各类入口函数：applyAward / applySurverAward / surverProList 等
 *   - 任务打分情况弹窗 & 导出（_awardShowTaskScoresModal / exportTaskScoreMatrix）
 * ============================================================================ */

// 动态交互和数据处理
var prefix = "/award_flow"
// 页面初始化
$(function() {
	load();
});
// ================= 第2步修改：QC任务阶段/按钮控制工具函数开始 =================

/**
 * 第2步修改：判断申报是否结束
 */
function isQcApplyClosed(row) {
    var end = row.applyEndDate || "";
    if (!end) return false;
    var endDate = new Date((end + "").replace(/-/g, "/"));
    if (isNaN(endDate.getTime())) return false;
    return endDate <= new Date();
}

/**
 * 第2步修改：获取QC任务阶段（优先后端taskStageCode，兜底用时间推断）
 * 返回：WAIT_APPLY / APPLYING / CHECKING / CHECK_END
 */
// function resolveQcTaskStageCode(row) {
//     if (row.taskStageCode) return row.taskStageCode;
//
//     // 兜底逻辑（如果后端暂未返回 taskStageCode）
//     var now = new Date();
//     var applyStart = row.applyStartDate ? new Date((row.applyStartDate + "").replace(/-/g, "/")) : null;
//     var applyEnd = row.applyEndDate ? new Date((row.applyEndDate + "").replace(/-/g, "/")) : null;
//     var checkStart = row.checkStartTime ? new Date((row.checkStartTime + "").replace(/-/g, "/")) : null;
//     var checkEnd = row.checkEndTime ? new Date((row.checkEndTime + "").replace(/-/g, "/")) : null;
//
//     var applyStarted = applyStart && !isNaN(applyStart.getTime()) && now >= applyStart;
//     var applyClosed = applyEnd && !isNaN(applyEnd.getTime()) && now >= applyEnd;
//     var checkStarted = checkStart && !isNaN(checkStart.getTime()) && now >= checkStart;
//     var checkEnded = checkEnd && !isNaN(checkEnd.getTime()) && now >= checkEnd;
//
//     if (!applyStarted && !checkStarted) return "WAIT_APPLY";
//     if (applyStarted && !checkStarted && !applyClosed) return "APPLYING";
//     if (checkEnded) return "CHECK_END";
//     if (checkStarted) return "CHECKING";
//     if (applyStarted && applyClosed) return "CHECKING";
//     return "WAIT_APPLY";
// }
/**
 * 第 2 步修改：获取 QC 任务阶段（优先后端 taskStageCode，兜底用时间推断）
 * 返回：WAIT_APPLY / APPLYING / CHECKING / CHECK_END
 */
// 根据当前时间和任务时间节点，判断QC奖任务所处阶段
function resolveQcTaskStageCode(row) {
	if (row.taskStageCode) return row.taskStageCode;

	// 兜底逻辑（如果后端暂未返回 taskStageCode）
	var now = new Date();
	var applyStart = row.applyStartDate ? new Date((row.applyStartDate + "").replace(/-/g, "/")) : null;
	var applyEnd = row.applyEndDate ? new Date((row.applyEndDate + "").replace(/-/g, "/")) : null;
	var checkStart = row.checkStartTime ? new Date((row.checkStartTime + "").replace(/-/g, "/")) : null;
	var checkEnd = row.checkEndTime ? new Date((row.checkEndTime + "").replace(/-/g, "/")) : null;

	var applyStarted = applyStart && !isNaN(applyStart.getTime()) && now >= applyStart;
	var applyClosed = applyEnd && !isNaN(applyEnd.getTime()) && now >= applyEnd;
	var checkStarted = checkStart && !isNaN(checkStart.getTime()) && now >= checkStart;
	var checkEnded = checkEnd && !isNaN(checkEnd.getTime()) && now >= checkEnd;

	// 新增：专家阶段时间解析
	var expertStart = row.expertStartTime ? new Date((row.expertStartTime + "").replace(/-/g, "/")) : null;
	var expertEnd = row.expertEndTime ? new Date((row.expertEndTime + "").replace(/-/g, "/")) : null;
	var expertStarted = expertStart && !isNaN(expertStart.getTime()) && now >= expertStart;
	var expertEnded = expertEnd && !isNaN(expertEnd.getTime()) && now >= expertEnd;
	var hasExpertTime = expertStart && !isNaN(expertStart.getTime());

	if (!applyStarted && !checkStarted) return "WAIT_APPLY";
	if (applyStarted && !checkStarted && !applyClosed) return "APPLYING";
	// 新增：申报结束、形审未开始的独立状态
	if (applyStarted && applyClosed && !checkStarted) return "APPLY_CLOSED";
	// 原代码：if (checkEnded) return "CHECK_END";
	// 新代码：形审结束后继续判断专家阶段
	if (checkEnded) {
		if (hasExpertTime) {
			if (expertEnded) return "FINISHED";
			if (expertStarted) return "SCORING";
			return "ASSIGN_EXPERTS";
		}
		return "CHECK_END";
	}
	if (checkStarted) return "CHECKING";
	return "WAIT_APPLY";
}
/**
 * 第2步修改：QC任务状态展示映射
 * 需求映射：等待申请 / 申请中 / 形式审查（含CHECK_END可按你们展示策略调整）
 */
// function resolveQcStageText(stageCode) {
//     if (stageCode === "WAIT_APPLY") return "等待申请";
//     if (stageCode === "APPLYING") return "申请中";
//     if (stageCode === "CHECKING") return "形式审查";
//     if (stageCode === "CHECK_END") return "结束"; // 若你要显示“形审结束”，改这里
//     return "等待申请";
// }

// 将任务阶段代码映射为中文显示文本
function resolveQcStageText(stageCode) {
	if (stageCode === "WAIT_APPLY") return "等待申请";
	if (stageCode === "APPLYING") return "申请中";
	if (stageCode === "APPLY_CLOSED") return "申报结束"; // 新增
	if (stageCode === "CHECKING") return "形式审查";
	if (stageCode === "CHECK_END") return "形审结束";
	// 新增：专家阶段状态文本
	if (stageCode === "ASSIGN_EXPERTS") return "分派专家";
	if (stageCode === "SCORING") return "专家打分";
	if (stageCode === "FINISHED") return "完成";
	return "等待申请";
}
/**
 * 第2步修改：QC项目申报按钮UI状态
 * 规则：
 * WAIT_APPLY：显示但不可点
 * APPLYING：可点
 * CHECKING：申报未结束可点，结束则隐藏
 * CHECK_END：隐藏
 */
// function resolveQcApplyBtnState(row) {
//     var stageCode = resolveQcTaskStageCode(row);
//     var applyClosed = isQcApplyClosed(row);
//
//     var state = {
//         stageCode: stageCode,
//         stageText: resolveQcStageText(stageCode),
//         showBtn: true,
//         enableBtn: false
//     };
//
//     if (stageCode === "WAIT_APPLY") {
//         state.showBtn = true;
//         state.enableBtn = false;
//     } else if (stageCode === "APPLYING") {
//         state.showBtn = true;
//         state.enableBtn = true;
//     } else if (stageCode === "CHECKING") {
//         if (applyClosed) {
//             state.showBtn = false;
//             state.enableBtn = false;
//         } else {
//             state.showBtn = true;
//             state.enableBtn = true;
//         }
//     } else if (stageCode === "CHECK_END") {
//         state.showBtn = true;
//         state.enableBtn = true;
//     }
//
//     return state;
// }

/**
 * 根据任务阶段决定"QC项目申报"按钮的显示与可用状态
 * 调用resolveQcTaskStageCode判断当前阶段
 * 配置状态：返回对象包含showBtn（是否显示）和enableBtn（是否可点）
 * 规则：申报中/已结束时可点；等待申请时置灰；形审结束及后续阶段隐藏按钮
 * @param row
 * @returns {{enableBtn: boolean, stageText: string, showBtn: boolean, stageCode: (*|string)}}
 */
function resolveQcApplyBtnState(row) {
	var stageCode = resolveQcTaskStageCode(row);
	var applyClosed = isQcApplyClosed(row);

	var state = {
		stageCode: stageCode,
		stageText: resolveQcStageText(stageCode),
		showBtn: true,
		enableBtn: false
	};

	if (stageCode === "WAIT_APPLY") {
		state.showBtn = true;
		state.enableBtn = false;
	} else if (stageCode === "APPLYING") {
		state.showBtn = true;
		state.enableBtn = true;
	} else if (stageCode === "APPLY_CLOSED") {
		// 新增：申报结束、形审未开始，隐藏申报按钮，只保留查看列表
		state.showBtn = true;
		state.enableBtn = true;
	} else if (stageCode === "CHECKING") {
		if (applyClosed) {
			state.showBtn = true;
			state.enableBtn = true;
		} else {
			state.showBtn = true;
			state.enableBtn = true;
		}
	} else if (stageCode === "CHECK_END") {
		state.showBtn = false;
		state.enableBtn = false;
	// 新增：专家阶段也隐藏申报按钮
	} else if (stageCode === "ASSIGN_EXPERTS" || stageCode === "SCORING" || stageCode === "FINISHED") {
		state.showBtn = false;
		state.enableBtn = false;
	}

	return state;
}
// ================= 第2步修改：QC任务阶段/按钮控制工具函数结束 =================

// ================= 第3步修改：QC协会角色任务页矩阵开始 =================

/**
 * 第3步修改：协会领导/联系人任务页按钮矩阵（两者一致）
 * WAIT_APPLY：QC奖项目列表、查看、编辑、删除
 * APPLYING：QC奖项目列表、编辑、分派、分组、删除
 * CHECKING/CHECK_END：QC奖项目列表、导入形式审查结果、查看、编辑、分组、分派、删除
 */
/**
 * 该函数根据 QC 奖任务的不同阶段，返回协会管理角色可执行的操作权限配置：
 *
 * - **默认配置**: 项目列表、查看、编辑始终可用
 * - **WAIT_APPLY**(等待申请): 可查看、编辑、删除
 * - **APPLYING**(申请中): 可查看、编辑、删除、分组
 * - **APPLY_CLOSED**(申报结束): 可查看、编辑、删除
 * - **CHECKING**(形式审查): 所有操作开放 (含分派、分组、导入结果)
 * - **CHECK_END**(形审结束): 保留查看、编辑、删除、导入结果，关闭分派和分组
 *
 * */
function resolveQcManagerTaskOps(stageCode) {
    var ops = {
        showQcProList: true,
        showView: true,
        showEdit: true,
        showDelete: false,
        showAssign: false,
        showGroup: false,
        showImportCheckResult: false
    };

    if (stageCode === "WAIT_APPLY") {
        ops.showView = true;
        ops.showEdit = true;
        ops.showDelete = true;
    } else if (stageCode === "APPLYING") {
        ops.showEdit = true;
        ops.showDelete = true;
        ops.showAssign = false;
        ops.showGroup = true;
		ops.showView = true;
    }else if (stageCode === "APPLY_CLOSED") {
		// 申报结束阶段，显示查看和编辑按钮
		ops.showView = true;
		ops.showEdit = true;
		ops.showDelete = true;
	}else if (stageCode === "CHECKING" ) {
        ops.showView = true;
        ops.showEdit = true;
        ops.showDelete = true;
        ops.showAssign = true;
        ops.showGroup = true;
        ops.showImportCheckResult = true;
    } else if (stageCode === "CHECK_END") {
        // 形审结束后仍保留编辑按钮
        ops.showView = true;
        ops.showEdit = true;
        ops.showDelete = true;
        ops.showAssign = false;
        ops.showGroup = false;
        ops.showImportCheckResult = true;
    // 新增：分派专家阶段 - 显示分派和分组按钮
    } else if (stageCode === "ASSIGN_EXPERTS") {
        ops.showView = true;
        ops.showEdit = true;
        ops.showDelete = true;
        ops.showAssign = true;
        ops.showGroup = true;
        ops.showImportCheckResult = false;
    // 新增：专家打分阶段 - 只读查看
    } else if (stageCode === "SCORING") {
        ops.showView = true;
        ops.showEdit = true;
        ops.showDelete = false;
        ops.showAssign = false;
        ops.showGroup = false;
        ops.showImportCheckResult = false;
    // 新增：完成阶段 - 只读查看
    } else if (stageCode === "FINISHED") {
        ops.showView = true;
        ops.showEdit = false;
        ops.showDelete = false;
        ops.showAssign = false;
        ops.showGroup = false;
        ops.showImportCheckResult = false;
    }

    return ops;
}
// /**
//  * 第3步修复：判断是否协会管理角色（领导/联系人）
//  * 说明：用现有权限按钮变量做最小侵入识别
//  */
// function isQcManagerRole() {
//     // 推荐后端在页面注入 window.pageRoleType = 'QC_MANAGER' / 'ENTERPRISE' / ...
//     if (typeof window.pageRoleType !== 'undefined') {
//         return window.pageRoleType === 'QC_MANAGER';
//     }
//     // 兼容旧逻辑（降级）
//     var canImport = (typeof s_check_result_import !== 'undefined' && s_check_result_import !== 'hidden');
//     var canManage = (typeof s_management_h !== 'undefined' && s_management_h !== 'hidden');
//     return canImport || canManage;
// }

function isQcManagerRole() {
    // 1) 优先后端明确注入角色（推荐）
    if (typeof window.pageRoleType !== 'undefined') {
        return window.pageRoleType === 'QC_MANAGER';
    }

    // 2) 兜底：使用页面隐藏域（模板已有 isAssociationRole）
    var isAssociationRole = $("#isAssociationRole").val();
    if (typeof isAssociationRole !== 'undefined') {
        return String(isAssociationRole) === 'true';
    }

    // 3) 最后兜底：仅看“专业组管理”权限，不看导入权限
    var canManage = (typeof s_management_h !== 'undefined' && s_management_h !== 'hidden');
    return canManage;
}

/**
 * 第3步修改：QC分组入口
 */
function qcGroup(taskId) {
    // 说明：按现有系统路由命名给默认地址，若不一致请替换为真实地址
    page('/qcProcess/toGroup?taskId=' + taskId, 'QC分组', 2026030401);
}

// ================= 第3步修改：QC协会角色任务页矩阵结束 =================

// /**
//  * 第3步修复：判断是否协会管理角色（领导/联系人）
//  * 说明：用现有权限变量做最小侵入判断
//  */
// function isQcManagerRole() {
//     var canImport = (typeof s_check_result_import !== 'undefined' && s_check_result_import !== 'hidden');
//     var canManage = (typeof s_management_h !== 'undefined' && s_management_h !== 'hidden');
//     return canImport || canManage;
// }


// 勘察奖入口界面，更不好找的那个，有勘察项目列表、查看、编辑、专业组管理、分数查询、删除
// bootstrap Table 配置和数据加载
function load() {
	/**
	 * 从浏览器本地存储中读取当前选中的奖项类型ID（如QC奖=3、科技奖=1等）
	 * 用于后续过滤只显示该类型的任务
	 * @type {string}
	 */
	var awrdId =  localStorage.getItem("enterType") + "" ;//输出
	$('#exampleTable')
		.bootstrapTable(
			{
				method : 'get', // 服务器数据的请求方式 get or post
				url : prefix + "/list_publish_tasks", // 服务器数据的加载地址
				//	showRefresh : true,
				//	showToggle : true,
				//	showColumns : true,
				iconSize : 'outline',
				toolbar : '#exampleToolbar',
				striped : true, // 设置为true会有隔行变色效果
				dataType : "json", // 服务器返回的数据类型
				pagination : true, // 设置为true会在底部显示分页条
				// queryParamsType : "limit",
				// //设置为limit则会发送符合RESTFull格式的参数
				singleSelect : false, // 设置为true将禁止多选
				// contentType : "application/x-www-form-urlencoded",
				// //发送到服务器的数据编码类型
				pageSize : 10, // 如果设置了分页，每页数据条数
				pageNumber : 1, // 如果设置了分布，首页页码
				//search : true, // 是否显示搜索框
				showColumns : false, // 是否显示内容下拉框（选择显示的列）
				sidePagination : "server", // 设置在哪里进行分页，可选值为"client" 或者 "server"
				queryParams : function(params) {
					return {
						//说明：传入后台的参数包括offset开始索引，limit步长，sort排序列，order：desc或者,以及所有列的键值对
						limit : params.limit,
						offset : params.offset,
					    name:$('#searchName').val(),
						awardId: awrdId,
					};
				},
				// //请求服务器数据时，你可以通过重写参数的方式添加一些额外的参数，例如 toolbar 中的参数 如果
				// queryParamsType = 'limit' ,返回参数必须包含
				// limit, offset, search, sort, order 否则, 需要包含:
				// pageSize, pageNumber, searchText, sortName,
				// sortOrder.
				// 返回false将会终止请求
				columns : [
					{
						// visible :false,
						field : 'id',
						title : '编号'
					},

					{
						field : 'taskName',
						title : '标题'/*,
						formatter: function(value,row,index){
							return '<a href="#" onclick="edit(\''+row.id+'\')">'+row.title+'</a>';
						}*/
					},
					{
						field : 'associationUserName',
						title : '协会负责人'
					},

                    {
                        field : 'taskStatStr',
                        title : '状态',
						formatter: function(value, row, index) {
							// 第2步修改：仅QC奖（awardId=3）使用阶段映射展示
							if (row.awardId == 3) {
								var qcState = resolveQcApplyBtnState(row);
								return qcState.stageText;
							}
							return value || '';
						}

                    },


                    {

						field : 'publishDate',
						title : '发布时间',
						formatter: function(value,row,index){
							return value.replace(/.[0-9]*$/,'');
						}
					},
					// {
					// 	// visible : false,
					// 	field : 'applyEndDate',
					// 	title : '结束时间',
					// 	formatter: function(value,row,index){
					// 		return value.replace(/.[0-9]*$/,'');
					// 	}
					// },

					// 协会领导：查看 项目汇总 分数查询
					//
					{
						visible:false,
						field :"proId",
						title:"项目id"
					},
					{
						visible:false,
						field:"selSpecialist",
						title:"是否选择专家"
					},
					/* ----------------------------------------------------------------
					 * 操作列：所有奖项共用此 formatter，按 awardId + 角色返回不同按钮组合
					 *   awardId == 1 科技进步奖 / 2 勘察设计奖(重点) / 3 QC奖 / 4 优质工程奖 / 5 工法奖
					 * 返回分支：
					 *   分支1: QC(3) + 协会管理角色   按任务阶段矩阵显示按钮
					 *   分支2: QC(3) + 企业用户         仅 "申报 + 查看"
					 *   分支3: 默认（含勘察奖2）        项目列表/申报/编辑/专业组管理/分派 等
					 * UI : flex 布局保证按钮居左对齐 + 自动换行（width:420 / align:left）
					 * ---------------------------------------------------------------- */
					{
						title : '操作',
						field : 'operation',
						align : 'left',
						// 新增：表头同样居左对齐（不删除原 align，仅补充表头对齐）
						halign : 'left',
						width : 420,
						formatter : function(value, row, index) {
							var asssociationProList = '<a class="btn btn-primary btn-sm ' + s_assciation_prolist + '" href="#" mce_href="#" title="查看" onclick="viewProList(\''
								+ row.id
								+ '\','
							    + row.awardId
								+')">项目列表</a> ';
                            var a = '<a class="btn btn-primary btn-sm ' + s_watch_h + '" href="#" mce_href="#" title="查看" onclick="watchPro(\''
                                + row.id
                                + '\')">查看</a> ';

							var file = '<a class="btn btn-primary btn-sm ' + s_expert_upload_file_btn + '" href="#" mce_href="#" title="上传附件" onclick="uplaodFile(\''
								+ row.id
								+ '\')">上传材料</a> ';




                            var e = '<a class="btn btn-primary btn-sm ' + s_edit_h + '" href="#" mce_href="#" title="编辑" onclick="edit(\''
								+ row.id
								+ '\')">编辑</a> ';


                            var d = '<a class="btn btn-warning btn-sm ' + s_remove_h + '" href="#" title="删除"  mce_href="#" onclick="remove(\''
								+ row.id
								+ '\')"><i class="fa fa-remove"></i></a> ';

							// var isSpecialAdmin = row.isSpecialAdmin ? '' : ' style="pointer-events:none" disabled="true" ';
							var isSpecialAdmin = '';
							// 科技进步奖的专家分派页面
							let specGroupAdminUrl = '/scienceProgressScience/toAssignExperts?taskId='+row.id+"&proType=science_progress";
							if (row.awardId == 2) {
								// 勘察奖：专业组管理跳转到专用页面，且始终显示
								specGroupAdminUrl = '/cpe/suverProcess/toSurverMajorGroupAdmin?taskId=' + row.id + '&proType=surver_pro_group';
							}
							let managementBtn = row.isSpecialAdmin ? s_management_h : 'hidden';
							if (row.awardId == 2) {
								managementBtn = s_management_h;
							}
							var f = '<a class="btn btn-primary btn-sm ' +
								managementBtn + '" ' +
								isSpecialAdmin +
								' href="javascript:page(\''+ specGroupAdminUrl  +'\',\'专业组管理\')" title="专业组管理"  mce_href="#">专业组管理</a> ';

							let projectScoreBtn = row.isScore ? '' : 'hidden';
							let viewScoreUrl = '/specialist/associationViewScore?taskId='+row.id+"&proType=science_progress";
							if (row.awardId == 2) {
                            	viewScoreUrl = '/surverScore/associationViewScore?taskId=' + row.id
                            }
							var g = '<a class="btn btn-primary btn-sm  ' +
								projectScoreBtn +
								'"  href="javascript:page(\''+ viewScoreUrl  +'\',\'分数查询\')" title="分数查询"  mce_href="#">分数查询</a> ';

							var ha = '<a class="btn btn-success btn-sm '+ s_project_summary +'" href="#" title="项目汇总"  mce_href="#" onclick="listPro(\''
								+ row.id
								+ '\')"> 项目汇总</a> ';//' + s_project_summary +  '





                            // var isAssignFlg = row.isAssign ? '' : ' style="pointer-events:none" disabled="true" ';
							var isAssignFlg = '';
							let taskAssignBtn = row.isAssign ? '' : 'hidden';

							let validateUrl = '/scienceTask/toAssign?taskId=' + row.id;
							if(row.awardId == 3) {
								//QC奖
								validateUrl = '/qcProcess/toAssign?taskId=' + row.id;
							}
							if (row.awardId == 2) {
								validateUrl = '/cpe/suverProcess/toAssign?taskId=' + row.id
							}

							var i = '<a class="btn btn-success btn-sm '+ taskAssignBtn +'" ' + isAssignFlg + ' href="javascript:page(\''+ validateUrl +'\',\'分派\',2022020700)" title="分派项目"  mce_href="#"  >分派</a> ';

							let selSpecialistBtn = row.isSpecialAdmin ? s_selSpecialist_h : 'hidden';
							var j = '<a class="btn btn-success btn-sm ' + selSpecialistBtn + '" href="#" title="选择专家"  mce_href="#" onclick="selectSpecialist(\''
								+ row.id
                                + '\',\''
							    + row.awardId
								+ '\')">分派专家</a> ';
							//取消此种方式的分派专家20220909
							j = '';

							// let qcApplyBtn =   row.awardId == 3 ? s_apply_qc_btn : 'hidden';
							// //奖项申报按钮
							// var applyQcBtn = '<a class="btn btn-success btn-sm ' + qcApplyBtn + '" href="#" title="QC项目申报"  mce_href="#" onclick="applyAward(\''
							// 	+ row.id
							// 	+ '\',\'qc\')">QC项目申报</a> ';
							// 第2步修改：QC项目申报按钮按阶段控制（显示/可点击）
							var applyQcBtn = '';
							if (row.awardId == 3) {
								var qcApplyState = resolveQcApplyBtnState(row);

								if (qcApplyState.showBtn) {
									if (qcApplyState.enableBtn) {
										// 可点击
										applyQcBtn = '<a class="btn btn-success btn-sm ' + s_apply_qc_btn + '" href="#" title="QC项目申报" mce_href="#" onclick="applyAward(\''
											+ row.id
											+ '\',\'qc\')">QC项目申报</a> ';
									} else {
										// 第2步修改：显示但置灰不可点击（WAIT_APPLY）
										applyQcBtn = '<a class="btn btn-default btn-sm ' + s_apply_qc_btn + '" href="javascript:void(0)" title="QC项目申报（当前阶段不可申报）" style="pointer-events:none;opacity:.65;cursor:not-allowed;">QC项目申报</a> ';
									}
								}
							}

							//工法奖项申报按钮
							let gfApplyBtn = row.isApply &&  row.awardId == 5 ? s_apply_gf_btn : 'hidden';
							var applyGfBtn = '<a class="btn btn-success btn-sm ' + gfApplyBtn + '" href="#" title="工法项目申报"  mce_href="#" onclick="applyAward(\''
								+ row.id
								+ '\',\'gf\')">工法项目申报</a> ';

							let surverProListBtn = row.awardId == 2 ? s_surver_pro_list_btn : 'hidden';
							var proListBtn =  '<a class="btn btn-success btn-sm ' + surverProListBtn + '" href="#" title="勘察奖项目列表"  mce_href="#" onclick="surverProList(\''
								+ row.id
								// + '\',\'design\')">勘察奖项目列表</a> ';
								+ '\',\'design\')">勘察项目列表</a> ';

                            let readonly = row.isApply ? 0 : 1;
							let applySurverBtn = row.isApply &&  row.awardId == 2 ? s_apply_surver_btn : 'hidden';
							var applyAwardBtn = '<select class="btn btn-success btn-sm ' + applySurverBtn
								+ '" onchange="applySurverAward(\''
								+row.id
								+'\',this.options[this.options.selectedIndex].value, ' + readonly + ')">' +
								'  <option value="">选择申报项目</option>' +
								// '  <option value="excellent">石油工程建设优秀勘察奖</option>' +
								// '  <option value="design">石油工程建设优秀设计奖</option>' +
								// '  <option value="software">石油工程建设优秀勘察设计计算机软件奖</option>' +
								// '  <option value="standard">石油工程建设优秀标准设计奖</option>' +
								'  <option value="excellent">勘察项目</option>' +
								'  <option value="design">设计项目</option>' +
								'  <option value="software">计算机软件项目</option>' +
								'  <option value="standard">标准设计项目</option>' +
								// '  <option value="consulting">石油工程建设优秀咨询奖</option>' +
								'</select>';

							// var surverEnterListBtn = '<a class="btn btn-success btn-sm ' + s_surver_enterprise_list_btn + '" href="#" title="企业列表"  mce_href="#" onclick="toEnterpriseList(\''
							// 	+ row.id
							// 	+ '\')">企业列表</a> ';

							// 第2步修改：统一删除“企业列表”按钮（尤其QC角色冲突）
							var surverEnterListBtn = ''; // 第3步修改：显式声明，避免污染全局

                            // 隐藏“导入形式审查结果”按钮（申报任务管理页）
                            let checkResultFlg = 'hidden';
                            let checkResultFileType = 'import_check_result';
                            if (row.awardId == 3) {
                                checkResultFileType = 'import_check_result_qc';
                            } else if (row.awardId == 2) {
                                checkResultFileType = 'import_check_result_surver';
                            }
							var checkResultImportBtn = '<a class="btn btn-success btn-sm ' + checkResultFlg + '" href="#" title="导入形式审查结果"  mce_href="#" onclick="uploadFileData(\''
                                + row.id
                                + '\',\'' + checkResultFileType
                                + '\')">导入形式审查结果</a> ';

                            //优质工程奖
                            if(row.awardId == 4) {
                              let readonlyBestPro = row.isApply ? 0 : 1
                              let applyBestProBtn = row.isApply ? s_apply_best_pro_btn : 'hidden';
                              applyAwardBtn = '<select class="btn btn-success btn-sm '+applyBestProBtn+'" onchange="applyBestProAward(\''+row.id+'\',this.options[this.options.selectedIndex].value,' + readonlyBestPro + ')">' +
                             	'  <option value="">选择申报项目</option>' +
                             	// '  <option value="bestPro">石油优质工程奖</option>' +
                             	'  <option value="bestProGold">石油优质工程奖</option>' +
                             	// '  <option value="bestProInstall">石油安装工程</option>' +
                             	'</select>';
                              proListBtn =  '<a class="btn btn-success btn-sm ' + s_best_pro_list_btn + '" href="#" title="优质工程奖项目列表"  mce_href="#" onclick="bestProList(\''
                             							    	+ row.id
                             								    + '\')">优质工程奖项目列表</a> ';
                            }

                            //科技奖
							let readonlyScience = row.isApply ? 0 : 1
							let applyTechnologyBtn = row.isApply &&  row.awardId == 1 ? s_apply_technology_btn : 'hidden';
							if(row.awardId == 1) {
                              applyAwardBtn = '<select class="btn btn-success btn-sm '+applyTechnologyBtn+'" onchange="applyTechnologyAward(\''+row.id+'\',this.options[this.options.selectedIndex].value,' + readonlyScience + ')">' +
                              	'  <option value="">选择申报项目</option>' +
                              	'  <option value="science">科学技术奖成果</option>' +
                              	'  <option value="team">先进团队成果</option>' +
                              	'  <option value="personal">先进个人成果</option>' +
                              	'</select>';
							}
							let gfProListBtn = row.awardId == 5 ? s_gf_pro_list_btn : 'hidden';
                            if(row.awardId == 5) {
                              proListBtn =  '<a class="btn btn-success btn-sm ' + gfProListBtn + '" href="#" title="工法奖项目列表"  mce_href="#" onclick="gfProList(\''
							    	+ row.id
								    + '\')">工法奖项目列表</a> ';
                            }

							let scienceProListBtn = row.awardId == 1 ? s_science_pro_list_btn : 'hidden';
							if(row.awardId == 1) {
								proListBtn =  '<a class="btn btn-success btn-sm ' + scienceProListBtn + '" href="#" title="科技奖项目列表"  mce_href="#" onclick="technologyProList(\''
									+ row.id
									+ '\')">科技奖项目列表</a> ';
							}



                            // let qcProListBtn = row.awardId == 3 ? s_qc_pro_list_btn : 'hidden';
                            // if(row.awardId == 3){
                            //   proListBtn =  '<a class="btn btn-success btn-sm ' + scienceProListBtn + '" href="#" title="QC奖项目列表"  mce_href="#" onclick="QcProList(\''
							//     	+ row.id
							// 	    + '\')">QC奖项目列表</a> ';
                            // }
							if(row.awardId == 3 && isQcManagerRole()){
								// 第3步修改：按阶段计算协会领导/联系人任务按钮矩阵
								var stageCode = resolveQcTaskStageCode(row);
								var qcOps = resolveQcManagerTaskOps(stageCode);

								// 修复：该页面没有 s_qc_pro_list_btn 变量，使用已存在的 qcProListBtn 权限变量
								var qcProListBtn = '<a class="btn btn-success btn-sm '
									+ s_qc_pro_list_btn
									+ '" href="#" title="QC奖项目列表" mce_href="#" onclick="QcProList(\'' + row.id + '\')">QC奖项目列表</a> ';

								// 查看
								var qcViewBtn = qcOps.showView
									? '<a class="btn btn-primary btn-sm ' + s_watch_h + '" href="#" mce_href="#" title="查看" onclick="watchPro(\'' + row.id + '\')">查看</a> '
									: '';

								// 编辑（只受权限控制，不受阶段控制，所有阶段都显示）
								var qcEditBtn = '<a class="btn btn-primary btn-sm ' + s_edit_h + '" href="#" mce_href="#" title="编辑" onclick="edit(\'' + row.id + '\')">编辑</a> ';

								// 删除
								var qcDeleteBtn = qcOps.showDelete
									? '<a class="btn btn-warning btn-sm ' + s_remove_h + '" href="#" title="删除" mce_href="#" onclick="remove(\'' + row.id + '\')"><i class="fa fa-remove"></i></a> '
									: '';

								// 分派
								var qcAssignBtn = qcOps.showAssign
									? '<a class="btn btn-success btn-sm" href="javascript:page(\'/qcProcess/toAssign?taskId=' + row.id + '\',\'分派\',2022020700)" title="分派项目" mce_href="#">分派</a> '
									: '';

								// 作用域：仅QC奖 + 协会管理角色
								// 专业组管理（移除时间限制，只要有权限就始终显示）
								// 管理员都可见的专业组管理按钮、无权限的专业组管理按钮
								var isQcAssociationContactRole70 = $("#isQcAssociationContactRole70").val();
								// var qcExpertGroupBtn = (typeof s_management_h !== 'undefined' && s_management_h !== 'hidden')
								var qcExpertGroupBtn = (typeof s_management_h !== 'undefined' && s_management_h !== 'hidden'
										&& String(isQcAssociationContactRole70) !== 'true')
									? '<a class="btn btn-primary btn-sm" href="javascript:page(\'/qcProcess/toAddSpecialist?taskId='
									+ row.id
									+ '\',\'专业组管理\')" title="专业组管理" mce_href="#">专业组管理</a> '
									: '';

								// // 分数查询（含导出评分汇总表）
								// var qcScoreBtn = (typeof s_project_score !== 'undefined' && s_project_score !== 'hidden')
								// 	? '<a class="btn btn-info btn-sm" href="javascript:viewTaskScoresByTaskId(\'' + row.id + '\')" title="分数查询" mce_href="#">分数查询</a> '
								// 	: '';

								// 导入形式审查结果
								var qcImportBtn = qcOps.showImportCheckResult
									? '<a class="btn btn-success btn-sm ' + checkResultFlg + '" href="#" title="导入形式审查结果" mce_href="#" onclick="uploadFileData(\''
										+ row.id + '\',\'import_check_result_qc\')">导入形式审查结果</a> '
									: '';

								// === 分支1 return：QC管理员（按阶段矩阵） ===
								// 第3步修改：QC角色冲突按钮不拼（企业列表/其他奖项申报入口）
								// === 美化前（已注释，保留参考）===
								// return '<div style="display:flex;flex-wrap:wrap;gap:6px;row-gap:6px;align-items:center;">' + qcProListBtn + qcImportBtn + qcViewBtn + qcEditBtn + qcAssignBtn + qcExpertGroupBtn + qcDeleteBtn + '</div>';
								// === 美化后（已注释，保留参考）===
								// return '<div style="display:flex;flex-wrap:wrap;justify-content:flex-start;align-items:center;gap:6px;row-gap:6px;text-align:left;padding:4px 6px;">' + qcProListBtn + qcImportBtn + qcViewBtn + qcEditBtn + qcAssignBtn + qcExpertGroupBtn + qcDeleteBtn + '</div>';
								// === 终版：参考 QC 视觉，使用 .op-btn-bar 类，按钮高度/字号/阴影统一 ===
								return qcProListBtn + qcImportBtn + qcViewBtn + qcEditBtn + qcAssignBtn + qcExpertGroupBtn + qcDeleteBtn;
								// return qcProListBtn + qcImportBtn + qcViewBtn + qcEditBtn + qcAssignBtn + qcDeleteBtn;
							}

							// if (row.awardId == 3 && isQcManagerRole()) {
							// 	// 协会矩阵：分派/分组/导入...
							// 	return qcProListBtn + qcImportBtn + qcViewBtn + qcEditBtn  + qcAssignBtn + qcDeleteBtn;
							// }

							// === 分支2：QC企业用户 ===
							if (row.awardId == 3 && !isQcManagerRole()) {
								// 企业矩阵：只保留项目列表 + 申报入口
								// === 美化前（已注释，保留参考）===
								// return '<div style="display:flex;flex-wrap:wrap;gap:6px;row-gap:6px;align-items:center;">' + applyQcBtn + a + '</div>';
								// === 美化后（已注释，保留参考）===
								// return '<div style="display:flex;flex-wrap:wrap;justify-content:flex-start;align-items:center;gap:6px;row-gap:6px;text-align:left;padding:4px 6px;">' + applyQcBtn + a + '</div>';
								// === 终版：参考 QC 视觉，使用 .op-btn-bar 类 ===
								return applyQcBtn + a;
							}

                            let cleanProBtn = '<a class="btn btn-success btn-sm ' + s_task_pro_clean_btn + '" href="#" title="删除全部项目"  mce_href="#" onclick="cleanTaskAllPro(\''
                                              								+ row.id
                                              								+ '\')">删除</a> ';
							// return proListBtn + applyAwardBtn + surverEnterListBtn + checkResultImportBtn + applyQcBtn + applyGfBtn + asssociationProList + a +file + e + d +  i + j + ha + f+ g + cleanProBtn;
							// === 分支3 return：默认（勘察奖2/科技奖1/工法奖5/优质工程奖4） ===
							// 第2步修改：不再拼接 surverEnterListBtn（企业列表），保留QC项目申报按钮
							// === 美化前（已注释，保留参考）===
							// return '<div style="display:flex;flex-wrap:wrap;gap:6px;row-gap:6px;align-items:center;">' + proListBtn + applyAwardBtn + checkResultImportBtn + applyQcBtn + applyGfBtn + asssociationProList + a + file + e + d + f + i + j + ha + g + cleanProBtn + '</div>';
							// === 美化后（已注释，保留参考）===
							// return '<div style="display:flex;flex-wrap:wrap;justify-content:flex-start;align-items:center;gap:6px;row-gap:6px;text-align:left;padding:4px 6px;">' + proListBtn + applyAwardBtn + checkResultImportBtn + applyQcBtn + applyGfBtn + asssociationProList + a + file + e + d + f + i + j + ha + g + cleanProBtn + '</div>';
							// === 终版：参考 QC 视觉，使用 .op-btn-bar 类 ===
							return proListBtn + applyAwardBtn + checkResultImportBtn + applyQcBtn + applyGfBtn + asssociationProList + a + file + e + d + f + i + j + ha + g + cleanProBtn;
						}
					} ],
				onPostBody:function (data) {
				}
			});
}

function cleanTaskAllPro(taskId) {
   layer.confirm('确定要删除选中任务下的全部项目吗？', {
        btn: ['确定', '取消']
      }, function () {
        $.ajax({
            url: "/proTask/remove",
            type: "post",
            data: {
                'taskId': taskId
            },
            success: function (r) {
                if (r.code == 0) {
                    layer.msg(r.msg);
                    reLoad();
                } else {
                    layer.msg(r.msg);
                }
            }
        });
    })
}

function reLoad() {
	$('#exampleTable').bootstrapTable('refresh');
}

function toEnterpriseList(taskId) {
	page('/surverEnterprise/toList?readonly=1&taskId=' + taskId, '企业信息列表', 20220218);
}

function uploadFileData(taskId, fileType) {

     parent.layer.open({
        zIndex:110,
	 	type : 2,
	 	title : '上传形式审查结果',
	 	maxmin : true,
	 	shadeClose : false, // 点击遮罩关闭层
	 	area : [ '800px', '520px' ],
	 	content : '/award_flow/to_uploadsmall?proId=0'+'&fileType='+fileType + '&taskId=' + taskId // iframe的url
	 });
}

function viewProList(taskId, awardId) {
	if(awardId == 1) {
		//TODO 暂未开发
	}

	if(awardId == 3) {
		page('/qcAward/view/proList?readonly=1&taskId=' + taskId, 'QC申报项目列表', 20220218);
	}

	if(awardId == 4) {
       //优质工程奖
       page('/enterpriseQualityAward/taskProList?readonly=1&taskId=' + taskId, '优质工程项目列表', 20220221);
	}
}

/***
 * 显示数据处理
 */
function addData(val) {
   var temp = "";
	switch (val) {
		case 1:
            temp = "";
			break;

    }

}
// 业务逻辑函数及复杂业务规则判断
function add() {
	var index =  localStorage.getItem("enterType") + "" ;//输出

	console.log("==========" + index)
	var addPage = layer.open({
		type : 2,
		title : '创建申报任务',
		maxmin : true,
		shadeClose : false, // 点击遮罩关闭层
		area : [ '800px', '520px' ],
		content : prefix + '/publish_award_task?awardId='+ index // iframe的url
	});
	layer.full(addPage);
}

function edit(id) {
	var index =  localStorage.getItem("enterType") + "" ;//输出
	var editPublishTaskPage = layer.open({
		type : 2,
		title : '编辑',
		maxmin : true,
		shadeClose : false, // 点击遮罩关闭层
		area : [ '800px', '520px' ],
		content : prefix + '/to_publish_task_edit?publishTaskId=' + id + '&awardId=' + index // iframe的url
	});
	layer.full(editPublishTaskPage);
}

/***
 * 查看
 * @param id
 */
function watchPro(id) {
	var index =  localStorage.getItem("enterType") + "" ;//输出

	var watchPublishTaskPage = layer.open({
        type : 2,
        title : '查看',
        maxmin : true,
        shadeClose : false, // 点击遮罩关闭层
        area : [ '800px', '520px' ],
        content : prefix + '/to_publish_task_watch?publishTaskId=' + id + '&awardId=' + index // iframe的url
    });
    layer.full(watchPublishTaskPage);
}

function remove(id) {
	layer.confirm('确定要删除选中的记录？', {
		btn : [ '确定', '取消' ]
	}, function() {
		$.ajax({
			url : prefix + "/remove",
			type : "post",
			data : {
				'id' : id
			},
			success : function(r) {
				if (r.code == 0) {
					layer.msg(r.msg);
					reLoad();
				} else {
					layer.msg(r.msg);
				}
			}
		});
	})
}

function apply(id){
	// var createProPage = layer.open({
	// 	type : 2,
	// 	title : '创建申请奖项项目',
	// 	maxmin : true,
	// 	shadeClose : false, // 点击遮罩关闭层
	// 	area : [ '800px', '520px' ],
	// 	content : prefix + '/form/' + id // iframe的url
	// });

	var createProPage = layer.open({
		type : 2,
		title : '科技进步奖成果申报',
		maxmin : true,
		shadeClose : false, // 点击遮罩关闭层
		area : [ '800px', '520px' ],
		content : prefix + '/chengguo/' + id // iframe的urlf
	});

	layer.full(createProPage);
}

function listPro(id) {
	var listProPage = layer.open({
		zIndex:90,
		type : 2,
		title : '项目列表',
		maxmin : true,
		shadeClose : false, // 点击遮罩关闭层
		area : [ '800px', '520px' ],
		content : '/enterprise_pro/to_list/' + id // iframe的url
	});

	layer.full(listProPage);
}

function assignPro(taskId) {
	//跳转到分配的页面
	layer.open({
		type : 2,
		title : '分派项目',
		maxmin : true,
		shadeClose : true, // 点击遮罩关闭层
		area : [ '800px', '520px' ],
		content : '/scienceTask/toAssign?taskId='+taskId // iframe的url
	});
}
//跳转到选择专家页面
function selectSpecialist(taskId, awardId) {
	if(awardId == '2') {
		page('/cpe/suverProcess/toAddSpecialist?taskId='+taskId,'分派专家',2022041900);
	}else {
		page('/specialist/to_select?taskId='+taskId,'分派专家',2022020700);
	}
}

/**
 * 申报奖项
 * @param taskId
 * @param awardType
 */
function applyAward(taskId, awardType) {
	let url = '';
	let title = '';

	if (awardType == 'qc') {
		url = '/qcAward/view/proList?taskId=' + taskId;
		title = '申报QC奖';
	}

	else if (awardType == 'gf') {
		url = '/gfAward/view/proList?taskId=' + taskId;
		title = '申报工法奖';
	}
	// ↓ 最后的 else 保持不变 ↓
	else {
		layer.msg('申报奖项类型不存在: ' + awardType);
		return;
	}

	page(url, title, 2022031000);
}

function applySurverAward(taskId, awardType, readonly) {
	let url = '';
	let title = '';
	if(awardType == 'design') {
		url = '/surverApply/toApplyDesign?taskId=' + taskId;
		// title = '石油工程建设优秀设计奖';
		title = '设计项目';
	}else if(awardType == 'software'){
		url = '/surverSoftwareApply/toApplySoftware?taskId=' + taskId + '&readonly=' + readonly;
		// title = '石油工程建设优秀勘察设计计算机软件奖';
		title = '计算机软件项目';
	}else if(awardType == 'consulting'){
		url = '/surverConsultingApply/toApply?taskId=' + taskId + '&readonly=' + readonly;
		title = '石油工程建设优秀咨询奖';
	}else if(awardType == 'standard'){
		url = '/surverStandardApply/toApply?taskId=' + taskId + '&readonly=' + readonly;
		// title = '石油工程建设优秀标准设计奖';
		title = '标准设计项目';
	}else if(awardType == 'excellent') {
		url = '/surverBaseExlentApply/toApply?taskId=' + taskId + '&readonly=' + readonly;
		// title = '石油工程建设优秀勘察奖';
		title = '勘察项目';
	}else {
		return;
	}
	page(url, title,2022032700);
}

//优质工程奖
function applyBestProAward(taskId, awardType) {
    let url = '';
	let title = '';
	if(awardType == 'bestPro') {
		url = '/petroleumEngineering/toQualityList?taskId=' + taskId;
		title = '石油优质工程奖';
	}else if(awardType == 'bestProGold'){
		url = '/petroleumEngineering/toQualityGoldList?taskId=' + taskId;
		title = '石油优质工程金奖';
	}else if(awardType == 'bestProInstall'){
		url = '/petroleumEngineering/toInstallList?taskId=' + taskId;
		title = '石油安装工程';
	}else {
		return;
	}
	page(url, title,2023020800);
}

function applyTechnologyAward(taskId, awardType) {
	let url = '';
	let title = '';
	if(awardType == 'science') {
		url = '/scienceProgressScience/toApplyPros?taskId=' + taskId;
		title = '科学技术奖成果';
	}else if(awardType == 'team'){
		url = '/enterprise_pro/to_apply_team_pros?taskId=' + taskId;
		title = '先进团队成果';
	}else if(awardType == 'personal'){
		url = '/sciencePersonal/toApplyPersonalPros?taskId=' + taskId;
		title = '先进个人成果';
	}else {
		return;
	}
	page(url, title,2022032700);
}

function applyExlentSurverAward(taskId, awardType) {
	let url = '';
	let title = '';
	if(awardType == 'excellent') {
		url = '/surverBaseExlentApply/toApply?taskId=' + taskId;
		// title = '石油工程建设优秀勘察奖';
		title = '勘察项目';
	}else {
		layer.msg('申报奖项类型不存在-'+ awardType);
		return;
	}
	page(url, title,2022032700);
}

function bestProList(taskId) {
    let url = '/petroleumEngineering/toProListMain?taskId=' + taskId;
	let title = '项目列表';
	page(url, title,2023020900);
}

function technologyProList(taskId) {
	let url = '/scienceProgressScience/toProListMain?taskId=' + taskId;
	let title = '项目列表';
	page(url, title,2022051400);

}


function gfProList(taskId) {
	let url = '/gfAward/toProListMain?taskId=' + taskId;
	let title = '项目列表';
	page(url, title,2022051400);
}



function QcProList(taskId) {
    let url = '/qcAward/toProListMain?taskId=' + taskId;
	let title = '项目列表';
	page(url, title,2023022800);
}

function surverProList(taskId, awardType) {
	let url = '/surverPro/toProListMain?taskId=' + taskId;
	let title = '项目列表';
	page(url, title,2022032800);

}

function uploadDoc(proId) {
	layer.open({
		type : 2,
		title : '上报资料',
		maxmin : true,
		shadeClose : false, // 点击遮罩关闭层
		area : [ '800px', '520px' ],
		content :  '/award_flow/to_upload_doc/' + proId // iframe的url
	});
}

function viewUploadDoc(proId) {
	layer.open({
		type : 2,
		title : '上报资料',
		maxmin : true,
		shadeClose : false, // 点击遮罩关闭层
		area : [ '800px', '520px' ],
		content :  '/award_flow/to_upload_doc/' + proId // iframe的url
	});
}

function resetPwd(id) {
}

/***
 * 分数查询
 * @param id
 */
function scoreQuery(id) {

}

/***
 * 项目汇总
 * @param id
 */
function projectSummary(id) {
}

/***
 *  专业组管理
 * @param id
 */
function professionalManage(id) {
    layer.open({
        type : 2,
        title : '专业组管理',
        maxmin : true,
        shadeClose : true, // 点击遮罩关闭层
        area : [ '800px', '520px' ],
        content : '/scienceProgressScience/toAssignExperts?taskId='+id // iframe的url
    });

}


function batchRemove() {
	var rows = $('#exampleTable').bootstrapTable('getSelections'); // 返回所有选择的行，当没有选择的记录时，返回一个空数组
	if (rows.length == 0) {
		layer.msg("请选择要删除的数据");
		return;
	}
	layer.confirm("确认要删除选中的'" + rows.length + "'条数据吗?", {
		btn : [ '确定', '取消' ]
	// 按钮
	}, function() {
		var ids = new Array();
		// 遍历所有选择的行数据，取每条数据对应的ID
		$.each(rows, function(i, row) {
			ids[i] = row['id'];
		});
		$.ajax({
			type : 'POST',
			data : {
				"ids" : ids
			},
			url : prefix + '/batchRemove',
			success : function(r) {
				if (r.code == 0) {
					layer.msg(r.msg);
					reLoad();
				} else {
					layer.msg(r.msg);
				}
			}
		});
	}, function() {});
}

function uplaodFile(taskId){
	page('/specialistDoc/view_doc?taskId=' + taskId, '上传资料', 2023082500);
}

// AJAX 数据请求
function viewTaskScoresByTaskId(taskId) {
    $.ajax({
        type: "GET",
        url: "/qcScore/getTaskScores",
        data: { taskId: taskId },
        success: function(data) {
            if (data.code == 0) {
                _awardShowTaskScoresModal(data.data, taskId);
            } else {
                layer.msg(data.msg || '查询失败', {icon: 2});
            }
        },
        error: function() {
            layer.msg('查询失败，请稍后重试', {icon: 2});
        }
    });
}
// 任务打分情况的总览弹窗
function _awardShowTaskScoresModal(projects, taskId) {
    if (!projects || projects.length == 0) {
        layer.msg('该任务暂无打分记录', {icon: 0});
        return;
    }
    var html = '<div style="padding:10px;">';
    html += '<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:15px;">';
    html += '<h4 style="margin:0;">任务打分情况总览</h4>';
    html += '<button class="btn btn-primary btn-sm" onclick="exportTaskScoreMatrix(\'' + taskId + '\')">导出Excel</button>';
    html += '</div>';
    html += '<table class="table table-bordered table-striped" style="margin-bottom:0;">';
    html += '<thead><tr>';
    html += '<th style="width:50px;">序号</th>';
    html += '<th style="width:130px;">分派专业组</th>';
    html += '<th style="width:110px;">申报账号</th>';
    html += '<th>课题名称</th>';
    html += '<th style="width:130px;">小组名称</th>';
    html += '<th style="width:80px;">课题类型</th>';
    html += '<th style="width:70px;">打分人数</th>';
    html += '<th style="width:70px;">平均分</th>';
    html += '<th style="width:70px;">操作</th>';
    html += '</tr></thead><tbody>';

    for (var i = 0; i < projects.length; i++) {
        var item = projects[i];
        var escapedName = (item.topicName || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '&quot;');
        var scoreCountHtml = item.scorerCount > 0
            ? '<span style="color:#337ab7;font-weight:bold;">' + item.scorerCount + '人</span>'
            : '<span style="color:#999;">未打分</span>';
        var avgScoreHtml = item.avgScore != null
            ? '<span style="color:#5cb85c;font-weight:bold;">' + item.avgScore + '</span>'
            : '<span style="color:#999;">-</span>';
        html += '<tr>';
        html += '<td>' + (i + 1) + '</td>';
        html += '<td>' + _awardEscapeHtml(item.qcGroupName || '-') + '</td>';
        html += '<td>' + _awardEscapeHtml(item.proCode || '-') + '</td>';
        html += '<td>' + _awardEscapeHtml(item.topicName || '-') + '</td>';
        html += '<td>' + _awardEscapeHtml(item.groupName || '-') + '</td>';
        html += '<td>' + _awardEscapeHtml(item.topicType || '-') + '</td>';
        html += '<td>' + scoreCountHtml + '</td>';
        html += '<td>' + avgScoreHtml + '</td>';
        html += '<td><button class="btn btn-primary btn-xs" onclick="_awardViewProjectScoreDetail(' + item.proId + ', \'' + escapedName + '\', \'' + taskId + '\')">详情</button></td>';

        html += '</tr>';

    }
    html += '</tbody></table>';
    html += '</div>';
    layer.open({
        type: 1,
        title: '任务打分情况',
        area: ['1120px', '650px'],
        content: html,
        btn: ['关闭']
    });
}

function _awardViewProjectScoreDetail(proId, topicName, taskId) {
    $.ajax({
        type: "GET",
        url: "/qcScore/getProjectScoreDetail",
        data: { taskId: taskId, proId: proId },
        success: function(data) {
            if (data.code == 0) {
                _awardShowProjectScoreDetailModal(data.data, topicName);
            } else {
                layer.msg(data.msg || '查询失败', {icon: 2});
            }
        },
        error: function() {
            layer.msg('查询失败，请稍后重试', {icon: 2});
        }
    });
}

function _awardShowProjectScoreDetailModal(scores, topicName) {
    if (!scores || scores.length == 0) {
        layer.msg('该课题暂无打分记录', {icon: 0});
        return;
    }
    var html = '<div style="padding:10px;">';
    html += '<h4 style="margin-bottom:15px;">课题：' + _awardEscapeHtml(topicName) + ' 的打分详情</h4>';
    html += '<table class="table table-bordered table-striped" style="margin-bottom:0;">';
    html += '<thead><tr>';
    html += '<th style="width:60px;">序号</th>';
    html += '<th>专家姓名</th>';
    html += '<th style="width:100px;">评分</th>';
    html += '<th style="width:150px;">打分时间</th>';
    html += '<th style="width:100px;">是否回避</th>';
    html += '</tr></thead><tbody>';
    for (var i = 0; i < scores.length; i++) {
        var item = scores[i];
        var isAvoided = item.isAvoided ? '是' : '否';
        var avoidedStyle = item.isAvoided ? 'color:#d9534f;' : '';
        html += '<tr>';
        html += '<td>' + (i + 1) + '</td>';
        html += '<td>' + _awardEscapeHtml(item.expertName || '-') + '</td>';
        html += '<td style="font-weight:bold;color:#5cb85c;">' + (item.score != null ? item.score : '-') + '</td>';
        html += '<td>' + (item.scoreTime || '-') + '</td>';
        html += '<td style="' + avoidedStyle + '">' + isAvoided + '</td>';
        html += '</tr>';
    }
    html += '</tbody></table>';
    html += '</div>';
    layer.open({
        type: 1,
        title: '课题打分详情',
        area: ['700px', '500px'],
        content: html
    });
}

function exportTaskScoreMatrix(taskId) {
    layer.msg('正在加载评分数据...', {icon: 16, shade: 0.3, time: 0});
    $.ajax({
        type: 'GET',
        url: '/qcScore/getTaskScoreMatrix',
        data: { taskId: taskId },
        success: function(data) {
            layer.closeAll('msg');
            if (data.code == 0) {
                _buildAndDownloadScoreMatrix(data.data);
            } else {
                layer.msg(data.msg || '查询失败', {icon: 2});
            }
        },
        error: function() {
            layer.closeAll('msg');
            layer.msg('查询失败，请稍后重试', {icon: 2});
        }
    });
}

function _buildAndDownloadScoreMatrix(matrixData) {
    var doExport = function() {
        var experts = matrixData.experts || [];
        var projects = matrixData.projects || [];
        var nFixed = 6; // 序号、分派专业组、申报账号、课题名称、小组名称、分类
        var nExperts = experts.length;
        var nTotal = nFixed + nExperts + 1 + 3; // +1资料分 +3灰色列

        var aoa = [];

        // 第0行：标题（整行合并）
        var titleRow = new Array(nTotal).fill('');
        titleRow[0] = '石油工程建设优秀质量管理小组活动成果资料评分表';
        aoa.push(titleRow);

        // 第1行：主列头
        var h1 = new Array(nTotal).fill('');
        h1[0] = '序号'; h1[1] = '分派专业组'; h1[2] = '申报账号'; h1[3] = '课题名称'; h1[4] = '小组名称'; h1[5] = '分类';
        h1[nFixed] = '专家打分';
        h1[nFixed + nExperts] = '资料分';
        h1[nFixed + nExperts + 1] = '完成名称';
        h1[nFixed + nExperts + 2] = '申报单位';
        h1[nFixed + nExperts + 3] = '小组成员';
        aoa.push(h1);

        // 第2行：专家子列头 + 资料分备注
        var h2 = new Array(nTotal).fill('');
        for (var i = 0; i < experts.length; i++) {
            h2[nFixed + i] = experts[i].loginAccount || '';
        }
        h2[nFixed + nExperts] = '小数点后两位';
        aoa.push(h2);

        // 数据行
        for (var j = 0; j < projects.length; j++) {
            var pro = projects[j];
            var row = new Array(nTotal).fill('');
            row[0] = j + 1;
            row[1] = pro.qcGroupName || '';
            row[2] = pro.proCode || '';
            row[3] = pro.topicName || '';
            row[4] = pro.groupName || '';
            row[5] = pro.topicType || '';
            for (var k = 0; k < experts.length; k++) {
                var acc = experts[k].loginAccount;
                var score = pro.expertScores && pro.expertScores[acc];
                row[nFixed + k] = (score != null && score !== '') ? score : '';
            }
            aoa.push(row);
        }

        var ws = XLSX.utils.aoa_to_sheet(aoa);

        // 合并单元格
        var merges = [
            {s:{r:0,c:0}, e:{r:0,c:nTotal-1}},      // 标题行
            {s:{r:1,c:0}, e:{r:2,c:0}},               // 序号
            {s:{r:1,c:1}, e:{r:2,c:1}},               // 分派专业组
            {s:{r:1,c:2}, e:{r:2,c:2}},               // 申报账号
            {s:{r:1,c:3}, e:{r:2,c:3}},               // 课题名称
            {s:{r:1,c:4}, e:{r:2,c:4}},               // 小组名称
            {s:{r:1,c:5}, e:{r:2,c:5}},               // 分类
            {s:{r:1,c:nFixed+nExperts},   e:{r:2,c:nFixed+nExperts}},    // 资料分
            {s:{r:1,c:nFixed+nExperts+1}, e:{r:2,c:nFixed+nExperts+1}},  // 完成名称
            {s:{r:1,c:nFixed+nExperts+2}, e:{r:2,c:nFixed+nExperts+2}},  // 申报单位
            {s:{r:1,c:nFixed+nExperts+3}, e:{r:2,c:nFixed+nExperts+3}}   // 小组成员
        ];
        if (nExperts > 1) {
            merges.push({s:{r:1,c:nFixed}, e:{r:1,c:nFixed+nExperts-1}}); // 专家打分跨列
        }
        ws['!merges'] = merges;

        // 列宽
        var cols = [{wch:6},{wch:14},{wch:14},{wch:28},{wch:16},{wch:10}];
        for (var m = 0; m < nExperts; m++) cols.push({wch:9});
        cols.push({wch:12},{wch:18},{wch:18},{wch:22});
        ws['!cols'] = cols;

        // 行高
        ws['!rows'] = [{hpt:24},{hpt:30},{hpt:22}];

        var wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '评分汇总');
        XLSX.writeFile(wb, '评分汇总表.xlsx');
    };

    if (typeof XLSX !== 'undefined') {
        doExport();
    } else {
        var s = document.createElement('script');
        s.src = 'https://cdn.sheetjs.com/xlsx-0.20.3/package/dist/xlsx.full.min.js';
        s.onload = function() { doExport(); };
        s.onerror = function() { layer.msg('Excel库加载失败，请检查网络', {icon: 2}); };
        document.head.appendChild(s);
    }
}

/**
 * 防止XSS（跨站脚本攻击）和确保数据正确显示
 * 将用户输入的特殊字符转义
 */
function _awardEscapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;')
		.replace(/'/g, '&#039;');
}