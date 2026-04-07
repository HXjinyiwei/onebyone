/**
 * 统一分页工具库
 * 为管理后台提供统一的分页功能
 */

// 分页配置
const PAGINATION_CONFIG = {
    defaultPageSize: 10,
    pageSizeOptions: [10, 20, 50, 100],
    maxPageButtons: 7
};

// 存储分页回调函数的全局对象
const paginationCallbacks = {};

/**
 * 创建分页HTML
 * @param {Object} paginationData - 分页数据
 * @param {string} containerId - 分页容器ID
 * @param {Function} onPageChange - 页码改变回调函数
 */
function createPaginationHTML(paginationData, containerId, onPageChange) {
    const {
        currentPage,
        pageSize,
        totalRecords,
        totalPages,
        hasNext,
        hasPrevious
    } = paginationData;
    
    const container = document.getElementById(containerId);
    if (!container) {
        console.error(`分页容器 ${containerId} 不存在`);
        return;
    }
    
    // 存储回调函数到全局对象中
    paginationCallbacks[containerId] = onPageChange;
    
    // 清空容器
    container.innerHTML = '';
    
    // 如果没有数据或只有一页，不显示分页
    if (totalRecords === 0 || totalPages <= 1) {
        return;
    }
    
    // 创建分页HTML
    let html = `
        <div class="layui-box layui-laypage layui-laypage-default">
            <span class="layui-laypage-total">共 ${totalRecords} 条记录</span>
    `;
    
    // 上一页按钮
    if (hasPrevious) {
        html += `<a href="javascript:;" class="layui-laypage-prev" onclick="paginationUtils.goToPage(${currentPage - 1}, '${containerId}')">上一页</a>`;
    } else {
        html += `<span class="layui-laypage-prev layui-disabled">上一页</span>`;
    }
    
    // 页码按钮
    const pageButtons = generatePageButtons(currentPage, totalPages);
    pageButtons.forEach(page => {
        if (page === currentPage) {
            html += `<span class="layui-laypage-curr"><em class="layui-laypage-em"></em><em>${page}</em></span>`;
        } else if (page === '...') {
            html += `<span class="layui-laypage-spr">…</span>`;
        } else {
            html += `<a href="javascript:;" onclick="paginationUtils.goToPage(${page}, '${containerId}')">${page}</a>`;
        }
    });
    
    // 下一页按钮
    if (hasNext) {
        html += `<a href="javascript:;" class="layui-laypage-next" onclick="paginationUtils.goToPage(${currentPage + 1}, '${containerId}')">下一页</a>`;
    } else {
        html += `<span class="layui-laypage-next layui-disabled">下一页</span>`;
    }
    
    // 每页大小选择
    html += `
        <span class="layui-laypage-skip">
            每页显示：
            <select class="layui-select" onchange="paginationUtils.changePageSize(this.value, '${containerId}')">
    `;
    
    PAGINATION_CONFIG.pageSizeOptions.forEach(size => {
        const selected = size === pageSize ? 'selected' : '';
        html += `<option value="${size}" ${selected}>${size}</option>`;
    });
    
    html += `
            </select>
        </span>
        <span class="layui-laypage-skip">
            跳转到：
            <input type="number" class="layui-input" min="1" max="${totalPages}" value="${currentPage}" style="width: 60px;">
            <button class="layui-btn layui-btn-sm" onclick="paginationUtils.jumpToPage(this.previousElementSibling.value, '${containerId}')">跳转</button>
        </span>
    </div>
    `;
    
    container.innerHTML = html;
    
    // 渲染Layui组件
    if (window.layui && layui.form) {
        layui.form.render('select');
    }
}

/**
 * 生成页码按钮数组
 * @param {number} currentPage - 当前页码
 * @param {number} totalPages - 总页数
 * @returns {Array} 页码按钮数组
 */
function generatePageButtons(currentPage, totalPages) {
    const maxButtons = PAGINATION_CONFIG.maxPageButtons;
    const buttons = [];
    
    if (totalPages <= maxButtons) {
        // 总页数小于等于最大按钮数，显示所有页码
        for (let i = 1; i <= totalPages; i++) {
            buttons.push(i);
        }
    } else {
        // 总页数大于最大按钮数，显示部分页码
        const half = Math.floor(maxButtons / 2);
        
        if (currentPage <= half + 1) {
            // 当前页在前半部分
            for (let i = 1; i <= maxButtons - 2; i++) {
                buttons.push(i);
            }
            buttons.push('...');
            buttons.push(totalPages);
        } else if (currentPage >= totalPages - half) {
            // 当前页在后半部分
            buttons.push(1);
            buttons.push('...');
            for (let i = totalPages - maxButtons + 3; i <= totalPages; i++) {
                buttons.push(i);
            }
        } else {
            // 当前页在中间
            buttons.push(1);
            buttons.push('...');
            const start = currentPage - Math.floor((maxButtons - 4) / 2);
            const end = currentPage + Math.floor((maxButtons - 4) / 2);
            for (let i = start; i <= end; i++) {
                buttons.push(i);
            }
            buttons.push('...');
            buttons.push(totalPages);
        }
    }
    
    return buttons;
}

