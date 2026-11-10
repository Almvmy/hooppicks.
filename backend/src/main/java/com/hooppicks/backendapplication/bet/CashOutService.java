package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.bet.CashOutPricing.Leg;
import com.hooppicks.backendapplication.bet.CashOutPricing.Live;
import com.hooppicks.backendapplication.bet.CashOutPricing.Quote;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.live.LiveMatchService;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Encaisser un ticket en attente avant la fin (prix : `CashOutPricing`). Rien ne revient
 * sur le solde (choix du propriétaire, comme un ticket gagnant) : la mise est sortie à la
 * pose, seul le bénéfice (valeur encaissée − mise) compte au classement de la semaine du
 * ticket. Sinon on miserait plus que ses 1000 pts dans la semaine.
 *
 * Le direct est lu avant de prendre le verrou (appels réseau), puis tout le reste est
 * revérifié sous le verrou de la synchro et la ligne du ticket verrouillée, comme les
 * actions admin sur les points : un encaissement ne croise jamais la résolution du même
 * ticket (sinon il serait payé deux fois). Synchro en cours : refus, on réessaie.
 */
@Service
public class CashOutService {

    private static final Logger log = LoggerFactory.getLogger(CashOutService.class);

    public record CashOutQuote(String betId, int value, boolean available, String suspended) {}

    public static class CashOutException extends RuntimeException {
        private final int status;
        private final Integer value;

        public CashOutException(int status, String message, Integer value) {
            super(message);
            this.status = status;
            this.value = value;
        }

        public int status() {
            return status;
        }

        /** La valeur du moment quand elle a changé depuis l'affichage. */
        public Integer value() {
            return value;
        }
    }

    private final BetRepository betRepository;
    private final MatchRepository matchRepository;
    private final LiveMatchService liveMatchService;
    private final PlayerPropsService playerPropsService;
    private final NbaSyncService nbaSyncService;
    private final TransactionOperations transactions;
    private final Clock clock;

    @Autowired
    public CashOutService(BetRepository betRepository, MatchRepository matchRepository, LiveMatchService liveMatchService,
                          PlayerPropsService playerPropsService, NbaSyncService nbaSyncService, TransactionOperations transactions) {
        this(betRepository, matchRepository, liveMatchService, playerPropsService, nbaSyncService, transactions, Clock.systemUTC());
    }

    CashOutService(BetRepository betRepository, MatchRepository matchRepository, LiveMatchService liveMatchService,
                   PlayerPropsService playerPropsService, NbaSyncService nbaSyncService, TransactionOperations transactions, Clock clock) {
        this.betRepository = betRepository;
        this.matchRepository = matchRepository;
        this.liveMatchService = liveMatchService;
        this.playerPropsService = playerPropsService;
        this.nbaSyncService = nbaSyncService;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** La valeur de chaque ticket en attente du joueur qui peut encore s'encaisser (ou qui le pourra). */
    public List<CashOutQuote> quotes(String userId) {
        List<Bet> pending = transactions.execute(status -> betRepository.findByUserIdOrderByPlacedAtDesc(userId).stream()
                .filter(b -> b.getStatus() == BetStatus.PENDING)
                .peek(b -> b.getSelections().size())
                .toList());
        if (pending == null || pending.isEmpty()) return List.of();
        Map<String, Match> matches = matches(pending.stream().flatMap(b -> b.getSelections().stream()).map(BetSelection::getMatchId).toList());
        Map<String, Live> live = live(matches.values());
        List<CashOutQuote> quotes = new ArrayList<>();
        for (Bet bet : pending) {
            price(bet, matches, live).ifPresent(q -> quotes.add(new CashOutQuote(bet.getId(), q.value(), q.available(), q.suspended())));
        }
        return quotes;
    }

    /** Encaisse si la valeur du moment est au moins celle que le joueur a vue (`expected`). */
    public Bet cashOut(String userId, String betId, int expected) {
        Bet preview = transactions.execute(status -> betRepository.findById(betId)
                .filter(b -> b.getUser().getId().equals(userId))
                .map(b -> {
                    b.getSelections().size();
                    return b;
                })
                .orElse(null));
        if (preview == null) throw new CashOutException(404, "Ticket introuvable.", null);
        if (preview.getStatus() != BetStatus.PENDING) throw new CashOutException(409, "Ce ticket est déjà réglé.", null);
        Map<String, Live> live = live(matches(preview.getSelections().stream().map(BetSelection::getMatchId).toList()).values());

        Bet[] done = new Bet[1];
        boolean ran = nbaSyncService.tryRunExclusive(() -> done[0] = transactions.execute(status -> settle(userId, betId, expected, live)));
        if (!ran) throw new CashOutException(409, "Les résultats sont en cours de mise à jour : réessaie dans quelques secondes.", null);
        return done[0];
    }

    private Bet settle(String userId, String betId, int expected, Map<String, Live> live) {
        Bet bet = betRepository.findByIdForUpdate(betId).filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> new CashOutException(404, "Ticket introuvable.", null));
        if (bet.getStatus() != BetStatus.PENDING) throw new CashOutException(409, "Ce ticket est déjà réglé.", null);
        Map<String, Match> matches = matches(bet.getSelections().stream().map(BetSelection::getMatchId).toList());
        Quote quote = price(bet, matches, live)
                .orElseThrow(() -> new CashOutException(409, "Ce ticket ne peut plus être encaissé.", null));
        if (quote.suspended() != null) throw new CashOutException(409, "Encaissement suspendu : " + quote.suspended().toLowerCase() + ".", null);
        if (quote.value() <= 0) throw new CashOutException(409, "Ce ticket ne vaut plus rien à encaisser.", null);
        if (quote.value() < expected) throw new CashOutException(409, "La valeur a changé : " + quote.value() + " pts.", quote.value());

        bet.setCashedOutFrom(bet.getPotentialPayout());
        bet.setPotentialPayout(quote.value());
        bet.setStatus(BetStatus.CASHED_OUT);
        bet.setResolvedAt(clock.instant());
        betRepository.save(bet);
        log.info("Ticket {} encaissé : {} pts (gain prévu {})", bet.getId(), quote.value(), bet.getCashedOutFrom());
        return bet;
    }

    private Optional<Quote> price(Bet bet, Map<String, Match> matches, Map<String, Live> live) {
        Instant now = clock.instant();
        List<Leg> legs = bet.getSelections().stream().map(s -> {
            Match m = matches.get(s.getMatchId());
            Optional<LegEvaluator.LegResult> prop = Optional.empty();
            if (m != null && m.getStatus() == MatchStatus.FINISHED && PlayerPropsService.isProp(s)) {
                prop = playerPropsService.evaluate(s, m, now);
            }
            return new Leg(s, m, live.get(s.getMatchId()), prop);
        }).toList();
        return CashOutPricing.price(bet.getPotentialPayout(), legs);
    }

    private Map<String, Match> matches(Collection<String> ids) {
        return matchRepository.findAllById(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Match::getId, Function.identity()));
    }

    // Seulement les matchs commencés ou sur le point de l'être : pas d'appel ESPN pour un match de la semaine prochaine.
    private Map<String, Live> live(Collection<Match> matches) {
        Instant now = clock.instant();
        Map<String, Live> live = new HashMap<>();
        for (Match m : matches) {
            if (m.getStatus() == MatchStatus.FINISHED) continue;
            boolean due = m.getStatus() == MatchStatus.LIVE || (m.getDate() != null && !m.getDate().isAfter(now));
            if (!due) continue;
            liveMatchService.pricing(m).ifPresent(l -> live.put(m.getId(), l));
        }
        return live;
    }
}
