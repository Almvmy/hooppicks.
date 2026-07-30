package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/bets")
public class BetAdminController {

    private final BetResolutionService betResolutionService;

    public BetAdminController(BetResolutionService betResolutionService) {
        this.betResolutionService = betResolutionService;
    }

    @PostMapping("/resolve")
    public Map<String, Integer> resolve() {
        return Map.of("resolved", betResolutionService.resolvePendingBets());
    }
}