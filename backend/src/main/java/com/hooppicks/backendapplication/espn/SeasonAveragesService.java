package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.bet.PlayerPropsService;
import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.season.SeasonCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Moyennes de la saison en cours calculées à partir de nos propres feuilles
 * de match (`PlayerMatchStat`, importées après chaque match), plutôt que par
 * un appel ESPN par joueur : en production, ces ~550 appels n'aboutissaient
 * plus depuis Railway et tous les joueurs gardaient leurs moyennes de la
 * saison passée (constaté le 24 octobre 2026), y compris pour les lignes des
 * paris joueurs. Une seule requête groupée, rejouée à chaque tick de synchro.
 *
 * Au premier match de la saison d'un joueur, ses moyennes en base passent
 * dans `prev*` (saison précédente), comme le faisait la synchro ESPN : c'est
 * ce qui fait glisser les lignes des paris joueurs sur 10 matchs.
 * Joueur relié par son nom normalisé (feuille ESPN et effectif ESPN écrivent
 * le même nom, accents près), comme au règlement des paris joueurs.
 */
@Service
public class SeasonAveragesService {

    private static final Logger log = LoggerFactory.getLogger(SeasonAveragesService.class);

    record Totals(int games, int starts, int minutes, int points, int rebounds, int assists, int steals,
                  int blocks, int turnovers, int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        Totals plus(Totals o) {
            return new Totals(games + o.games, starts + o.starts, minutes + o.minutes, points + o.points,
                    rebounds + o.rebounds, assists + o.assists, steals + o.steals, blocks + o.blocks,
                    turnovers + o.turnovers, fgm + o.fgm, fga + o.fga, tpm + o.tpm, tpa + o.tpa, ftm + o.ftm, fta + o.fta);
        }
    }

    private final PlayerMatchStatRepository statRepository;
    private final RosterPlayerRepository rosterRepository;
    private final SeasonCalendar seasonCalendar;

    public SeasonAveragesService(PlayerMatchStatRepository statRepository, RosterPlayerRepository rosterRepository,
                                 SeasonCalendar seasonCalendar) {
        this.statRepository = statRepository;
        this.rosterRepository = rosterRepository;
        this.seasonCalendar = seasonCalendar;
    }

    /** Recalcule les moyennes de la saison. Renvoie le nombre de joueurs mis à jour. */
    @Transactional
    public int refresh() {
        String season = seasonCalendar.currentSeason();
        Map<String, Totals> byName = new HashMap<>();
        for (Object[] row : statRepository.seasonTotals(LeaderboardPeriod.seasonStart(ZonedDateTime.now(java.time.ZoneOffset.UTC)))) {
            byName.merge(PlayerPropsService.normalize((String) row[0]), totals(row), Totals::plus);
        }
        if (byName.isEmpty()) return 0;

        int updated = 0;
        for (RosterPlayer p : rosterRepository.findAll()) {
            Totals t = byName.get(PlayerPropsService.normalize(p.getFirstName() + " " + p.getLastName()));
            if (t == null || t.games() == 0) continue;
            if (apply(p, t, season)) updated++;
        }
        if (updated > 0) log.info("Moyennes {} recalculées pour {} joueur(s)", season, updated);
        return updated;
    }

    /** true si quelque chose a changé (rien n'est réécrit sinon). */
    static boolean apply(RosterPlayer p, Totals t, String season) {
        if (season.equals(p.getStatsSeasonLabel()) && Objects.equals(p.getGamesPlayed(), t.games())
                && Objects.equals(p.getPointsPerGame(), avg(t.points(), t.games()))) {
            return false;
        }
        // Première fois cette saison : les moyennes en base sont celles d'une saison passée.
        if (p.getStatsSeasonLabel() != null && !season.equals(p.getStatsSeasonLabel())) {
            p.setPrevSeasonLabel(p.getStatsSeasonLabel());
            p.setPrevGamesPlayed(p.getGamesPlayed());
            p.setPrevPointsPerGame(p.getPointsPerGame());
            p.setPrevReboundsPerGame(p.getReboundsPerGame());
            p.setPrevAssistsPerGame(p.getAssistsPerGame());
            p.setPrevThreePointersMadePerGame(p.getThreePointersMadePerGame());
            p.setPrevStealsPerGame(p.getStealsPerGame());
            p.setPrevBlocksPerGame(p.getBlocksPerGame());
            p.setPrevMinutesPerGame(p.getMinutesPerGame());
        }
        int g = t.games();
        p.setStatsSeasonLabel(season);
        p.setGamesPlayed(g);
        p.setGamesStarted(t.starts());
        p.setMinutesPerGame(avg(t.minutes(), g));
        p.setPointsPerGame(avg(t.points(), g));
        p.setReboundsPerGame(avg(t.rebounds(), g));
        p.setAssistsPerGame(avg(t.assists(), g));
        p.setStealsPerGame(avg(t.steals(), g));
        p.setBlocksPerGame(avg(t.blocks(), g));
        p.setTurnoversPerGame(avg(t.turnovers(), g));
        p.setThreePointersMadePerGame(avg(t.tpm(), g));
        p.setFieldGoalPct(pct(t.fgm(), t.fga()));
        p.setThreePointPct(pct(t.tpm(), t.tpa()));
        p.setFreeThrowPct(pct(t.ftm(), t.fta()));
        return true;
    }

    private static Totals totals(Object[] r) {
        return new Totals(n(r[1]), n(r[2]), n(r[3]), n(r[4]), n(r[5]), n(r[6]), n(r[7]), n(r[8]), n(r[9]),
                n(r[10]), n(r[11]), n(r[12]), n(r[13]), n(r[14]), n(r[15]));
    }

    private static int n(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    // Arrondi au dixième, comme ESPN : 26,04 et 26,0 ne réécrivent pas la ligne pour rien.
    static Double avg(int total, int games) {
        return games == 0 ? null : Math.round(total * 10.0 / games) / 10.0;
    }

    static Double pct(int made, int attempted) {
        return attempted == 0 ? null : Math.round(made * 1000.0 / attempted) / 10.0;
    }

    /** Pour les tests. */
    static Totals totalsOf(List<Integer> v) {
        return new Totals(v.get(0), v.get(1), v.get(2), v.get(3), v.get(4), v.get(5), v.get(6), v.get(7),
                v.get(8), v.get(9), v.get(10), v.get(11), v.get(12), v.get(13), v.get(14));
    }
}
