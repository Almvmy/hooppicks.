package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.entity.Debate;
import com.hooppicks.backendapplication.entity.DebateVote;
import com.hooppicks.backendapplication.repository.DebateRepository;
import com.hooppicks.backendapplication.repository.DebateVoteRepository;
import com.hooppicks.backendapplication.repository.NightMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le débat du jour : une question par soirée NBA (12h → 12h GMT), deux camps.
 * Résultat et arguments visibles seulement après avoir voté (on se fait d'abord
 * son avis), pour tout le monde une fois le débat clos. Les arguments sont des
 * messages du fil (NightMessage) : mêmes règles, mêmes signalements, même console.
 * Sans notification : tous les jours, ce serait du bruit.
 */
@Service
public class DebateService {

    static final int MAX_QUESTION = 140;
    static final int MAX_OPTION = 40;
    private static final int PLANNING_DAYS = 7;

    public record DebateDto(LocalDate day, String question, String optionA, String optionB, boolean open, String myVote,
                            Long votesA, Long votesB, long total, long comments, List<PastDebate> history) {}

    public record PastDebate(LocalDate day, String question, String optionA, String optionB, long votesA, long votesB) {}

    public record PlannedDebate(LocalDate day, String question, String optionA, String optionB, boolean planned, long votes) {}

    private final DebateRepository debateRepository;
    private final DebateVoteRepository voteRepository;
    private final NightMessageRepository messageRepository;
    private final NightThreadService threadService;
    private final Clock clock;

    @Autowired
    public DebateService(DebateRepository debateRepository, DebateVoteRepository voteRepository,
                         NightMessageRepository messageRepository, NightThreadService threadService) {
        this(debateRepository, voteRepository, messageRepository, threadService, Clock.system(BankrollService.ZONE));
    }

    DebateService(DebateRepository debateRepository, DebateVoteRepository voteRepository,
                  NightMessageRepository messageRepository, NightThreadService threadService, Clock clock) {
        this.debateRepository = debateRepository;
        this.voteRepository = voteRepository;
        this.messageRepository = messageRepository;
        this.threadService = threadService;
        this.clock = clock;
    }

    private LocalDate today() {
        return NightThreadService.nightOf(clock.instant());
    }

    // Deux joueurs peuvent ouvrir l'app en même temps à midi : le second retrouve la ligne du premier.
    private Debate ensure(LocalDate day) {
        return debateRepository.findById(day).orElseGet(() -> {
            DebateBank.Question q = DebateBank.of(day);
            Debate debate = new Debate();
            debate.setDay(day);
            debate.setQuestion(q.text());
            debate.setOptionA(q.a());
            debate.setOptionB(q.b());
            try {
                return debateRepository.saveAndFlush(debate);
            } catch (DataIntegrityViolationException e) {
                return debateRepository.findById(day).orElseThrow(() -> e);
            }
        });
    }

    public DebateDto today(String userId) {
        return dto(userId, ensure(today()));
    }

    /** Un jour passé ou aujourd'hui, jamais un débat prévu à l'avance : on ne dévoile pas la question de demain. */
    public Optional<DebateDto> ofDay(String userId, LocalDate day) {
        if (day.equals(today())) return Optional.of(today(userId));
        if (day.isAfter(today())) return Optional.empty();
        return debateRepository.findById(day).map(d -> dto(userId, d));
    }

    private DebateDto dto(String userId, Debate d) {
        LocalDate day = d.getDay();
        boolean open = day.equals(today());
        String mine = voteRepository.findById(voteId(userId, day)).map(DebateVote::getSide).orElse(null);
        boolean reveal = !open || mine != null;
        return new DebateDto(day, d.getQuestion(), d.getOptionA(), d.getOptionB(), open, mine,
                reveal ? voteRepository.countByDayAndSide(day, "A") : null,
                reveal ? voteRepository.countByDayAndSide(day, "B") : null,
                voteRepository.countByDay(day), messageRepository.countVisibleForDebate(day), history(day));
    }

    private List<PastDebate> history(LocalDate before) {
        return debateRepository.findTop7ByDayLessThanOrderByDayDesc(before).stream()
                .map(d -> new PastDebate(d.getDay(), d.getQuestion(), d.getOptionA(), d.getOptionB(),
                        voteRepository.countByDayAndSide(d.getDay(), "A"), voteRepository.countByDayAndSide(d.getDay(), "B")))
                .filter(p -> p.votesA() + p.votesB() > 0)
                .toList();
    }

