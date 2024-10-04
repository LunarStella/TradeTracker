package com.example.tradetracker.dto;

import java.util.List;

public record PagingResponseDto<T>(
        int currentPage,           // 현재 페이지 번호
        boolean hasNextPage,        // 다음 페이지 존재 여부
        int totalPages,              // 전체 페이지 수
        List<T> data             // 현재 페이지의 데이터 리스트
) {}