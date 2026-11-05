package com.hooppicks.backendapplication.social;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.league.LeagueService;
import com.hooppicks.backendapplication.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SocialService {

    // Un profil montre une dizaine de tickets : de la marge, sans laisser tout demander.
    static final int MAX_TICKETS = 30;

    /** Un membre de ses ligues qui a joué ce match. `status` : pending, won, lost. */
    public record FriendPick(String username, int avatarNumber, String avatarPosition, String avatarColorway,
                             String avatarIcon, String label, String status) {}

    public record Reactions(Map<String, Integer> counts, List<String> mine) {}

    private final LeagueMembershipRepository membershipRepository;
    private final BetSelectionRepository selectionRepository;
    private final BetRepository betRepository;
    private final ActivityReactionRepository reactionRepository;
    private final LeagueService leagueService;

    public SocialService(LeagueMembershipRepository membershipRepository, BetSelectionRepository selectionRepository,
                         BetRepository betRepository, ActivityReactionRepository reactionRepository, LeagueService leagueService) {
        this.membershipRepository = membershipRepository;
        this.selectionRepository = selectionRepository;
        this.betRepository = betRepository;
        this.reactionRepository = reactionRepository;
        this.leagueService = leagueService;
    }

    /**
     * Ce que les membres de ses ligues ont joué sur ce match. Tickets en attente
     * compris : le fil d'une ligue les montre déjà à ses membres, jamais aux
     * autres joueurs. Une ligne par joueur et par sélection.
     */
    @Transactional(readOnly = true)
    public List<FriendPick> friendsOnMatch(String matchId, String userId) {
        Set<String> mates = new HashSet<>();
        for (LeagueMembership mine : membershipRepository.findByUserId(userId)) {
            for (LeagueMembership m : membershipRepository.findByLeagueId(mine.getLeague().getId())) {
                mates.add(m.getUser().getId());
            }
        }
        mates.remove(userId);
        if (mates.isEmpty()) return List.of();

        return selectionRepository.findFriendsOnMatch(matchId, mates).stream().map(s -> {
            User u = s.getBet().getUser();
            return new FriendPick(u.getUsername(), u.getAvatarNumber(), u.getAvatarPosition(), u.getAvatarColorway(),
                    u.getAvatarIcon(), s.getLabel(), s.getBet().getStatus().name().toLowerCase());
        }).toList();
    }

    /** Réactions des tickets réglés demandés. Un ticket en attente n'en a pas ici : il reste privé hors des ligues. */
    @Transactional(readOnly = true)
    public Map<String, Reactions> ticketReactions(List<String> ids, String userId) {
        List<String> capped = ids.stream().limit(MAX_TICKETS).toList();
        if (capped.isEmpty()) return Map.of();
        Map<String, Reactions> out = new HashMap<>();
        for (String id : capped) out.put(id, new Reactions(new HashMap<>(), new ArrayList<>()));
        for (ActivityReaction r : reactionRepository.findByTargetTypeInAndTargetIdIn(List.of("BET"), capped)) {
            Reactions reactions = out.get(r.getTargetId());
            reactions.counts().merge(r.getEmoji(), 1, Integer::sum);
            if (r.getUser().getId().equals(userId)) reactions.mine().add(r.getEmoji());
        }
        return out;
    }

    /** Sur un ticket réglé seulement : ceux que les profils montrent à tout le monde. */
    @Transactional
    public void reactToTicket(String betId, String userId, String emoji) {
        Bet bet = betRepository.findById(betId).orElseThrow(() -> new IllegalArgumentException("Ticket introuvable."));
        if (bet.getStatus() == BetStatus.PENDING) throw new IllegalArgumentException("Ce ticket n'est pas encore réglé.");
        leagueService.applyReaction("BET", betId, userId, emoji);
    }
}
