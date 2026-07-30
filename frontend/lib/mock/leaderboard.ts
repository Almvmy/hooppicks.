import { LeaderboardEntry } from "@/lib/types";

const MOCK_LEADERBOARD: LeaderboardEntry[] = [
  { rank: 1, username: "Almamy", points: 4820, winRate: 68, totalBets: 41 },
  { rank: 2, username: "KaramokoNBA", points: 4310, winRate: 61, totalBets: 55 },
  { rank: 3, username: "SwishQueen", points: 3990, winRate: 59, totalBets: 38 },
  { rank: 4, username: "DunkOrDie", points: 3540, winRate: 52, totalBets: 63 },
  { rank: 5, username: "CourtVision", points: 3120, winRate: 55, totalBets: 29 },
  { rank: 6, username: "TripleDouble", points: 2870, winRate: 48, totalBets: 47 },
  { rank: 7, username: "HoopDreams225", points: 2410, winRate: 44, totalBets: 52 },
];

// TEMPORAIRE : sera remplacé par fetch("/api/leaderboard").
export async function fetchLeaderboard(): Promise<LeaderboardEntry[]> {
  await new Promise((r) => setTimeout(r, 500));
  return MOCK_LEADERBOARD;
}