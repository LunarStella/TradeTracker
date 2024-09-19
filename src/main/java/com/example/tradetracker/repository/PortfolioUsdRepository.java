package com.example.tradetracker.repository;

import com.example.tradetracker.model.PortfolioUsdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioUsdRepository extends JpaRepository<PortfolioUsdEntity, Long> {
    PortfolioUsdEntity findByUserIdAndStockName(Long userId, String stockName);
}
