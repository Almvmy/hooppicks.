package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.CreateLeagueRequest;
import com.hooppicks.backendapplication.dto.JoinLeagueRequest;
import com.hooppicks.backendapplication.dto.LeaderboardEntryDto;
import com.hooppicks.backendapplication.dto.LeagueActivityDto;
import com.hooppicks.backendapplication.dto.LeagueDto;
import com.hooppicks.backendapplication.dto.LeagueMemberDto;
import com.hooppicks.backendapplication.dto.LeaguePreviewDto;
import com.hooppicks.backendapplication.league.LeagueService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leagues")
public class LeagueController {

    private final LeagueService leagueService;
    private final SessionStore sessionStore;

    public LeagueController(LeagueService leagueService, SessionStore sessionStore) {
        this.leagueService = leagueService;
        this.sessionStore = sessionStore;
    }

    @GetMapping
    public ResponseEntity<?> getMyLeagues(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(leagueService.getUserLeagues(userId));
    }

    @PostMapping
    public ResponseEntity<?> createLeague(@Valid @RequestBody CreateLeagueRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(leagueService.createLeague(userId, body.name()));
    }

    @GetMapping("/preview/{code}")
    public ResponseEntity<?> previewLeague(@PathVariable String code, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            LeaguePreviewDto preview = leagueService.previewByCode(code);
            return ResponseEntity.ok(preview);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getLeagueMembers(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            List<LeagueMemberDto> members = leagueService.getMembers(id, userId);
            return ResponseEntity.ok(members);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<?> getLeagueActivity(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            List<LeagueActivityDto> activity = leagueService.getRecentActivity(id, userId);
            return ResponseEntity.ok(activity);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinLeague(@Valid @RequestBody JoinLeagueRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            LeagueDto league = leagueService.joinLeague(userId, body.inviteCode());
            return ResponseEntity.ok(league);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<?> getLeagueLeaderboard(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        try {
            List<LeaderboardEntryDto> leaderboard = leagueService.getLeagueLeaderboard(id, userId);
            return ResponseEntity.ok(leaderboard);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveLeague(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        leagueService.leaveLeague(id, userId);
        return ResponseEntity.noContent().build();
    }
}
