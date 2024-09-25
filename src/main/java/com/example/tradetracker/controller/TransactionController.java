package com.example.tradetracker.controller;

import com.example.tradetracker.dto.TransactionRequestDto;
import com.example.tradetracker.dto.TransactionResponseDto;
import com.example.tradetracker.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// 내가 사고 판 주식들을 관리 api
@Slf4j
@CrossOrigin
@AllArgsConstructor
@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    private final TransactionService TransactionService;

    // 내가 주식을 산 것을 기록
    @PostMapping("/buy")
    public ResponseEntity<TransactionResponseDto> recordPurchase(@RequestBody TransactionRequestDto request){
        log.info("주식 매수 기록 생성");

        // **해당 값이 제대로 들어왔는지 filter 기능 만들기!!

        TransactionResponseDto result = null;

        // 서비스 층 구매 처리
        // 미국 주식을 구매
        if("US".equals(request.getMarketType())){
          result = this.TransactionService.createUsStockPurchaseTransaction(request);
        }


        log.info("Transaction result: {}", result);

        // 잘못된 형식으로 형태로 오면 오류 표시?!

        return ResponseEntity.ok(result);
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionResponseDto> recordSell(@RequestBody TransactionRequestDto request){
        log.info("주식 매도 기록 생성");

        // **해당 값이 제대로 들어왔는지 filter 기능 만들기!!

        TransactionResponseDto result = null;

        // 서비스 층 구매 처리
        // 미국 주식을 구매
        if("US".equals(request.getMarketType())){
            result = this.TransactionService.createUsStockSellTransaction(request);
        }


        log.info("Transaction result: {}", result);

        // 잘못된 형식으로 형태로 오면 오류 표시?!

        return ResponseEntity.ok(result);
    }
}
