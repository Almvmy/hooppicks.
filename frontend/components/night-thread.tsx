"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, Flag, Reply, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Skeleton } from "@/components/ui/skeleton";
import { PlayerAvatar } from "@/components/player-avatar";
import { MessageText, ReplyQuote, ThreadComposer, threadMessageId, type ReplyTarget } from "@/components/thread-composer";
import { fetchProfile } from "@/lib/api/auth";
import {
  blockPlayer,
  deleteNightMessage,
  fetchNightFeed,
  postNightMessage,
  reactToNightMessage,
  reportNightMessage,
  type NightMessage,
} from "@/lib/api/night-thread";
import { fetchDebateComments, postDebateComment } from "@/lib/api/debate";
import type { MentionCandidate } from "@/lib/api/social";
import { cn, formatRelativeTime } from "@/lib/utils";

const MAX_LENGTH = 280;
// Doit matcher ALLOWED_EMOJIS côté backend (LeagueService).
const EMOJIS = ["👍", "🔥", "👎"];

function chipClass(active: boolean) {
  return cn(
    "shrink-0 whitespace-nowrap rounded-full px-3 py-1.5 text-xs font-semibold transition-colors",
    active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
  );
}

/**
 * Le fil de la soirée NBA. `matchId` : page d'un match, seulement ses messages,
 * et ce qu'on écrit porte son étiquette. `debateDay` : les arguments d'un débat,
 * avec le camp de chacun. Sans : toute la soirée, filtrable par match.
 */
