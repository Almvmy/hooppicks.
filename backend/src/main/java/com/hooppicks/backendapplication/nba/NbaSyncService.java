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
    private final NbaRateLimiter rateLimiter;

    public NbaSyncService(NbaApiClient nbaApiClient, TeamRepository teamRepository,
                          MatchRepository matchRepository, PlayerRepository playerRepository,
                          BetResolutionService betResolutionService, NbaRateLimiter rateLimiter) {
        this.nbaApiClient = nbaApiClient;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.betResolutionService = betResolutionService;
        this.rateLimiter = rateLimiter;
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
     * Recherche des joueurs chez balldontlie et met en cache le résultat en
     * base — appelée uniquement en cas de "cache miss" côté PlayerController,
     * jamais en boucle sur toutes les équipes (c'est justement l'ancienne
     * approche, par team_ids[], qui tronquait silencieusement les effectifs
     * de plus de 100 joueurs et faisait rater des joueurs comme Ja Morant).
     *
     * Protégée par NbaRateLimiter : si le quota est déjà consommé pour cette
     * minute, renvoie une liste vide plutôt que de risquer un 429 — l'appelant
     * retombe alors sur ce qu'il avait déjà en cache local.
     */
    public List<Player> searchAndCachePlayers(String query, String teamId) {
        if (!rateLimiter.tryAcquire()) {
            return List.of();
        }

        List<NbaPlayerDto> results = nbaApiClient.searchPlayers(query, teamId);
        List<Player> saved = new java.util.ArrayList<>();

        for (NbaPlayerDto p : results) {
            Player player = playerRepository.findByExternalId(p.id()).orElseGet(Player::new);
            player.setExternalId(p.id());
            player.setFirstName(p.firstName());
            player.setLastName(p.lastName());
            player.setPosition(p.position());
            player.setHeight(p.height());
            player.setWeight(p.weight());

            if (p.team() != null) {
                teamRepository.findById(String.valueOf(p.team().id())).ifPresent(player::setTeam);
            }

            playerRepository.save(player);
            saved.add(player);
        }

        return saved;
    }
}