package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Player;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.nba.dto.NbaGameDto;
import com.hooppicks.backendapplication.nba.dto.NbaPlayerDto;
import com.hooppicks.backendapplication.nba.dto.NbaTeamDto;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.PlayerRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class NbaSyncService {

    private final NbaApiClient nbaApiClient;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    private final BetResolutionService betResolutionService;

    // Délai entre deux appels /players lors d'une synchro d'effectifs, pour
    // rester bien sous les 5 req/min du plan gratuit balldontlie MÊME si la
    // synchro des matchs (toutes les 5 min, 1 requête) tombe pile en même
    // temps. 20s => 3 req/min pour cette boucle + 1 req/min max pour les
    // matchs = 4 req/min max, marge de sécurité gardée sous la limite de 5.
    private static final long DELAY_BETWEEN_TEAM_REQUESTS_MS = 20_000;

    public NbaSyncService(NbaApiClient nbaApiClient, TeamRepository teamRepository,
                          MatchRepository matchRepository, PlayerRepository playerRepository,
                          BetResolutionService betResolutionService) {
        this.nbaApiClient = nbaApiClient;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.betResolutionService = betResolutionService;
    }


    @Transactional
    public int syncTeams() {
        List<NbaTeamDto> teams = nbaApiClient.fetchAllTeams();
        int count = 0;
        for (NbaTeamDto t : teams) {
            if (t.city() == null || t.city().isBlank()) continue; // ignore les franchises historiques disparues (ids 37+)

            String id = String.valueOf(t.id());
            Team team = teamRepository.findById(id).orElse(new Team());
            team.setId(id);
            team.setName(t.name());
            team.setAbbreviation(t.abbreviation());
            team.setConference("East".equals(t.conference()) ? "Est" : "Ouest");
            team.setDivision(t.division());
            teamRepository.save(team);
            count++;
        }
        return count;
    }

    /**
     * Résultat d'une synchro : nombre de matchs mis à jour, et nombre de
     * paris résolus dans la foulée (matchs qui viennent de passer à
     * "terminé" pendant cette synchro).
     */
    public record SyncResult(int gamesSynced, int betsResolved) {
    }

    @Transactional
    public SyncResult syncGames(List<LocalDate> dates) {
        List<NbaGameDto> games = nbaApiClient.fetchGamesForDates(dates);
        int count = 0;

        for (NbaGameDto g : games) {
            Match match = matchRepository.findByExternalId(g.id()).orElseGet(Match::new);
            boolean isNew = match.getId() == null;

            Team home = teamRepository.findById(String.valueOf(g.homeTeam().id())).orElse(null);
            Team away = teamRepository.findById(String.valueOf(g.visitorTeam().id())).orElse(null);
            if (home == null || away == null) continue; // équipe historique non importée, on ignore ce match

            match.setExternalId(g.id());
            match.setHomeTeam(home);
            match.setAwayTeam(away);
            match.setDate(Instant.parse(g.datetime()));
            match.setHomeScore(g.homeTeamScore());
            match.setAwayScore(g.visitorTeamScore());
            match.setStatus(resolveStatus(g));

            if (isNew) {
                assignPlaceholderOdds(match);
            }

            matchRepository.save(match);
            count++;
        }

        // Dès que des matchs sont synchronisés (et donc potentiellement
        // passés à FINISHED), on résout tout de suite les paris en attente
        // qui les concernent — plus besoin d'appeler /admin/bets/resolve à la main.
        int resolved = betResolutionService.resolvePendingBets();

        return new SyncResult(count, resolved);
    }

    private MatchStatus resolveStatus(NbaGameDto g) {
        if ("Final".equalsIgnoreCase(g.status())) return MatchStatus.FINISHED;
        Instant gameTime = Instant.parse(g.datetime());
        if (gameTime.isAfter(Instant.now())) return MatchStatus.SCHEDULED;
        return MatchStatus.LIVE; // heuristique : l'heure de coup d'envoi est passée et ce n'est pas encore "Final"
    }

    private void assignPlaceholderOdds(Match match) {
        // L'API gratuite ne fournit pas de vraies cotes — on en génère des plausibles,
        // une seule fois à la création du match (jamais modifiées ensuite, cf. décision "cotes fixes").
        match.setMoneylineHome(1.80);
        match.setMoneylineAway(2.00);
        match.setSpreadValue(-2.5);
        match.setSpreadOddsHome(1.9);
        match.setSpreadOddsAway(1.9);
        match.setTotalValue(220.5);
        match.setTotalOddsOver(1.9);
        match.setTotalOddsUnder(1.9);
    }

    /**
     * Version asynchrone de syncPlayers(), pensée pour être appelée depuis un
     * contrôleur qui doit répondre immédiatement (sans quoi le proxy Railway
     * coupe la connexion avant la fin des ~10 minutes de synchro et une partie
     * des équipes n'est jamais traitée). Grâce à @Async, cette méthode tourne
     * dans un thread séparé, indépendant du cycle de vie de la requête HTTP :
     * même si le client (curl, navigateur) se déconnecte, le job continue
     * jusqu'au bout côté serveur.
     */
    @Async
    public void syncPlayersAsync() {
        System.out.println("[NbaSyncService] Démarrage de la synchro des effectifs en tâche de fond...");
        try {
            int count = syncPlayers();
            System.out.println("[NbaSyncService] Synchro des effectifs terminée : " + count + " joueur(s) synchronisé(s).");
        } catch (Exception e) {
            System.out.println("[NbaSyncService] Échec de la synchro des effectifs : " + e.getMessage());
        }
    }

    /**
     * Synchronise l'effectif de chaque équipe déjà connue en base, une
     * franchise à la fois avec un délai entre les appels pour respecter le
     * quota gratuit de balldontlie. Volontairement déclenchée à la main
     * (endpoint admin) plutôt qu'automatique : les effectifs bougent très
     * rarement, pas besoin de la refaire tourner en continu.
     */
    public int syncPlayers() {
        List<Team> teams = teamRepository.findAll();
        int count = 0;

        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            List<NbaPlayerDto> players = nbaApiClient.fetchPlayersForTeam(team.getId());

            for (NbaPlayerDto p : players) {
                Player player = playerRepository.findByExternalId(p.id()).orElseGet(Player::new);
                player.setExternalId(p.id());
                player.setFirstName(p.firstName());
                player.setLastName(p.lastName());
                player.setPosition(p.position());
                player.setHeight(p.height());
                player.setWeight(p.weight());
                player.setTeam(team);
                playerRepository.save(player);
                count++;
            }

            boolean isLastTeam = i == teams.size() - 1;
            if (!isLastTeam) {
                sleepBetweenRequests();
            }
        }

        return count;
    }

    private void sleepBetweenRequests() {
        try {
            Thread.sleep(DELAY_BETWEEN_TEAM_REQUESTS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}