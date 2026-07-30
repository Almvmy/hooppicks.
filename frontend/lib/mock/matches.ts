import { Match, Team, MatchOdds } from "@/lib/types";

const LAKERS: Team = { id: "lal", name: "Lakers", abbreviation: "LAL", conference: "Ouest", division: "Pacific" };
const CELTICS: Team = { id: "bos", name: "Celtics", abbreviation: "BOS", conference: "Est", division: "Atlantic" };
const WARRIORS: Team = { id: "gsw", name: "Warriors", abbreviation: "GSW", conference: "Ouest", division: "Pacific" };
const NETS: Team = { id: "bkn", name: "Nets", abbreviation: "BKN", conference: "Est", division: "Atlantic" };
const BUCKS: Team = { id: "mil", name: "Bucks", abbreviation: "MIL", conference: "Est", division: "Central" };
const HEAT: Team = { id: "mia", name: "Heat", abbreviation: "MIA", conference: "Est", division: "Southeast" };
const NUGGETS: Team = { id: "den", name: "Nuggets", abbreviation: "DEN", conference: "Ouest", division: "Northwest" };
const SUNS: Team = { id: "phx", name: "Suns", abbreviation: "PHX", conference: "Ouest", division: "Pacific" };

export const MOCK_TEAMS: Team[] = [
  LAKERS, CELTICS, WARRIORS, NETS, BUCKS, HEAT, NUGGETS, SUNS,
];

const ODDS_1: MatchOdds = { moneylineHome: 1.65, moneylineAway: 2.25, spreadValue: -4.5, spreadOddsHome: 1.9, spreadOddsAway: 1.9, totalValue: 218.5, totalOddsOver: 1.87, totalOddsUnder: 1.95 };
const ODDS_2: MatchOdds = { moneylineHome: 1.4, moneylineAway: 2.9, spreadValue: -7.5, spreadOddsHome: 1.9, spreadOddsAway: 1.9, totalValue: 224.5, totalOddsOver: 1.9, totalOddsUnder: 1.9 };
const ODDS_3: MatchOdds = { moneylineHome: 1.8, moneylineAway: 2.0, spreadValue: -2.5, spreadOddsHome: 1.9, spreadOddsAway: 1.9, totalValue: 210.5, totalOddsOver: 1.9, totalOddsUnder: 1.9 };
const ODDS_4: MatchOdds = { moneylineHome: 2.1, moneylineAway: 1.75, spreadValue: 1.5, spreadOddsHome: 1.9, spreadOddsAway: 1.9, totalValue: 216.5, totalOddsOver: 1.9, totalOddsUnder: 1.9 };
const ODDS_5: MatchOdds = { moneylineHome: 1.55, moneylineAway: 2.45, spreadValue: -5.5, spreadOddsHome: 1.9, spreadOddsAway: 1.9, totalValue: 221.5, totalOddsOver: 1.9, totalOddsUnder: 1.9 };
const ODDS_6: MatchOdds = { moneylineHome: 2.3, moneylineAway: 1.6, spreadValue: 3.5, spreadOddsHome: 1.9, spreadOddsAway: 1.9, totalValue: 205.5, totalOddsOver: 1.9, totalOddsUnder: 1.9 };

const MOCK_MATCHES: Match[] = [
  { id: "m1", homeTeam: LAKERS, awayTeam: CELTICS, date: "2026-07-10T20:00:00Z", status: "scheduled", odds: ODDS_1 },
  { id: "m2", homeTeam: WARRIORS, awayTeam: NETS, date: "2026-07-10T22:30:00Z", status: "scheduled", odds: ODDS_2 },
  { id: "m3", homeTeam: BUCKS, awayTeam: HEAT, date: "2026-07-09T19:00:00Z", status: "live", homeScore: 58, awayScore: 61, odds: ODDS_3 },
  { id: "m4", homeTeam: NUGGETS, awayTeam: SUNS, date: "2026-07-08T21:00:00Z", status: "finished", homeScore: 112, awayScore: 104, odds: ODDS_4 },
  { id: "m5", homeTeam: CELTICS, awayTeam: WARRIORS, date: "2026-07-12T20:00:00Z", status: "scheduled", odds: ODDS_5 },
  { id: "m6", homeTeam: HEAT, awayTeam: LAKERS, date: "2026-07-07T18:30:00Z", status: "finished", homeScore: 98, awayScore: 101, odds: ODDS_6 },
];

// TEMPORAIRE : sera remplacé par fetch("/api/matches?team=&conference=&division=")
export async function fetchMatches(): Promise<Match[]> {
  await new Promise((r) => setTimeout(r, 500));
  return MOCK_MATCHES;
}

export async function fetchMatchById(id: string): Promise<Match | undefined> {
  await new Promise((r) => setTimeout(r, 400));
  return MOCK_MATCHES.find((m) => m.id === id);
}