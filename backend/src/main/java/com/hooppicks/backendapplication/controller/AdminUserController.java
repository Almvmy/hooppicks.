package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Protégé par AdminAuthFilter (clé X-Admin-Key), comme le reste de /admin/**.
 * Sert à bootstrapper un compte admin sans avoir à ouvrir une connexion SQL
 * directe à la base à chaque nouvel environnement (local, Railway...).
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/promote")
    public ResponseEntity<?> promoteToAdmin(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("Aucun compte avec cet email.");
        }

        user.setAdmin(true);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
