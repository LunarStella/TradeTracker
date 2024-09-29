package com.example.tradetracker.repository;

import com.example.tradetracker.model.StockProfitLossUsdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockProfitLossUsdRepository extends JpaRepository <StockProfitLossUsdEntity, Long> {
    Optional<StockProfitLossUsdEntity> findByTransactionId(Long transactionId);
}
