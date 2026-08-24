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
}

export interface TeamRank {
  id: string;
  name: string;
  abbreviation: string;
  conference: Conference;
  division: string;
  rank: number;
  eloRating: number;
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

export interface Player {
  id: string;
  firstName: string;
  lastName: string;
  position: string | null;
  height: string | null; // format brut balldontlie, ex: "6-6" (pieds-pouces)
  weight: string | null; // en livres
  team: Team | null;
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
  username: string;
  message: string;
  occurredAt: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
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