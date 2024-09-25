package com.example.tradetracker.repository;

import com.example.tradetracker.model.StockProfitUsdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockProfitUsdRepository extends JpaRepository <StockProfitUsdEntity, Long> {
}
