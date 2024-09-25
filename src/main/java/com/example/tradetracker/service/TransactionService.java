package com.example.tradetracker.service;


import com.example.tradetracker.dto.TransactionRequestDto;
import com.example.tradetracker.dto.TransactionResponseDto;
import com.example.tradetracker.model.PortfolioUsdEntity;
import com.example.tradetracker.model.StockProfitUsdEntity;
import com.example.tradetracker.model.TransactionUsdEntity;
import com.example.tradetracker.model.UserEntity;
import com.example.tradetracker.repository.PortfolioUsdRepository;
import com.example.tradetracker.repository.StockProfitUsdRepository;
import com.example.tradetracker.repository.TransactionUsdRepository;
import com.example.tradetracker.repository.UserRepository;
import com.example.tradetracker.util.TransactionCalculator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.CacheRequest;
import java.time.LocalDateTime;

@Service
@Transactional
@AllArgsConstructor
public class TransactionService {
    private final UserRepository userRepository;
    private final TransactionUsdRepository transactionUsdRepository;
    private final PortfolioUsdRepository portfolioUsdRepository;
    private final StockProfitUsdRepository stockProfitUsdRepository;

    // 주식 매수
    public TransactionResponseDto createUsStockPurchaseTransaction (TransactionRequestDto request) {
        Long userId = 1L; // 임의의 사용자 ID 설정
        TransactionCalculator calculator = new TransactionCalculator();
//        BigDecimal totalExecutedAmount;  // 총 매수 금액
//        BigDecimal transactionFee;       // 수수료
//        BigDecimal updatedSeedMoney;     // 업데이트된 시드머니

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 주식 총 매수 금액 계산 (가격 * 수량)
        calculator.calculateTotalExecutedAmount(request.getPrice(),request.getQuantity());
//        totalExecutedAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 수수료 계산 (매수 금액 * 수수료율)
        calculator.calculateTransactionFee(request.getCommissionRate());
//        transactionFee = totalExecutedAmount.multiply(request.getCommissionRate());

        // 시드머니에서 수수료 차감
        calculator.calculateUpdatedSeedMoney(user.getSeedMoneyUsd(), request.getTransactionType());
        calculator.calculateUpdatedInvestAmount(user.getInvestmentAmountUsd(), request.getTransactionType());
//        updatedSeedMoney = user.getSeedMoneyUsd().subtract(transactionFee);
        if (calculator.getUpdatedSeedMoney().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("시드머니가 부족합니다.");
        }

        // 투자 금액이 시드머니를 넘지 않도록 검증
        if (user.getInvestmentAmountUsd().compareTo(user.getSeedMoneyUsd()) > 0) {
            throw new RuntimeException("투자 금액이 시드머니를 초과할 수 없습니다.");
        }

        // 유저의 시드머니 및 투자 금액 업데이트
        user = user
                .withSeedMoneyUsd(calculator.getUpdatedSeedMoney())
                .withInvestmentAmountUsd(calculator.getUpdatedInvestAmount());
//        user = user.withSeedMoneyUsd(updatedSeedMoney)
//                .withInvestmentAmountUsd(user.getInvestmentAmountUsd().add(totalExecutedAmount));

        userRepository.save(user);


        // 주식 거래 정보 생성 및 저장
        TransactionUsdEntity transaction = TransactionUsdEntity.builder()
                .user(user)
                .transactionType(TransactionUsdEntity.TransactionType.valueOf(request.getTransactionType()))
                .stockName(request.getStockName())
                .transactionPrice(request.getPrice())
                .quantity(request.getQuantity())
                .transactionDate(request.getDate())
                .transactionFee(calculator.getTransactionFee())
                .totalAmount(calculator.getTotalExecutedAmount())
                .build();
//        TransactionUsdEntity transaction = TransactionUsdEntity.builder()
//                .user(user)
//                .transactionType(TransactionUsdEntity.TransactionType.valueOf(request.getTransactionType()))
//                .stockName(request.getStockName())
//                .transactionPrice(request.getPrice())
//                .quantity(request.getQuantity())
//                .transactionDate(LocalDateTime.now())
//                .transactionFee(transactionFee)
//                .totalAmount(totalExecutedAmount)
//                .build();

        transactionUsdRepository.save(transaction);

        // 포토폴리오에서 주식 존재 여부 확인
        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, request.getStockName());

        // 포토폴리오에 새로 산 주식 추가
        if (existingPortfolio != null) {
            // 존재 O: 평균 단가 및 수량 업데이트
            calculator.calculatePortfolioPurchaseTotalAmount(existingPortfolio.getTotalAmount());
            calculator.calculatePortfolioPurchaseTotalQuantity(existingPortfolio.getQuantity(), request.getQuantity());
            calculator.calculatePortfolioPurchaseAveragePrice();
//            BigDecimal totalAmount = existingPortfolio.getTotalAmount().add(calculator.getTotalExecutedAmount());
//            BigDecimal totalQuantity = BigDecimal.valueOf(existingPortfolio.getQuantity()).add(BigDecimal.valueOf(request.getQuantity()));
//            BigDecimal newAveragePrice = totalAmount.divide(totalQuantity, RoundingMode.HALF_UP);

            existingPortfolio = existingPortfolio
                    .withQuantity(calculator.getPortfolioPurchaseTotalQuantity())
                    .withTotalAmount(calculator.getPortfolioPurchaseTotalAmount())
                    .withPrice(calculator.getPortfolioPurchaseAveragePrice());

            portfolioUsdRepository.save(existingPortfolio);
        } else {
            // 존재 X: 새로운 포트폴리오 항목 생성
            PortfolioUsdEntity newPortfolio = PortfolioUsdEntity.builder()
                    .user(user)
                    .stockName(request.getStockName())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .totalAmount(calculator.getTotalExecutedAmount())
                    .build();

            portfolioUsdRepository.save(newPortfolio);
        }

