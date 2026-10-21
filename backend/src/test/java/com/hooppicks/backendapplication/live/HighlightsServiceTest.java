package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.espn.EspnStatsClient;
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
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));

        assertThat(new HighlightsService(rest, repo, sync).refresh()).isEqualTo(1);
        assertThat(m.getHighlightsVideoId()).isEqualTo("good");
        verify(repo).save(m);
    }

    private static final String API_JSON = """
            {"items": [
              {"snippet": {"title": "PISTONS at CELTICS | FULL GAME HIGHLIGHTS", "publishedAt": "2026-10-21T03:10:00Z",
                           "resourceId": {"videoId": "api1"}}},
              {"snippet": {"title": "sans id", "publishedAt": "2026-10-21T03:10:00Z", "resourceId": {}}}
            ]}""";

    @Test
    void lit_la_reponse_de_l_api_youtube() throws Exception {
        tools.jackson.databind.JsonNode json = new tools.jackson.databind.ObjectMapper().readTree(API_JSON);

        List<HighlightsService.Video> videos = HighlightsService.parseApi(json);

        assertThat(videos).extracting(HighlightsService.Video::id).containsExactly("api1");
        assertThat(videos.get(0).published()).isEqualTo(Instant.parse("2026-10-21T03:10:00Z"));
    }

    @Test
    void avec_une_cle_l_api_d_abord_puis_le_flux_si_elle_echoue() throws Exception {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        NbaSyncService sync = mock(NbaSyncService.class);
        when(sync.tryRunExclusive(any())).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return true;
        });
        Match m = match("Celtics", "Pistons");
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));
        when(rest.getForObject(eq(HighlightsService.API_URL + "cle"), eq(tools.jackson.databind.JsonNode.class)))
                .thenReturn(new tools.jackson.databind.ObjectMapper().readTree(API_JSON));

        assertThat(new HighlightsService(rest, repo, sync, "cle").refresh()).isEqualTo(1);
        assertThat(m.getHighlightsVideoId()).isEqualTo("api1");
        verify(rest, never()).getForObject(eq(HighlightsService.FEED_URL), eq(String.class));

        Match other = match("Celtics", "Pistons");
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(other));
        when(repo.findById(other.getId())).thenReturn(java.util.Optional.of(other));
        when(rest.getForObject(eq(HighlightsService.API_URL + "cle"), eq(tools.jackson.databind.JsonNode.class)))
                .thenThrow(new RuntimeException("quota"));
        when(rest.getForObject(eq(HighlightsService.FEED_URL), eq(String.class))).thenReturn(feed(
                entry("rss", "PISTONS at CELTICS | FULL GAME HIGHLIGHTS", "2026-10-21T03:10:00+00:00")));

        assertThat(new HighlightsService(rest, repo, sync, "cle").refresh()).isEqualTo(1);
        assertThat(other.getHighlightsVideoId()).isEqualTo("rss");
    }

    @Test
    void videos_bloquees_en_cote_d_ivoire() throws Exception {
        tools.jackson.databind.JsonNode json = new tools.jackson.databind.ObjectMapper().readTree("""
                {"items": [
                  {"id": "bloquee", "contentDetails": {"regionRestriction": {"blocked": ["FR", "CI"]}}},
                  {"id": "ailleurs", "contentDetails": {"regionRestriction": {"blocked": ["FR"]}}},
                  {"id": "liste", "contentDetails": {"regionRestriction": {"allowed": ["US", "CA"]}}},
                  {"id": "partout", "contentDetails": {}}
                ]}""");

        assertThat(HighlightsService.parseBlocked(json, "CI")).containsExactlyInAnyOrder("bloquee", "liste");
    }

    @Test
    void une_video_reliee_puis_bloquee_est_remplacee_par_une_lisible() throws Exception {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        NbaSyncService sync = mock(NbaSyncService.class);
        when(sync.tryRunExclusive(any())).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return true;
        });
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        when(rest.getForObject(eq(HighlightsService.API_URL + "cle"), eq(tools.jackson.databind.JsonNode.class))).thenReturn(mapper.readTree("""
                {"items": [
                  {"snippet": {"title": "PISTONS at CELTICS | FULL GAME HIGHLIGHTS", "publishedAt": "2026-10-21T03:10:00Z", "resourceId": {"videoId": "full"}}},
                  {"snippet": {"title": "Pistons vs Celtics Highlights", "publishedAt": "2026-10-21T02:00:00Z", "resourceId": {"videoId": "court"}}}
                ]}"""));
        when(rest.getForObject(startsWith(HighlightsService.VIDEOS_URL), eq(tools.jackson.databind.JsonNode.class))).thenReturn(mapper.readTree("""
                {"items": [{"id": "full", "contentDetails": {"regionRestriction": {"blocked": ["CI"]}}}, {"id": "court", "contentDetails": {}}]}"""));
        Match m = match("Celtics", "Pistons");
        m.setHighlightsVideoId("full");
        m.setHighlightsTitle("PISTONS at CELTICS | FULL GAME HIGHLIGHTS");
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));

        assertThat(new HighlightsService(rest, repo, sync, "cle").refresh()).isEqualTo(1);
        assertThat(m.getHighlightsVideoId()).isEqualTo("court");
    }

    // --- ESPN en secours, la vidéo la plus longue l'emporte ---

    private static NbaSyncService lockRunning() {
        NbaSyncService sync = mock(NbaSyncService.class);
        when(sync.tryRunExclusive(any())).thenAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return true;
        });
        return sync;
    }

    private static Match espnMatch() {
        Match m = match("Celtics", "Pistons");
        m.setEspnEventId("401909088");
        return m;
    }

    @Test
    void sans_resume_youtube_le_resume_espn() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        EspnStatsClient espn = mock(EspnStatsClient.class);
        when(rest.getForObject(eq(HighlightsService.FEED_URL), eq(String.class))).thenReturn(feed());
        Match m = espnMatch();
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));
        when(espn.fetchGameHighlights("401909088")).thenReturn(java.util.Optional.of(
                new EspnStatsClient.EspnVideo("50232313", "Boston Celtics vs. Detroit Pistons: Game Highlights", 74)));

        assertThat(new HighlightsService(rest, repo, lockRunning(), "", espn).refresh()).isEqualTo(1);
        assertThat(m.getHighlightsVideoId()).isEqualTo("50232313");
        assertThat(m.getHighlightsProvider()).isEqualTo("espn");
        assertThat(m.getHighlightsSeconds()).isEqualTo(74);
    }

    @Test
    void le_resume_youtube_complet_passe_devant_espn_sans_meme_le_chercher() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        EspnStatsClient espn = mock(EspnStatsClient.class);
        when(rest.getForObject(eq(HighlightsService.FEED_URL), eq(String.class))).thenReturn(feed(
                entry("full", "PISTONS at CELTICS | FULL GAME HIGHLIGHTS", "2026-10-21T03:10:00+00:00")));
        Match m = espnMatch();
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));

        new HighlightsService(rest, repo, lockRunning(), "", espn).refresh();

        assertThat(m.getHighlightsVideoId()).isEqualTo("full");
        assertThat(m.getHighlightsProvider()).isEqualTo("youtube");
        verifyNoInteractions(espn);
    }

    @Test
    void un_resume_youtube_plus_long_remplace_celui_d_espn() throws Exception {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        EspnStatsClient espn = mock(EspnStatsClient.class);
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        when(rest.getForObject(eq(HighlightsService.API_URL + "cle"), eq(tools.jackson.databind.JsonNode.class))).thenReturn(mapper.readTree("""
                {"items": [{"snippet": {"title": "Celtics vs Pistons Highlights", "publishedAt": "2026-10-21T03:10:00Z", "resourceId": {"videoId": "yt"}}}]}"""));
        when(rest.getForObject(startsWith(HighlightsService.VIDEOS_URL), eq(tools.jackson.databind.JsonNode.class))).thenReturn(mapper.readTree("""
                {"items": [{"id": "yt", "contentDetails": {"duration": "PT9M41S"}}]}"""));
        Match m = espnMatch();
        m.setHighlightsVideoId("50232313");
        m.setHighlightsProvider("espn");
        m.setHighlightsSeconds(74);
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));

        assertThat(new HighlightsService(rest, repo, lockRunning(), "cle", espn).refresh()).isEqualTo(1);
        assertThat(m.getHighlightsVideoId()).isEqualTo("yt");
        assertThat(m.getHighlightsSeconds()).isEqualTo(581);
        verifyNoInteractions(espn);
    }

    @Test
    void un_extrait_youtube_plus_court_que_le_resume_espn_ne_le_remplace_pas() throws Exception {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        EspnStatsClient espn = mock(EspnStatsClient.class);
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        when(rest.getForObject(eq(HighlightsService.API_URL + "cle"), eq(tools.jackson.databind.JsonNode.class))).thenReturn(mapper.readTree("""
                {"items": [{"snippet": {"title": "Celtics vs Pistons Highlights", "publishedAt": "2026-10-21T03:10:00Z", "resourceId": {"videoId": "yt"}}}]}"""));
        when(rest.getForObject(startsWith(HighlightsService.VIDEOS_URL), eq(tools.jackson.databind.JsonNode.class))).thenReturn(mapper.readTree("""
                {"items": [{"id": "yt", "contentDetails": {"duration": "PT45S"}}]}"""));
        Match m = espnMatch();
        m.setHighlightsVideoId("50232313");
        m.setHighlightsProvider("espn");
        m.setHighlightsSeconds(74);
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(espn.fetchGameHighlights("401909088")).thenReturn(java.util.Optional.of(
                new EspnStatsClient.EspnVideo("50232313", "Game Highlights", 74)));

        assertThat(new HighlightsService(rest, repo, lockRunning(), "cle", espn).refresh()).isZero();
        assertThat(m.getHighlightsVideoId()).isEqualTo("50232313");
    }

    // --- Chaînes des équipes ---

    private static Match detBos() {
        Match m = match("Pistons", "Celtics");
        m.getHomeTeam().setAbbreviation("DET");
        m.getAwayTeam().setAbbreviation("BOS");
        return m;
    }

    private static HighlightsService.Video video(String title, String published, String team) {
        return new HighlightsService.Video("v", title, Instant.parse(published), team);
    }

    @Test
    void resume_d_une_chaine_d_equipe_qui_nomme_l_adversaire() {
        HighlightsService.Candidate c = HighlightsService.Candidate.of(detBos());

        assertThat(HighlightsService.isAbout(c, video(
                "The Boston Celtics fall to the Detroit Pistons 121 - 113 | Extended Highlights", "2026-10-21T02:00:00Z", "BOS"))).isTrue();
        assertThat(HighlightsService.isAbout(c, video(
                "1st Win of the Season | Detroit Pistons vs. Boston Celtics Highlights | 10.20.26 #NBA", "2026-10-21T02:30:00Z", "DET"))).isTrue();
        // Chaîne d'une autre équipe : jamais.
        assertThat(HighlightsService.isAbout(c, video(
                "Celtics vs Pistons Highlights", "2026-10-21T02:00:00Z", "NYK"))).isFalse();
        assertThat(HighlightsService.estimate("The Boston Celtics fall to the Detroit Pistons | Extended Highlights")).isEqualTo(600);
    }

    @Test
    void surnoms_et_extraits_d_un_seul_joueur() {
        Match phiNyk = match("Knicks", "76ers");
        phiNyk.getHomeTeam().setAbbreviation("NYK");
        phiNyk.getAwayTeam().setAbbreviation("PHI");
        HighlightsService.Candidate c = HighlightsService.Candidate.of(phiNyk);

        assertThat(HighlightsService.isAbout(c, video(
                "Game Highlights: Sixers Take Down the Knicks in Season Opener", "2026-10-21T03:00:00Z", "PHI"))).isTrue();
        assertThat(HighlightsService.isAbout(c, video(
                "Joel Embiid Highlights (31 Points, 12 Rebounds) | 76ers vs Knicks", "2026-10-21T03:00:00Z", "PHI"))).isFalse();
        assertThat(HighlightsService.isAbout(c, video(
                "Sixers vs Knicks Highlights", "2026-10-23T12:00:00Z", "PHI"))).as("publiée 2 jours après").isFalse();
    }

    @Test
    void lit_les_chaines_des_deux_equipes_tant_qu_il_manque_un_resume_long() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        EspnStatsClient espn = mock(EspnStatsClient.class);
        when(rest.getForObject(eq(HighlightsService.FEED_URL), eq(String.class))).thenReturn(feed());
        when(rest.getForObject(eq(TeamChannels.feedUrl(TeamChannels.BY_ABBREVIATION.get("BOS").channelId())), eq(String.class))).thenReturn(feed(
                entry("ext", "The Boston Celtics fall to the Detroit Pistons 121 - 113 | Extended Highlights", "2026-10-21T02:00:00+00:00")));
        when(rest.getForObject(eq(TeamChannels.feedUrl(TeamChannels.BY_ABBREVIATION.get("DET").channelId())), eq(String.class))).thenReturn(feed());
        Match m = detBos();
        m.setEspnEventId("401909088");
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));

        assertThat(new HighlightsService(rest, repo, lockRunning(), "", espn).refresh()).isEqualTo(1);
        assertThat(m.getHighlightsVideoId()).isEqualTo("ext");
        assertThat(m.getHighlightsProvider()).isEqualTo("youtube");
        verifyNoInteractions(espn);
    }

    @Test
    void resume_deja_long_on_ne_lit_pas_les_chaines_des_equipes() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        when(rest.getForObject(eq(HighlightsService.FEED_URL), eq(String.class))).thenReturn(feed());
        Match m = detBos();
        m.setHighlightsVideoId("long");
        m.setHighlightsSeconds(600);
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));

        new HighlightsService(rest, repo, lockRunning(), "", null).refresh();

        verify(rest, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void chaine_injoignable_rien_ne_change_sans_espn() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        when(rest.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("timeout"));
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(match("Celtics", "Pistons")));

        assertThat(new HighlightsService(rest, repo, mock(NbaSyncService.class)).refresh()).isZero();
        verify(repo, never()).save(any());
    }

    @Test
    void chaine_injoignable_espn_quand_meme() {
        RestTemplate rest = mock(RestTemplate.class);
        MatchRepository repo = mock(MatchRepository.class);
        EspnStatsClient espn = mock(EspnStatsClient.class);
        when(rest.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("timeout"));
        Match m = espnMatch();
        when(repo.findByStatusAndDateBetween(eq(MatchStatus.FINISHED), any(), any())).thenReturn(List.of(m));
        when(repo.findById(m.getId())).thenReturn(java.util.Optional.of(m));
        when(espn.fetchGameHighlights("401909088")).thenReturn(java.util.Optional.of(
                new EspnStatsClient.EspnVideo("50232313", "Game Highlights", 74)));

        assertThat(new HighlightsService(rest, repo, lockRunning(), "", espn).refresh()).isEqualTo(1);
        assertThat(m.getHighlightsProvider()).isEqualTo("espn");
    }
}
