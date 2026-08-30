package com.hooppicks.backendapplication;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// scheduler-enabled=false : sans ça, ce test à contexte Spring complet
// déclenche pour de vrai NbaSyncScheduler au démarrage (appels réseau
// balldontlie + ESPN) : lent et fragile, voir NbaSyncScheduler.
@SpringBootTest
@TestPropertySource(properties = "nba.sync.scheduler-enabled=false")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
