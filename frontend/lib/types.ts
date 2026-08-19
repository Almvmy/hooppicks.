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

export interface LeaderboardEntry {
  rank: number;
  username: string;
  points: number;
  winRate: number;
  totalBets: number;
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
}