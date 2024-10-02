package com.example.tradetracker.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Test Error
    TEST_ERROR(10000, HttpStatus.BAD_REQUEST, "테스트 에러입니다."),

    //400
    // Insufficient Seed Money Error
    INSUFFICIENT_SEED_MONEY(40001, HttpStatus.BAD_REQUEST, "시드머니가 부족합니다."), // 추가된 에러
    // Investment Amount Exceeds Seed Money Error
    INVESTMENT_AMOUNT_EXCEEDS_SEED_MONEY(40002, HttpStatus.BAD_REQUEST, "투자 금액이 시드머니를 초과할 수 없습니다."),
    // Insufficient Stock Quantity Error
    INSUFFICIENT_STOCK_QUANTITY(40003, HttpStatus.BAD_REQUEST, "포트폴리오에 주식이 없거나 매도 수량이 부족합니다."),
    // Excess Sell Quantity Error
    EXCESS_SELL_QUANTITY(40004, HttpStatus.BAD_REQUEST, "매도 수량이 포트폴리오 수량을 초과했습니다."),


    // 404 Not Found
    NOT_FOUND_END_POINT(40400, HttpStatus.NOT_FOUND, "존재하지 않는 API입니다."),
    // User ID Not Found Error
    ID_NOT_FOUND_USER(40401, HttpStatus.NOT_FOUND, "해당 유저 ID를 찾을 수 없습니다."),
    // Transaction ID Not Found Error
    ID_NOT_FOUND_TRANSACTION(40402, HttpStatus.NOT_FOUND, "해당 Transaction ID를 찾을 수 없습니다."),
    // Portfolio ID Not Found Error
    ID_NOT_FOUND_PORTFOLIO(40403, HttpStatus.NOT_FOUND, "해당 Portfolio ID를 찾을 수 없습니다."),
    // Stock Profit/Loss Not Found
    ID_NOT_FOUND_PROFIT_LOSS(40405, HttpStatus.NOT_FOUND, "해당 손익 기록을 찾을 수 없습니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");

    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;
}