        // 응답 DTO 생성 및 반환
        return TransactionResponseDto.builder()
                .transactionId(transaction.getId())
                .marketType(request.getMarketType())
                .transactionType(request.getTransactionType())
                .stockName(request.getStockName())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .totalExecutedAmount(calculator.getTotalExecutedAmount())
                .commissionValue(calculator.getTransactionFee())
                .date(request.getDate())
                .build();
    }

    public TransactionResponseDto createUsStockSellTransaction (TransactionRequestDto request) {
        Long userId = 1L; // 임의의 사용자 ID 설정
        TransactionCalculator calculator = new TransactionCalculator();

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 주식 총 매도 금액 계산 (가격 * 수량)
        calculator.calculateTotalExecutedAmount(request.getPrice(),request.getQuantity());
        // 수수료 계산 (매도 금액 * 수수료율)
        calculator.calculateTransactionFee(request.getCommissionRate());

        // 포토폴리오
        // 포트폴리오에서 해당 주식 정보 가져오기
        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, request.getStockName());

        // 포트폴리오에 해당 주식 없거나 수량이 적으면 오류
        if (existingPortfolio == null || existingPortfolio.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("포트폴리오에 주식이 없거나 매도 수량이 부족합니다.");
        }

        // 매도 후 포토폴리오 구하기
        calculator.calculatePortfolioSellTotalQuantity(existingPortfolio.getQuantity(), request.getQuantity());
        calculator.calculatePortfolioSellTotalAmount(existingPortfolio.getPrice());

        // 손익 계산하기
        // 주식 1개 손익 차이 금액
        calculator.calculateProfitOrLossPerStock(request.getPrice(), existingPortfolio.getPrice());
        // 총 손익 차이 금액
        calculator.calculateProfitOrLossTotalAmount(request.getQuantity());
        // 주가 매도 매수 차이 비율, 수수료 포함 차이 금액, 수익 여부 계산
        calculator.calculateProfitOrLossDetails(existingPortfolio.getPrice());


        // 포트폴리오 엔티티 업데이트
        if (calculator.getPortfolioSellTotalQuantity() > 0) {
            // 수량이 0보다 크면 업데이트
            existingPortfolio = existingPortfolio
                    .withQuantity(calculator.getPortfolioSellTotalQuantity())
                    .withTotalAmount(calculator.getPortfolioSellTotalAmount());
            portfolioUsdRepository.save(existingPortfolio);
        } else {
            // 수량이 0이면 해당 포트폴리오 삭제
            portfolioUsdRepository.delete(existingPortfolio);
        }


        // 주식 거래 정보 생성 및 저장
        TransactionUsdEntity transaction = TransactionUsdEntity.builder()
                .user(user)
                .transactionType(TransactionUsdEntity.TransactionType.valueOf(request.getTransactionType()))
                .stockName(request.getStockName())
                .transactionPrice(request.getPrice())
                .quantity(request.getQuantity())
                .transactionDate(request.getDate())
                .transactionFee(calculator.getTransactionFee())
                .totalAmount(calculator.getTotalExecutedAmount())
                .build();

        transactionUsdRepository.save(transaction);


        StockProfitUsdEntity stockProfit = StockProfitUsdEntity.builder()
                .user(user) // UserEntity 인스턴스
                .transaction(transaction) // TransactionUsdEntity 인스턴스
                .isProfit(calculator.isProfit())
                .perStockDiff(calculator.getProfitOrLossPerStock())
                .totalDiff(calculator.getProfitOrLossTotalAmount())
                .priceDiffRatio(calculator.getPriceDifferenceRatio())
                .commissionInclusivePriceDiff(calculator.getCommissionIncludedPriceDiff())
                .createdAt(LocalDateTime.now())
                .build();

        stockProfitUsdRepository.save(stockProfit);



        // 유저
        // 수수료까지 계산해서 손익분 시드머니 계산하기
        calculator.calculateUpdatedSeedMoney(user.getSeedMoneyUsd(), request.getTransactionType());
        // 투자한 돈에서 매도한 금액 빼기
        calculator.calculateUpdatedInvestAmount(user.getInvestmentAmountUsd(), request.getTransactionType());

        user = user
                .withSeedMoneyUsd(calculator.getUpdatedSeedMoney())
                .withInvestmentAmountUsd(calculator.getUpdatedInvestAmount());

        userRepository.save(user);



        // 응답 DTO 생성 및 반환
        return TransactionResponseDto.builder()
                .transactionId(transaction.getId())
                .marketType(request.getMarketType())
                .transactionType(request.getTransactionType())
                .stockName(request.getStockName())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .totalExecutedAmount(calculator.getTotalExecutedAmount())
                .commissionValue(calculator.getTransactionFee())
                .date(request.getDate())
                .build();
    }
}
