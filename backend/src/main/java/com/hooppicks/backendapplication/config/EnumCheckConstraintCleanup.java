package com.hooppicks.backendapplication.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate pose sur chaque colonne d'enum une contrainte CHECK figée à la
 * création de la table, et ddl-auto=update ne la met jamais à jour : toute
 * valeur d'enum ajoutée ensuite est refusée par la base à l'insertion. C'est
 * arrivé avec NotificationType.FAVORITE_TEAM, que les tests (mockés) ne
 * pouvaient pas voir. Faute d'outil de migration, on retire ces contraintes
 * au démarrage : la valeur est déjà garantie par l'enum Java.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnumCheckConstraintCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnumCheckConstraintCleanup.class);

    private static final List<String[]> CONSTRAINTS = List.of(
            new String[]{"app_notification", "app_notification_type_check"},
            new String[]{"wallet_transaction", "wallet_transaction_type_check"},
            new String[]{"bet", "bet_status_check"},
            new String[]{"match", "match_status_check"},
            new String[]{"match", "match_type_check"},
            new String[]{"duel", "duel_status_check"}
    );

    private final JdbcTemplate jdbcTemplate;

    public EnumCheckConstraintCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String[] c : CONSTRAINTS) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + c[0] + " DROP CONSTRAINT IF EXISTS " + c[1]);
            } catch (Exception e) {
                log.warn("Contrainte {} non retirée : {}", c[1], e.getMessage());
            }
        }
    }
}
