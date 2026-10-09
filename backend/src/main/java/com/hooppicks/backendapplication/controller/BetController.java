package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.PlayerPropsService;
import com.hooppicks.backendapplication.boost.BoostService;
import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.dto.PlaceBetRequest;
import com.hooppicks.backendapplication.dto.PlacedBetDto;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.repository.MatchRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/bets")
public class BetController {

    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;
    private final SessionStore sessionStore;

    private final MatchRepository matchRepository;
    private final BankrollService bankrollService;
    private final PlayerPropsService playerPropsService;
    private final BoostService boostService;

    public BetController(BetRepository betRepository, UserRepository userRepository,
                         WalletTransactionRepository transactionRepository, SessionStore sessionStore,
                         MatchRepository matchRepository, BankrollService bankrollService,
                         PlayerPropsService playerPropsService, BoostService boostService) {
        this.playerPropsService = playerPropsService;
        this.boostService = boostService;
        this.betRepository = betRepository;
        this.bankrollService = bankrollService;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.sessionStore = sessionStore;
        this.matchRepository = matchRepository;

    }

    @GetMapping
    public ResponseEntity<List<PlacedBetDto>> getBets(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        List<PlacedBetDto> bets = betRepository.findByUserIdOrderByPlacedAtDesc(userId)
                .stream().map(PlacedBetDto::from).toList();
        return ResponseEntity.ok(bets);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> placeBet(@Valid @RequestBody PlaceBetRequest request, HttpServletRequest httpRequest) {
        String userId = sessionStore.getUserIdFromRequest(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();

        // Une seule sélection par match : deux issues sur le même match dans un
        // même ticket n'a pas de sens (parfois contradictoire) et gonflerait la
        // cote du parlay artificiellement.
        Set<String> matchIds = request.selections().stream()
                .map(PlaceBetRequest.SelectionInput::matchId)
                .collect(java.util.stream.Collectors.toSet());
        if (matchIds.size() != request.selections().size()) {
            return ResponseEntity.badRequest().body("Un même match ne peut apparaître qu'une seule fois dans le ticket.");
        }

        // Verrou pessimiste sur la ligne utilisateur : empêche deux requêtes
        // concurrentes (double-clic, deux onglets) de lire le même solde avant
        // que l'une des deux ne l'ait débité : la seconde attend la fin de la
        // transaction de la première et voit le solde déjà à jour.
        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();
        // Lundi 12h passé mais solde pas encore remis à niveau par le
        // scheduler : on le fait ici, sous le même verrou, avant de débiter.
        bankrollService.ensureCurrent(user);

        if (request.stake() > user.getWalletBalance()) {
            return ResponseEntity.badRequest().body("Solde insuffisant");
        }

        // On garde les matchs déjà chargés pour la validation : la cote appliquée au
        // pari vient de là (autoritaire, côté serveur), jamais de request.selections().odds()
        // : sinon un client pourrait renvoyer une ancienne cote plus favorable
        // maintenant que les cotes bougent (cf. OddsService).
        Map<String, Match> matchesById = new HashMap<>();
        // Pari joueur : la ligne recalculée maintenant fait foi (comme les cotes).
        Map<String, PlayerPropsService.PlayerProp> propsByMatch = new HashMap<>();
        for (PlaceBetRequest.SelectionInput s : request.selections()) {
            Match match = matchRepository.findById(s.matchId()).orElse(null);
            if (match == null) {
                return ResponseEntity.badRequest().body("Match introuvable.");
            }
            // L'heure de coup d'envoi fait foi, pas seulement le statut : celui-ci
            // n'est rafraîchi qu'à chaque synchro (5 min), et reste « à venir »
            // aussi longtemps que l'API de matchs ne répond plus. Sans ce
            // contrôle, on pouvait parier match commencé, voire score connu.
            boolean started = match.getDate() != null && !match.getDate().isAfter(java.time.Instant.now());
            if (match.getStatus() != MatchStatus.SCHEDULED || started) {
                return ResponseEntity.badRequest().body(
                        "Ce match n'est plus ouvert aux paris : " + matchLabel(match)
                );
            }
            // Seulement les matchs de la semaine de jeu en cours : un pari compte
            // pour la semaine où il est posé. Sinon on misait le solde de fin de
            // semaine (remis à 1000 lundi de toute façon) sur des matchs des
            // semaines suivantes, et le score d'une semaine close bougeait encore.
            java.time.Instant weekEnd = bankrollService.currentWeekStart().plus(java.time.Duration.ofDays(7));
            if (match.getDate() != null && !match.getDate().isBefore(weekEnd)) {
                return ResponseEntity.badRequest().body(
                        "Ce match ouvre aux paris lundi à 12h GMT, avec la semaine de jeu suivante : " + matchLabel(match)
                );
            }
            if (!isKnownSelection(s.market(), s.outcome())) {
                return ResponseEntity.badRequest().body("Sélection inconnue.");
            }
            if (PlayerPropsService.isPropMarket(s.market())) {
                PlayerPropsService.PlayerProp prop = s.playerId() == null ? null
                        : playerPropsService.find(match, s.market(), s.playerId()).orElse(null);
                if (prop == null) {
                    return ResponseEntity.badRequest().body("Ce pari joueur n'est plus proposé : " + matchLabel(match));
                }
                // Moyenne du joueur mise à jour entre l'affichage et la validation :
                // on ne pose pas en silence sur une autre ligne que celle qu'il a vue.
                if (s.line() == null || Math.abs(s.line() - prop.line()) > 0.01) {
                    return ResponseEntity.badRequest().body("La ligne de " + prop.playerName() + " est passée à "
                            + formatLine(prop.line()) + " " + PlayerPropsService.Stat.of(prop.market()).orElseThrow().unit
                            + " : vérifie ton ticket.");
                }
                propsByMatch.put(s.matchId(), prop);
            }
            matchesById.put(s.matchId(), match);
        }

        // Cote boostée du jour : pari « vainqueur » sur le match de la soirée,
        // ticket plafonné à BoostService.MAX_STAKE.
        String boostedMatchId = boostService.chosenMatchId().orElse(null);
        boolean boosted = request.selections().stream()
                .anyMatch(s -> "moneyline".equals(s.market()) && s.matchId().equals(boostedMatchId));
        if (boosted && request.stake() > BoostService.MAX_STAKE) {
            return ResponseEntity.badRequest().body("Cote boostée : mise limitée à " + BoostService.MAX_STAKE + " pts par ticket.");
        }

        double totalOdds = request.selections().stream()
                .mapToDouble(s -> oddsFor(s, matchesById, propsByMatch, boostedMatchId))
                .reduce(1, (a, b) -> a * b);
        int potentialPayout = (int) Math.min(Integer.MAX_VALUE, Math.round(request.stake() * totalOdds));

        Bet bet = new Bet();
        bet.setUser(user);
        bet.setStake(request.stake());
        bet.setTotalOdds(totalOdds);
        bet.setPotentialPayout(potentialPayout);
        bet.setStatus(BetStatus.PENDING);

        request.selections().forEach(s -> {
            BetSelection selection = new BetSelection();
            selection.setBet(bet);
            selection.setMatchId(s.matchId());
            // Libellés construits ici, jamais repris du client : ils sont
            // affichés dans l'historique et les notifications.
            Match m = matchesById.get(s.matchId());
            selection.setMatchLabel(matchLabel(m));
            selection.setMarket(s.market());
            selection.setOutcome(s.outcome());
            PlayerPropsService.PlayerProp prop = propsByMatch.get(s.matchId());
            if (prop != null) {
                selection.setPlayerId(prop.playerId());
                selection.setPlayerName(prop.playerName());
                selection.setPropLine(prop.line());
                selection.setLabel(propLabel(prop.playerName(), prop.market(), s.outcome(), prop.line()));
            } else {
                selection.setLabel(selectionLabel(m, s.market(), s.outcome()));
            }
            selection.setOdds(oddsFor(s, matchesById, propsByMatch, boostedMatchId));
            bet.getSelections().add(selection);
        });

        betRepository.save(bet);

        // Débit du solde + trace de la transaction : deux opérations liées, jamais l'une sans l'autre
        user.setWalletBalance(user.getWalletBalance() - request.stake());
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(TransactionType.BET_PLACED);
        tx.setAmount(-request.stake());
        tx.setDescription("Ticket engagé (" + request.selections().size() + " sélection(s))");
        transactionRepository.save(tx);

        return ResponseEntity.ok(PlacedBetDto.from(bet));
    }

    private static boolean isKnownSelection(String market, String outcome) {
        return switch (market == null ? "" : market) {
            case "moneyline", "spread" -> "home".equals(outcome) || "away".equals(outcome);
            case "total" -> "over".equals(outcome) || "under".equals(outcome);
            default -> PlayerPropsService.isPropMarket(market) && ("over".equals(outcome) || "under".equals(outcome));
        };
    }

    /** « Wizards vs Knicks » : extérieur puis domicile, comme dans l'app. */
    static String matchLabel(Match match) {
        return match.getAwayTeam().getName() + " vs " + match.getHomeTeam().getName();
    }

    /** « LAL (V) », « WAS +3,5 », « Plus de 222,5 » (spreadValue : ligne de l'équipe à domicile). */
    static String selectionLabel(Match match, String market, String outcome) {
        boolean home = "home".equals(outcome);
        String abbr = home ? match.getHomeTeam().getAbbreviation() : match.getAwayTeam().getAbbreviation();
        return switch (market) {
            case "moneyline" -> abbr + " (V)";
            case "spread" -> {
                double line = home ? match.getSpreadValue() : -match.getSpreadValue();
                yield abbr + " " + (line > 0 ? "+" : line < 0 ? "-" : "") + formatLine(Math.abs(line));
            }
            case "total" -> ("over".equals(outcome) ? "Plus de " : "Moins de ") + formatLine(match.getTotalValue());
            default -> throw new IllegalArgumentException("Marché inconnu : " + market);
        };
    }

    /** « J. Brunson · Plus de 26,5 pts », « N. Jokić · Moins de 12,5 rbd ». */
    static String propLabel(String playerName, String market, String outcome, double line) {
        String[] parts = playerName.split(" ", 2);
        String shortName = parts.length == 2 ? parts[0].charAt(0) + ". " + parts[1] : playerName;
        return shortName + " · " + ("over".equals(outcome) ? "Plus de " : "Moins de ") + formatLine(line) + " "
                + PlayerPropsService.Stat.of(market).map(st -> st.unit).orElse("pts");
    }

    private double oddsFor(PlaceBetRequest.SelectionInput s, Map<String, Match> matches,
                           Map<String, PlayerPropsService.PlayerProp> props, String boostedMatchId) {
        PlayerPropsService.PlayerProp prop = props.get(s.matchId());
        if (prop != null) return "over".equals(s.outcome()) ? prop.overOdds() : prop.underOdds();
        double odds = resolveOdds(matches.get(s.matchId()), s.market(), s.outcome());
        return "moneyline".equals(s.market()) && s.matchId().equals(boostedMatchId) ? BoostService.boosted(odds) : odds;
    }

    private static String formatLine(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value).replace('.', ',');
    }

    private double resolveOdds(Match match, String market, String outcome) {
        return switch (market) {
            case "moneyline" -> "home".equals(outcome) ? match.getMoneylineHome() : match.getMoneylineAway();
            case "spread" -> "home".equals(outcome) ? match.getSpreadOddsHome() : match.getSpreadOddsAway();
            case "total" -> "over".equals(outcome) ? match.getTotalOddsOver() : match.getTotalOddsUnder();
            default -> throw new IllegalArgumentException("Marché inconnu : " + market);
        };
    }
}