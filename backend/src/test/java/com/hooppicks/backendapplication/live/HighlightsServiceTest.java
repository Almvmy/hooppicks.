package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HighlightsServiceTest {

    private static final Instant TIPOFF = Instant.parse("2026-10-20T23:30:00Z");

    private static String entry(String id, String title, String published) {
        return "<entry><id>yt:video:" + id + "</id><yt:videoId>" + id + "</yt:videoId><title>" + title
                + "</title><published>" + published + "</published></entry>";
    }

    private static String feed(String... entries) {
        return "<feed><title>NBA</title><published>2005-11-21T01:20:33+00:00</published>" + String.join("", entries) + "</feed>";
    }

    private static Team team(String name) {
        Team t = new Team();
        t.setName(name);
        return t;
    }

    private static Match match(String home, String away) {
        Match m = new Match();
        m.setId("m-" + home);
        m.setHomeTeam(team(home));
        m.setAwayTeam(team(away));
        m.setDate(TIPOFF);
        m.setStatus(MatchStatus.FINISHED);
        return m;
    }

    @Test
    void lit_le_flux_et_ignore_le_titre_de_la_chaine() {
        List<HighlightsService.Video> videos = HighlightsService.parse(feed(
                entry("abc", "PISTONS at CELTICS | FULL GAME HIGHLIGHTS | October 20, 2026", "2026-10-21T03:10:00+00:00"),
                entry("def", "Cade &amp; Ausar 🔥", "2026-10-21T03:00:00+00:00")));

        assertThat(videos).extracting(HighlightsService.Video::id).containsExactly("abc", "def");
        assertThat(videos.get(1).title()).isEqualTo("Cade & Ausar 🔥");
        assertThat(videos.get(0).published()).isEqualTo(Instant.parse("2026-10-21T03:10:00Z"));
    }

    @Test
    void relie_le_resume_qui_nomme_les_deux_equipes_publie_apres_le_match() {
        List<HighlightsService.Video> videos = HighlightsService.parse(feed(
                entry("old", "Pistons vs Celtics Highlights | 2025 Playoffs", "2026-04-30T03:00:00+00:00"),
                entry("short", "Cade Cunningham Steals It And Throws It Down 💥😤", "2026-10-21T00:30:00+00:00"),
                entry("other", "LAKERS at WARRIORS | FULL GAME HIGHLIGHTS", "2026-10-21T06:00:00+00:00"),
                entry("good", "PISTONS at CELTICS | FULL GAME HIGHLIGHTS | October 20, 2026", "2026-10-21T03:10:00+00:00")));

        assertThat(HighlightsService.bestFor(match("Celtics", "Pistons"), videos).id()).isEqualTo("good");
    }

    @Test
    void nets_ne_trouve_pas_hornets() {
        List<HighlightsService.Video> videos = HighlightsService.parse(feed(
                entry("x", "HORNETS at KNICKS | FULL GAME HIGHLIGHTS", "2026-10-21T03:10:00+00:00")));

        assertThat(HighlightsService.bestFor(match("Knicks", "Nets"), videos)).isNull();
        assertThat(HighlightsService.bestFor(match("Knicks", "Hornets"), videos).id()).isEqualTo("x");
    }

    @Test
    void un_full_game_remplace_un_resume_court_jamais_l_inverse() {
        List<HighlightsService.Video> videos = HighlightsService.parse(feed(
                entry("short", "Celtics vs Pistons Highlights in 2 minutes", "2026-10-21T02:00:00+00:00"),
                entry("full", "PISTONS at CELTICS | FULL GAME HIGHLIGHTS", "2026-10-21T03:10:00+00:00")));

        Match withShort = match("Celtics", "Pistons");
        withShort.setHighlightsVideoId("short");
        withShort.setHighlightsTitle("Celtics vs Pistons Highlights in 2 minutes");
        assertThat(HighlightsService.bestFor(withShort, videos).id()).isEqualTo("full");

        Match withFull = match("Celtics", "Pistons");
        withFull.setHighlightsVideoId("full");
        withFull.setHighlightsTitle("PISTONS at CELTICS | FULL GAME HIGHLIGHTS");
        assertThat(HighlightsService.bestFor(withFull, videos)).isNull();
    }

    @Test
    void refresh_enregistre_sous_le_verrou_de_la_synchro() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        NbaSyncService sync = mock(NbaSyncService.class);
        when(sync.tryRunExclusive(any())).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return true;
        });
        when(rest.getForObject(eq(HighlightsService.FEED_URL), eq(String.class))).thenReturn(feed(
                entry("good", "PISTONS at CELTICS | FULL GAME HIGHLIGHTS", "2026-10-21T03:10:00+00:00")));
        Match m = match("Celtics", "Pistons");
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));

        assertThat(new HighlightsService(rest, repo, sync).refresh()).isEqualTo(1);
        assertThat(m.getHighlightsVideoId()).isEqualTo("good");
        verify(repo).save(m);
    }

    @Test
    void chaine_injoignable_rien_ne_change() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        when(rest.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("timeout"));

        assertThat(new HighlightsService(rest, repo, mock(NbaSyncService.class)).refresh()).isZero();
        verifyNoInteractions(repo);
    }
}
