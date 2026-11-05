package com.hooppicks.backendapplication.engagement;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.DuelRepository;
import com.hooppicks.backendapplication.repository.MissionCompletionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MissionServiceTest {

    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    private static final Instant NOW = Instant.parse("2026-11-05T15:00:00Z");

    private final BetRepository betRepository = mock(BetRepository.class);
    private final DuelRepository duelRepository = mock(DuelRepository.class);
    private final MissionCompletionRepository completionRepository = mock(MissionCompletionRepository.class);
    private final MissionService service = new MissionService(betRepository, duelRepository, completionRepository, Clock.fixed(NOW, BankrollService.ZONE));

    private static BetSelection selection(String market, String label, double odds, String player) {
        BetSelection s = new BetSelection();
        s.setMarket(market);
        s.setOutcome("home");
        s.setLabel(label);
        s.setOdds(odds);
        s.setPlayerName(player);
        return s;
    }

    private static Bet bet(BetStatus status, int stake, Instant placedAt, BetSelection... selections) {
        Bet b = new Bet();
        b.setStatus(status);
        b.setStake(stake);
        b.setPlacedAt(placedAt);
        b.getSelections().addAll(List.of(selections));
        return b;
    }

    private static int progress(String key, MissionService.WeekData data) {
        Set<String> seen = new HashSet<>();
        // Chaque mission finit par être tirée une semaine ou une autre.
        for (long day = 0; day < 3000; day += 7) {
            for (MissionService.Mission m : MissionService.forWeek(LocalDate.ofEpochDay(day))) {
                seen.add(m.key());
                if (m.key().equals(key)) return m.progress().applyAsInt(data);
            }
        }
        throw new AssertionError("Mission jamais tirée : " + key + ", vues : " + seen);
    }

    @BeforeEach
    void setUp() {
        when(duelRepository.findAllForUser("me")).thenReturn(List.of());
    }

    @Test
    void trois_missions_stables_sur_la_semaine_une_a_jouer_et_une_a_gagner() {
        List<String> keys = MissionService.forWeek(WEEK).stream().map(MissionService.Mission::key).toList();

        assertThat(keys).hasSize(3).doesNotHaveDuplicates();
        assertThat(keys.get(1)).startsWith("win_");
        assertThat(keys.get(0)).doesNotStartWith("win_");
        assertThat(MissionService.forWeek(WEEK).stream().map(MissionService.Mission::key).toList()).isEqualTo(keys);
    }

    @Test
    void les_missions_changent_d_une_semaine_a_l_autre() {
        Set<List<String>> tirages = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            tirages.add(MissionService.forWeek(WEEK.plusWeeks(i)).stream().map(MissionService.Mission::key).toList());
        }
        assertThat(tirages.size()).isGreaterThan(3);
    }

    @Test
    void progression_de_chaque_type_d_objectif() {
        Instant monday = Instant.parse("2026-11-02T20:00:00Z");
        Instant tuesdayMorning = Instant.parse("2026-11-03T02:00:00Z"); // même soirée NBA que lundi soir
        Instant wednesday = Instant.parse("2026-11-04T20:00:00Z");
        MissionService.WeekData data = new MissionService.WeekData(List.of(
                bet(BetStatus.WON, 300, monday, selection("moneyline", "BKN (V)", 2.5, null)),
                bet(BetStatus.LOST, 100, tuesdayMorning, selection("alt_spread", "CLE -10,5", 3.1, null)),
                bet(BetStatus.VOID, 400, wednesday, selection("player_points", "M. Porter Jr. · 30+ pts", 3.95, "Michael Porter Jr.")),
                bet(BetStatus.WON, 50, wednesday, selection("player_rebounds", "N. Jokic · Plus de 12,5 rbd", 1.96, "Nikola Jokic"),
                        selection("alt_total", "Plus de 219,5", 1.4, null), selection("spread", "OKC -3,5", 1.96, null))
        ), List.of());

        assertThat(progress("nights", data)).isEqualTo(2);
        assertThat(progress("stake", data)).isEqualTo(450); // un ticket remboursé ne compte pas
        assertThat(progress("alt", data)).isEqualTo(3); // ligne alternative ou palier
        assertThat(progress("win_prop", data)).isEqualTo(1); // le ticket joueur remboursé ne compte pas
        assertThat(progress("win_underdog", data)).isEqualTo(1);
        assertThat(progress("win_parlay", data)).isEqualTo(1);
        assertThat(progress("win_total", data)).isEqualTo(1);
        assertThat(progress("duel", data)).isZero();
    }

    @Test
    void une_mission_reussie_est_notee_une_seule_fois() {
        MissionService.Mission mission = MissionService.forWeek(WEEK).get(0);
        // Assez de tickets pour réussir n'importe quelle mission « à jouer » (hors duel).
        when(betRepository.findByUserIdOrderByPlacedAtDesc("me")).thenReturn(List.of(
                bet(BetStatus.PENDING, 500, Instant.parse("2026-11-02T20:00:00Z"), selection("alt_spread", "CLE -10,5", 3.1, null)),
                bet(BetStatus.PENDING, 10, Instant.parse("2026-11-03T20:00:00Z"), selection("alt_spread", "BOS -8,5", 2.1, null)),
                bet(BetStatus.PENDING, 10, Instant.parse("2026-11-04T20:00:00Z"), selection("alt_spread", "NYK -7,5", 2.1, null))));
        Duel duel = new Duel();
        duel.setWeek(WEEK);
        duel.setStatus(DuelStatus.ACCEPTED);
        when(duelRepository.findAllForUser("me")).thenReturn(List.of(duel));
        when(completionRepository.existsById(any())).thenReturn(false);

        MissionService.Missions result = service.forUser("me");

        assertThat(result.missions().get(0).done()).isTrue();
        String id = "me:" + WEEK + ":" + mission.key();
        verify(completionRepository).save(org.mockito.ArgumentMatchers.argThat(c -> c.getId().equals(id)));

        clearInvocations(completionRepository);
        when(completionRepository.existsById(id)).thenReturn(true);
        service.forUser("me");
        verify(completionRepository, never()).save(org.mockito.ArgumentMatchers.argThat(c -> c.getId().equals(id)));
    }

    @Test
    void prochaine_recompense_selon_le_total_de_missions() {
        when(betRepository.findByUserIdOrderByPlacedAtDesc("me")).thenReturn(List.of());

        when(completionRepository.countByUserId("me")).thenReturn(2L);
        assertThat(service.forUser("me").nextRewardAt()).isEqualTo(MissionService.ROCKET_AT);

        when(completionRepository.countByUserId("me")).thenReturn(6L);
        assertThat(service.forUser("me").nextRewardLabel()).isEqualTo("Étoile");

        when(completionRepository.countByUserId("me")).thenReturn(20L);
        assertThat(service.forUser("me").nextRewardAt()).isNull();
    }
}