    private static String voteId(String userId, LocalDate day) {
        return userId + ":" + day;
    }

    public DebateDto vote(String userId, String side) {
        if (!"A".equals(side) && !"B".equals(side)) throw new IllegalArgumentException("Camp inconnu.");
        Debate debate = ensure(today());
        DebateVote vote = new DebateVote();
        vote.setId(voteId(userId, debate.getDay()));
        vote.setDay(debate.getDay());
        vote.setUserId(userId);
        vote.setSide(side);
        voteRepository.save(vote);
        return dto(userId, debate);
    }

    /** Pas de camp, pas d'arguments, tant que le débat est ouvert. */
    public NightThreadService.Feed comments(String userId, LocalDate day) {
        Debate debate = debateRepository.findById(day).orElseThrow(() -> new IllegalArgumentException("Débat introuvable."));
        boolean open = day.equals(today());
        if (day.isAfter(today())) throw new IllegalArgumentException("Débat introuvable.");
        if (open && !voteRepository.existsById(voteId(userId, day))) throw new IllegalArgumentException("Vote d'abord pour voir les arguments.");
        Map<String, String> sides = new HashMap<>();
        for (DebateVote v : voteRepository.findByDay(day)) sides.put(v.getUserId(), "A".equals(v.getSide()) ? debate.getOptionA() : debate.getOptionB());
        return threadService.debateFeed(userId, day, sides, open ? null : "Ce débat est clos.");
    }

    public NightThreadService.Feed comment(String userId, LocalDate day, String text, String replyToId) {
        if (!day.equals(today())) throw new IllegalArgumentException("Ce débat est clos.");
        if (!voteRepository.existsById(voteId(userId, day))) throw new IllegalArgumentException("Choisis ton camp avant d'argumenter.");
        threadService.postDebate(userId, text, day, replyToId);
        return comments(userId, day);
    }

    // --- Console ---

    /** Les 7 prochains jours, tels que les joueurs les verront. */
    public List<PlannedDebate> planning() {
        List<PlannedDebate> days = new ArrayList<>();
        for (int i = 0; i < PLANNING_DAYS; i++) {
            LocalDate day = today().plusDays(i);
            Debate d = debateRepository.findById(day).orElse(null);
            DebateBank.Question q = DebateBank.of(day);
            days.add(d == null
                    ? new PlannedDebate(day, q.text(), q.a(), q.b(), false, 0)
                    : new PlannedDebate(day, d.getQuestion(), d.getOptionA(), d.getOptionB(), Boolean.TRUE.equals(d.getPlanned()), voteRepository.countByDay(day)));
        }
        return days;
    }

    private void requireEditable(LocalDate day) {
        if (day.isBefore(today())) throw new IllegalArgumentException("Ce jour est passé.");
        // Changer la question sous les votes déjà posés leur ferait dire autre chose.
        if (voteRepository.countByDay(day) > 0) throw new IllegalArgumentException("Des joueurs ont déjà voté sur cette question.");
    }

    public void plan(LocalDate day, String question, String optionA, String optionB) {
        requireEditable(day);
        String q = question == null ? "" : question.strip();
        String a = optionA == null ? "" : optionA.strip();
        String b = optionB == null ? "" : optionB.strip();
        if (q.isEmpty() || a.isEmpty() || b.isEmpty()) throw new IllegalArgumentException("Question et camps obligatoires.");
        if (q.length() > MAX_QUESTION) throw new IllegalArgumentException("Question trop longue (" + MAX_QUESTION + " caractères au plus).");
        if (a.length() > MAX_OPTION || b.length() > MAX_OPTION) throw new IllegalArgumentException("Camp trop long (" + MAX_OPTION + " caractères au plus).");
        if (a.equalsIgnoreCase(b)) throw new IllegalArgumentException("Les deux camps doivent être différents.");
        Debate debate = new Debate();
        debate.setDay(day);
        debate.setQuestion(q);
        debate.setOptionA(a);
        debate.setOptionB(b);
        debate.setPlanned(true);
        debateRepository.save(debate);
    }

    /** Retour à la question de la liste. */
    public void unplan(LocalDate day) {
        requireEditable(day);
        if (debateRepository.existsById(day)) debateRepository.deleteById(day);
    }
}
