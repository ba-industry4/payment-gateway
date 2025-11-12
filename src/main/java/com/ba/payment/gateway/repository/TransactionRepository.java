package com.ba.payment.gateway.repository;

import com.ba.payment.gateway.entity.Merchant;
import com.ba.payment.gateway.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findByMerchant(Merchant merchant);
    List<Transaction> findByMerchantAndStatus(Merchant merchant, String status);
    List<Transaction> findByStatus(String status);
}
