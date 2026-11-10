package com.hooppicks.backendapplication.engagement;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * La semaine de jeu qui vient de finir d'un joueur, pour la story du lundi et
 * sa carte à partager. Mêmes points que le classement de la semaine (bénéfice
 * net, jamais moins de 0). Vide si le joueur n'a aucun ticket réglé cette
 * semaine-là : pas de story pour dire « tu n'as pas joué ».
 */
@Service
public class WeeklyRecapService {

    public record BestTicket(String betId, String labels, double odds, long profit) {}

    public record LeagueLine(String id, String name, int rank, int members, boolean champion) {}

    /** result : won, lost ou tie. */
    public record DuelLine(String opponent, long mine, long theirs, String result) {}

    public record Recap(LocalDate week, long points, long net, int tickets, int won, Integer rank, int players,
                        boolean champion, BestTicket best, List<LeagueLine> leagues, List<DuelLine> duels) {}

    private final BetRepository betRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final DuelRepository duelRepository;
    private final WeeklyTitleRepository titleRepository;
    private final Clock clock;

    @Autowired
    public WeeklyRecapService(BetRepository betRepository, LeagueMembershipRepository membershipRepository,
                              DuelRepository duelRepository, WeeklyTitleRepository titleRepository) {
        this(betRepository, membershipRepository, duelRepository, titleRepository, Clock.system(BankrollService.ZONE));
    }

    WeeklyRecapService(BetRepository betRepository, LeagueMembershipRepository membershipRepository,
                       DuelRepository duelRepository, WeeklyTitleRepository titleRepository, Clock clock) {
        this.betRepository = betRepository;
        this.membershipRepository = membershipRepository;
        this.duelRepository = duelRepository;
        this.titleRepository = titleRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<Recap> lastWeek(String userId) {
        LocalDate week = BankrollService.weekOf(clock.instant()).minusWeeks(1);
        Instant start = BankrollService.startOf(week);
        Instant end = start.plus(Duration.ofDays(7));

        List<Bet> settled = betRepository.findByUserIdOrderByPlacedAtDesc(userId).stream()
                .filter(b -> !b.getPlacedAt().isBefore(start) && b.getPlacedAt().isBefore(end))
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST || b.getStatus() == BetStatus.CASHED_OUT)
                .toList();
        if (settled.isEmpty()) return Optional.empty();

        // Encaissé : la valeur encaissée − mise, comme au classement.
        long net = settled.stream().mapToLong(b -> b.getStatus() == BetStatus.LOST ? -b.getStake() : b.getPotentialPayout() - b.getStake()).sum();
        int won = (int) settled.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
        BestTicket best = settled.stream().filter(b -> b.getStatus() == BetStatus.WON)
                .max(Comparator.comparingInt(b -> b.getPotentialPayout() - b.getStake()))
                .map(b -> new BestTicket(b.getId(),
                        b.getSelections().stream().map(BetSelection::getLabel).collect(Collectors.joining(" + ")),
                        b.getTotalOdds(), b.getPotentialPayout() - b.getStake()))
                .orElse(null);

        Map<String, Long> scores = new HashMap<>();
        for (Object[] row : betRepository.getAllWeekScores(week.atStartOfDay())) {
            scores.put((String) row[0], ((Number) row[1]).longValue());
        }
        long mine = scores.getOrDefault(userId, 0L);
        Integer rank = scores.containsKey(userId) ? rankOf(mine, scores.values()) : null;

        Set<String> titledLeagues = new HashSet<>();
        boolean champion = false;
        for (WeeklyTitle t : titleRepository.findByUserIdOrderByWeekDesc(userId)) {
            if (!week.equals(t.getWeek())) continue;
            if (t.getLeagueId() == null) champion = true;
            else titledLeagues.add(t.getLeagueId());
        }

        List<LeagueLine> leagues = new ArrayList<>();
        for (LeagueMembership m : membershipRepository.findByUserId(userId)) {
            League league = m.getLeague();
            List<Long> members = membershipRepository.findByLeagueId(league.getId()).stream()
                    .map(o -> scores.getOrDefault(o.getUser().getId(), 0L)).toList();
            leagues.add(new LeagueLine(league.getId(), league.getName(), rankOf(mine, members), members.size(),
                    titledLeagues.contains(league.getId())));
        }

        List<DuelLine> duels = duelRepository.findAllForUser(userId).stream()
                .filter(d -> week.equals(d.getWeek()) && d.getStatus() == DuelStatus.FINISHED)
                .map(d -> duelLine(d, userId))
                .toList();

        return Optional.of(new Recap(week, Math.max(0, net), net, settled.size(), won, rank, scores.size(), champion, best, leagues, duels));
    }

    // Ex æquo : même place, comme au classement.
    private static int rankOf(long mine, Collection<Long> all) {
        return 1 + (int) all.stream().filter(s -> s > mine).count();
    }

    private static DuelLine duelLine(Duel d, String userId) {
        boolean challenger = d.getChallenger().getId().equals(userId);
        long mine = orZero(challenger ? d.getChallengerPoints() : d.getOpponentPoints());
        long theirs = orZero(challenger ? d.getOpponentPoints() : d.getChallengerPoints());
        String opponent = (challenger ? d.getOpponent() : d.getChallenger()).getUsername();
        String result = d.getWinnerId() == null ? "tie" : d.getWinnerId().equals(userId) ? "won" : "lost";
        return new DuelLine(opponent, mine, theirs, result);
    }

    private static long orZero(Long v) {
        return v == null ? 0 : v;
    }
}
