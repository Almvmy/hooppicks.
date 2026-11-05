import { apiFetch } from "@/lib/api/http";
import type { AvatarColorway, AvatarIcon, AvatarPosition } from "@/lib/types";

export interface NightMessage {
  id: string;
  /** Message automatique : fin de quart-temps, grosse perf, gros gain. */
  auto: boolean;
  username: string | null;
  avatarNumber: number | null;
  avatarPosition: AvatarPosition | null;
  avatarColorway: AvatarColorway | null;
  avatarIcon: AvatarIcon | null;
  text: string;
  matchId: string | null;
  matchLabel: string | null;
  /** Ce que l'auteur a joué sur ce match : seulement une fois le match commencé. */
  camp: string | null;
  createdAt: string;
  reactionCounts: Record<string, number>;
  myReactions: string[];
  mine: boolean;
  reportedByMe: boolean;
}

export interface NightFeed {
  night: string;
  messages: NightMessage[];
  cannotWrite: string | null;
}

export interface BlockedPlayer {
  username: string;
}

export interface ReportedMessage {
  id: string;
  username: string | null;
  text: string;
  matchId: string | null;
  createdAt: string;
  reports: number;
  hidden: boolean;
}

export interface ChatModeration {
  reported: ReportedMessage[];
  words: string[];
  muted: { username: string; until: string }[];
}

const post = <T,>(path: string, body?: unknown) =>
  apiFetch<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) });

export const fetchNightFeed = (matchId?: string) =>
  apiFetch<NightFeed>(matchId ? `/night-thread?matchId=${encodeURIComponent(matchId)}` : "/night-thread");
export const postNightMessage = (text: string, matchId?: string) => post<NightFeed>("/night-thread", { text, matchId: matchId ?? null });
export const deleteNightMessage = (id: string) => post<unknown>(`/night-thread/${id}/delete`);
export const reportNightMessage = (id: string) => post<unknown>(`/night-thread/${id}/report`);
export const reactToNightMessage = (id: string, emoji: string) => post<unknown>(`/night-thread/${id}/react`, { emoji });
export const fetchBlockedPlayers = () => apiFetch<BlockedPlayer[]>("/night-thread/blocks");
export const blockPlayer = (username: string) => post<BlockedPlayer[]>("/night-thread/block", { username });
export const unblockPlayer = (username: string) => post<BlockedPlayer[]>("/night-thread/unblock", { username });

export const fetchChatModeration = () => apiFetch<ChatModeration>("/console/chat");
export const adminDeleteMessage = (id: string) => post<unknown>(`/console/chat/messages/${id}/delete`);
export const adminRestoreMessage = (id: string) => post<unknown>(`/console/chat/messages/${id}/restore`);
export const adminMutePlayer = (username: string, days: number) => post<unknown>(`/console/chat/users/${encodeURIComponent(username)}/mute`, { days });
export const adminUnmutePlayer = (username: string) => post<unknown>(`/console/chat/users/${encodeURIComponent(username)}/unmute`);
export const adminAddWord = (word: string) => post<unknown>("/console/chat/words", { word });
export const adminRemoveWord = (word: string) => post<unknown>("/console/chat/words/delete", { word });
