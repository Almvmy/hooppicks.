package com.hooppicks.backendapplication.league;

import com.hooppicks.backendapplication.dto.LeaderboardEntryDto;
import com.hooppicks.backendapplication.dto.LeagueActivityDto;
import com.hooppicks.backendapplication.dto.LeagueDto;
import com.hooppicks.backendapplication.dto.LeagueMemberDto;
import com.hooppicks.backendapplication.dto.LeaguePreviewDto;
import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.League;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LeagueService {

    // Alphabet réduit : sans 0/O/1/I/L pour éviter les confusions à la lecture/saisie.
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final NotificationRepository notificationRepository;

    public LeagueService(LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                          UserRepository userRepository, BetRepository betRepository,
                          NotificationRepository notificationRepository) {
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public LeagueDto createLeague(String ownerId, String name) {
        League league = new League();
        league.setName(name);
        league.setOwnerId(ownerId);
        league.setInviteCode(generateUniqueInviteCode());
        leagueRepository.save(league);

        addMembership(league, ownerId);

        return toDto(league, ownerId);
    }

    @Transactional
    public LeagueDto joinLeague(String userId, String inviteCode) {
        League league = leagueRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Code d'invitation invalide."));

        if (membershipRepository.findByLeagueIdAndUserId(league.getId(), userId).isEmpty()) {
            User joiningUser = addMembership(league, userId);
            notifyExistingMembersOfNewJoin(league, joiningUser);
        }

        return toDto(league, userId);
    }

    public List<LeagueDto> getUserLeagues(String userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(m -> toDto(m.getLeague(), userId))
                .toList();
    }

    /**
     * Aperçu d'une ligue par son code, sans rejoindre — sert à afficher une
     * confirmation ("Rejoindre [nom] ?") avant l'adhésion effective.
     */
    public LeaguePreviewDto previewByCode(String inviteCode) {
        League league = leagueRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Code d'invitation invalide."));
        long memberCount = membershipRepository.countByLeagueId(league.getId());
        return new LeaguePreviewDto(league.getId(), league.getName(), memberCount);
    }

    /**
     * Liste des membres d'une ligue (tous, y compris ceux sans pari résolu —
     * contrairement au classement, qui ne montre que les membres avec au
     * moins un pari WON/LOST). Lève IllegalStateException si l'appelant
     * n'est pas membre, au contrôleur de traduire ça en 403.
     */
    public List<LeagueMemberDto> getMembers(String leagueId, String requestingUserId) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }

        League league = leagueRepository.findById(leagueId).orElseThrow();
        return membershipRepository.findByLeagueId(leagueId).stream()
                .sorted((a, b) -> a.getJoinedAt().compareTo(b.getJoinedAt()))
                .map(m -> new LeagueMemberDto(
                        m.getUser().getUsername(),
                        m.getUser().getId().equals(league.getOwnerId()),
                        m.getJoinedAt()
                ))
                .toList();
    }

    /**
     * Fil d'activité : adhésions + gros paris gagnés récemment parmi les
     * membres, fusionnés et triés du plus récent au plus ancien. Rend la
     * ligue vivante plutôt qu'un simple classement figé. Même garde
     * d'appartenance que le reste (403 via IllegalStateException).
     */
    public List<LeagueActivityDto> getRecentActivity(String leagueId, String requestingUserId) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }

        List<LeagueMembership> memberships = membershipRepository.findByLeagueId(leagueId);
        List<String> memberIds = memberships.stream().map(m -> m.getUser().getId()).toList();

        List<LeagueActivityDto> activity = new ArrayList<>();

        for (LeagueMembership m : memberships) {
            activity.add(new LeagueActivityDto(m.getUser().getUsername(), "a rejoint la ligue", m.getJoinedAt()));
        }

        for (Bet bet : betRepository.findTop10ByUser_IdInAndStatusOrderByResolvedAtDesc(memberIds, BetStatus.WON)) {
            activity.add(new LeagueActivityDto(
                    bet.getUser().getUsername(),
                    "a gagné un ticket : +" + bet.getPotentialPayout() + " pts",
                    bet.getResolvedAt()
            ));
        }

        return activity.stream()
                .sorted(Comparator.comparing(LeagueActivityDto::occurredAt).reversed())
                .limit(10)
                .toList();
    }

    /**
     * Classement scopé aux membres de la ligue. Lève IllegalStateException si
     * l'appelant n'est pas membre — au contrôleur de traduire ça en 403.
     */
    public List<LeaderboardEntryDto> getLeagueLeaderboard(String leagueId, String requestingUserId) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }

        List<String> memberIds = membershipRepository.findByLeagueId(leagueId).stream()
                .map(m -> m.getUser().getId())
                .toList();

        List<Object[]> rows = betRepository.getLeaderboardRawForUsers(memberIds);
        List<LeaderboardEntryDto> result = new ArrayList<>();

        int rank = 1;
        for (Object[] row : rows) {
            String username = (String) row[1];
            long points = (Long) row[2];
            long totalBets = (Long) row[3];
            long wonBets = (Long) row[4];
            int winRate = totalBets == 0 ? 0 : (int) Math.round((wonBets * 100.0) / totalBets);

            result.add(new LeaderboardEntryDto(rank++, username, (int) points, winRate, (int) totalBets));
        }
        return result;
    }

    @Transactional
    public void leaveLeague(String leagueId, String userId) {
        membershipRepository.findByLeagueIdAndUserId(leagueId, userId).ifPresent(membership -> {
            League league = membership.getLeague();
            boolean wasOwner = league.getOwnerId().equals(userId);

            membershipRepository.delete(membership);
            membershipRepository.flush();

            List<LeagueMembership> remaining = membershipRepository.findByLeagueId(leagueId);

            // Plus personne dans la ligue : autant la nettoyer plutôt que de
            // laisser un code d'invitation orphelin traîner indéfiniment.
            if (remaining.isEmpty()) {
                leagueRepository.deleteById(leagueId);
            } else if (wasOwner) {
                // Pas de transfert manuel dans ce produit : si le proprio part,
                // la ligue ne doit pas se retrouver avec un ownerId pointant
                // vers quelqu'un qui n'en fait plus partie — on transfère au
                // membre le plus ancien plutôt que de laisser la propriété orpheline.
                LeagueMembership oldest = remaining.stream()
                        .min(Comparator.comparing(LeagueMembership::getJoinedAt))
                        .orElseThrow();
                league.setOwnerId(oldest.getUser().getId());
                leagueRepository.save(league);
            }
        });
    }

    private User addMembership(League league, String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LeagueMembership membership = new LeagueMembership();
        membership.setLeague(league);
        membership.setUser(user);
        membershipRepository.save(membership);
        return user;
    }

    private void notifyExistingMembersOfNewJoin(League league, User joiningUser) {
        for (LeagueMembership membership : membershipRepository.findByLeagueId(league.getId())) {
            if (membership.getUser().getId().equals(joiningUser.getId())) continue; // pas de notif à soi-même
            if (!membership.getUser().isNotifyLeagueActivity()) continue;

            AppNotification notification = new AppNotification();
            notification.setUser(membership.getUser());
            notification.setType(NotificationType.SYSTEM);
            notification.setMessage(joiningUser.getUsername() + " a rejoint ta ligue \"" + league.getName() + "\" !");
            notificationRepository.save(notification);
        }
    }

    private LeagueDto toDto(League league, String requestingUserId) {
        long memberCount = membershipRepository.countByLeagueId(league.getId());
        boolean isOwner = league.getOwnerId().equals(requestingUserId);
        return new LeagueDto(league.getId(), league.getName(), league.getInviteCode(),
                memberCount, isOwner, league.getCreatedAt());
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (leagueRepository.existsByInviteCode(code));
        return code;
    }
}
