package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.entity.League;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.league.LeagueService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.EmailVerificationTokenRepository;
import com.hooppicks.backendapplication.repository.PasswordResetTokenRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final LeagueService leagueService;
    private final BetRepository betRepository;
    private final WalletTransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final SessionStore sessionStore;

    public AccountDeletionService(UserRepository userRepository, LeagueRepository leagueRepository,
                                   LeagueMembershipRepository membershipRepository, LeagueService leagueService,
                                   BetRepository betRepository, WalletTransactionRepository transactionRepository,
                                   NotificationRepository notificationRepository,
                                   PasswordResetTokenRepository tokenRepository,
                                   EmailVerificationTokenRepository verificationTokenRepository,
                                   SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.leagueService = leagueService;
        this.betRepository = betRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.tokenRepository = tokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.sessionStore = sessionStore;
    }

    @Transactional
    public void deleteAccount(String userId) {
        // Ligues possédées : pas de transfert de propriété possible (fonctionnalité
        // inexistante), donc la ligue ne peut pas survivre sans son créateur :
        // on la supprime entièrement plutôt que de laisser ownerId orphelin.
        for (League league : leagueRepository.findByOwnerId(userId)) {
            membershipRepository.deleteByLeagueId(league.getId());
            leagueRepository.delete(league);
        }

        // Ligues où l'utilisateur n'est que membre : on réutilise la logique
        // déjà là (gère aussi l'auto-suppression de la ligue si elle se vide).
        for (LeagueMembership membership : membershipRepository.findByUserId(userId)) {
            leagueService.leaveLeague(membership.getLeague().getId(), userId);
        }

        betRepository.deleteByUserId(userId);
        transactionRepository.deleteByUserId(userId);
        notificationRepository.deleteByUserId(userId);
        tokenRepository.deleteByUserId(userId);
        verificationTokenRepository.deleteByUserId(userId);

        userRepository.deleteById(userId);
        sessionStore.invalidateAllForUser(userId);
    }
}
