package com.example.tradetracker.dto;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// @Data를 사용하면 getter, setter
// @NoArgsContructor을 사용하면 변수를 생성하지 않아도 된다
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto {
    private String marketType; // 한국 or 미국 주식인지 (KR or US)
    private String transactionType; // 매수 or 매도 ('BUY', 'SELL')
    private String stockName; // 주식 종류
    private BigDecimal price; // 주식 가격
    private int quantity; // 주식 개수
    private BigDecimal commissionRate; // 수수료 비율
    private LocalDate date; // 날짜
}
