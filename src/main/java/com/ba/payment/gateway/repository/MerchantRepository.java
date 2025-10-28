package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.Merchant;
import com.ba.payment.gateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    List<Merchant> findByUser(User user);
    Optional<Merchant> findByBusinessEmail(String businessEmail);
    List<Merchant> findByStatus(String status);
}
