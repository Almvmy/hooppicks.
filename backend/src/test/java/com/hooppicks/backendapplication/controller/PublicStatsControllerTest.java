package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.PublicStatsDto;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicStatsControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BetRepository betRepository;
    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private PublicStatsController controller;

    @Test
    void ne_compte_que_les_matchs_termines() {
        when(userRepository.count()).thenReturn(42L);
        when(betRepository.count()).thenReturn(310L);
        when(leagueRepository.count()).thenReturn(7L);
        when(matchRepository.countByStatus(MatchStatus.FINISHED)).thenReturn(95L);

        PublicStatsDto stats = controller.getPublicStats();

        assertThat(stats).isEqualTo(new PublicStatsDto(42L, 310L, 7L, 95L));
    }
}
