package com.example.tradetracker.repository;

import com.example.tradetracker.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    // 필요한 추가적인 쿼리 메소드를 여기에 정의할 수 있습니다.
}
