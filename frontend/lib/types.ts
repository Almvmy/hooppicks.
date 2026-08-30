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
  emailVerified: boolean;
}

export interface NotificationPreferences {
  notifyMatchStarting: boolean;
  notifyBetResults: boolean;
  notifyLeagueActivity: boolean;
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


export type BetMarket = "moneyline" | "spread" | "total";
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
}

export interface PlacedBet {
  id: string;
  selections: BetSelection[];
  stake: number;
  totalOdds: number;
  potentialPayout: number;
  status: BetStatus;
  placedAt: string;
}

export interface NewsItem {
  title: string;
  link: string;
  description: string;
  source: string;
  publishedAt: string; // ISO
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
}

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
}

export interface AdminBet {
  id: string;
  username: string;
  selections: { matchLabel: string; label: string }[];
  stake: number;
  potentialPayout: number;
  placedAt: string;
}

export type NotificationType = "bet_won" | "bet_lost" | "match_starting" | "system";

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
}