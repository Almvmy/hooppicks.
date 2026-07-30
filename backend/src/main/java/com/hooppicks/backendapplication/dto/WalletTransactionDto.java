package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.WalletTransaction;

public record WalletTransactionDto(
        String id,
        String type,
        int amount,
        String description,
        String date
) {
    public static WalletTransactionDto from(WalletTransaction tx) {
        return new WalletTransactionDto(
                tx.getId(),
                tx.getType().name().toLowerCase(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getDate().toString()
        );
    }
}