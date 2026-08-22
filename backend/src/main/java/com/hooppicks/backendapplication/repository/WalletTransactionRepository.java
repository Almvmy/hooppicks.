package com.hooppicks.backendapplication.repository;


import com.hooppicks.backendapplication.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
    List<WalletTransaction> findByUserIdOrderByDateDesc(String userId);

    void deleteByUserId(String userId);
}
