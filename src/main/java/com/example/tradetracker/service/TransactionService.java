package com.example.tradetracker.service;


import com.example.tradetracker.dto.TransactionRequestDto;
import com.example.tradetracker.dto.TransactionResponseDto;
import com.example.tradetracker.model.PortfolioUsdEntity;
import com.example.tradetracker.model.TransactionUsdEntity;
import com.example.tradetracker.model.UserEntity;
import com.example.tradetracker.repository.PortfolioUsdRepository;
import com.example.tradetracker.repository.TransactionUsdRepository;
import com.example.tradetracker.repository.UserRepository;
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
    public TransactionResponseDto createUsStockTransaction (TransactionRequestDto request) {
        Long userId = 1L; // 임의의 사용자 ID 설정
        BigDecimal totalPurchaseAmount;  // 총 매수 금액
        BigDecimal transactionFee;       // 수수료
        BigDecimal updatedSeedMoney;     // 업데이트된 시드머니
        BigDecimal updatedInvestmentAmount; // 업데이트된 투자 금액

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 주식 총 매수 금액 계산 (가격 * 수량)
        totalPurchaseAmount = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 수수료 계산 (매수 금액 * 수수료율)
        transactionFee = totalPurchaseAmount.multiply(request.getCommissionRate());

        // 시드머니에서 수수료 차감
        updatedSeedMoney = user.getSeedMoneyUsd().subtract(transactionFee);
        if (updatedSeedMoney.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("시드머니가 부족합니다.");
        }



        // 투자 금액이 시드머니를 넘지 않도록 검증
        if (user.getInvestmentAmountUsd().compareTo(user.getSeedMoneyUsd()) > 0) {
            throw new RuntimeException("투자 금액이 시드머니를 초과할 수 없습니다.");
        }

        // 유저의 시드머니 및 투자 금액 업데이트
        user = user.withSeedMoneyUsd(updatedSeedMoney)
                .withInvestmentAmountUsd(user.getInvestmentAmountUsd().add(totalPurchaseAmount));

        userRepository.save(user);


        // 주식 거래 정보 생성 및 저장
        TransactionUsdEntity transaction = TransactionUsdEntity.builder()
                .user(user)
                .transactionType(TransactionUsdEntity.TransactionType.valueOf(request.getTransactionType()))
                .stockName(request.getStockName())
                .transactionPrice(request.getPrice())
                .quantity(request.getQuantity())
                .transactionDate(LocalDateTime.now())
                .transactionFee(transactionFee)
                .totalAmount(totalPurchaseAmount)
                .build();

        transactionUsdRepository.save(transaction);

        // 포토폴리오에서 주식 존재 여부 확인
        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, request.getStockName());

        // 포토폴리오에 새로 산 주식 추가
        if (existingPortfolio != null) {
            // 존재 O: 평균 단가 및 수량 업데이트
            BigDecimal totalAmount = existingPortfolio.getTotalAmount().add(totalPurchaseAmount);
            BigDecimal totalQuantity = BigDecimal.valueOf(existingPortfolio.getQuantity()).add(BigDecimal.valueOf(request.getQuantity()));
            BigDecimal newAveragePrice = totalAmount.divide(totalQuantity, RoundingMode.HALF_UP);

            existingPortfolio = existingPortfolio.withPrice(newAveragePrice)
                    .withQuantity(existingPortfolio.getQuantity() + request.getQuantity())
                    .withTotalAmount(existingPortfolio.getTotalAmount().add(totalPurchaseAmount));

            portfolioUsdRepository.save(existingPortfolio);
        } else {
            // 존재 X: 새로운 포트폴리오 항목 생성
            PortfolioUsdEntity newPortfolio = PortfolioUsdEntity.builder()
                    .userId(userId)
                    .stockName(request.getStockName())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .totalAmount(totalPurchaseAmount)
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
                .totalPurchaseAmount(totalPurchaseAmount)
                .commissionValue(transactionFee) // 수수료 반환
                .build();
    }

}
