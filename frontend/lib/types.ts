export type TransactionType = "bet_win" | "bet_loss" | "bet_placed" | "bonus";

export interface WalletTransaction {
  id: string;
  type: TransactionType;
  amount: number; // positif = gain, négatif = perte
  description: string;
  date: string; // ISO
}

export interface WalletData {
  balance: number;
}

export type AvatarPosition = "PG" | "SG" | "SF" | "PF" | "C";
export type AvatarColorway = "orange" | "purple" | "blue" | "green" | "red" | "teal";
export type AvatarIcon = "dunk" | "three" | "handles" | "defense" | "playmaker";

export interface UserProfile {
  username: string;
  email: string;
  winRate: number;
  totalBets: number;
  currentWinStreak: number;
  favoriteTeam: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  isAdmin: boolean;
  notifyMatchStarting: boolean;
  notifyBetResults: boolean;
  notifyLeagueActivity: boolean;
  notifyFavoriteTeam: boolean;
  emailVerified: boolean;
}

export interface NotificationPreferences {
  notifyMatchStarting: boolean;
  notifyBetResults: boolean;
  notifyLeagueActivity: boolean;
  notifyFavoriteTeam: boolean;
}

/** Tes paris vus depuis ton équipe favorite, sélection par sélection (GET /favorite-team/stats). */
export interface FavoriteTeamStats {
  teamAbbreviation: string | null;
  forLegs: number;
  forWon: number;
  forLost: number;
  againstLegs: number;
  againstWon: number;
  againstLost: number;
}

export interface UpdateProfileInput {
  favoriteTeam?: string;
  avatarNumber?: number;
  avatarPosition?: AvatarPosition;
  avatarColorway?: AvatarColorway;
  avatarIcon?: AvatarIcon;
}

export type MatchStatus = "scheduled" | "live" | "finished";
export type Conference = "Est" | "Ouest";
export type MatchType = "preseason" | "regular" | "nba_cup" | "all_star" | "play_in" | "playoffs";

export interface Team {
  id: string;
  name: string;
  abbreviation: string;
  conference: Conference;
  division: string;
  logoUrl: string | null;
  outPlayersCount: number | null;
}

export interface TeamRank {
  id: string;
  name: string;
  abbreviation: string;
  conference: Conference;
  division: string;
  rank: number;
  eloRating: number;
  // Classement officiel (ESPN) : distinct de l'Elo, pas encore synchronisé
  // pour toutes les équipes tant que EspnStandingsService n'a pas tourné.
  wins: number | null;
  losses: number | null;
  streak: string | null;
  conferenceSeed: number | null;
  gamesBehind: string | null;
  logoUrl: string | null;
}

export interface PickPercentages {
  // null = personne n'a encore parié sur ce marché (distinct de 0%).
  moneylineHomePct: number | null;
  moneylineAwayPct: number | null;
  spreadHomePct: number | null;
  spreadAwayPct: number | null;
  totalOverPct: number | null;
  totalUnderPct: number | null;
}

export interface Match {
  id: string;
  homeTeam: Team;
  awayTeam: Team;
  date: string; // ISO
  status: MatchStatus;
  /** Phase lue sur ESPN ; null tant que le match n'y est pas relié. */
  type: MatchType | null;
  stageLabel: string | null;
  seriesSummary: string | null;
  homeScore?: number;
  awayScore?: number;
  odds: MatchOdds;
  pickPercentages: PickPercentages | null;
}

export interface RosterPlayer {
  id: string;
  firstName: string | null;
  lastName: string | null;
  position: string | null;
  jersey: string | null;
  height: string | null; // ex: "6' 5\""
  weight: string | null; // ex: "184 lbs"
  headshotUrl: string | null;
  team: Team | null;
  injuryStatus: string | null; // ex: "Day-To-Day", "Out" (null si pas blessé)
  // Moyennes saison (ESPN) : null tant que EspnPlayerStatsService n'a pas
  // encore traité ce joueur (synchro par petits lots, voir le backend).
  statsSeasonLabel: string | null;
  gamesPlayed: number | null;
  gamesStarted: number | null;
  minutesPerGame: number | null;
  pointsPerGame: number | null;
  reboundsPerGame: number | null;
  assistsPerGame: number | null;
  stealsPerGame: number | null;
  blocksPerGame: number | null;
  turnoversPerGame: number | null;
  fieldGoalPct: number | null;
  threePointPct: number | null;
  freeThrowPct: number | null;
}

