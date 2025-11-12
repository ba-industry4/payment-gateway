package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.Webhook;
import com.ba.payment.gateway.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> findByWebhook(Webhook webhook);
    List<WebhookEvent> findByStatus(String status);
    List<WebhookEvent> findByWebhookAndStatus(Webhook webhook, String status);
}
