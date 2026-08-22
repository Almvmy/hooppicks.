package com.hooppicks.backendapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // Sans SecurityManager (le cas par défaut ici), la JVM met en cache une
        // résolution DNS réussie pour toute la durée de vie du process — si le
        // premier appel vers un domaine à répartition de charge (ESPN, Akamai)
        // tombe sur une IP mal routée depuis ce réseau, TOUS les appels suivants
        // restent bloqués dessus, retries compris. On force un TTL court pour
        // que les prochaines résolutions aient une chance de retomber ailleurs.
        java.security.Security.setProperty("networkaddress.cache.ttl", "10");
        SpringApplication.run(BackendApplication.class, args);
    }

}