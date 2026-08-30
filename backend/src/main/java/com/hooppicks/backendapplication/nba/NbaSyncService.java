package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.Player;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.nba.dto.NbaGameDto;
import com.hooppicks.backendapplication.nba.dto.NbaPlayerDto;
import com.hooppicks.backendapplication.nba.dto.NbaTeamDto;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.PlayerRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class NbaSyncService {

    private static final Logger log = LoggerFactory.getLogger(NbaSyncService.class);

    // Empêche deux synchros de tourner en même temps (la planifiée toutes les
    // 5 min + un déclenchement manuel admin) : sans ça, les deux peuvent lire
    // le même match "previousStatus=SCHEDULED" avant que l'une des deux ne
    // l'ait sauvegardé en FINISHED, et appliquer deux fois l'Elo / les gains.
    private final ReentrantLock syncLock = new ReentrantLock();

    private final NbaApiClient nbaApiClient;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final BetRepository betRepository;
    private final NotificationRepository notificationRepository;

    private final BetResolutionService betResolutionService;
    private final NbaRateLimiter rateLimiter;
    private final EloService eloService;
    private final OddsService oddsService;

    public NbaSyncService(NbaApiClient nbaApiClient, TeamRepository teamRepository,
                          MatchRepository matchRepository, PlayerRepository playerRepository,
                          BetRepository betRepository, NotificationRepository notificationRepository,
                          BetResolutionService betResolutionService,
                          NbaRateLimiter rateLimiter, EloService eloService, OddsService oddsService) {
        this.nbaApiClient = nbaApiClient;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.betRepository = betRepository;
        this.notificationRepository = notificationRepository;
        this.betResolutionService = betResolutionService;
        this.rateLimiter = rateLimiter;
        this.eloService = eloService;
        this.oddsService = oddsService;
    }

    @Transactional
    public int syncTeams() {
        List<NbaTeamDto> teams = nbaApiClient.fetchAllTeams();
        int count = 0;
        for (NbaTeamDto t : teams) {
            // Ignore les franchises historiques disparues (ids 37+, ville vide) et les
            // clubs hors NBA (EuroLeague etc.) que l'API renvoie mélangés dans la même
            // liste : repérables par une conference qui n'est ni "East" ni "West".
            boolean isRealNbaConference = "East".equals(t.conference()) || "West".equals(t.conference());
            if (t.city() == null || t.city().isBlank() || !isRealNbaConference) continue;

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
        if (!syncLock.tryLock()) {
            log.info("Synchro déjà en cours, celle-ci est ignorée.");
            return new SyncResult(0, 0);
        }
        try {
            return doSyncGames(dates);
        } finally {
            syncLock.unlock();
        }
    }

    private SyncResult doSyncGames(List<LocalDate> dates) {
        List<NbaGameDto> games = nbaApiClient.fetchGamesForDates(dates);
        int count = 0;

        for (NbaGameDto g : games) {
            Match match = matchRepository.findByExternalId(g.id()).orElseGet(Match::new);
            boolean isNew = match.getId() == null;
            MatchStatus previousStatus = match.getStatus();

            Team home = teamRepository.findById(String.valueOf(g.homeTeam().id())).orElse(null);
            Team away = teamRepository.findById(String.valueOf(g.visitorTeam().id())).orElse(null);
            if (home == null || away == null) continue; // équipe historique non importée, on ignore ce match

            match.setExternalId(g.id());
            match.setHomeTeam(home);
            match.setAwayTeam(away);
            match.setDate(Instant.parse(g.datetime()));
            match.setHomeScore(g.homeTeamScore());
            match.setAwayScore(g.visitorTeamScore());
            MatchStatus newStatus = resolveStatus(g);
            match.setStatus(newStatus);

            // Cotes dynamiques (Elo) : recalculées à chaque synchro tant que le
            // match n'a pas commencé ET qu'aucun pari n'est encore posé dessus :
            // sinon la ligne resterait injuste pour qui a déjà parié dessus
            // (cf. BetResolutionService.evaluateSelection, qui relit spreadValue/
            // totalValue en direct sur le match au moment de la résolution).
            if (isNew || (newStatus == MatchStatus.SCHEDULED
                    && !betRepository.existsPendingBetForMatch(match.getId()))) {
                oddsService.applyOdds(match, home, away);
            }

            boolean justFinished = previousStatus != MatchStatus.FINISHED && newStatus == MatchStatus.FINISHED;
            boolean justWentLive = previousStatus == MatchStatus.SCHEDULED && newStatus == MatchStatus.LIVE;

            matchRepository.save(match);

            if (justFinished && match.getHomeScore() != null && match.getAwayScore() != null) {
                eloService.applyResult(home, away, match.getHomeScore(), match.getAwayScore());
                teamRepository.save(home);
                teamRepository.save(away);
            }

            if (justWentLive) {
                notifyMatchStarting(match, home, away);
            }

            count++;
        }

        // Dès que des matchs sont synchronisés (et donc potentiellement
        // passés à FINISHED), on résout tout de suite les paris en attente
        // qui les concernent : plus besoin d'appeler /admin/bets/resolve à la main.
        int resolved = betResolutionService.resolvePendingBets();

        return new SyncResult(count, resolved);
    }

    /**
     * Prévient les utilisateurs qui ont un pari en attente sur ce match qu'il
     * vient de démarrer : déclenché une seule fois, au moment où le statut
     * calculé passe de SCHEDULED à LIVE (cf. justWentLive dans syncGames).
     */
    private void notifyMatchStarting(Match match, Team home, Team away) {
        List<User> users = betRepository.findUsersWithPendingBetOnMatch(match.getId());
        String message = "Ça démarre : " + home.getName() + " vs " + away.getName() + " !";

        for (User user : users) {
            if (!user.isNotifyMatchStarting()) continue;

            AppNotification notification = new AppNotification();
            notification.setUser(user);
            notification.setType(NotificationType.MATCH_STARTING);
            notification.setMessage(message);
            notificationRepository.save(notification);
        }
    }

    private MatchStatus resolveStatus(NbaGameDto g) {
        if ("Final".equalsIgnoreCase(g.status())) return MatchStatus.FINISHED;
        Instant gameTime = Instant.parse(g.datetime());
        if (gameTime.isAfter(Instant.now())) return MatchStatus.SCHEDULED;
        return MatchStatus.LIVE; // heuristique : l'heure de coup d'envoi est passée et ce n'est pas encore "Final"
    }

    /**
     * Filet de secours pour PlayerController : n'est appelée que si la
     * recherche dans RosterPlayer (effectifs ESPN, cf. EspnRosterService)
     * ne renvoie rien : soit parce que le joueur n'y est vraiment pas, soit
     * parce qu'ESPN nous bloque de nouveau (Akamai) et que la synchro
     * n'a jamais tourné. Dans ce cas on retombe sur balldontlie en dernier
     * recours, moins riche (pas de photo, pas de stats) mais qui ne dépend
     * pas d'ESPN. Protégée par NbaRateLimiter : si le quota gratuit
     * balldontlie est déjà consommé pour cette minute, renvoie une liste
     * vide plutôt que de risquer un 429.
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