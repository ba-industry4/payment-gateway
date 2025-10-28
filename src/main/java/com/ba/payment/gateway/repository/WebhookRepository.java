package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.Merchant;
import com.ba.payment.gateway.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    List<Webhook> findByMerchant(Merchant merchant);
    List<Webhook> findByMerchantAndActive(Merchant merchant, Boolean active);
}
