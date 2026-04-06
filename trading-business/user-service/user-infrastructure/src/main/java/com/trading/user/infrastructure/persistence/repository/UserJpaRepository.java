package com.trading.user.infrastructure.persistence.repository;

import com.trading.user.infrastructure.persistence.po.UserPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户JPA仓储
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserPO, String> {
    
    Optional<UserPO> findByUsername(String username);
    
    Optional<UserPO> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Page<UserPO> findByStatus(String status, Pageable pageable);
}
