package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.ApiKey;
import com.ba.payment.gateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyValue(String keyValue);
    List<ApiKey> findByUser(User user);
    List<ApiKey> findByUserAndActive(User user, Boolean active);
}
