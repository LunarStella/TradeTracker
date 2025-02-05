package com.example.tradetracker.service;


import com.example.tradetracker.dto.PagingResponseDto;
import com.example.tradetracker.dto.TransactionRequestDto;
import com.example.tradetracker.dto.TransactionResponseDto;
import com.example.tradetracker.model.PortfolioUsdEntity;
import com.example.tradetracker.model.StockProfitLossUsdEntity;
import com.example.tradetracker.model.TransactionUsdEntity;
import com.example.tradetracker.model.UserEntity;
import com.example.tradetracker.repository.PortfolioUsdRepository;
import com.example.tradetracker.repository.StockProfitLossUsdRepository;
import com.example.tradetracker.repository.TransactionUsdRepository;
import com.example.tradetracker.repository.UserRepository;
import com.example.tradetracker.response.CustomException;
import com.example.tradetracker.response.ErrorCode;
import com.example.tradetracker.util.TransactionCalculator;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class TransactionService {
    private final UserRepository userRepository;
    private final TransactionUsdRepository transactionUsdRepository;
    private final PortfolioUsdRepository portfolioUsdRepository;
    private final StockProfitLossUsdRepository stockProfitLossUsdRepository;

    // 주식 매수
    public TransactionResponseDto createUsStockPurchaseTransaction (TransactionRequestDto request) {
        Long userId = 1L; // 임의의 사용자 ID 설정
        TransactionCalculator calculator = new TransactionCalculator();
//        BigDecimal totalExecutedAmount;  // 총 매수 금액
//        BigDecimal transactionFee;       // 수수료
//        BigDecimal updatedSeedMoney;     // 업데이트된 시드머니

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ID_NOT_FOUND_USER));

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
            throw new CustomException(ErrorCode.INSUFFICIENT_SEED_MONEY);
        }

        // 투자 금액이 시드머니를 넘지 않도록 검증
        if (user.getInvestmentAmountUsd().compareTo(user.getSeedMoneyUsd()) > 0) {
            throw new CustomException(ErrorCode.INVESTMENT_AMOUNT_EXCEEDS_SEED_MONEY);
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
            calculator.calculatePortfolioIncreaseTotalQuantity(existingPortfolio.getQuantity(), request.getQuantity());
            calculator.calculatePortfolioIncreaseTotalAmount(existingPortfolio.getTotalAmount());
            calculator.calculatePortfolioUpdatedAveragePrice();
//            BigDecimal totalAmount = existingPortfolio.getTotalAmount().add(calculator.getTotalExecutedAmount());
//            BigDecimal totalQuantity = BigDecimal.valueOf(existingPortfolio.getQuantity()).add(BigDecimal.valueOf(request.getQuantity()));
//            BigDecimal newAveragePrice = totalAmount.divide(totalQuantity, RoundingMode.HALF_UP);

            existingPortfolio = existingPortfolio
                    .withQuantity(calculator.getPortfolioUpdatedTotalQuantity())
                    .withTotalAmount(calculator.getPortfolioUpdatedTotalAmount())
                    .withPrice(calculator.getPortfolioUpdatedAveragePrice());

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
        return new TransactionResponseDto(
                transaction.getId(),
                request.getMarketType(),
                request.getTransactionType(),
                request.getStockName(),
                request.getPrice(),
                request.getQuantity(),
                calculator.getTotalExecutedAmount(),
                calculator.getTransactionFee(),
                request.getDate()
        );

    }

