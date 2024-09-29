package com.example.tradetracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "stock_profit_loss_usd")
public class StockProfitLossUsdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_user"))
    private UserEntity user;

    @OneToOne
    @JoinColumn(name = "transaction_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_usd"))
    private TransactionUsdEntity transaction;

    @Column(name = "is_profit", nullable = false)  // 이득 여부 컬럼 추가
    private Boolean isProfit;  // Boolean 타입으로 선언

    @Column(name = "per_stock_diff", nullable = false, precision = 10, scale = 4)
    private BigDecimal perStockDiff;

    @Column(name = "total_diff", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalDiff;

    @Column(name = "price_diff_ratio", nullable = false, precision = 6, scale = 4)
    private BigDecimal priceDiffRatio;

    @Column(name = "commission_inclusive_price_diff", nullable = false, precision = 10, scale = 4)
    private BigDecimal commissionInclusivePriceDiff;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

}