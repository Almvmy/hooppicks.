package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.PickPercentagesService;
import com.hooppicks.backendapplication.dto.MatchDto;
import com.hooppicks.backendapplication.dto.PickPercentagesDto;
import com.hooppicks.backendapplication.dto.PlayerBoxScoreDto;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.live.LiveMatchService;
import com.hooppicks.backendapplication.live.MatchPreviewService;
import org.springframework.http.ResponseEntity;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchRepository matchRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;
    private final PickPercentagesService pickPercentagesService;
    private final LiveMatchService liveMatchService;
    private final MatchPreviewService matchPreviewService;

    public MatchController(MatchRepository matchRepository, PlayerMatchStatRepository playerMatchStatRepository,
                            PickPercentagesService pickPercentagesService, LiveMatchService liveMatchService,
                            MatchPreviewService matchPreviewService) {
        this.liveMatchService = liveMatchService;
        this.matchPreviewService = matchPreviewService;
        this.matchRepository = matchRepository;
        this.playerMatchStatRepository = playerMatchStatRepository;
        this.pickPercentagesService = pickPercentagesService;
    }

    static final int MAX_IDS = 200;

    /**
     * Sans filtre, toute la base : une réponse qui grossit chaque semaine de
     * la saison (et le calcul des pourcentages de choix avec). L'app demande
     * donc une fenêtre de dates (from/to, jours UTC inclus), la saison d'une
     * équipe (teamId) ou des matchs précis (ids, ceux de ses paris). Le cas
     * sans filtre reste pour les onglets ouverts avant la mise à jour.
     */
    @GetMapping
    public List<MatchDto> getAllMatches(@RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(required = false) String teamId,
                                        @RequestParam(required = false) String ids) {
        List<Match> matches;
        if (ids != null) {
            List<String> wanted = Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .distinct().limit(MAX_IDS).toList();
            matches = matchRepository.findAllById(wanted);
        } else if (teamId != null) {
            matches = matchRepository.findByTeamId(teamId);
        } else if (from != null || to != null) {
            Instant start = from == null ? Instant.EPOCH : LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = to == null ? Instant.parse("9999-12-31T00:00:00Z")
                    : LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            matches = matchRepository.findByDateBetween(start, end);
        } else {
            matches = matchRepository.findAll();
        }
        Map<String, PickPercentagesDto> percentages = pickPercentagesService.forMatches(
                matches.stream().map(Match::getId).toList());
        return matches.stream()
                .map(m -> MatchDto.from(m, percentages.get(m.getId())))
                .toList();
    }

    // Direct (ESPN, affichage seulement) : liste vide ou 204 si ESPN ne répond
    // plus, l'app garde alors le score et le statut de la synchro.
    @GetMapping("/live")
    public List<LiveMatchService.LiveStatusDto> getLiveBoard() {
        return liveMatchService.board();
    }

    @GetMapping("/{id}/live")
    public ResponseEntity<LiveMatchService.LiveMatchDto> getLive(@PathVariable String id) {
        return liveMatchService.detail(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    // Avant-match : forme des deux équipes et dernières confrontations.
    @GetMapping("/{id}/preview")
    public ResponseEntity<MatchPreviewService.PreviewDto> getPreview(@PathVariable String id) {
        return matchPreviewService.preview(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public MatchDto getMatchById(@PathVariable String id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match introuvable : " + id));
        PickPercentagesDto percentages = pickPercentagesService.forMatches(List.of(id)).get(id);
        return MatchDto.from(match, percentages);
    }

    // Vide (jamais 404) si la feuille de match n'a pas encore été importée :
    // match pas encore terminé, ou pas encore traité par EspnStatsService.
    @GetMapping("/{id}/boxscore")
    public List<PlayerBoxScoreDto> getBoxScore(@PathVariable String id) {
        return playerMatchStatRepository.findByMatchIdOrderByPointsDesc(id).stream()
                .map(PlayerBoxScoreDto::from)
                .toList();
    }
}
