package com.hooppicks.backendapplication.quiz;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compose le quiz d'un jour à partir de nos données : la nuit dernière, les
 * effectifs, le classement. Personne n'a de questions à écrire, et elles changent
 * avec la saison. Sans données suffisantes (intersaison), la liste d'histoire
 * prend le relais. Aucun accès à la base ici : tout arrive déjà lu.
 *
 * Seulement les joueurs connus : 20 minutes ou plus et parmi les 120 meilleurs
 * marqueurs (4 par équipe). Deviner l'équipe d'un remplaçant que personne ne suit,
 * ce n'est plus du basket, c'est du hasard.
 *
 * Le même modèle ne revient pas deux jours de suite (`History.recentKinds`) : le
 * joueur changeait mais la formule restait, et le quiz sonnait pareil chaque jour.
 */
final class QuizGenerator {

    static final int QUESTIONS = 5;
    private static final int FROM_DATA = 3;
    private static final int FROM_LAST_NIGHT = 2;
    private static final int KNOWN_PLAYERS = 120;

    record PlayerFact(String name, String team, String jersey, String headshot, Double minutes, Integer games,
                      Double points, Double rebounds, Double assists, Double threes) {
        PlayerFact(String name, String team, String jersey, String headshot, Double minutes, Integer games, Double points, Double rebounds, Double assists) {
            this(name, team, jersey, headshot, minutes, games, points, rebounds, assists, null);
        }
    }

    record TeamFact(String name, String conference, Integer wins, Integer losses, Integer seed, String streak) {
        TeamFact(String name, String conference, Integer wins, Integer losses) {
            this(name, conference, wins, losses, null, null);
        }
    }

    record GameFact(String away, String home, int awayScore, int homeScore) {}

    record NightLine(String player, int points, int rebounds, int assists, int threes) {
        NightLine(String player, int points) {
            this(player, points, 0, 0, 0);
        }
    }

    record Facts(List<PlayerFact> players, List<TeamFact> teams, List<GameFact> lastNight, List<NightLine> lastNightLines) {}

    /** Ce qui est déjà sorti : les questions d'histoire de la saison, les modèles des derniers jours. */
    record History(Set<String> usedBankTexts, Set<String> recentKinds) {
        static final History NONE = new History(Set.of(), Set.of());
    }

    record Question(String kind, String text, String imageUrl, List<String> choices, int correct) {}

    private record Template(String kind, Function<Random, Optional<Question>> make) {}

    private QuizGenerator() {}

    static List<Question> generate(LocalDate day, Facts facts) {
        return generate(day, facts, History.NONE);
    }

    static List<Question> generate(LocalDate day, Facts facts, History history) {
        Random random = new Random(day.toEpochDay());
        List<PlayerFact> rotation = facts.players().stream()
                .filter(p -> p.minutes() != null && p.minutes() >= 20 && p.team() != null)
                .sorted(Comparator.comparingDouble((PlayerFact p) -> p.points() == null ? 0 : p.points()).reversed())
                .limit(KNOWN_PLAYERS)
                .toList();
        List<NightLine> lines = facts.lastNightLines();
        List<TeamFact> teams = facts.teams();

        // La nuit dernière d'abord : c'est ce qui récompense ceux qui suivent.
        List<Template> lastNight = new ArrayList<>(List.of(
                new Template("nuit-points", r -> nightBest(r, "nuit-points", lines, NightLine::points, "Qui a marqué le plus de points la nuit dernière ?")),
                new Template("nuit-rebonds", r -> nightBest(r, "nuit-rebonds", lines, NightLine::rebounds, "Qui a pris le plus de rebonds la nuit dernière ?")),
                new Template("nuit-passes", r -> nightBest(r, "nuit-passes", lines, NightLine::assists, "Qui a fait le plus de passes la nuit dernière ?")),
                new Template("nuit-trois", r -> nightBest(r, "nuit-trois", lines, NightLine::threes, "Qui a réussi le plus de tirs à 3 points la nuit dernière ?")),
                new Template("nuit-triple", r -> tripleDouble(r, lines)),
                new Template("nuit-combien", r -> howManyPoints(r, lines)),
                new Template("nuit-score", r -> finalScore(r, facts.lastNight())),
                new Template("nuit-ecart", r -> biggestWin(r, facts.lastNight()))));
        List<Template> others = new ArrayList<>(List.of(
                new Template("photo", r -> whoIsThis(r, rotation)),
                new Template("equipe", r -> playerTeam(r, rotation)),
                new Template("numero", r -> jersey(r, rotation)),
                new Template("coequipier", r -> teammate(r, rotation)),
                new Template("moy-points", r -> bestAverage(r, "moy-points", rotation, "de points", PlayerFact::points)),
                new Template("moy-rebonds", r -> bestAverage(r, "moy-rebonds", rotation, "de rebonds", PlayerFact::rebounds)),
                new Template("moy-passes", r -> bestAverage(r, "moy-passes", rotation, "de passes", PlayerFact::assists)),
                new Template("moy-trois", r -> bestAverage(r, "moy-trois", rotation, "de tirs à 3 points réussis", PlayerFact::threes)),
                new Template("bilan", r -> bestRecord(r, teams)),
                new Template("conference", r -> conference(r, teams)),
                new Template("leader", r -> conferenceLeader(r, teams)),
                new Template("serie", r -> longestStreak(r, teams))));
        Collections.shuffle(lastNight, random);
        Collections.shuffle(others, random);

        List<Question> questions = new ArrayList<>();
        take(lastNight, FROM_LAST_NIGHT, random, history, questions);
        take(others, FROM_DATA - questions.size(), random, history, questions);

        int fromBank = QUESTIONS - questions.size();
        for (QuizBank.Item item : QuizBank.pick(day, fromBank, history.usedBankTexts())) {
            questions.add(multipleChoice(random, "histoire-" + item.theme(), item.text(), null, item.choices().get(0), item.choices().subList(1, item.choices().size())));
        }
        Collections.shuffle(questions, random);
        return List.copyOf(questions);
    }

