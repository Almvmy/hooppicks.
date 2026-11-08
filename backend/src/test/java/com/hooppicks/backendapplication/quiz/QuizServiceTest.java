package com.hooppicks.backendapplication.quiz;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.push.PushService;
import com.hooppicks.backendapplication.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T22:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);

    private final QuizQuestionRepository questions = mock(QuizQuestionRepository.class);
    private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
    private final QuizChampionRepository champions = mock(QuizChampionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final PushService push = mock(PushService.class);

    private QuizService at(Instant now) {
        return new QuizService(questions, attempts, champions, mock(RosterPlayerRepository.class), mock(TeamRepository.class), mock(MatchRepository.class),
                mock(PlayerMatchStatRepository.class), users, notifications, push, TransactionOperations.withoutTransaction(), Clock.fixed(now, BankrollService.ZONE));
    }

    private QuizAttempt playing(String shots, Instant startedAt) {
        QuizAttempt a = new QuizAttempt();
        a.setId("me:" + DAY);
        a.setUserId("me");
        a.setDay(DAY);
        a.setWeek(WEEK);
        a.setShots(shots);
        a.setQuestionStartedAt(startedAt);
        when(attempts.findByIdForUpdate("me:" + DAY)).thenReturn(Optional.of(a));
        return a;
    }

    @BeforeEach
    void setUp() {
        List<QuizQuestion> five = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            QuizQuestion q = new QuizQuestion();
            q.setPosition(i);
            q.setText("Question " + i);
            q.setChoices("A\nB\nC\nD");
            q.setCorrect(1);
            five.add(q);
        }
        when(questions.findByDayOrderByPositionAsc(any())).thenReturn(five);
        when(attempts.findByIdForUpdate(any())).thenReturn(Optional.empty());
        when(attempts.findByWeek(any())).thenReturn(List.of());
    }

    @Test
    void avant_de_jouer_aucune_question_n_est_envoyee() {
        QuizService.QuizState state = at(NOW).state("me");

        assertThat(state.status()).isEqualTo("ready");
        assertThat(state.question()).isNull();
    }

    @Test
    void jouer_lance_le_chrono_de_la_premiere_question() {
        QuizService.QuizState state = at(NOW).start("me");

        ArgumentCaptor<QuizAttempt> saved = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(attempts).save(saved.capture());
        assertThat(saved.getValue().getWeek()).isEqualTo(WEEK);
        assertThat(state.status()).isEqualTo("playing");
        assertThat(state.question().index()).isZero();
        assertThat(state.question().secondsLeft()).isEqualTo(15);
    }

    @Test
    void relancer_ne_recree_pas_la_partie_un_seul_essai_par_jour() {
        playing("10", NOW);

        QuizService.QuizState state = at(NOW).start("me");

        verify(attempts, never()).save(any());
        assertThat(state.shots()).isEqualTo("10");
    }

    @Test
    void bonne_reponse_dans_le_temps_un_panier_et_le_temps_compte() {
        QuizAttempt a = playing("", NOW.minusSeconds(4));

        QuizService.AnswerResult r = at(NOW).answer("me", 0, 1);

        assertThat(r.hit()).isTrue();
        assertThat(a.getShots()).isEqualTo("1");
        assertThat(a.getTotalMillis()).isEqualTo(4000);
    }

    @Test
    void mauvaise_reponse_ratee_au_temps_maximum() {
        QuizAttempt a = playing("", NOW.minusSeconds(4));

        QuizService.AnswerResult r = at(NOW).answer("me", 0, 2);

        assertThat(r.hit()).isFalse();
        assertThat(r.correct()).isEqualTo(1);
        assertThat(a.getShots()).isEqualTo("0");
        assertThat(a.getTotalMillis()).isEqualTo(15000);
    }

    @Test
    void trop_tard_ratee_meme_avec_la_bonne_reponse() {
        QuizAttempt a = playing("", NOW.minusSeconds(18));

        assertThat(at(NOW).answer("me", 0, 1).hit()).isFalse();
        assertThat(a.getShots()).isEqualTo("0");
    }

    @Test
    void une_connexion_lente_garde_deux_secondes_de_marge() {
        playing("", NOW.minusSeconds(16));

        assertThat(at(NOW).answer("me", 0, 1).hit()).isTrue();
    }

    @Test
    void un_double_appui_ne_joue_pas_deux_fois_la_question() {
        QuizAttempt a = playing("1", NOW);

        at(NOW).answer("me", 0, 1);

        assertThat(a.getShots()).isEqualTo("1");
    }

    @Test
    void le_chrono_suivant_ne_compte_pas_le_temps_d_afficher_la_reponse() {
        QuizAttempt a = playing("", NOW.minusSeconds(4));

        QuizService.AnswerResult r = at(NOW).answer("me", 0, 1);

        assertThat(a.getQuestionStartedAt()).isEqualTo(NOW.plus(QuizService.FEEDBACK));
        assertThat(r.state().question().secondsLeft()).isEqualTo(15);
        assertThat(r.state().question().index()).isEqualTo(1);
    }

    @Test
    void la_reponse_n_est_jamais_envoyee_avec_la_question() {
        playing("", NOW);

        QuizService.QuizState state = at(NOW).state("me");

        assertThat(state.question().toString()).doesNotContain("correct");
        assertThat(state.review()).isNull();
    }

    @Test
    void quitter_une_question_la_compte_ratee_une_seule_a_la_fois() {
        QuizAttempt a = playing("", NOW.minus(Duration.ofHours(2)));

        QuizService.QuizState state = at(NOW).state("me");

        assertThat(a.getShots()).isEqualTo("0");
        assertThat(state.question().index()).isEqualTo(1);
        assertThat(state.question().secondsLeft()).isEqualTo(15);
    }

    @Test
    void partie_finie_le_detail_avec_les_bonnes_reponses() {
        playing("10110", NOW);

        QuizService.QuizState state = at(NOW).state("me");

        assertThat(state.status()).isEqualTo("done");
        assertThat(state.question()).isNull();
        assertThat(state.review()).hasSize(5);
        assertThat(state.review().get(0).answer()).isEqualTo("B");
        assertThat(state.review().get(1).hit()).isFalse();
    }

    @Test
    void sans_partie_on_ne_repond_pas() {
        assertThatThrownBy(() -> at(NOW).answer("me", 0, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- Classement ---

    private QuizAttempt day(String userId, int offset, String shots, long millis) {
        QuizAttempt a = new QuizAttempt();
        a.setUserId(userId);
        a.setDay(WEEK.plusDays(offset));
        a.setWeek(WEEK);
        a.setShots(shots);
        a.setTotalMillis(millis);
        return a;
    }

    private User user(String id) {
        User u = new User();
        u.setId(id);
        u.setUsername(id);
        return u;
    }

    @Test
    void seuls_les_5_meilleurs_jours_comptent() {
        // 7 jours : deux mauvais jours sont écartés, 5 paniers × 5 jours = 25.
        List<QuizAttempt> week = new ArrayList<>();
        for (int i = 0; i < 5; i++) week.add(day("me", i, "11111", 10_000));
        week.add(day("me", 5, "00000", 75_000));
        week.add(day("me", 6, "00000", 75_000));
        when(attempts.findByWeek(WEEK)).thenReturn(week);

        QuizService.Standing s = at(NOW).state("me").standing();

        assertThat(s.baskets()).isEqualTo(25);
        assertThat(s.days()).isEqualTo(7);
        assertThat(s.level()).isEqualTo("MVP");
    }

    @Test
    void celui_qui_rate_deux_jours_peut_encore_battre_celui_qui_joue_tous_les_jours() {
        List<QuizAttempt> week = new ArrayList<>();
        for (int i = 0; i < 5; i++) week.add(day("me", i, "11111", 10_000));
        for (int i = 0; i < 7; i++) week.add(day("autre", i, "11110", 10_000));
        when(attempts.findByWeek(WEEK)).thenReturn(week);

        assertThat(at(NOW).state("me").standing().rank()).isEqualTo(1);
    }

    @Test
    void a_egalite_de_paniers_le_plus_rapide_passe_devant() {
        when(attempts.findByWeek(WEEK)).thenReturn(List.of(day("lent", 0, "11111", 60_000), day("me", 0, "11111", 20_000)));
        when(users.findAllById(any())).thenReturn(List.of(user("lent"), user("me")));

        QuizService.Board board = at(NOW).board("me");

        assertThat(board.entries()).extracting(QuizService.BoardEntry::username).containsExactly("me", "lent");
        assertThat(board.me().rank()).isEqualTo(1);
        assertThat(board.me().seconds()).isEqualTo(20);
    }

    @Test
    void les_niveaux_de_la_semaine() {
        assertThat(QuizService.level(0)).isEqualTo("Rookie");
        assertThat(QuizService.level(8)).isEqualTo("Titulaire");
        assertThat(QuizService.level(15)).isEqualTo("All-Star");
        assertThat(QuizService.level(22)).isEqualTo("MVP");
    }

    @Test
    void la_semaine_commence_le_lundi_a_8h() {
        // Lundi avant 8h GMT : encore le jour de dimanche, donc la semaine d'avant.
        assertThat(at(Instant.parse("2026-11-09T07:59:00Z")).state("me").day()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(at(Instant.parse("2026-11-09T08:00:00Z")).state("me").day()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(QuizService.weekOf(LocalDate.of(2026, 11, 8))).isEqualTo(WEEK);
        assertThat(QuizService.weekOf(LocalDate.of(2026, 11, 9))).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void champion_de_la_semaine_finie_une_seule_fois_et_prevenu() {
        LocalDate lastWeek = WEEK.minusWeeks(1);
        QuizAttempt a = day("me", 0, "11111", 10_000);
        when(attempts.findByWeek(lastWeek)).thenReturn(List.of(a));
        when(users.findById("me")).thenReturn(Optional.of(user("me")));

        at(NOW).recordEndedWeek();

        ArgumentCaptor<QuizChampion> saved = ArgumentCaptor.forClass(QuizChampion.class);
        verify(champions).save(saved.capture());
        assertThat(saved.getValue().getWeek()).isEqualTo(lastWeek);
        assertThat(saved.getValue().getBaskets()).isEqualTo(5);
        verify(notifications).save(any());

        clearInvocations(champions, notifications);
        when(champions.existsById(lastWeek)).thenReturn(true);
        at(NOW).recordEndedWeek();
        verify(champions, never()).save(any());
        verify(notifications, never()).save(any());
    }

    @Test
    void pas_de_champion_sans_panier() {
        when(attempts.findByWeek(WEEK.minusWeeks(1))).thenReturn(List.of(day("me", 0, "00000", 75_000)));

        at(NOW).recordEndedWeek();

        verify(champions, never()).save(any());
    }
}
