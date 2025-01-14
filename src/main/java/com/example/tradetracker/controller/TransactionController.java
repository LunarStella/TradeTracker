package com.example.tradetracker.controller;

import com.example.tradetracker.dto.PagingResponseDto;
import com.example.tradetracker.dto.TransactionRequestDto;
import com.example.tradetracker.dto.TransactionResponseDto;
import com.example.tradetracker.response.ApiResponse;
import com.example.tradetracker.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// 내가 사고 판 주식들을 관리 api
@Slf4j
@CrossOrigin
@AllArgsConstructor
@RestController
@RequestMapping("/api/transaction/")
public class TransactionController {

    private final TransactionService TransactionService;

    // 내가 주식을 산 것을 기록
    @PostMapping("us/buy")
    public ApiResponse<TransactionResponseDto> recordPurchase(@RequestBody TransactionRequestDto request){
        log.info("주식 매수 기록 생성");

        // **해당 값이 제대로 들어왔는지 filter 기능 만들기!!

        TransactionResponseDto result = null;

        // 서비스 층 구매 처리
        // 미국 주식을 구매
        result = this.TransactionService.createUsStockPurchaseTransaction(request);


        log.info("Transaction result: {}", result);

        // 잘못된 형식으로 형태로 오면 오류 표시?!

        return ApiResponse.ok(result);
    }

    @DeleteMapping("us/buy/{transactionId}")
    public ApiResponse<?> deletePurchase(@PathVariable Long transactionId) {
        log.info("주식 매수 기록 삭제");

        this.TransactionService.deleteUsStockPurchaseTransaction(transactionId);

        log.info("Transaction deleted for transactionId: {}", transactionId);

        return ApiResponse.ok(null);  // 삭제 성공 시 본문 없이 응답
    }

    @PostMapping("us/sell")
    public ApiResponse<TransactionResponseDto> recordSell(@RequestBody TransactionRequestDto request){
        log.info("주식 매도 기록 생성");

        // **해당 값이 제대로 들어왔는지 filter 기능 만들기!!

        TransactionResponseDto result = null;

        // 서비스 층 구매 처리
        // 미국 주식을 구매
        result = this.TransactionService.createUsStockSellTransaction(request);


        log.info("Transaction result: {}", result);

        // 잘못된 형식으로 형태로 오면 오류 표시?!

        return ApiResponse.ok(result);
    }

    @DeleteMapping("us/sell/{transactionId}")
    public ApiResponse<?> deleteSell(@PathVariable Long transactionId) {
        log.info("주식 매도 기록 삭제");

        this.TransactionService.deleteUsStockSellTransaction(transactionId);

        log.info("Transaction deleted for transactionId: {}", transactionId);

        return ApiResponse.ok(null);
    }

//     날짜 순으로 매수 매도 기록 조회 API
    @GetMapping("date")
    public ApiResponse<PagingResponseDto> getTransactionsSortedByDate(
            @RequestParam String marketType,
            @RequestParam int page) {

        log.info("거래 기록 보기");
        // 날짜 순으로 매수 매도 기록 조회 로직
        PagingResponseDto<TransactionResponseDto> result = this.TransactionService.getTransactionsSortedByDate(page, marketType);

        return ApiResponse.ok(result);
    }

}
