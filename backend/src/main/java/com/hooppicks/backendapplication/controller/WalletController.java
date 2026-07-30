package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.WalletDataDto;
import com.hooppicks.backendapplication.dto.WalletTransactionDto;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;
    private final SessionStore sessionStore;

    public WalletController(UserRepository userRepository, WalletTransactionRepository transactionRepository, SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.sessionStore = sessionStore;
    }

    @GetMapping
    public ResponseEntity<WalletDataDto> getWallet(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(new WalletDataDto(user.getWalletBalance()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransactionDto>> getTransactions(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        List<WalletTransactionDto> transactions = transactionRepository.findByUserIdOrderByDateDesc(userId)
                .stream().map(WalletTransactionDto::from).toList();
        return ResponseEntity.ok(transactions);
    }
}
