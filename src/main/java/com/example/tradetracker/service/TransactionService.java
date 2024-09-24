package com.example.tradetracker.service;


import com.example.tradetracker.dto.TransactionRequestDto;
import com.example.tradetracker.dto.TransactionResponseDto;
import com.example.tradetracker.model.PortfolioUsdEntity;
import com.example.tradetracker.model.TransactionUsdEntity;
import com.example.tradetracker.model.UserEntity;
import com.example.tradetracker.repository.PortfolioUsdRepository;
import com.example.tradetracker.repository.TransactionUsdRepository;
import com.example.tradetracker.repository.UserRepository;
import com.example.tradetracker.util.TransactionCalculator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional
@AllArgsConstructor
public class TransactionService {
    private final UserRepository userRepository;
    private final TransactionUsdRepository transactionUsdRepository;
    private final PortfolioUsdRepository portfolioUsdRepository;

    // 주식 매수
    public TransactionResponseDto createUsStockPurchaseTransaction (TransactionRequestDto request) {
        Long userId = 1L; // 임의의 사용자 ID 설정
        TransactionCalculator calculator = new TransactionCalculator();
//        BigDecimal totalPurchaseAmount;  // 총 매수 금액
//        BigDecimal transactionFee;       // 수수료
//        BigDecimal updatedSeedMoney;     // 업데이트된 시드머니

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 주식 총 매수 금액 계산 (가격 * 수량)
        calculator.calculateTotalPurchaseAmount(request.getPrice(),request.getQuantity());
//        totalPurchaseAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 수수료 계산 (매수 금액 * 수수료율)
        calculator.calculateTransactionFee(request.getCommissionRate());
//        transactionFee = totalPurchaseAmount.multiply(request.getCommissionRate());

        // 시드머니에서 수수료 차감
        calculator.calculateUpdatedSeedMoney(user.getSeedMoneyUsd());
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
                .withInvestmentAmountUsd(user.getInvestmentAmountUsd().add(calculator.getTotalPurchaseAmount()));
//        user = user.withSeedMoneyUsd(updatedSeedMoney)
//                .withInvestmentAmountUsd(user.getInvestmentAmountUsd().add(totalPurchaseAmount));

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
                .totalAmount(calculator.getTotalPurchaseAmount())
                .build();
//        TransactionUsdEntity transaction = TransactionUsdEntity.builder()
//                .user(user)
//                .transactionType(TransactionUsdEntity.TransactionType.valueOf(request.getTransactionType()))
//                .stockName(request.getStockName())
//                .transactionPrice(request.getPrice())
//                .quantity(request.getQuantity())
//                .transactionDate(LocalDateTime.now())
//                .transactionFee(transactionFee)
//                .totalAmount(totalPurchaseAmount)
//                .build();

        transactionUsdRepository.save(transaction);

        // 포토폴리오에서 주식 존재 여부 확인
        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, request.getStockName());

