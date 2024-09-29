package com.example.tradetracker.repository;

import com.example.tradetracker.model.TransactionUsdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionUsdRepository extends JpaRepository<TransactionUsdEntity, Long> {
}