    // Les modèles qui n'ont pas servi ces derniers jours d'abord ; les autres si ça ne suffit pas.
    private static void take(List<Template> templates, int count, Random random, History history, List<Question> into) {
        Stream<Template> ordered = Stream.concat(
                templates.stream().filter(t -> !history.recentKinds().contains(t.kind())),
                templates.stream().filter(t -> history.recentKinds().contains(t.kind())));
        int added = 0;
        for (Template t : (Iterable<Template>) ordered::iterator) {
            if (added >= count) break;
            Optional<Question> q = t.make().apply(random);
            if (q.isPresent()) {
                into.add(q.get());
                added++;
            }
        }
    }

    private static Question multipleChoice(Random random, String kind, String text, String image, String right, List<String> wrong) {
        List<String> choices = new ArrayList<>(wrong);
        choices.add(right);
        Collections.shuffle(choices, random);
        return new Question(kind, text, image, List.copyOf(choices), choices.indexOf(right));
    }

    private static <T> List<T> pick(Random random, List<T> from, int count) {
        List<T> copy = new ArrayList<>(from);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private static Optional<Question> nightBest(Random random, String kind, List<NightLine> lines, ToIntFunction<NightLine> stat, String text) {
        List<NightLine> sorted = lines.stream().sorted(Comparator.comparingInt(stat).reversed()).toList();
        // Le meilleur doit l'être seul, sinon deux réponses seraient justes.
        if (sorted.size() < 4 || stat.applyAsInt(sorted.get(0)) == 0 || stat.applyAsInt(sorted.get(0)) == stat.applyAsInt(sorted.get(1))) return Optional.empty();
        List<String> wrong = pick(random, sorted.subList(1, Math.min(sorted.size(), 15)), 3).stream().map(NightLine::player).toList();
        return Optional.of(multipleChoice(random, kind, text, null, sorted.get(0).player(), wrong));
    }

    private static boolean isTripleDouble(NightLine l) {
        return l.points() >= 10 && l.rebounds() >= 10 && l.assists() >= 10;
    }

    private static Optional<Question> tripleDouble(Random random, List<NightLine> lines) {
        List<NightLine> doubles = lines.stream().filter(QuizGenerator::isTripleDouble).toList();
        List<NightLine> others = lines.stream().filter(l -> !isTripleDouble(l)).sorted(Comparator.comparingInt(NightLine::points).reversed()).limit(15).toList();
        if (doubles.isEmpty() || others.size() < 3) return Optional.empty();
        return Optional.of(multipleChoice(random, "nuit-triple", "Qui a réussi un triple-double la nuit dernière ?", null,
                doubles.get(random.nextInt(doubles.size())).player(), pick(random, others, 3).stream().map(NightLine::player).toList()));
    }

    private static Optional<Question> howManyPoints(Random random, List<NightLine> lines) {
        List<NightLine> scorers = lines.stream().sorted(Comparator.comparingInt(NightLine::points).reversed()).limit(12).filter(l -> l.points() >= 15).toList();
        if (scorers.isEmpty()) return Optional.empty();
        NightLine l = scorers.get(random.nextInt(scorers.size()));
        int n = l.points();
        return Optional.of(multipleChoice(random, "nuit-combien", "Combien de points a marqué " + l.player() + " la nuit dernière ?", null, String.valueOf(n),
                List.of(String.valueOf(n + 4), String.valueOf(n - 3), String.valueOf(n + 9))));
    }

    private static Optional<Question> finalScore(Random random, List<GameFact> games) {
        if (games.isEmpty()) return Optional.empty();
        GameFact g = games.get(random.nextInt(games.size()));
        String right = score(g, g.awayScore(), g.homeScore());
        // Le vainqueur inversé et des écarts crédibles, pas un 150-60 qui se repère tout seul.
        List<String> wrong = List.of(
                score(g, g.homeScore(), g.awayScore()),
                score(g, g.awayScore() + 7, g.homeScore() - 4),
                score(g, g.awayScore() - 6, g.homeScore() + 5));
        if (wrong.contains(right)) return Optional.empty();
        return Optional.of(multipleChoice(random, "nuit-score", g.away() + " @ " + g.home() + " la nuit dernière : quel score final ?", null, right, wrong));
    }

    private static String score(GameFact g, int away, int home) {
        return g.away() + " " + away + " - " + home + " " + g.home();
    }

    private static int margin(GameFact g) {
        return Math.abs(g.homeScore() - g.awayScore());
    }

    private static Optional<Question> biggestWin(Random random, List<GameFact> games) {
        if (games.size() < 4) return Optional.empty();
        List<GameFact> four = pick(random, games, 4).stream().sorted(Comparator.comparingInt(QuizGenerator::margin).reversed()).toList();
        if (margin(four.get(0)) == margin(four.get(1))) return Optional.empty();
        Function<GameFact, String> label = g -> g.away() + " @ " + g.home();
        return Optional.of(multipleChoice(random, "nuit-ecart", "Quel match a fini avec le plus gros écart la nuit dernière ?", null, label.apply(four.get(0)),
                four.subList(1, 4).stream().map(label).toList()));
    }

    private static Optional<Question> whoIsThis(Random random, List<PlayerFact> rotation) {
        List<PlayerFact> known = rotation.stream().filter(p -> p.headshot() != null && !p.headshot().isBlank()).toList();
        if (known.size() < 4) return Optional.empty();
        List<PlayerFact> four = pick(random, known, 4);
        return Optional.of(multipleChoice(random, "photo", "Qui est ce joueur ?", four.get(0).headshot(), four.get(0).name(),
                four.subList(1, 4).stream().map(PlayerFact::name).toList()));
    }

    private static Optional<Question> playerTeam(Random random, List<PlayerFact> rotation) {
        List<String> teams = rotation.stream().map(PlayerFact::team).distinct().toList();
        if (rotation.isEmpty() || teams.size() < 4) return Optional.empty();
        PlayerFact p = rotation.get(random.nextInt(rotation.size()));
        List<String> wrong = pick(random, teams.stream().filter(t -> !t.equals(p.team())).toList(), 3);
        return Optional.of(multipleChoice(random, "equipe", "Dans quelle équipe joue " + p.name() + " ?", null, p.team(), wrong));
    }

    private static Optional<Question> jersey(Random random, List<PlayerFact> rotation) {
        List<PlayerFact> numbered = rotation.stream().filter(p -> p.jersey() != null && p.jersey().matches("\\d{1,2}")).toList();
        if (numbered.isEmpty()) return Optional.empty();
        PlayerFact p = numbered.get(random.nextInt(numbered.size()));
        List<String> wrong = pick(random, Stream.iterate(0, n -> n + 1).limit(56).map(String::valueOf).filter(n -> !n.equals(p.jersey())).toList(), 3);
        return Optional.of(multipleChoice(random, "numero", "Quel numéro porte " + p.name() + " ?", null, p.jersey(), wrong));
    }

    private static Optional<Question> teammate(Random random, List<PlayerFact> rotation) {
        if (rotation.size() < 4) return Optional.empty();
        PlayerFact p = rotation.get(random.nextInt(rotation.size()));
        List<PlayerFact> mates = rotation.stream().filter(o -> o.team().equals(p.team()) && !o.name().equals(p.name())).toList();
        List<PlayerFact> others = rotation.stream().filter(o -> !o.team().equals(p.team())).toList();
        if (mates.isEmpty() || others.size() < 3) return Optional.empty();
        return Optional.of(multipleChoice(random, "coequipier", "Lequel joue avec " + p.name() + " ?", null, mates.get(random.nextInt(mates.size())).name(),
                pick(random, others, 3).stream().map(PlayerFact::name).toList()));
    }

    private static Optional<Question> bestAverage(Random random, String kind, List<PlayerFact> rotation, String stat, Function<PlayerFact, Double> value) {
        List<PlayerFact> eligible = rotation.stream().filter(p -> p.games() != null && p.games() >= 3 && value.apply(p) != null).toList();
        if (eligible.size() < 4) return Optional.empty();
        ToDoubleFunction<PlayerFact> number = value::apply;
        List<PlayerFact> four = pick(random, eligible, 4).stream().sorted(Comparator.comparingDouble(number).reversed()).toList();
        // Assez d'écart pour que la réponse ne se joue pas à une décimale.
        if (number.applyAsDouble(four.get(0)) - number.applyAsDouble(four.get(1)) < 0.5) return Optional.empty();
        return Optional.of(multipleChoice(random, kind, "Lequel a la meilleure moyenne " + stat + " cette saison ?", null, four.get(0).name(),
                four.subList(1, 4).stream().map(PlayerFact::name).toList()));
    }

    private static double winRate(TeamFact t) {
        return (double) t.wins() / (t.wins() + t.losses());
    }

    private static Optional<Question> bestRecord(Random random, List<TeamFact> teams) {
        List<TeamFact> played = teams.stream().filter(t -> t.wins() != null && t.losses() != null && t.wins() + t.losses() > 0).toList();
        if (played.size() < 4) return Optional.empty();
        List<TeamFact> four = pick(random, played, 4).stream().sorted(Comparator.comparingDouble(QuizGenerator::winRate).reversed()).toList();
        if (winRate(four.get(0)) == winRate(four.get(1))) return Optional.empty();
        return Optional.of(multipleChoice(random, "bilan", "Quelle équipe a le meilleur bilan ?", null, four.get(0).name(),
                four.subList(1, 4).stream().map(TeamFact::name).toList()));
    }

    private static Optional<Question> conference(Random random, List<TeamFact> teams) {
        String side = random.nextBoolean() ? "Est" : "Ouest";
        List<String> inside = teams.stream().filter(t -> side.equals(t.conference())).map(TeamFact::name).filter(Objects::nonNull).collect(Collectors.toList());
        List<String> outside = teams.stream().filter(t -> t.conference() != null && !side.equals(t.conference())).map(TeamFact::name).toList();
        if (inside.isEmpty() || outside.size() < 3) return Optional.empty();
        return Optional.of(multipleChoice(random, "conference", "Laquelle de ces équipes joue à l'" + side + " ?", null, inside.get(random.nextInt(inside.size())), pick(random, outside, 3)));
    }

    private static Optional<Question> conferenceLeader(Random random, List<TeamFact> teams) {
        String side = random.nextBoolean() ? "Est" : "Ouest";
        List<TeamFact> conference = teams.stream().filter(t -> side.equals(t.conference()) && t.seed() != null).toList();
        TeamFact leader = conference.stream().filter(t -> t.seed() == 1).findFirst().orElse(null);
        List<TeamFact> others = conference.stream().filter(t -> t.seed() != 1).toList();
        if (leader == null || others.size() < 3) return Optional.empty();
        return Optional.of(multipleChoice(random, "leader", "Qui mène la conférence " + side + " ?", null, leader.name(), pick(random, others, 3).stream().map(TeamFact::name).toList()));
    }

    // « W5 » : 5 victoires de suite ; une série de défaites ne compte pas.
    private static int winStreak(TeamFact t) {
        String s = t.streak();
        return s != null && s.matches("W\\d+") ? Integer.parseInt(s.substring(1)) : 0;
    }

    private static Optional<Question> longestStreak(Random random, List<TeamFact> teams) {
        List<TeamFact> known = teams.stream().filter(t -> t.streak() != null).toList();
        if (known.size() < 4) return Optional.empty();
        List<TeamFact> four = pick(random, known, 4).stream().sorted(Comparator.comparingInt(QuizGenerator::winStreak).reversed()).toList();
        if (winStreak(four.get(0)) < 2 || winStreak(four.get(0)) == winStreak(four.get(1))) return Optional.empty();
        return Optional.of(multipleChoice(random, "serie", "Laquelle de ces équipes a la plus longue série de victoires ?", null, four.get(0).name(),
                four.subList(1, 4).stream().map(TeamFact::name).toList()));
    }
}