/**
 * 跳转到指定页码
 * @param {number} page - 目标页码
 * @param {string} containerId - 分页容器ID
 */
function goToPage(page, containerId) {
    const onPageChange = paginationCallbacks[containerId];
    if (typeof onPageChange === 'function') {
        onPageChange(page);
    }
}

/**
 * 改变每页大小
 * @param {number} pageSize - 每页大小
 * @param {string} containerId - 分页容器ID
 */
function changePageSize(pageSize, containerId) {
    const onPageChange = paginationCallbacks[containerId];
    if (typeof onPageChange === 'function') {
        onPageChange(1, parseInt(pageSize));
    }
}

/**
 * 跳转到指定页码（通过输入框）
 * @param {string} page - 目标页码
 * @param {string} containerId - 分页容器ID
 */
function jumpToPage(page, containerId) {
    const pageNum = parseInt(page);
    if (isNaN(pageNum) || pageNum < 1) {
        layer.msg('请输入有效的页码', {icon: 2});
        return;
    }
    
    const onPageChange = paginationCallbacks[containerId];
    if (typeof onPageChange === 'function') {
        onPageChange(pageNum);
    }
}

/**
 * 创建分页容器
 * @param {string} containerId - 分页容器ID
 * @returns {string} 分页容器HTML
 */
function createPaginationContainer(containerId) {
    return `<div id="${containerId}" class="pagination-container" style="margin-top: 20px; text-align: center;"></div>`;
}

/**
 * 初始化分页
 * @param {Object} options - 分页选项
 * @param {string} options.containerId - 分页容器ID
 * @param {Function} options.onPageChange - 页码改变回调函数
 * @param {number} options.currentPage - 当前页码（默认1）
 * @param {number} options.pageSize - 每页大小（默认10）
 * @param {number} options.totalRecords - 总记录数（默认0）
 */
function initPagination(options) {
    const {
        containerId,
        onPageChange,
        currentPage = 1,
        pageSize = PAGINATION_CONFIG.defaultPageSize,
        totalRecords = 0
    } = options;
    
    const totalPages = Math.ceil(totalRecords / pageSize);
    const paginationData = {
        currentPage,
        pageSize,
        totalRecords,
        totalPages,
        hasNext: currentPage < totalPages,
        hasPrevious: currentPage > 1
    };
    
    createPaginationHTML(paginationData, containerId, onPageChange);
}

/**
 * 更新分页数据
 * @param {Object} paginationData - 分页数据
 * @param {string} containerId - 分页容器ID
 * @param {Function} onPageChange - 页码改变回调函数
 */
function updatePagination(paginationData, containerId, onPageChange) {
    createPaginationHTML(paginationData, containerId, onPageChange);
}

/**
 * 获取分页参数
 * @param {number} currentPage - 当前页码
 * @param {number} pageSize - 每页大小
 * @returns {Object} 分页参数对象
 */
function getPaginationParams(currentPage = 1, pageSize = PAGINATION_CONFIG.defaultPageSize) {
    return {
        page: currentPage,
        pageSize: pageSize
    };
}

/**
 * 处理分页响应数据
 * @param {Object} response - API响应数据
 * @returns {Object} 分页数据对象
 */
function handlePaginationResponse(response) {
    if (!response || !response.data) {
        return {
            data: [],
            currentPage: 1,
            pageSize: PAGINATION_CONFIG.defaultPageSize,
            totalRecords: 0,
            totalPages: 0,
            hasNext: false,
            hasPrevious: false
        };
    }
    
    const { data, currentPage, pageSize, totalRecords, totalPages, hasNext, hasPrevious } = response.data;
    
    return {
        data: data || [],
        currentPage: currentPage || 1,
        pageSize: pageSize || PAGINATION_CONFIG.defaultPageSize,
        totalRecords: totalRecords || 0,
        totalPages: totalPages || 0,
        hasNext: hasNext || false,
        hasPrevious: hasPrevious || false
    };
}

// 导出工具函数
const paginationUtils = {
    createPaginationHTML,
    generatePageButtons,
    goToPage,
    changePageSize,
    jumpToPage,
    createPaginationContainer,
    initPagination,
    updatePagination,
    getPaginationParams,
    handlePaginationResponse,
    PAGINATION_CONFIG
};

// 全局注册
if (typeof window !== 'undefined') {
    window.paginationUtils = paginationUtils;
}
