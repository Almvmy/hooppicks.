package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.RosterPlayerDto;
import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint public (comme /matches et /leaderboard) : pas besoin de session
 * pour consulter des infos joueurs, ce n'est pas une donnée sensible.
 *
 * Cherche d'abord dans RosterPlayer (effectifs actuels, synchronisés depuis
 * ESPN — cf. EspnRosterService) : contrairement à l'ancienne version qui
 * cherchait chez balldontlie, dont le free tier ne distingue pas les joueurs
 * actifs des retraités (une recherche "LeBron" y remontait des homonymes
 * vieux de plusieurs décennies), RosterPlayer ne contient que l'effectif du
 * moment.
 *
 * Si RosterPlayer est totalement vide, on retombe sur balldontlie en dernier
 * recours (cf. NbaSyncService.searchAndCachePlayers) : ESPN nous bloque
 * (Akamai) et la synchro n'a jamais tourné, mieux vaut un résultat incomplet
 * (balldontlie n'a ni photo ni stats saison) qu'une recherche qui ne renvoie
 * jamais rien tant qu'ESPN est indisponible. Important : ce n'est PAS "cette
 * recherche précise n'a rien trouvé" qui déclenche le repli — sinon chercher
 * un joueur retraité redonnerait exactement le bug qu'on corrige ici. Le
 * signal, c'est l'absence totale de données ESPN, vérifié une fois via
 * count() plutôt qu'à chaque recherche sans résultat.
 */
@RestController
@RequestMapping("/players")
public class PlayerController {

    private static final int MIN_SEARCH_LENGTH = 2;

    private final RosterPlayerRepository rosterPlayerRepository;
    private final NbaSyncService nbaSyncService;

    public PlayerController(RosterPlayerRepository rosterPlayerRepository, NbaSyncService nbaSyncService) {
        this.rosterPlayerRepository = rosterPlayerRepository;
        this.nbaSyncService = nbaSyncService;
    }

    @GetMapping
    public List<RosterPlayerDto> searchPlayers(
            @RequestParam String search,
            @RequestParam(required = false) String teamId
    ) {
        if (search == null || search.trim().length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        // RosterPlayer vide = ESPN n'a jamais réussi à synchroniser (Akamai,
        // panne...), pas juste "cette recherche n'a rien donné" — sinon
        // chercher un joueur retraité retomberait sur balldontlie et
        // réintroduirait le bug qu'on corrige ici.
        if (rosterPlayerRepository.count() == 0) {
            return nbaSyncService.searchAndCachePlayers(search, teamId).stream()
                    .map(RosterPlayerDto::fromBalldontlie)
                    .toList();
        }

        List<RosterPlayer> local = rosterPlayerRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByLastNameAsc(search, search);

        if (teamId != null && !teamId.isBlank()) {
            local = local.stream()
                    .filter(p -> p.getTeam() != null && teamId.equals(p.getTeam().getId()))
                    .toList();
        }

        return local.stream().map(RosterPlayerDto::from).toList();
    }
}
