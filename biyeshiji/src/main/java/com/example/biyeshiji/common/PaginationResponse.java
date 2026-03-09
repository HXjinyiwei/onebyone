package com.example.biyeshiji.common;

import lombok.Data;
import java.util.List;

/**
 * 分页响应对象
 * @param <T> 数据类型
 */
@Data
public class PaginationResponse<T> {
    private List<T> data;          // 当前页数据
    private int currentPage;       // 当前页码
    private int pageSize;          // 每页大小
    private long totalRecords;     // 总记录数
    private int totalPages;        // 总页数
    private boolean hasNext;       // 是否有下一页
    private boolean hasPrevious;   // 是否有上一页

    public PaginationResponse() {
    }

    public PaginationResponse(List<T> data, int currentPage, int pageSize, long totalRecords) {
        this.data = data;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalRecords = totalRecords;
        this.totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        this.hasNext = currentPage < totalPages;
        this.hasPrevious = currentPage > 1;
    }

    /**
     * 创建分页响应
     * @param data 当前页数据
     * @param currentPage 当前页码
     * @param pageSize 每页大小
     * @param totalRecords 总记录数
     * @return 分页响应对象
     */
    public static <T> PaginationResponse<T> of(List<T> data, int currentPage, int pageSize, long totalRecords) {
        return new PaginationResponse<>(data, currentPage, pageSize, totalRecords);
    }
}