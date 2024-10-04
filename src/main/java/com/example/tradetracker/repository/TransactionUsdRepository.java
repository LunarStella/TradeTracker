package com.example.tradetracker.repository;

import com.example.tradetracker.model.TransactionUsdEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

@Repository
public interface TransactionUsdRepository extends JpaRepository<TransactionUsdEntity, Long> {
    // User ID로 거래 기록을 조회하고 날짜 내림차순으로 정렬
    Page<TransactionUsdEntity> findAllByUserIdOrderByTransactionDateDesc(Long userId, Pageable pageable);
}


