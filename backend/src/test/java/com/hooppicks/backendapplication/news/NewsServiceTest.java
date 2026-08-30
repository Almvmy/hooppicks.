package com.hooppicks.backendapplication.news;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NewsServiceTest {

    private final NewsService service = new NewsService(new RestTemplate(), new DeepLService(new RestTemplate()));

    /**
     * ESPN étiquette toujours ses pubDate "EST" (UTC-5), même en plein été
     * quand l'heure réelle de l'Est américain est EDT (UTC-4) : bug piégé en
     * prod : les articles récents s'affichaient avec une date dans le futur.
     * En août, l'heure réelle est EDT -> l'instant attendu est décalé de -4h
     * par rapport à l'heure murale, pas -5h comme l'étiquette EST le suggère.
     */
    @Test
    void parseDate_ignoresEstLabelDuringDaylightSavingTime() {
        Instant parsed = service.parseDate("Wed, 26 Aug 2026 19:00:55 EST");

        assertThat(parsed).isEqualTo(Instant.parse("2026-08-26T23:00:55Z"));
    }

    /** En janvier (hiver), l'Est américain est bien en EST (UTC-5) : l'étiquette est alors correcte. */
    @Test
    void parseDate_appliesEstOffsetOutsideDaylightSavingTime() {
        Instant parsed = service.parseDate("Thu, 15 Jan 2026 19:00:55 EST");

        assertThat(parsed).isEqualTo(Instant.parse("2026-01-16T00:00:55Z"));
    }

    @Test
    void parseDate_fallsBackToNowOnUnparseableInput() {
        Instant before = Instant.now();
        Instant parsed = service.parseDate("not a date");
        Instant after = Instant.now();

        assertThat(parsed).isBetween(before, after);
    }

    @Test
    void parseDate_fallsBackToNowOnNullInput() {
        Instant before = Instant.now();
        Instant parsed = service.parseDate(null);
        Instant after = Instant.now();

        assertThat(parsed).isBetween(before, after);
    }
}
