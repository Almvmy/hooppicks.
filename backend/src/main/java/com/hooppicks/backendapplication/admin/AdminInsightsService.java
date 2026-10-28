package com.hooppicks.backendapplication.admin;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Chiffres de pilotage de la console admin, en SQL natif (agrégats sur tous
 * les tickets) :
 * - équilibre des cotes : réussite réelle de chaque type de pari face à celle
 *   que ses cotes annonçaient. Une réussite bien au-dessus veut dire des cotes
 *   trop généreuses (c'est ainsi que la base fixe des totaux à 220,5 avait été
 *   repérée : « plus de » gagnait trop souvent) ;
 * - activité par semaine de jeu, et joueurs revenus d'une semaine à l'autre ;
 * - de quoi nourrir les réseaux : plus gros gains et paris les plus joués.
 */
@Service
public class AdminInsightsService {

    static final int BALANCE_DAYS = 30;
    static final int ACTIVITY_WEEKS = 8;
    private static final double VIG = 1.02;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminInsightsService(NamedParameterJdbcTemplate jdbc) {
        this(jdbc, Clock.systemUTC());
    }

    AdminInsightsService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Un type de pari : réussite réelle et réussite annoncée par les cotes (marge retirée), en %. */
    public record BetTypeBalance(String type, long tickets, long won, double actualPct, double expectedPct, long playersNet) {}

    public record WeekActivity(String week, long activePlayers, long returningPlayers, long tickets) {}

    public record BigWin(String betId, String username, String labels, int stake, double odds, long profit, Instant resolvedAt) {}

    public record PopularPick(String label, String matchLabel, long tickets) {}

    public record Insights(int balanceDays, List<BetTypeBalance> balance, List<WeekActivity> activity,
                           List<BigWin> bigWins, List<PopularPick> popular) {}

    // Tickets à une sélection seulement : le résultat d'un combiné ne dit rien
    // d'une sélection en particulier. Les combinés ont leur propre ligne.
    private static final String BALANCE_SQL = """
        SELECT grp, COUNT(*) AS tickets, SUM(CASE WHEN status = 'WON' THEN 1 ELSE 0 END) AS won,
               AVG(1.0 / odds) AS implied,
               SUM(CASE WHEN status = 'WON' THEN potential_payout - stake ELSE -stake END) AS net
        FROM (
            SELECT b.status, b.stake, b.potential_payout, bs.odds,
                   CASE
                     WHEN bs.market = 'moneyline' AND bs.odds < 2 THEN 'Vainqueur · favori'
                     WHEN bs.market = 'moneyline' THEN 'Vainqueur · outsider'
                     WHEN bs.market = 'spread' THEN 'Écart'
                     WHEN bs.market = 'alt_spread' THEN 'Écart alternatif'
                     WHEN bs.market IN ('total', 'alt_total') AND bs.outcome = 'over' THEN 'Total · plus de'
                     WHEN bs.market IN ('total', 'alt_total') THEN 'Total · moins de'
                     WHEN bs.label ~ '[0-9]\\+ ' THEN 'Paliers joueurs'
                     WHEN bs.outcome = 'over' THEN 'Joueurs · plus de'
                     ELSE 'Joueurs · moins de'
                   END AS grp
            FROM bet b JOIN bet_selection bs ON bs.bet_id = b.id
            WHERE b.status IN ('WON', 'LOST') AND b.placed_at >= :since
              AND (SELECT COUNT(*) FROM bet_selection x WHERE x.bet_id = b.id) = 1
            UNION ALL
            SELECT b.status, b.stake, b.potential_payout, b.total_odds, 'Combinés'
            FROM bet b
            WHERE b.status IN ('WON', 'LOST') AND b.placed_at >= :since
              AND (SELECT COUNT(*) FROM bet_selection x WHERE x.bet_id = b.id) > 1
        ) t
        GROUP BY grp
        ORDER BY tickets DESC
        """;

