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
 * Colonnes agrandies après coup : ddl-auto=update crée une colonne mais ne change
 * jamais sa taille ensuite. Le message d'une notification (cloche) était resté à 255
 * caractères alors qu'une annonce de la console en accepte 280 : l'envoi d'une annonce
 * un peu longue finissait en erreur 500 (constaté en novembre 2026). Agrandir un
 * varchar sous Postgres ne réécrit pas la table.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ColumnWidening implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ColumnWidening.class);

    /** {table, colonne, taille voulue} : garder la même taille que le @Column de l'entité. */
    private static final List<String[]> COLUMNS = List.<String[]>of(
            new String[]{"app_notification", "message", "1000"}
    );

    private final JdbcTemplate jdbcTemplate;

    public ColumnWidening(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String[] c : COLUMNS) {
            try {
                Integer current = jdbcTemplate.queryForObject(
                        "SELECT character_maximum_length FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                        Integer.class, c[0], c[1]);
                int wanted = Integer.parseInt(c[2]);
                if (current != null && current < wanted) {
                    jdbcTemplate.execute("ALTER TABLE " + c[0] + " ALTER COLUMN " + c[1] + " TYPE varchar(" + wanted + ")");
                    log.info("Colonne {}.{} agrandie de {} à {} caractères", c[0], c[1], current, wanted);
                }
            } catch (Exception e) {
                log.warn("Colonne {}.{} non agrandie : {}", c[0], c[1], e.getMessage());
            }
        }
    }
}
