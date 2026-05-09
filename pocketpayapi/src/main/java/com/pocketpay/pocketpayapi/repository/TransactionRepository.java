package com.pocketpay.pocketpayapi.repository;

import com.pocketpay.pocketpayapi.entity.Transaction;
// import com.pocketpay.pocketpayapi.enums.TransactionStatus;
import com.pocketpay.pocketpayapi.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySenderWallet_IdOrReceiverWallet_Id(Long senderId, Long receiverId, Pageable pageable);
    Page<Transaction> findBySenderWallet_IdOrReceiverWallet_IdAndType(Long senderId, Long receiverId, TransactionType type, Pageable pageable);
    Optional<Transaction> findByReference(String reference);
}