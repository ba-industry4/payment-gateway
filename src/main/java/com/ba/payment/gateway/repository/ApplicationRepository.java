package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.Application;
import com.ba.payment.gateway.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByAppKey(String appKey);

    Optional<Application> findByIdAndActiveTrue(Long id);

    Optional<Application> findByAppKeyAndActiveTrue(String appKey);

    List<Application> findByMerchant(Merchant merchant);

    List<Application> findByMerchantAndActiveTrue(Merchant merchant);

    boolean existsByAppKey(String appKey);
}

