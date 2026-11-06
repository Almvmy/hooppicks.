package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Debate;
import com.hooppicks.backendapplication.entity.DebateVote;
import com.hooppicks.backendapplication.repository.DebateRepository;
import com.hooppicks.backendapplication.repository.DebateVoteRepository;
import com.hooppicks.backendapplication.repository.NightMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DebateServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T22:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private final DebateRepository debates = mock(DebateRepository.class);
    private final DebateVoteRepository votes = mock(DebateVoteRepository.class);
    private final NightMessageRepository messages = mock(NightMessageRepository.class);
    private final NightThreadService thread = mock(NightThreadService.class);
    private final com.hooppicks.backendapplication.repository.PlayerMatchStatRepository stats = mock(com.hooppicks.backendapplication.repository.PlayerMatchStatRepository.class);
    private final com.hooppicks.backendapplication.repository.TeamRepository teams = mock(com.hooppicks.backendapplication.repository.TeamRepository.class);
    private final DebateService service = new DebateService(debates, votes, messages, thread, stats, teams, Clock.fixed(NOW, BankrollService.ZONE));

    private Debate debate(LocalDate day) {
        Debate d = new Debate();
        d.setDay(day);
        d.setQuestion("Le plus grand ?");
        d.setOptionA("Jordan");
        d.setOptionB("LeBron");
        return d;
    }

    private void voted(LocalDate day, String side) {
        DebateVote v = new DebateVote();
        v.setSide(side);
        v.setUserId("me");
        when(votes.findById("me:" + day)).thenReturn(Optional.of(v));
        when(votes.existsById("me:" + day)).thenReturn(true);
    }

    @BeforeEach
    void setUp() {
        when(debates.findById(TODAY)).thenReturn(Optional.of(debate(TODAY)));
        when(votes.findById(any())).thenReturn(Optional.empty());
        when(votes.countByDayAndSide(TODAY, "A")).thenReturn(7L);
        when(votes.countByDayAndSide(TODAY, "B")).thenReturn(3L);
        when(votes.countByDay(TODAY)).thenReturn(10L);
    }

    @Test
    void la_premiere_ouverture_cree_le_debat_de_la_soiree_depuis_la_liste() {
        when(debates.findById(TODAY)).thenReturn(Optional.empty());
        when(debates.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        DebateService.DebateDto dto = service.today("me");

        assertThat(dto.day()).isEqualTo(TODAY);
        assertThat(dto.question()).isEqualTo(DebateBank.of(TODAY).text());
    }

    private void noDebateYet() {
        when(debates.findById(TODAY)).thenReturn(Optional.empty());
        when(debates.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    private com.hooppicks.backendapplication.entity.PlayerMatchStat line(String name, int points, com.hooppicks.backendapplication.entity.MatchType type) {
        com.hooppicks.backendapplication.entity.Match match = new com.hooppicks.backendapplication.entity.Match();
        match.setType(type);
        com.hooppicks.backendapplication.entity.PlayerMatchStat s = new com.hooppicks.backendapplication.entity.PlayerMatchStat();
        s.setMatch(match);
        s.setPlayerName(name);
        s.setPoints(points);
        return s;
    }

    @Test
    void un_exploit_de_la_nuit_passe_avant_la_liste() {
        noDebateYet();
        when(stats.findFinishedBetween(any(), any())).thenReturn(List.of(line("Edwards", 61, com.hooppicks.backendapplication.entity.MatchType.REGULAR)));

        assertThat(service.today("me").question()).startsWith("Edwards à 61 points");
    }

    @Test
    void pas_de_debat_d_actualite_sur_la_presaison() {
        noDebateYet();
        when(stats.findFinishedBetween(any(), any())).thenReturn(List.of(line("Edwards", 61, com.hooppicks.backendapplication.entity.MatchType.PRESEASON)));

        assertThat(service.today("me").question()).isEqualTo(DebateBank.of(TODAY).text());
    }

    @Test
    void jamais_deux_jours_d_actualite_de_suite() {
        noDebateYet();
        Debate yesterday = debate(TODAY.minusDays(1));
        yesterday.setKind("serie");
        when(debates.findByDayGreaterThanEqual(any())).thenReturn(List.of(yesterday));
        when(stats.findFinishedBetween(any(), any())).thenReturn(List.of(line("Edwards", 61, com.hooppicks.backendapplication.entity.MatchType.REGULAR)));

        assertThat(service.today("me").question()).doesNotContain("Edwards");
    }

    @Test
    void une_question_de_la_liste_ne_revient_pas_de_la_saison() {
        noDebateYet();
        DebateBank.Question todayQuestion = DebateBank.of(TODAY);
        Debate past = debate(TODAY.minusDays(30));
        past.setQuestion(todayQuestion.text());
        past.setOptionA(todayQuestion.a());
        when(debates.findByDayGreaterThanEqual(any())).thenReturn(List.of(past));

        assertThat(service.today("me").question()).isNotEqualTo(todayQuestion.text());
    }

    @Test
    void la_liste_compte_au_moins_150_questions_toutes_differentes() {
        assertThat(DebateBank.QUESTIONS.size()).isGreaterThanOrEqualTo(150);
        assertThat(DebateBank.QUESTIONS.stream().map(q -> DebateBank.key(q.text(), q.a())).toList()).doesNotHaveDuplicates();
    }

    @Test
    void deux_ouvertures_simultanees_ne_font_pas_d_erreur() {
        Debate other = debate(TODAY);
        when(debates.findById(TODAY)).thenReturn(Optional.empty()).thenReturn(Optional.of(other));
        when(debates.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("clé déjà là"));

        assertThat(service.today("me").question()).isEqualTo("Le plus grand ?");
    }

    @Test
    void la_liste_ne_repete_aucune_question_avant_d_en_avoir_fait_le_tour() {
        int size = DebateBank.QUESTIONS.size();
        HashSet<String> seen = new HashSet<>();
        for (int i = 0; i < size; i++) seen.add(DebateBank.of(TODAY.plusDays(i)).text());
        assertThat(seen).hasSize(size);
        assertThat(DebateBank.QUESTIONS).allSatisfy(q -> {
            assertThat(q.text().length()).isLessThanOrEqualTo(DebateService.MAX_QUESTION);
            assertThat(q.a().length()).isLessThanOrEqualTo(DebateService.MAX_OPTION);
            assertThat(q.b().length()).isLessThanOrEqualTo(DebateService.MAX_OPTION);
            assertThat(q.a()).isNotEqualTo(q.b());
        });
    }

    @Test
    void le_resultat_reste_cache_tant_qu_on_n_a_pas_vote() {
        DebateService.DebateDto dto = service.today("me");

        assertThat(dto.open()).isTrue();
        assertThat(dto.votesA()).isNull();
        assertThat(dto.votesB()).isNull();
        assertThat(dto.total()).isEqualTo(10);
    }

    @Test
    void le_resultat_s_affiche_apres_le_vote() {
        voted(TODAY, "A");

        DebateService.DebateDto dto = service.today("me");

        assertThat(dto.myVote()).isEqualTo("A");
        assertThat(dto.votesA()).isEqualTo(7);
        assertThat(dto.votesB()).isEqualTo(3);
    }

    @Test
    void un_debat_clos_montre_le_resultat_a_tout_le_monde() {
        LocalDate past = TODAY.minusDays(1);
        when(debates.findById(past)).thenReturn(Optional.of(debate(past)));
        when(votes.countByDayAndSide(past, "A")).thenReturn(4L);

        DebateService.DebateDto dto = service.ofDay("me", past).orElseThrow();

        assertThat(dto.open()).isFalse();
        assertThat(dto.votesA()).isEqualTo(4);
    }

    @Test
    void la_question_de_demain_n_est_pas_devoilee() {
        LocalDate tomorrow = TODAY.plusDays(1);
        assertThat(service.ofDay("me", tomorrow)).isEmpty();
        verify(debates, never()).findById(tomorrow);
    }

    @Test
    void on_vote_sur_le_debat_du_jour_et_le_vote_a_une_seule_cle_par_joueur() {
        service.vote("me", "B");

        ArgumentCaptor<DebateVote> saved = ArgumentCaptor.forClass(DebateVote.class);
        verify(votes).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo("me:" + TODAY);
        assertThat(saved.getValue().getSide()).isEqualTo("B");
    }

    @Test
    void camp_inconnu_refuse() {
        assertThatThrownBy(() -> service.vote("me", "C")).isInstanceOf(IllegalArgumentException.class);
        verify(votes, never()).save(any());
    }

    @Test
    void pas_d_argument_sans_camp() {
        assertThatThrownBy(() -> service.comment("me", TODAY, "Jordan évidemment", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.comments("me", TODAY)).isInstanceOf(IllegalArgumentException.class);
        verify(thread, never()).postDebate(any(), any(), any(), any());
    }

    @Test
    void on_n_argumente_plus_sur_un_debat_clos_mais_on_le_lit() {
        LocalDate past = TODAY.minusDays(1);
        voted(past, "A");
        when(debates.findById(past)).thenReturn(Optional.of(debate(past)));
        assertThatThrownBy(() -> service.comment("me", past, "Trop tard", null)).isInstanceOf(IllegalArgumentException.class);

        service.comments("autre", past);
        verify(thread).debateFeed(eq("autre"), eq(past), anyMap(), eq("Ce débat est clos."));
    }

    @Test
    void chaque_argument_porte_le_camp_de_son_auteur() {
        voted(TODAY, "A");
        DebateVote v = new DebateVote();
        v.setUserId("kader");
        v.setSide("B");
        when(votes.findByDay(TODAY)).thenReturn(List.of(v));

        service.comment("me", TODAY, "Jordan évidemment", "m1");

        verify(thread).postDebate("me", "Jordan évidemment", TODAY, "m1");
        verify(thread).debateFeed("me", TODAY, java.util.Map.of("kader", "LeBron"), null);
    }

    @Test
    void un_admin_ne_change_pas_une_question_deja_votee_ni_un_jour_passe() {
        assertThatThrownBy(() -> service.plan(TODAY, "Autre ?", "Oui", "Non")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.plan(TODAY.minusDays(1), "Autre ?", "Oui", "Non")).isInstanceOf(IllegalArgumentException.class);
        verify(debates, never()).save(any());
    }

    @Test
    void un_admin_prevoit_une_question_pour_plus_tard() {
        service.plan(TODAY.plusDays(2), " Autre ? ", " Oui ", "Non");

        ArgumentCaptor<Debate> saved = ArgumentCaptor.forClass(Debate.class);
        verify(debates).save(saved.capture());
        assertThat(saved.getValue().getQuestion()).isEqualTo("Autre ?");
        assertThat(saved.getValue().getPlanned()).isTrue();
    }

    @Test
    void question_ou_camps_invalides_refuses() {
        LocalDate later = TODAY.plusDays(2);
        assertThatThrownBy(() -> service.plan(later, " ", "Oui", "Non")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.plan(later, "Q ?", "Oui", "oui")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.plan(later, "x".repeat(141), "Oui", "Non")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void le_planning_montre_les_7_prochains_jours_sans_toucher_a_la_base() {
        List<DebateService.PlannedDebate> days = service.planning();

        assertThat(days).hasSize(7);
        assertThat(days.get(1).question()).isEqualTo(DebateBank.of(TODAY.plusDays(1)).text());
        verify(debates, never()).saveAndFlush(any());
    }
}
