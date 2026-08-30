package com.hooppicks.backendapplication.controller;

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

    public BetController(BetRepository betRepository, UserRepository userRepository,
                         WalletTransactionRepository transactionRepository, SessionStore sessionStore,
                         MatchRepository matchRepository) {
        this.betRepository = betRepository;
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

        if (request.stake() > user.getWalletBalance()) {
            return ResponseEntity.badRequest().body("Solde insuffisant");
        }

        // On garde les matchs déjà chargés pour la validation : la cote appliquée au
        // pari vient de là (autoritaire, côté serveur), jamais de request.selections().odds()
        // : sinon un client pourrait renvoyer une ancienne cote plus favorable
        // maintenant que les cotes bougent (cf. OddsService).
        Map<String, Match> matchesById = new HashMap<>();
        for (PlaceBetRequest.SelectionInput s : request.selections()) {
            Match match = matchRepository.findById(s.matchId()).orElse(null);
            if (match == null) {
                return ResponseEntity.badRequest().body("Match introuvable.");
            }
            if (match.getStatus() != MatchStatus.SCHEDULED) {
                return ResponseEntity.badRequest().body(
                        "Ce match n'est plus ouvert aux paris : " + match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName()
                );
            }
            matchesById.put(s.matchId(), match);
        }

        double totalOdds = request.selections().stream()
                .mapToDouble(s -> resolveOdds(matchesById.get(s.matchId()), s.market(), s.outcome()))
                .reduce(1, (a, b) -> a * b);
        int potentialPayout = (int) Math.round(request.stake() * totalOdds);

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
            selection.setMatchLabel(s.matchLabel());
            selection.setMarket(s.market());
            selection.setOutcome(s.outcome());
            selection.setLabel(s.label());
            selection.setOdds(resolveOdds(matchesById.get(s.matchId()), s.market(), s.outcome()));
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

    private double resolveOdds(Match match, String market, String outcome) {
        return switch (market) {
            case "moneyline" -> "home".equals(outcome) ? match.getMoneylineHome() : match.getMoneylineAway();
            case "spread" -> "home".equals(outcome) ? match.getSpreadOddsHome() : match.getSpreadOddsAway();
            case "total" -> "over".equals(outcome) ? match.getTotalOddsOver() : match.getTotalOddsUnder();
            default -> throw new IllegalArgumentException("Marché inconnu : " + market);
        };
    }
}