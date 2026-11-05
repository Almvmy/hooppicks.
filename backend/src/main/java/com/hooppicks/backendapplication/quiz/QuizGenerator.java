package com.hooppicks.backendapplication.quiz;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compose le quiz d'un jour à partir de nos données : la nuit dernière, les
 * effectifs, le classement. Personne n'a de questions à écrire, et elles changent
 * avec la saison. Sans données suffisantes (intersaison), la liste d'histoire
 * prend le relais. Aucun accès à la base ici : tout arrive déjà lu.
 *
 * Seulement des joueurs de rotation (20 minutes ou plus) : deviner le numéro du
 * douzième homme, ce n'est plus du basket, c'est du hasard.
 */
final class QuizGenerator {

    static final int QUESTIONS = 5;
    private static final int FROM_DATA = 3;

    record PlayerFact(String name, String team, String jersey, String headshot, Double minutes, Integer games,
                      Double points, Double rebounds, Double assists) {}

    record TeamFact(String name, String conference, Integer wins, Integer losses) {}

    record GameFact(String away, String home, int awayScore, int homeScore) {}

    record NightLine(String player, int points) {}

    record Facts(List<PlayerFact> players, List<TeamFact> teams, List<GameFact> lastNight, List<NightLine> lastNightLines) {}

    record Question(String text, String imageUrl, List<String> choices, int correct) {}

    private QuizGenerator() {}

    static List<Question> generate(LocalDate day, Facts facts) {
        Random random = new Random(day.toEpochDay());
        List<PlayerFact> rotation = facts.players().stream()
                .filter(p -> p.minutes() != null && p.minutes() >= 20 && p.team() != null)
                .toList();

        // La nuit dernière d'abord : c'est ce qui récompense ceux qui suivent.
        List<Function<Random, Optional<Question>>> lastNight = new ArrayList<>(List.of(
                r -> topScorer(r, facts.lastNightLines()),
                r -> finalScore(r, facts.lastNight())));
        List<Function<Random, Optional<Question>>> others = new ArrayList<>(List.of(
                r -> whoIsThis(r, rotation),
                r -> playerTeam(r, rotation),
                r -> jersey(r, rotation),
                r -> teammate(r, rotation),
                r -> bestAverage(r, rotation, "de points", PlayerFact::points),
                r -> bestAverage(r, rotation, "de rebonds", PlayerFact::rebounds),
                r -> bestAverage(r, rotation, "de passes", PlayerFact::assists),
                r -> bestRecord(r, facts.teams()),
                r -> conference(r, facts.teams())));
        Collections.shuffle(lastNight, random);
        Collections.shuffle(others, random);

        List<Question> questions = new ArrayList<>();
        lastNight.stream().limit(1).map(g -> g.apply(random)).flatMap(Optional::stream).forEach(questions::add);
        for (Function<Random, Optional<Question>> g : others) {
            if (questions.size() >= FROM_DATA) break;
            g.apply(random).ifPresent(questions::add);
        }
        for (int n = 0; questions.size() < QUESTIONS; n++) {
            QuizBank.Item item = QuizBank.of(day, n);
            questions.add(multipleChoice(random, item.text(), null, item.choices().get(0), item.choices().subList(1, item.choices().size())));
        }
        Collections.shuffle(questions, random);
        return List.copyOf(questions);
    }

    private static Question multipleChoice(Random random, String text, String image, String right, List<String> wrong) {
        List<String> choices = new ArrayList<>(wrong);
        choices.add(right);
        Collections.shuffle(choices, random);
        return new Question(text, image, List.copyOf(choices), choices.indexOf(right));
    }