//     매수 주식 거래 기록 삭제 서비스 함수
    public void deleteUsStockPurchaseTransaction(Long transactionId){
        Long userId = 1L; // 임의의 사용자 ID 설정
        TransactionCalculator calculator = new TransactionCalculator();

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ID_NOT_FOUND_USER));

        // transaction_id로 검색
        TransactionUsdEntity existingTransaction = transactionUsdRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.ID_NOT_FOUND_TRANSACTION));

        // 포토폴리오
        // 포트폴리오에서 해당 주식 정보 가져오기
        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, existingTransaction.getStockName());

        // 해당 id 트랜젝션에서 가져옴
        calculator.setTotalExecutedAmount(existingTransaction.getTotalAmount());
        calculator.setTransactionFee((existingTransaction.getTransactionFee()));


        // 포트폴리오에 해당 주식 없으면 오류
        if (existingPortfolio == null) {
            throw new CustomException(ErrorCode.ID_NOT_FOUND_PORTFOLIO);
        }

        // 포토폴리오 주식 개수에서 삭제 구입 주식 개수 뺌
        calculator.calculatePortfolioDecreaseTotalQuantity(existingPortfolio.getQuantity(), existingTransaction.getQuantity());

        // 수량이 0보다 크면 업데이트
        if(calculator.getPortfolioUpdatedTotalQuantity() > 0){
            // 포토폴리오 총 주식 금액에서 삭제 구입 총 주식 금액 뺌
            calculator.calculatePortfolioDecreaseTotalAmount(existingPortfolio.getTotalAmount());
            // 수정 된 주식 개수와 총 금액을 통해 평균 단가 구함
            calculator.calculatePortfolioUpdatedAveragePrice();

            existingPortfolio = existingPortfolio
                    .withQuantity(calculator.getPortfolioUpdatedTotalQuantity())
                    .withTotalAmount(calculator.getPortfolioUpdatedTotalAmount())
                    .withPrice(calculator.getPortfolioUpdatedAveragePrice());

            portfolioUsdRepository.save(existingPortfolio);

        } else if(calculator.getPortfolioUpdatedTotalQuantity() == 0){
            // 수량이 0이면 해당 포트폴리오 삭제
            portfolioUsdRepository.delete(existingPortfolio);

        } else{
            // 수량 초과
            throw new CustomException(ErrorCode.EXCESS_SELL_QUANTITY);
        }


        //유저
        // 시드머니: 수수료 뺀 거 다시 더하기
        // 투자 금액: 매수 한 금액 빼기
        // REFACTOR 필요!!
        user = user
                .withSeedMoneyUsd(user.getSeedMoneyUsd().add(calculator.getTransactionFee()))
                .withInvestmentAmountUsd(user.getInvestmentAmountUsd().subtract(calculator.getTotalExecutedAmount()));

        userRepository.save(user);

        // 거래
        // 삭제
        transactionUsdRepository.delete(existingTransaction);

    }

    public TransactionResponseDto createUsStockSellTransaction (TransactionRequestDto request) {
        Long userId = 1L; // 임의의 사용자 ID 설정
        TransactionCalculator calculator = new TransactionCalculator();

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->  new CustomException(ErrorCode.ID_NOT_FOUND_USER));


        // 포토폴리오
        // 포트폴리오에서 해당 주식 정보 가져오기
        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, request.getStockName());

        // 포트폴리오에 해당 주식 없거나 수량이 적으면 오류
        if (existingPortfolio == null || existingPortfolio.getQuantity() < request.getQuantity()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK_QUANTITY);
        }

        // 주식 총 매도 금액 계산 (포토폴리오 가격 * 수량)
        calculator.calculateTotalExecutedAmount(existingPortfolio.getPrice(),request.getQuantity());
        // 수수료 계산 (매도 금액 * 수수료율)
        calculator.calculateTransactionFee(request.getCommissionRate());

        // 매도 후 포토폴리오 구하기
        calculator.calculatePortfolioDecreaseTotalQuantity(existingPortfolio.getQuantity(), request.getQuantity());
        calculator.calculatePortfolioDecreaseTotalAmount(existingPortfolio.getTotalAmount());

        // 손익 계산하기
        // 주식 1개 손익 차이 금액
        calculator.calculateProfitOrLossPerStock(request.getPrice(), existingPortfolio.getPrice());
        // 총 손익 차이 금액
        calculator.calculateProfitOrLossTotalAmount(request.getQuantity());
        // 주가 매도 매수 차이 비율, 수수료 포함 차이 금액, 수익 여부 계산
        calculator.calculateProfitOrLossDetails(existingPortfolio.getPrice());


        // 포트폴리오 엔티티 업데이트
        if (calculator.getPortfolioUpdatedTotalQuantity() > 0) {
            // 수량이 0보다 크면 업데이트
            existingPortfolio = existingPortfolio
                    .withQuantity(calculator.getPortfolioUpdatedTotalQuantity())
                    .withTotalAmount(calculator.getPortfolioUpdatedTotalAmount());
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


        StockProfitLossUsdEntity stockProfitLoss = StockProfitLossUsdEntity.builder()
                .user(user) // UserEntity 인스턴스
                .transaction(transaction) // TransactionUsdEntity 인스턴스
                .isProfit(calculator.isProfit())
                .perStockDiff(calculator.getProfitOrLossPerStock())
                .totalDiff(calculator.getProfitOrLossTotalAmount())
                .priceDiffRatio(calculator.getPriceDifferenceRatio())
                .commissionInclusivePriceDiff(calculator.getCommissionIncludedPriceDiff())
                .createdAt(LocalDateTime.now())
                .build();

        stockProfitLossUsdRepository.save(stockProfitLoss);



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
        return new TransactionResponseDto(
                transaction.getId(),
                request.getMarketType(),
                request.getTransactionType(),
                request.getStockName(),
                request.getPrice(),
                request.getQuantity(),
                calculator.getTotalExecutedAmount(),
                calculator.getTransactionFee(),
                request.getDate()
        );
    }

    //     매수 주식 거래 기록 삭제 서비스 함수
    public void deleteUsStockSellTransaction(Long transactionId){
        Long userId = 1L; // 임의의 사용자 ID 설정
        TransactionCalculator calculator = new TransactionCalculator();

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ID_NOT_FOUND_USER));

        // transaction_id로 검색
        TransactionUsdEntity existingTransaction = transactionUsdRepository.findById(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.ID_NOT_FOUND_TRANSACTION));

        // 손익
        StockProfitLossUsdEntity existingProfitLoss = stockProfitLossUsdRepository.findByTransactionId(transactionId)
                .orElseThrow(() ->  new CustomException(ErrorCode.ID_NOT_FOUND_PROFIT_LOSS));

        // 포토폴리오
        // 포트폴리오에서 해당 주식 정보 가져오기
        PortfolioUsdEntity existingPortfolio = portfolioUsdRepository.findByUserIdAndStockName(userId, existingTransaction.getStockName());

        calculator.setTransactionFee((existingTransaction.getTransactionFee()));

        // 포트폴리오에 주식이 존재 x
        if (existingPortfolio == null) {
            calculator.calculatePortfolioOriginalAveragePrice(existingTransaction.getTransactionPrice(),existingProfitLoss.getPerStockDiff());
            calculator.calculatePortfolioOriginalTotalAmount(existingTransaction.getQuantity());

            // 새로운 포트폴리오 항목 생성
            PortfolioUsdEntity newPortfolio = PortfolioUsdEntity.builder()
                    .user(user)
                    .stockName(existingTransaction.getStockName())
                    .price(calculator.getPortfolioOriginalAveragePrice())
                    .quantity(existingTransaction.getQuantity())
                    .totalAmount(calculator.getPortfolioOriginalTotalAmount())
                    .build();

            portfolioUsdRepository.save(newPortfolio);

        } else {
            // 포토폴리오 수정
            // 나중에 REFECTORING 필요 !!
            calculator.calculatePortfolioIncreaseTotalQuantity(existingPortfolio.getQuantity(), existingTransaction.getQuantity());
            calculator.setPortfolioOriginalAveragePrice(existingPortfolio.getPrice());
            calculator.calculatePortfolioOriginalTotalAmount(calculator.getPortfolioUpdatedTotalQuantity());


            existingPortfolio = existingPortfolio
                    .withQuantity(calculator.getPortfolioUpdatedTotalQuantity())
                    .withTotalAmount(calculator.getPortfolioOriginalTotalAmount());

            portfolioUsdRepository.save(existingPortfolio);
        }


        //유저
        // 시드머니: 수수료 뺀 거 다시 더하기
        // 투자 금액: 매수 한 금액 빼기
        // REFACTORING 필요 !!
        user = user
                .withSeedMoneyUsd(user.getSeedMoneyUsd().subtract(existingProfitLoss.getCommissionInclusivePriceDiff()))
                .withInvestmentAmountUsd(user.getInvestmentAmountUsd().add(existingTransaction.getTotalAmount()));

        userRepository.save(user);

        // 손익 레코드 삭제
        stockProfitLossUsdRepository.delete(existingProfitLoss);

        // 거래 레코드 삭제
        transactionUsdRepository.delete(existingTransaction);

    }

    // 날짜 순으로 매수 매도 기록 조회
    public PagingResponseDto<TransactionResponseDto> getTransactionsSortedByDate(int page, String marketType) {
        Long userId = 1L; // 임의의 사용자 ID 설정
        Pageable pageable = PageRequest.of(page, 10);
        Page<TransactionUsdEntity> transactionPage;

        // 유저 정보 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ID_NOT_FOUND_USER));

        // marketType에 따라 거래 기록을 조회
       if ("US".equals(marketType)) {
           transactionPage = transactionUsdRepository.findAllByUserIdOrderByTransactionDateDesc(userId, pageable);
        } else {
            throw new IllegalArgumentException("유효하지 않은 시장 유형입니다.");
        }


        // TransactionResponseDto로 변환
        List<TransactionResponseDto> transactionResponses = transactionPage.getContent().stream()
                .map(transaction -> new TransactionResponseDto(
                        transaction.getId(),
                        marketType,
                        transaction.getTransactionType().name(),
                        transaction.getStockName(),
                        transaction.getTransactionPrice(),
                        transaction.getQuantity(),
                        transaction.getTotalAmount(),
                        transaction.getTransactionFee(),
                        transaction.getTransactionDate()
                ))
                .collect(Collectors.toList());

        // 페이지 정보 추출
        int currentPage = transactionPage.getNumber(); // 현재 페이지 번호
        boolean hasNext = transactionPage.hasNext(); // 다음 페이지 여부
        int totalPages = transactionPage.getTotalPages(); // 전체 페이지 수

        // PagingResponseDto 생성
        PagingResponseDto<TransactionResponseDto> pagingResponse = new PagingResponseDto<>(
                currentPage,
                hasNext,
                totalPages,
                transactionResponses
                );

        return pagingResponse;
    }

}
