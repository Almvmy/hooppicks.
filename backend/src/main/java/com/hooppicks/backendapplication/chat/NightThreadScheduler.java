package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.dto.PlayerBoxScoreDto;
import com.hooppicks.backendapplication.live.LiveMatchService;
import com.hooppicks.backendapplication.monitor.JobMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lit le direct (mis en cache par LiveMatchService : pas d'appel de plus qu'un
 * joueur qui regarderait le match) puis écrit les messages automatiques.
 * Hors transaction pour le réseau, comme les résumés vidéo.
 */
@Component
@ConditionalOnProperty(name = "nba.sync.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class NightThreadScheduler {

    private static final Logger log = LoggerFactory.getLogger(NightThreadScheduler.class);

    private final LiveMatchService liveMatchService;
    private final NightThreadEvents events;
    private final JobMonitor jobMonitor;
    private int tick;

    public NightThreadScheduler(LiveMatchService liveMatchService, NightThreadEvents events, JobMonitor jobMonitor) {
        this.liveMatchService = liveMatchService;
        this.events = events;
        this.jobMonitor = jobMonitor;
    }

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 50 * 1000)
    public void run() {
        try {
            List<LiveMatchService.LiveStatusDto> board = liveMatchService.board();
            // La feuille de match demande un appel ESPN par match : une minute sur trois suffit.
            Map<String, List<PlayerBoxScoreDto>> boxes = new HashMap<>();
            if (tick++ % 3 == 0) {
                for (LiveMatchService.LiveStatusDto live : board) {
                    if (!"in".equals(live.state())) continue;
                    liveMatchService.detail(live.matchId()).ifPresent(d -> boxes.put(live.matchId(), d.players()));
                }
            }
            int posted = events.record(board, boxes, Instant.now());
            jobMonitor.ok("fil-soiree", posted + " message(s)");
        } catch (Exception e) {
            jobMonitor.failed("fil-soiree", e);
            log.warn("Messages automatiques du fil en échec", e);
        }
    }
}