    private static <T> List<T> pick(Random random, List<T> from, int count) {
        List<T> copy = new ArrayList<>(from);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private static Optional<Question> topScorer(Random random, List<NightLine> lines) {
        List<NightLine> sorted = lines.stream().sorted(Comparator.comparingInt(NightLine::points).reversed()).toList();
        // Le meilleur doit l'être seul, sinon deux réponses seraient justes.
        if (sorted.size() < 4 || sorted.get(0).points() == sorted.get(1).points()) return Optional.empty();
        List<String> wrong = pick(random, sorted.subList(1, Math.min(sorted.size(), 15)), 3).stream().map(NightLine::player).toList();
        return Optional.of(multipleChoice(random, "Qui a marqué le plus de points la nuit dernière ?", null, sorted.get(0).player(), wrong));
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
        return Optional.of(multipleChoice(random, g.away() + " @ " + g.home() + " la nuit dernière : quel score final ?", null, right, wrong));
    }

    private static String score(GameFact g, int away, int home) {
        return g.away() + " " + away + " - " + home + " " + g.home();
    }

    private static Optional<Question> whoIsThis(Random random, List<PlayerFact> rotation) {
        List<PlayerFact> known = rotation.stream().filter(p -> p.headshot() != null && !p.headshot().isBlank()).toList();
        if (known.size() < 4) return Optional.empty();
        List<PlayerFact> four = pick(random, known, 4);
        return Optional.of(multipleChoice(random, "Qui est ce joueur ?", four.get(0).headshot(), four.get(0).name(),
                four.subList(1, 4).stream().map(PlayerFact::name).toList()));
    }

    private static Optional<Question> playerTeam(Random random, List<PlayerFact> rotation) {
        List<String> teams = rotation.stream().map(PlayerFact::team).distinct().toList();
        if (rotation.isEmpty() || teams.size() < 4) return Optional.empty();
        PlayerFact p = rotation.get(random.nextInt(rotation.size()));
        List<String> wrong = pick(random, teams.stream().filter(t -> !t.equals(p.team())).toList(), 3);
        return Optional.of(multipleChoice(random, "Dans quelle équipe joue " + p.name() + " ?", null, p.team(), wrong));
    }

    private static Optional<Question> jersey(Random random, List<PlayerFact> rotation) {
        List<PlayerFact> numbered = rotation.stream().filter(p -> p.jersey() != null && p.jersey().matches("\\d{1,2}")).toList();
        if (numbered.isEmpty()) return Optional.empty();
        PlayerFact p = numbered.get(random.nextInt(numbered.size()));
        List<String> wrong = pick(random, Stream.iterate(0, n -> n + 1).limit(56).map(String::valueOf).filter(n -> !n.equals(p.jersey())).toList(), 3);
        return Optional.of(multipleChoice(random, "Quel numéro porte " + p.name() + " ?", null, p.jersey(), wrong));
    }

    private static Optional<Question> teammate(Random random, List<PlayerFact> rotation) {
        if (rotation.size() < 4) return Optional.empty();
        PlayerFact p = rotation.get(random.nextInt(rotation.size()));
        List<PlayerFact> mates = rotation.stream().filter(o -> o.team().equals(p.team()) && !o.name().equals(p.name())).toList();
        List<PlayerFact> others = rotation.stream().filter(o -> !o.team().equals(p.team())).toList();
        if (mates.isEmpty() || others.size() < 3) return Optional.empty();
        return Optional.of(multipleChoice(random, "Lequel joue avec " + p.name() + " ?", null, mates.get(random.nextInt(mates.size())).name(),
                pick(random, others, 3).stream().map(PlayerFact::name).toList()));
    }

    private static Optional<Question> bestAverage(Random random, List<PlayerFact> rotation, String stat, Function<PlayerFact, Double> value) {
        List<PlayerFact> eligible = rotation.stream().filter(p -> p.games() != null && p.games() >= 3 && value.apply(p) != null).toList();
        if (eligible.size() < 4) return Optional.empty();
        ToDoubleFunction<PlayerFact> number = value::apply;
        List<PlayerFact> four = pick(random, eligible, 4).stream().sorted(Comparator.comparingDouble(number).reversed()).toList();
        // Au moins un point d'écart : pas de question qui se joue à une décimale.
        if (number.applyAsDouble(four.get(0)) - number.applyAsDouble(four.get(1)) < 1) return Optional.empty();
        return Optional.of(multipleChoice(random, "Lequel a la meilleure moyenne " + stat + " cette saison ?", null, four.get(0).name(),
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
        return Optional.of(multipleChoice(random, "Quelle équipe a le meilleur bilan ?", null, four.get(0).name(),
                four.subList(1, 4).stream().map(TeamFact::name).toList()));
    }

    private static Optional<Question> conference(Random random, List<TeamFact> teams) {
        String side = random.nextBoolean() ? "Est" : "Ouest";
        List<String> inside = teams.stream().filter(t -> side.equals(t.conference())).map(TeamFact::name).filter(Objects::nonNull).collect(Collectors.toList());
        List<String> outside = teams.stream().filter(t -> t.conference() != null && !side.equals(t.conference())).map(TeamFact::name).toList();
        if (inside.isEmpty() || outside.size() < 3) return Optional.empty();
        return Optional.of(multipleChoice(random, "Laquelle de ces équipes joue à l'" + side + " ?", null, inside.get(random.nextInt(inside.size())), pick(random, outside, 3)));
    }
}
