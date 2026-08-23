package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.LeaderboardEntryDto;
import com.hooppicks.backendapplication.repository.BetRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final BetRepository betRepository;

    public LeaderboardController(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @GetMapping
    public List<LeaderboardEntryDto> getLeaderboard() {
        List<Object[]> rows = betRepository.getLeaderboardRaw();
        List<LeaderboardEntryDto> result = new ArrayList<>();

        int rank = 1;
        for (Object[] row : rows) {
            String username = (String) row[1];
            long points = (Long) row[2];
            long totalBets = (Long) row[3];
            long wonBets = (Long) row[4];
            int winRate = totalBets == 0 ? 0 : (int) Math.round((wonBets * 100.0) / totalBets);

            result.add(new LeaderboardEntryDto(rank++, username, (int) points, winRate, (int) totalBets,
                    (Integer) row[5], (String) row[6], (String) row[7], (String) row[8]));
        }
        return result;
    }
}
