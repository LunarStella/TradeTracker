package com.example.tradetracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


public record TransactionResponseDto(
        Long transactionId,     // 트랜잭션 ID
        String marketType,      // 한국 or 미국 주식인지 (KR or US)
        String transactionType,  // 매수 or 매도 ('BUY', 'SELL')
        String stockName,       // 주식 종류
        BigDecimal price,       // 주식 가격
        int quantity,           // 주식 개수
        BigDecimal totalExecutedAmount,
        BigDecimal commissionValue, // 수수료 총 액수
        LocalDate date          // 날짜
) {
}
