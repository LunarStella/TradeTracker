package com.example.tradetracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "portfolio_usd")
public class PortfolioUsdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "stock_name", nullable = false)
    private String stockName;

    @With
    @Column(name = "average_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal price;

    @With
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @With
    @Column(name = "total_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalAmount;

}
