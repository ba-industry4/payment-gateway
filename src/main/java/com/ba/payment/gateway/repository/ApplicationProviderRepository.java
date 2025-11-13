package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.Application;
import com.ba.payment.gateway.entity.ApplicationProvider;
import com.ba.payment.gateway.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationProviderRepository extends JpaRepository<ApplicationProvider, Long> {

    List<ApplicationProvider> findByApplication(Application application);

    List<ApplicationProvider> findByApplicationAndActiveTrue(Application application);

    @Query("SELECT ap FROM ApplicationProvider ap WHERE ap.application = :application AND ap.active = true ORDER BY ap.priority DESC")
    List<ApplicationProvider> findActiveProvidersByApplicationOrderedByPriority(@Param("application") Application application);

    Optional<ApplicationProvider> findByApplicationAndIsDefaultTrueAndActiveTrue(Application application);

    Optional<ApplicationProvider> findByApplicationAndPaymentProvider(Application application, PaymentProvider paymentProvider);

    @Query("SELECT ap FROM ApplicationProvider ap WHERE ap.application = :application AND ap.paymentProvider.name = :providerName AND ap.active = true")
    Optional<ApplicationProvider> findByApplicationAndProviderName(@Param("application") Application application, @Param("providerName") String providerName);

    boolean existsByApplicationAndPaymentProvider(Application application, PaymentProvider paymentProvider);
}