    // Semaine de jeu = lundi 12h GMT, comme le classement.
    private static final String ACTIVITY_SQL = """
        WITH w AS (
            SELECT b.user_id, date_trunc('week', (b.placed_at AT TIME ZONE 'UTC') - INTERVAL '12 hours') AS wk, COUNT(*) AS n
            FROM bet b WHERE b.placed_at >= :since
            GROUP BY b.user_id, wk
        )
        SELECT w.wk, COUNT(*) AS active,
               SUM(CASE WHEN EXISTS (SELECT 1 FROM w p WHERE p.user_id = w.user_id AND p.wk = w.wk - INTERVAL '7 days') THEN 1 ELSE 0 END) AS returning,
               SUM(w.n) AS tickets
        FROM w GROUP BY w.wk ORDER BY w.wk
        """;

    private static final String BIG_WINS_SQL = """
        SELECT b.id, u.username, b.stake, b.total_odds, b.potential_payout - b.stake AS profit, b.resolved_at,
               (SELECT string_agg(bs.label, ' + ' ORDER BY bs.label) FROM bet_selection bs WHERE bs.bet_id = b.id) AS labels
        FROM bet b JOIN app_user u ON u.id = b.user_id
        WHERE b.status = 'WON' AND b.resolved_at >= :since
        ORDER BY profit DESC
        LIMIT 8
        """;

    private static final String POPULAR_SQL = """
        SELECT bs.label, bs.match_label, COUNT(*) AS n
        FROM bet_selection bs JOIN bet b ON b.id = bs.bet_id
        WHERE b.placed_at >= :since
        GROUP BY bs.label, bs.match_label
        ORDER BY n DESC
        LIMIT 8
        """;

    @Transactional(readOnly = true)
    public Insights insights() {
        Instant now = clock.instant();
        MapSqlParameterSource balanceSince = new MapSqlParameterSource("since", Timestamp.from(now.minus(Duration.ofDays(BALANCE_DAYS))));
        List<BetTypeBalance> balance = jdbc.query(BALANCE_SQL, balanceSince, (rs, i) -> {
            long tickets = rs.getLong("tickets");
            long won = rs.getLong("won");
            return new BetTypeBalance(rs.getString("grp"), tickets, won,
                    round1(tickets == 0 ? 0 : won * 100.0 / tickets),
                    round1(rs.getDouble("implied") * 100 / VIG),
                    rs.getLong("net"));
        });

        MapSqlParameterSource activitySince = new MapSqlParameterSource("since", Timestamp.from(now.minus(Duration.ofDays(7L * ACTIVITY_WEEKS + 7))));
        List<WeekActivity> activity = jdbc.query(ACTIVITY_SQL, activitySince, (rs, i) -> new WeekActivity(
                rs.getTimestamp("wk").toLocalDateTime().toLocalDate().toString(),
                rs.getLong("active"), rs.getLong("returning"), rs.getLong("tickets")));
        if (activity.size() > ACTIVITY_WEEKS) activity = activity.subList(activity.size() - ACTIVITY_WEEKS, activity.size());

        MapSqlParameterSource weekSince = new MapSqlParameterSource("since", Timestamp.from(now.minus(Duration.ofDays(7))));
        List<BigWin> bigWins = jdbc.query(BIG_WINS_SQL, weekSince, (rs, i) -> new BigWin(
                rs.getString("id"), rs.getString("username"), rs.getString("labels"), rs.getInt("stake"),
                rs.getDouble("total_odds"), rs.getLong("profit"), rs.getTimestamp("resolved_at").toInstant()));
        List<PopularPick> popular = jdbc.query(POPULAR_SQL, weekSince,
                (rs, i) -> new PopularPick(rs.getString("label"), rs.getString("match_label"), rs.getLong("n")));

        return new Insights(BALANCE_DAYS, balance, activity, bigWins, popular);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