export function NightThread({ matchId, debateDay }: { matchId?: string; debateDay?: string }) {
  const queryClient = useQueryClient();
  const { data: profile, isLoading: profileLoading } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile, retry: false });
  const queryKey = debateDay ? ["night-thread", "debat", debateDay] : ["night-thread", matchId ?? "soiree"];
  const feed = useQuery({
    queryKey,
    queryFn: () => (debateDay ? fetchDebateComments(debateDay) : fetchNightFeed(matchId)),
    enabled: !!profile,
    refetchInterval: 15_000,
  });
  const [draft, setDraft] = useState("");
  const [replyTo, setReplyTo] = useState<ReplyTarget | null>(null);
  const [filter, setFilter] = useState<string | null>(null);

  const tagged = useMemo(() => {
    const seen = new Map<string, string>();
    for (const m of feed.data?.messages ?? []) if (m.matchId && m.matchLabel) seen.set(m.matchId, m.matchLabel);
    return [...seen.entries()];
  }, [feed.data]);
  // Ceux qui écrivent dans ce fil : les premiers proposés après un @.
  const authors = useMemo(() => {
    const seen = new Map<string, MentionCandidate>();
    for (const m of feed.data?.messages ?? []) {
      if (m.mine || !m.username || seen.has(m.username)) continue;
      seen.set(m.username, { username: m.username, avatarNumber: m.avatarNumber ?? 0, avatarPosition: m.avatarPosition!, avatarColorway: m.avatarColorway!, avatarIcon: m.avatarIcon! });
    }
    return [...seen.values()];
  }, [feed.data]);
  const activeFilter = filter && tagged.some(([id]) => id === filter) ? filter : null;
  const messages = (feed.data?.messages ?? []).filter((m) => !activeFilter || m.matchId === activeFilter);

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["night-thread"] });
  const onError = (e: Error) => toast.error(e.message || "Action impossible. Réessaie.");

  const send = useMutation({
    mutationFn: () => (debateDay ? postDebateComment(debateDay, draft.trim(), replyTo?.id) : postNightMessage(draft.trim(), matchId ?? activeFilter ?? undefined, replyTo?.id)),
    onSuccess: () => {
      setDraft("");
      setReplyTo(null);
      refresh();
      if (debateDay) queryClient.invalidateQueries({ queryKey: ["debate"] });
    },
    onError,
  });
  const react = useMutation({ mutationFn: (v: { id: string; emoji: string }) => reactToNightMessage(v.id, v.emoji), onSuccess: refresh, onError });
  const remove = useMutation({ mutationFn: deleteNightMessage, onSuccess: refresh, onError });
  const report = useMutation({
    mutationFn: reportNightMessage,
    onSuccess: () => {
      toast.success("Message signalé");
      refresh();
    },
    onError,
  });
  const block = useMutation({
    mutationFn: blockPlayer,
    onSuccess: (_, username) => {
      toast.success(`${username} bloqué`);
      refresh();
      queryClient.invalidateQueries({ queryKey: ["blocked-players"] });
    },
    onError,
  });

  if (!profile && !profileLoading) {
    return (
      <p className="glass rounded-2xl px-4 py-6 text-center text-sm text-muted-foreground">
        <Link href="/login" className="font-semibold text-primary hover:underline">
          Connecte-toi
        </Link>{" "}
        pour lire le fil.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {feed.data?.cannotWrite ? (
        <p className="glass-inset-quiet rounded-2xl px-4 py-3 text-sm text-muted-foreground">{feed.data.cannotWrite}</p>
      ) : (
        feed.data && (
          <ThreadComposer
            value={draft}
            onChange={setDraft}
            onSubmit={() => send.mutate()}
            pending={send.isPending}
            placeholder={debateDay ? "Ton argument…" : "Écris dans le fil…"}
            maxLength={MAX_LENGTH}
            authors={authors}
            replyTo={replyTo}
            onCancelReply={() => setReplyTo(null)}
          />
        )
      )}

      {!matchId && !debateDay && tagged.length > 0 && (
        <div className="glass-scroll edge-scroll flex gap-2 overflow-x-auto pb-0.5">
          <button type="button" onClick={() => setFilter(null)} className={chipClass(!activeFilter)}>
            Tout
          </button>
          {tagged.map(([id, label]) => (
            <button key={id} type="button" onClick={() => setFilter(id)} className={chipClass(activeFilter === id)}>
              {label}
            </button>
          ))}
        </div>
      )}

      {(feed.isLoading || profileLoading) && <Skeleton className="h-40 w-full rounded-2xl" />}
      {feed.isError && <p className="text-sm text-destructive">Impossible de charger le fil.</p>}
      {feed.data && messages.length === 0 && <p className="py-6 text-center text-sm text-muted-foreground">Rien pour l&apos;instant.</p>}

      <div className="flex flex-col gap-2">
        {messages.map((m) =>
          m.auto ? (
            <AutoLine key={m.id} message={m} showMatch={!matchId} />
          ) : (
            <div key={m.id} id={threadMessageId(m.id)} className="glass flex gap-2.5 rounded-2xl px-3 py-2.5">
              <Link href={`/u/${encodeURIComponent(m.username ?? "")}`} className="shrink-0">
                <PlayerAvatar
                  number={m.avatarNumber ?? 0}
                  position={m.avatarPosition!}
                  colorway={m.avatarColorway!}
                  icon={m.avatarIcon!}
                  size="xs"
                />
              </Link>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
                  <Link href={`/u/${encodeURIComponent(m.username ?? "")}`} className="text-sm font-semibold hover:underline">
                    {m.username}
                  </Link>
                  {m.camp && <span className="rounded-full bg-primary/10 px-1.5 font-mono text-[10px] font-semibold text-primary">{debateDay ? m.camp : `joue ${m.camp}`}</span>}
                  {!matchId && m.matchLabel && (
                    <Link href={`/matches/${m.matchId}?onglet=fil`} className="font-mono text-[10px] text-muted-foreground hover:underline">
                      {m.matchLabel}
                    </Link>
                  )}
                  <span className="ml-auto text-[10px] text-muted-foreground">{formatRelativeTime(m.createdAt)}</span>
                </div>
                {m.reply && (
                  <div className="mt-1">
                    <ReplyQuote reply={m.reply} />
                  </div>
                )}
                <p className="mt-0.5 break-words text-sm">
                  <MessageText text={m.text} />
                </p>
                <div className="mt-1.5 flex items-center gap-1">
                  {EMOJIS.map((emoji) => {
                    const count = m.reactionCounts[emoji] ?? 0;
                    return (
                      <button
                        key={emoji}
                        type="button"
                        disabled={react.isPending}
                        onClick={() => react.mutate({ id: m.id, emoji })}
                        className={cn(
                          "flex items-center gap-1 rounded-full px-2 py-0.5 text-xs transition-colors",
                          m.myReactions.includes(emoji) ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
                        )}
                      >
                        <span>{emoji}</span>
                        {count > 0 && <span className="font-mono text-[10px]">{count}</span>}
                      </button>
                    );
                  })}
                  <span className="ml-auto flex items-center gap-1 text-muted-foreground">
                    {!feed.data?.cannotWrite && m.username && (
                      <IconAction label="Répondre" onClick={() => setReplyTo({ id: m.id, username: m.username!, text: m.text })}>
                        <Reply className="h-3.5 w-3.5" />
                      </IconAction>
                    )}
                    {m.mine ? (
                      <IconAction label="Supprimer" onClick={() => remove.mutate(m.id)}>
                        <Trash2 className="h-3.5 w-3.5" />
                      </IconAction>
                    ) : (
                      <>
                        <IconAction label={m.reportedByMe ? "Signalé" : "Signaler"} disabled={m.reportedByMe} onClick={() => report.mutate(m.id)}>
                          <Flag className={cn("h-3.5 w-3.5", m.reportedByMe && "text-destructive")} />
                        </IconAction>
                        <IconAction label={`Bloquer ${m.username}`} onClick={() => m.username && block.mutate(m.username)}>
                          <Ban className="h-3.5 w-3.5" />
                        </IconAction>
                      </>
                    )}
                  </span>
                </div>
              </div>
            </div>
          )
        )}
      </div>
    </div>
  );
}

function IconAction({ label, onClick, disabled, children }: { label: string; onClick: () => void; disabled?: boolean; children: React.ReactNode }) {
  return (
    <button type="button" aria-label={label} title={label} disabled={disabled} onClick={onClick} className="rounded-full p-1.5 transition-colors hover:text-foreground disabled:opacity-60">
      {children}
    </button>
  );
}

function AutoLine({ message, showMatch }: { message: NightMessage; showMatch: boolean }) {
  return (
    <p className="flex flex-wrap items-center justify-center gap-x-2 px-3 py-1 text-center text-xs text-muted-foreground">
      <span className="font-medium">{message.text}</span>
      {showMatch && message.matchLabel && message.matchId && !message.text.includes(" - ") && (
        <Link href={`/matches/${message.matchId}?onglet=fil`} className="font-mono text-[10px] hover:underline">
          {message.matchLabel}
        </Link>
      )}
      <span className="text-[10px]">{formatRelativeTime(message.createdAt)}</span>
    </p>
  );
}
