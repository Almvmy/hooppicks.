package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.PlayerPropsService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.repository.BetSelectionRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

// Public comme /matches : les paris joueurs se consultent sans compte.
@RestController
public class PlayerPropsController {

    // Une soirée NBA chargée compte 15 matchs : de la marge, sans laisser
    // demander toute la saison d'un coup.
    static final int MAX_MATCHES = 20;

    private final PlayerPropsService playerPropsService;
    private final MatchRepository matchRepository;
    private final BetSelectionRepository betSelectionRepository;

    public PlayerPropsController(PlayerPropsService playerPropsService, MatchRepository matchRepository,
                                 BetSelectionRepository betSelectionRepository) {
        this.playerPropsService = playerPropsService;
        this.matchRepository = matchRepository;
        this.betSelectionRepository = betSelectionRepository;
    }

    /**
     * Ligne de pari joueur et part des joueurs HoopPicks sur « plus de »
     * (overPct, null tant que personne n'a choisi cette ligne ; picks = nombre de sélections).
     */
    public record PropWithPicks(String matchId, String market, String playerId, String playerName,
                                String teamAbbreviation, String headshotUrl, double line, double overOdds,
                                double underOdds, double average, String injuryStatus, Integer overPct, long picks) {}

    @GetMapping("/matches/{id}/props")
    public List<PropWithPicks> props(@PathVariable String id) {
        return matchRepository.findById(id).map(m -> withPicks(List.of(m))).orElse(List.of());
    }

    /** Paris joueurs de plusieurs matchs (une soirée NBA) : onglet « Paris joueurs ». */
    @GetMapping("/props")
    public ResponseEntity<List<PropWithPicks>> propsOf(@RequestParam List<String> ids) {
        if (ids.size() > MAX_MATCHES) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(withPicks(matchRepository.findAllById(new LinkedHashSet<>(ids))));
    }

    private List<PropWithPicks> withPicks(List<Match> matches) {
        if (matches.isEmpty()) return List.of();
        // matchId|market|playerId -> [plus, moins]
        Map<String, long[]> counts = new HashMap<>();
        for (Object[] row : betSelectionRepository.countPropPicks(matches.stream().map(Match::getId).toList())) {
            long[] c = counts.computeIfAbsent(row[0] + "|" + row[1] + "|" + row[2], k -> new long[2]);
            c["over".equals(row[3]) ? 0 : 1] += (Long) row[4];
        }
        List<PropWithPicks> out = new ArrayList<>();
        for (Match match : matches) {
            for (PlayerPropsService.PlayerProp p : playerPropsService.propsFor(match)) {
                long[] c = counts.getOrDefault(match.getId() + "|" + p.market() + "|" + p.playerId(), new long[2]);
                long total = c[0] + c[1];
                out.add(new PropWithPicks(match.getId(), p.market(), p.playerId(), p.playerName(), p.teamAbbreviation(),
                        p.headshotUrl(), p.line(), p.overOdds(), p.underOdds(), p.average(), p.injuryStatus(),
                        total == 0 ? null : (int) Math.round(c[0] * 100.0 / total), total));
            }
        }
        return out;
    }
}