export interface PlayerLeaders {
  points: RosterPlayer[];
  rebounds: RosterPlayer[];
  assists: RosterPlayer[];
  steals: RosterPlayer[];
  blocks: RosterPlayer[];
}

export interface PlayerRecentGame {
  date: string; // ISO
  opponentAbbreviation: string | null;
  result: string | null; // "W" ou "L"
  score: string | null;
  minutes: string;
  points: number;
  rebounds: number;
  assists: number;
}

export interface PlayerBoxScore {
  playerName: string;
  teamAbbreviation: string;
  starter: boolean;
  minutes: string;
  points: number;
  rebounds: number;
  assists: number;
  steals: number;
  blocks: number;
  turnovers: number;
  plusMinus: number;
  fieldGoals: string; // "9-24"
  threePoints: string;
  freeThrows: string;
}

export interface MatchOdds {
  moneylineHome: number;
  moneylineAway: number;
  spreadValue: number; // ex: -4.5 (favori = équipe à domicile)
  spreadOddsHome: number;
  spreadOddsAway: number;
  totalValue: number; // ex: 215.5
  totalOddsOver: number;
  totalOddsUnder: number;
}


export type PlayerPropMarket = "player_points" | "player_rebounds" | "player_assists" | "player_pra";
export type BetMarket = "moneyline" | "spread" | "total" | PlayerPropMarket;
export type BetOutcome = "home" | "away" | "over" | "under";
export type BetStatus = "pending" | "won" | "lost" | "void";

export interface BetSelection {
  id: string; // `${matchId}-${market}-${outcome}`
  matchId: string;
  matchLabel: string;
  market: BetMarket;
  outcome: BetOutcome;
  label: string; // ex: "Lakers -4.5"
  odds: number;
  /** Pari joueur : joueur visé et ligne vue au moment du choix (le serveur refuse si elle a bougé). */
  playerId?: string;
  line?: number;
}

/** Pari joueur proposé sur un match (PlayerPropsService). */
export interface PlayerProp {
  market: PlayerPropMarket;
  playerId: string;
  playerName: string;
  teamAbbreviation: string;
  headshotUrl: string | null;
  line: number;
  overOdds: number;
  underOdds: number;
  /** Moyenne de la saison dans la statistique du marché. */
  average: number;
}

export interface PlacedBet {
  id: string;
  selections: BetSelection[];
  stake: number;
  totalOdds: number;
  potentialPayout: number;
  status: BetStatus;
  placedAt: string;
  resolvedAt: string | null;
}

export interface NewsItem {
  title: string;
  link: string;
  description: string;
  source: string;
  publishedAt: string; // ISO
  // Renseignés par l'API JSON d'ESPN ; null / [] / false en repli sur le RSS.
  imageUrl: string | null;
  teams: string[]; // sigles de notre base (BOS, NYK…)
  video: boolean;
}

export interface LeaderboardEntry {
  rank: number;
  username: string;
  points: number;
  winRate: number;
  totalBets: number;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  favoriteTeam: string | null; // nom complet ("Boston Celtics"), null si aucune
  /** Places gagnées (+) ou perdues (-) depuis la veille ; null sans historique (et hors classement saison). */
  rankChange: number | null;
  /** Classé aujourd'hui mais absent du classement de la veille. */
  newcomer: boolean;
  /** 5 derniers tickets résolus, du plus récent au plus ancien. */
  recentForm: ("W" | "L")[];
  /** Série en cours : +4 = 4 gagnés d'affilée, -2 = 2 perdus. */
  streak: number;
}

export type LeaderboardPeriod = "season" | "month" | "week";

export interface League {
  id: string;
  name: string;
  inviteCode: string;
  memberCount: number;
  isOwner: boolean;
  createdAt: string;
}

