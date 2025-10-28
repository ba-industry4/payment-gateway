package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.PaymentMethod;
import com.ba.payment.gateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUser(User user);
    List<PaymentMethod> findByUserAndActive(User user, Boolean active);
}
