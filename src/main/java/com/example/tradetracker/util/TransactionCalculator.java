package com.example.tradetracker.util;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class TransactionCalculator {

    // 업데이트된 시드머니
    private BigDecimal totalExecutedAmount;  // 채결한 주식 총 금액
    private BigDecimal transactionFee;       // 수수료

    private int portfolioPurchaseTotalQuantity; // 매수 후 토폴리오 주식 개수
    private BigDecimal portfolioPurchaseTotalAmount; // 매수 후 포토폴리오 총 주식 금액
    private BigDecimal portfolioPurchaseAveragePrice; // 매수 후 포토폴리오 주식 평균 가격

    private int portfolioSellTotalQuantity; // 매도 후 포토폴리오 주식 개수
    private BigDecimal portfolioSellTotalAmount; // 매도 후 포토폴리오 총 주식 금액

    private BigDecimal profitOrLossPerStock; // 주식 1개 손익 차이 금액
    private BigDecimal profitOrLossTotalAmount; // 총 손익 차이 금액
    private BigDecimal priceDifferenceRatio; // 가격 차이 비율
    private BigDecimal commissionIncludedPriceDiff; // 수수료 포함 가격 차이
    private boolean isProfit; // 수익 여부

    private BigDecimal updatedSeedMoney;  // 시드머니 업데이트
    private BigDecimal updatedInvestAmount; // 투자한 금액 업데이트

    // 기본 생성자
    public TransactionCalculator() {
        this.totalExecutedAmount = BigDecimal.ZERO;
        this.transactionFee = BigDecimal.ZERO;

        this.portfolioPurchaseTotalQuantity = 0;
        this.portfolioPurchaseTotalAmount = BigDecimal.ZERO;
        this.portfolioPurchaseAveragePrice = BigDecimal.ZERO;

        this.portfolioSellTotalQuantity = 0;
        this.portfolioSellTotalAmount = BigDecimal.ZERO;

        this.profitOrLossPerStock = BigDecimal.ZERO;
        this.profitOrLossTotalAmount = BigDecimal.ZERO;
        this.priceDifferenceRatio = BigDecimal.ZERO;
        this.commissionIncludedPriceDiff = BigDecimal.ZERO;
        this.isProfit = true;

        this.updatedSeedMoney = BigDecimal.ZERO;
        this.updatedInvestAmount = BigDecimal.ZERO;
    }



    // 총 채결 금액 계산 (가격 * 수량)
    public void calculateTotalExecutedAmount(BigDecimal price, int quantity) {
        this.totalExecutedAmount = price.multiply(BigDecimal.valueOf(quantity));
    }

    // 수수료 계산 (매수 또는 매도 금액 * 수수료율)
    public void calculateTransactionFee(BigDecimal commissionRate) {
        this.transactionFee = this.totalExecutedAmount.multiply(commissionRate);
    }


    //포토폴리오에 존재하는 기존 주식에 매수한 주식 개수 더하기
    public void calculatePortfolioPurchaseTotalQuantity(Integer portfolioTotalQuantity, int transactionStockQuantity) {
        this.portfolioPurchaseTotalQuantity = portfolioTotalQuantity + transactionStockQuantity;
    }

    //포토폴리오에 존재하는 기존 주식에 매수한 주식 총 금액 더하기
    public void calculatePortfolioPurchaseTotalAmount(BigDecimal portfolioTotalAmount) {
        this.portfolioPurchaseTotalAmount = portfolioTotalAmount.add(this.totalExecutedAmount);
    }

    // 포토폴리오 주식 평단가 구하기
    public void calculatePortfolioPurchaseAveragePrice() {
        this.portfolioPurchaseAveragePrice = this.portfolioPurchaseTotalAmount.divide(BigDecimal.valueOf(this.portfolioPurchaseTotalQuantity),
                RoundingMode.HALF_UP);
    }

    // 포트폴리오에서 매도 후 남은 주식 개수 계산
    public void calculatePortfolioSellTotalQuantity(Integer portfolioTotalQuantity, int transactionStockQuantity) {
        this.portfolioSellTotalQuantity = portfolioTotalQuantity - transactionStockQuantity; // 매도한 주식 수만큼 차감
    }

    // 포트폴리오 주식 총 금액 구하기: 매도 후 남은 개수에 포토폴리오 평균 단가 곱하기
    public void calculatePortfolioSellTotalAmount(BigDecimal portfolioAveragePrice) {
        this.portfolioSellTotalAmount = portfolioAveragePrice.multiply(BigDecimal.valueOf(this.portfolioSellTotalQuantity));
    }

    // 주식 1개 손익 차이 금액
    public void calculateProfitOrLossPerStock(BigDecimal sellPrice, BigDecimal averageStockPrice) {
        this.profitOrLossPerStock = sellPrice.subtract(averageStockPrice);
    }

    // 총 손익 차이 금액 계산 (1개당 손익 차이 * 매도 수량)
    public void calculateProfitOrLossTotalAmount(int sellQuantity) {
        this.profitOrLossTotalAmount = this.profitOrLossPerStock.multiply(BigDecimal.valueOf(sellQuantity));
    }

    // 가격 차이 비율 계산
    // 가격 차이 비율 및 수수료 포함 가격 차이 계산
    public void calculateProfitOrLossDetails(BigDecimal averageStockPrice) {
        // 가격 차이 비율 계산
        this.priceDifferenceRatio = this.profitOrLossPerStock.divide(averageStockPrice, RoundingMode.HALF_UP);

        // 수수료 포함 가격 차이 계산
        this.commissionIncludedPriceDiff = this.profitOrLossTotalAmount.subtract(this.transactionFee);

        // 수익 여부 업데이트
        this.isProfit = this.commissionIncludedPriceDiff.compareTo(BigDecimal.ZERO) > 0;
    }

    // 시드머니 업데이트
    public void calculateUpdatedSeedMoney(BigDecimal currentSeedMoney, String transactionType) {
        if ("buy".equalsIgnoreCase(transactionType)){
            // 매수 거래의 경우 수수료를 차감
            this.updatedSeedMoney = currentSeedMoney.subtract(this.transactionFee);
        } else if ("sell".equalsIgnoreCase(transactionType)) {
            // 매도 거래의 경우 수수료 포함 가격 차이와 더함 (음수여도 작동)
            this.updatedSeedMoney =  currentSeedMoney.add(this.commissionIncludedPriceDiff);
        }
    }

    // 투자 금액 업데이트
    public void calculateUpdatedInvestAmount(BigDecimal currentInvestAmount, String transactionType ) {
        if ("buy".equalsIgnoreCase(transactionType)) {
            // 매수 거래의 경우 매수 금액을 더함
            this.updatedInvestAmount = currentInvestAmount.add(this.totalExecutedAmount);
        } else if ("sell".equalsIgnoreCase(transactionType)) {
            // 매도 거래의 경우 매도 금액을 차감
            this.updatedInvestAmount = currentInvestAmount.subtract(this.totalExecutedAmount);
        }
    }

}