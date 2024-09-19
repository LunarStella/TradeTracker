package com.example.tradetracker.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name ="user")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "password", nullable = false)
    private String password;

    @With
    @Column(name = "seed_money_krw", nullable = false, precision = 12, scale = 2)
    private BigDecimal seedMoneyKrw;

    @With
    @Column(name = "seed_money_usd", nullable = false, precision = 10, scale = 4)
    private BigDecimal seedMoneyUsd;

    @With
    @Column(name = "investment_amount_krw", nullable = false, precision = 12, scale = 2)
    private BigDecimal investmentAmountKrw;

    @With
    @Column(name = "investment_amount_usd", nullable = false, precision = 10, scale = 4)
    private BigDecimal investmentAmountUsd;

}

