package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {
    Optional<PaymentProvider> findByName(String name);
    List<PaymentProvider> findByActive(Boolean active);
    List<PaymentProvider> findByType(String type);
}
