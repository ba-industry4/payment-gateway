package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.Refund;
import com.ba.payment.gateway.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundId(String refundId);
    List<Refund> findByTransaction(Transaction transaction);
    List<Refund> findByStatus(String status);
}
