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
import org.springframework.web.bind.annotation.*;

import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.repository.MatchRepository;

import java.util.List;

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
    public ResponseEntity<?> placeBet(@Valid @RequestBody PlaceBetRequest request, HttpServletRequest httpRequest) {
        String userId = sessionStore.getUserIdFromRequest(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        if (request.stake() > user.getWalletBalance()) {
            return ResponseEntity.badRequest().body("Solde insuffisant");
        }

        for (PlaceBetRequest.SelectionInput s : request.selections()) {
            var match = matchRepository.findById(s.matchId()).orElse(null);
            if (match == null) {
                return ResponseEntity.badRequest().body("Match introuvable.");
            }
            if (match.getStatus() != MatchStatus.SCHEDULED) {
                return ResponseEntity.badRequest().body(
                        "Ce match n'est plus ouvert aux paris : " + match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName()
                );
            }
        }
        double totalOdds = request.selections().stream().mapToDouble(PlaceBetRequest.SelectionInput::odds).reduce(1, (a, b) -> a * b);
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
            selection.setOdds(s.odds());
            bet.getSelections().add(selection);
        });

        betRepository.save(bet);

        // Débit du solde + trace de la transaction — deux opérations liées, jamais l'une sans l'autre
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
}