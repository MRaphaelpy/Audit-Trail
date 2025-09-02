package com.mraphaelpy.auditoria.repository;

import com.mraphaelpy.auditoria.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = :failedAttempts WHERE u.username = :username")
    void updateFailedAttempts(@Param("username") String username, @Param("failedAttempts") Integer failedAttempts);
    
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = :locked, u.lockTime = :lockTime WHERE u.username = :username")
    void updateAccountLockStatus(@Param("username") String username, @Param("locked") Boolean locked, @Param("lockTime") LocalDateTime lockTime);
    
    @Modifying
    @Query("UPDATE User u SET u.twoFactorCode = :code, u.twoFactorCodeExpiry = :expiry WHERE u.username = :username")
    void updateTwoFactorCode(@Param("username") String username, @Param("code") String code, @Param("expiry") LocalDateTime expiry);
    
    @Modifying
    @Query("UPDATE User u SET u.twoFactorCode = null, u.twoFactorCodeExpiry = null WHERE u.username = :username")
    void clearTwoFactorCode(@Param("username") String username);
}