export interface LeaguePreview {
  id: string;
  name: string;
  memberCount: number;
}

export interface LeagueMember {
  username: string;
  isOwner: boolean;
  joinedAt: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
}

export interface LeagueActivity {
  targetType: "BET" | "MEMBERSHIP";
  targetId: string;
  username: string;
  message: string;
  occurredAt: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  reactionCounts: Record<string, number>;
  myReactions: string[];
}

export interface AdminStatus {
  lastSyncAt: string | null;
  lastGamesSynced: number;
  lastBetsResolved: number;
  syncMode: string | null;
  totalUsers: number;
  totalMatches: number;
  pendingBets: number;
}

export interface AdminUser {
  id: string;
  username: string;
  email: string;
  isAdmin: boolean;
  emailVerified: boolean;
  walletBalance: number;
  createdAt: string | null;
  totalBets: number;
  favoriteTeam: string | null;
}

/** Match vu depuis la console : verrouillé (corrigé à la main) et paris qui en dépendent. */
export interface AdminMatch {
  match: Match;
  locked: boolean;
  pendingBets: number;
  resolvedBets: number;
}

export interface AdminOverview {
  totalUsers: number;
  newUsers7d: number;
  activeBettors7d: number;
  betsPlaced24h: number;
  betsPlaced7d: number;
  pointsStaked7d: number;
  pointsInCirculation: number;
  pendingBets: number;
  totalMatches: number;
  lockedMatches: number;
  unlinkedMatches: number;
  matchesByType: Record<string, number>;
  rosterPlayers: number;
  playersWithoutStats: number;
  lastSyncAt: string | null;
  lastGamesSynced: number;
  lastBetsResolved: number;
  syncMode: string | null;
  series: { date: string; signups: number; bets: number }[];
}

export interface AdminAuditEntry {
  id: string;
  adminId: string;
  adminUsername: string;
  action: string;
  target: string | null;
  details: string | null;
  date: string;
}

export interface AdminBet {
  id: string;
  username: string;
  selections: { matchLabel: string; label: string }[];
  stake: number;
  potentialPayout: number;
  placedAt: string;
}

export type NotificationType = "bet_won" | "bet_lost" | "match_starting" | "favorite_team" | "system" | "duel" | "bet_progress";

export interface AppNotification {
  id: string;
  type: NotificationType;
  message: string;
  date: string;
  read: boolean;
}

export interface UserBadge {
  id: string;
  label: string;
  description: string;
  unlocked: boolean;
  icon: string;
}

export interface PublicProfile {
  username: string;
  winRate: number;
  totalBets: number;
  favoriteTeam: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  badges: UserBadge[];
  memberSince: string | null;
  seasonPoints: number;
  seasonRank: number | null;
  seasonPlayers: number;
  weekPoints: number;
  weekRank: number | null;
  weekPlayers: number;
  currentStreak: number;
  bestStreak: number;
  /** Tickets réglés seulement : les tickets en attente restent privés. */
  recentTickets: PlacedBet[];
  bestTicket: PlacedBet | null;
  commonLeagues: string[];
  isMe: boolean;
  palmares: Palmares;
}
export type DuelStatus = "pending" | "accepted" | "declined" | "cancelled" | "expired" | "finished";

/** Un duel vu par le joueur connecté : « moi » contre « l'adversaire ». */
export interface Duel {
  id: string;
  status: DuelStatus;
  iChallenged: boolean;
  week: string;
  opponentUsername: string;
  opponentAvatarNumber: number;
  opponentAvatarPosition: AvatarPosition;
  opponentAvatarColorway: AvatarColorway;
  opponentAvatarIcon: AvatarIcon;
  /** En direct pour un duel de la semaine en cours, figés une fois terminé. */
  myPoints: number | null;
  opponentPoints: number | null;
  result: "won" | "lost" | "tie" | null;
  createdAt: string;
}

/** Trophées gardés à vie (PalmaresService). */
export interface Palmares {
  weeklyTitles: number;
  leagueTitles: { leagueName: string; count: number }[];
  duelWins: number;
  bestWeek: number | null;
}
