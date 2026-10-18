package com.hooppicks.backendapplication.boost;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.DailyBoostRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BoostServiceTest {

    private final DailyBoostRepository boostRepository = mock(DailyBoostRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    // Jeudi 22 octobre 2026, 15h GMT : soirée du 22.
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), ZoneOffset.UTC);
    private final BoostService service = new BoostService(boostRepository, matchRepository, clock);

    private static Team team(String abbr, int wins, int losses) {
        Team t = new Team();
        t.setAbbreviation(abbr);
        t.setWins(wins);
        t.setLosses(losses);
        return t;
    }

    private static Match match(String id, Team home, Team away, double mlHome, double mlAway, MatchType type) {
        Match m = new Match();
        m.setId(id);
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        m.setMoneylineHome(mlHome);
        m.setMoneylineAway(mlAway);
        m.setType(type);
        return m;
    }

    @Test
    void choisit_le_choc_de_la_soiree_et_le_fige() {
        Match choc = match("choc", team("OKC", 60, 22), team("BOS", 58, 24), 1.7, 2.2, MatchType.REGULAR);
        Match faible = match("faible", team("WAS", 20, 62), team("UTA", 22, 60), 1.9, 1.95, MatchType.REGULAR);
        when(boostRepository.findById(LocalDate.of(2026, 10, 22))).thenReturn(Optional.empty());
        when(matchRepository.findByStatusAndDateBetween(eq(MatchStatus.SCHEDULED), any(), any()))
                .thenReturn(List.of(faible, choc));
        when(matchRepository.findById("choc")).thenReturn(Optional.of(choc));

        BoostService.Boost boost = service.current().orElseThrow();

        assertThat(boost.matchId()).isEqualTo("choc");
        assertThat(boost.homeOdds()).isEqualTo(1.96); // 1,7 × 1,15 = 1,955
        verify(boostRepository).saveAndFlush(argThat(b -> b.getMatchId().equals("choc")));
    }

    @Test
    void presaison_seulement_si_rien_d_autre() {
        Match presaison = match("pre", team("OKC", 60, 22), team("BOS", 58, 24), 1.7, 2.2, MatchType.PRESEASON);
        Match saison = match("reg", team("WAS", 20, 62), team("UTA", 22, 60), 1.9, 1.95, MatchType.REGULAR);
        when(boostRepository.findById(any())).thenReturn(Optional.empty());
        when(matchRepository.findByStatusAndDateBetween(eq(MatchStatus.SCHEDULED), any(), any()))
                .thenReturn(List.of(presaison, saison));
        when(matchRepository.findById("reg")).thenReturn(Optional.of(saison));

        assertThat(service.current()).map(BoostService.Boost::matchId).contains("reg");
    }

    @Test
    void deja_choisi_on_ne_rechoisit_pas() {
        DailyBoost chosen = new DailyBoost();
        chosen.setNight(LocalDate.of(2026, 10, 22));
        chosen.setMatchId("choc");
        when(boostRepository.findById(LocalDate.of(2026, 10, 22))).thenReturn(Optional.of(chosen));
        when(matchRepository.findById("choc")).thenReturn(Optional.of(
                match("choc", team("OKC", 60, 22), team("BOS", 58, 24), 1.7, 2.2, MatchType.REGULAR)));

        assertThat(service.current()).map(BoostService.Boost::matchId).contains("choc");
        verify(matchRepository, never()).findByStatusAndDateBetween(any(), any(), any());
    }

    @Test
    void avant_midi_on_est_encore_sur_la_soiree_de_la_veille() {
        BoostService morning = new BoostService(boostRepository, matchRepository,
                Clock.fixed(Instant.parse("2026-10-23T03:00:00Z"), ZoneOffset.UTC));
        assertThat(morning.currentNight()).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    private Match openingNight() {
        // Soirée d'ouverture le 20 octobre (premier match à 23h30 GMT), deux matchs.
        Match opener = match("ouv1", team("NYK", 50, 32), team("BOS", 58, 24), 1.8, 2.0, MatchType.REGULAR);
        opener.setDate(Instant.parse("2026-10-20T23:30:00Z"));
        Match second = match("ouv2", team("LAL", 45, 37), team("GSW", 48, 34), 2.1, 1.75, MatchType.REGULAR);
        second.setDate(Instant.parse("2026-10-21T02:00:00Z"));
        when(matchRepository.findFirstByTypeAndDateAfterOrderByDateAsc(eq(MatchType.REGULAR), any()))
                .thenReturn(Optional.of(opener));
        when(matchRepository.findByDateBetween(Instant.parse("2026-10-20T12:00:00Z"), Instant.parse("2026-10-21T12:00:00Z")))
                .thenReturn(List.of(opener, second));
        return opener;
    }

    @Test
    void soiree_d_ouverture_annoncee_a_l_avance_tous_ses_matchs_boostes() {
        BoostService before = new BoostService(boostRepository, matchRepository,
                Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), ZoneOffset.UTC));
        openingNight();
        when(boostRepository.findById(any())).thenReturn(Optional.empty());

        BoostService.Boost boost = before.current().orElseThrow();

        assertThat(boost.matchId()).isNull();
        assertThat(boost.openingNight()).isTrue();
        assertThat(boost.matches()).extracting(BoostService.BoostedOdds::matchId).containsExactly("ouv1", "ouv2");
        assertThat(before.boostedMatchIds()).containsExactlyInAnyOrder("ouv1", "ouv2");
    }

    @Test
    void soiree_d_ouverture_passee_plus_de_boost_sur_ses_matchs_a_l_affichage() {
        openingNight();
        when(boostRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(service.current()).isEmpty(); // le 22, rien à booster ce soir-là
    }
}
