import { apiFetch } from "@/lib/api/http";
import type { AvatarColorway, AvatarIcon, AvatarPosition } from "@/lib/types";

/** Un membre de ses ligues qui a joué ce match. */
export interface FriendPick {
  username: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  label: string;
  status: "pending" | "won" | "lost" | "void";
}

export interface TicketReactions {
  counts: Record<string, number>;
  mine: string[];
}

/** Un joueur proposé pendant qu'on écrit @ka. */
export interface MentionCandidate {
  username: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
}

export async function fetchMentions(q: string, leagueId?: string): Promise<MentionCandidate[]> {
  const league = leagueId ? `&leagueId=${encodeURIComponent(leagueId)}` : "";
  return apiFetch<MentionCandidate[]>(`/mentions?q=${encodeURIComponent(q)}${league}`);
}

export async function fetchFriendsOnMatch(matchId: string): Promise<FriendPick[]> {
  return apiFetch<FriendPick[]>(`/matches/${matchId}/friends`);
}

export async function fetchTicketReactions(ids: string[]): Promise<Record<string, TicketReactions>> {
  return apiFetch<Record<string, TicketReactions>>(`/bets/reactions?ids=${ids.map(encodeURIComponent).join(",")}`);
}

export async function reactToTicket(betId: string, emoji: string): Promise<Record<string, TicketReactions>> {
  return apiFetch<Record<string, TicketReactions>>(`/bets/${betId}/react`, { method: "POST", body: JSON.stringify({ emoji }) });
}
