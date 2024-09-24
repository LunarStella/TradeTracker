package com.example.tradetracker.util;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class TransactionCalculator {

    private BigDecimal totalPurchaseAmount;  // 구입한 주식 총 매수 금액
    private BigDecimal transactionFee;       // 수수료
    private BigDecimal updatedSeedMoney;     // 업데이트된 시드머니
    
    private BigDecimal updatedPortfolioTotalAmount; // 업데이트 된 포토폴리오 총 주식 금액
    private int updatedPortfolioTotalQuantity; // 업데이트 된 포토폴리오 주식 개수
    private BigDecimal updatedPortfolioAveragePrice; // 업데이트 된 포토폴리오 주식 평균 가격

    // 기본 생성자
    public TransactionCalculator() {
        this.totalPurchaseAmount = BigDecimal.ZERO;
        this.transactionFee = BigDecimal.ZERO;
        this.updatedSeedMoney = BigDecimal.ZERO;
        this.updatedPortfolioTotalAmount = BigDecimal.ZERO;
        this.updatedPortfolioTotalQuantity = 0;
        this.updatedPortfolioAveragePrice = BigDecimal.ZERO;
    }

    // 총 매수 금액 계산 (가격 * 수량)
    public void calculateTotalPurchaseAmount(BigDecimal price, int quantity) {
        this.totalPurchaseAmount = price.multiply(BigDecimal.valueOf(quantity));
    }

    // 수수료 계산 (매수 또는 매도 금액 * 수수료율)
    public void calculateTransactionFee(BigDecimal commissionRate) {
        this.transactionFee = this.totalPurchaseAmount.multiply(commissionRate);
    }

    // 시드머니 업데이트 (기존 시드머니 - 수수료)
    public void calculateUpdatedSeedMoney(BigDecimal currentSeedMoney) {
        this.updatedSeedMoney = currentSeedMoney.subtract(this.transactionFee);
    }


    //포토폴리오에 존재하는 기존 주식에 매수한 주식 총 금액 더하기
    public void calculatePortfolioUpdateTotalAmount(BigDecimal portfolioTotalAmount){
        this.updatedPortfolioTotalAmount = portfolioTotalAmount.add(this.totalPurchaseAmount);
    }

    public void calculatePortfolioUpdatedTotalQuantity(Integer portfolioTotalQuantity, int transactionStockQuantity){
        this.updatedPortfolioTotalQuantity = portfolioTotalQuantity + transactionStockQuantity;
    }

    public void calculatePortfolioUpdatedAveragePrice(){
        this.updatedPortfolioAveragePrice = this.updatedPortfolioTotalAmount.divide(BigDecimal.valueOf(this.updatedPortfolioTotalQuantity),
                RoundingMode.HALF_UP);
    }

    // 주식당 손익 계산 (매도 가격 - 평균 매입 가격)
    public BigDecimal calculateProfitOrLossPerStock(BigDecimal sellPrice, BigDecimal purchasePrice) {
        return sellPrice.subtract(purchasePrice);
    }

    // 총 손익 계산 (주식당 손익 * 수량)
    public BigDecimal calculateTotalProfitOrLoss(BigDecimal profitOrLossPerStock, int quantity) {
        return profitOrLossPerStock.multiply(BigDecimal.valueOf(quantity));
    }

    // 평균 단가 계산 (총 금액 / 총 수량)
    public BigDecimal calculateAveragePrice(BigDecimal totalAmount, BigDecimal totalQuantity) {
        return totalAmount.divide(totalQuantity, RoundingMode.HALF_UP);
    }

    // 포트폴리오 총 금액 업데이트 (기존 총 금액 + 매수 또는 매도 금액)
    public BigDecimal updatePortfolioTotalAmount(BigDecimal existingTotalAmount, BigDecimal transactionAmount) {
        return existingTotalAmount.add(transactionAmount);
    }

    // 포트폴리오 총 수량 업데이트 (기존 수량 + 매수 또는 매도 수량)
    public BigDecimal updatePortfolioTotalQuantity(int existingQuantity, int transactionQuantity) {
        return BigDecimal.valueOf(existingQuantity + transactionQuantity);
    }

    // 포트폴리오 수익률 계산 (현재 가격 - 평균 매입 가격) / 평균 매입 가격
    public BigDecimal calculateProfitRatio(BigDecimal currentPrice, BigDecimal averagePurchasePrice) {
        return (currentPrice.subtract(averagePurchasePrice))
                .divide(averagePurchasePrice, RoundingMode.HALF_UP);
    }
}