        // 포토폴리오에 새로 산 주식 추가
        if (existingPortfolio != null) {
            // 존재 O: 평균 단가 및 수량 업데이트
            calculator.calculatePortfolioUpdateTotalAmount(existingPortfolio.getTotalAmount());
            calculator.calculatePortfolioUpdatedTotalQuantity(existingPortfolio.getQuantity(), request.getQuantity());
            calculator.calculatePortfolioUpdatedAveragePrice();
//            BigDecimal totalAmount = existingPortfolio.getTotalAmount().add(calculator.getTotalPurchaseAmount());
//            BigDecimal totalQuantity = BigDecimal.valueOf(existingPortfolio.getQuantity()).add(BigDecimal.valueOf(request.getQuantity()));
//            BigDecimal newAveragePrice = totalAmount.divide(totalQuantity, RoundingMode.HALF_UP);

            existingPortfolio = existingPortfolio
                    .withTotalAmount(calculator.getUpdatedPortfolioTotalAmount())
                    .withQuantity(calculator.getUpdatedPortfolioTotalQuantity())
                    .withPrice(calculator.getUpdatedPortfolioAveragePrice());

            portfolioUsdRepository.save(existingPortfolio);
        } else {
            // 존재 X: 새로운 포트폴리오 항목 생성
            PortfolioUsdEntity newPortfolio = PortfolioUsdEntity.builder()
                    .user(user)
                    .stockName(request.getStockName())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .totalAmount(calculator.getTotalPurchaseAmount())
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
                .totalPurchaseAmount(calculator.getTotalPurchaseAmount())
                .commissionValue(calculator.getTransactionFee())
                .date(request.getDate())
                .build();
    }

//    public TransactionResponseDto createUsStockSellTransaction (TransactionRequestDto request) {
//        Long userId = 1L; // 임의의 사용자 ID 설정
//        BigDecimal totalPurchaseAmount;  // 총 매수 금액
//        BigDecimal transactionFee;       // 수수료
//        BigDecimal updatedSeedMoney;     // 업데이트된 시드머니
//        BigDecimal updatedInvestmentAmount; // 업데이트된 투자 금액
//
//        // 유저 정보 조회
//        UserEntity user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // 주식 총 매도 금액 계산 (가격 * 수량)
//        totalPurchaseAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
//
//        // 수수료 계산 (매도 금액 * 수수료율)
//        transactionFee = totalPurchaseAmount.multiply(request.getCommissionRate());
//
//
//        // 포토폴리오
//        // 포트폴리오에서 해당 주식 정보 가져오기
//        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, request.getStockName());
//
//        // 포트폴리오에 해당 주식 없거나 수량이 적으면 오류
//        if (existingPortfolio == null || existingPortfolio.getQuantity() < request.getQuantity()) {
//            throw new RuntimeException("포트폴리오에 주식이 없거나 매도 수량이 부족합니다.");
//        }
//
//        // 매도 주가에서 포트폴리오 평균 주가 빼기
//        // 포토폴리오 평균 가격
//        BigDecimal averagePurchasePrice = existingPortfolio.getPrice();
//        // 주식당 손익 가격
//        BigDecimal profitOrLossPerStock = request.getPrice().subtract(averagePurchasePrice);
//        BigDecimal profitOrLossTotalValue = profitOrLossPerStock.multiply(BigDecimal.valueOf(request.getQuantity()));
//
//        // 포트폴리오에서 총 주식 수 및 총 금액 업데이트
//        int updatedQuantity = existingPortfolio.getQuantity() - request.getQuantity();
//        BigDecimal updatedTotalValue = existingPortfolio.getPrice().multiply(BigDecimal.valueOf(updatedQuantity));
//
//        // 포트폴리오 엔티티 업데이트
//        if (updatedQuantity > 0) {
//            // 수량이 0보다 크면 업데이트
//            existingPortfolio = existingPortfolio.withQuantity(updatedQuantity)
//                    .withTotalAmount(updatedTotalValue);
//            portfolioUsdRepository.save(existingPortfolio);
//        } else {
//            // 수량이 0이면 해당 포트폴리오 삭제
//            portfolioUsdRepository.delete(existingPortfolio);
//        }
//
//
//        // 주식 거래 정보 생성 및 저장
//        TransactionUsdEntity transaction = TransactionUsdEntity.builder()
//                .user(user)
//                .transactionType(TransactionUsdEntity.TransactionType.valueOf(request.getTransactionType()))
//                .stockName(request.getStockName())
//                .transactionPrice(request.getPrice())
//                .quantity(request.getQuantity())
//                .transactionDate(request.getDate())
//                .transactionFee(transactionFee)
//                .totalAmount(totalPurchaseAmount)
//                .build();
//
//        transactionUsdRepository.save(transaction);
//
//
//
//        // 주식 손익
//
//
//
//
//
//
//        // 유저
//        // 투자한 돈에서 매도한 금액 빼기
//
//        // 수수료까지 계산해서 손익분 시드머니 계산하기
//
//
//
//
//
//
//
//
//
//
//
//
//        // 응답 DTO 생성 및 반환
//        return TransactionResponseDto.builder()
//                .transactionId(transaction.getId())
//                .marketType(request.getMarketType())
//                .transactionType(request.getTransactionType())
//                .stockName(request.getStockName())
//                .price(request.getPrice())
//                .quantity(request.getQuantity())
//                .totalPurchaseAmount(totalPurchaseAmount)
//                .commissionValue(transactionFee) // 수수료 반환
//                .build();
//    }
}
