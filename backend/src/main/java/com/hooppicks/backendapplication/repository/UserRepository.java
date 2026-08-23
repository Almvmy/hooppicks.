package com.hooppicks.backendapplication.repository;


import com.hooppicks.backendapplication.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    // SELECT ... FOR UPDATE : verrouille la ligne le temps de la transaction
    // pour empêcher deux requêtes concurrentes (double-clic, deux onglets) de
    // lire le même solde avant que l'une des deux ne l'ait débité.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") String id);
}