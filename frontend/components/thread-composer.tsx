"use client";

import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { CornerDownRight, Send, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { PlayerAvatar } from "@/components/player-avatar";
import { fetchMentions, type MentionCandidate } from "@/lib/api/social";
import type { ReplyPreview } from "@/lib/types";
import { cn } from "@/lib/utils";

export interface ReplyTarget {
  id: string;
  username: string;
  text: string;
}

const MAX_SUGGESTIONS = 6;
// Même écriture d'un pseudo que SocialNotifier côté serveur.
const MENTION_AT_CARET = /(?<![\p{L}\p{N}_])@([\p{L}\p{N}_.-]{0,30})$/u;

/** Id d'un message dans la page : c'est là que ramène l'extrait d'une réponse. */
export const threadMessageId = (id: string) => `fil-${id}`;

const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

/**
 * @pseudo cliquable. `mentions` : les pseudos tagués, reconnus par le serveur à l'envoi
 * (un pseudo peut contenir une espace ou un emoji, « @Spidey 🕷 ») ; sans eux (anciens
 * messages), on reconnaît les @mots simples comme avant.
 */
export function MessageText({ text, mentions }: { text: string; mentions?: string[] | null }) {
  const names = [...new Set(mentions ?? [])].sort((a, b) => b.length - a.length);
  const pattern = names.length
    ? new RegExp(`((?<![\\p{L}\\p{N}_])@(?:${names.map(escapeRegExp).join("|")})(?![\\p{L}\\p{N}_]))`, "iu")
    : /((?<![\p{L}\p{N}_])@[\p{L}\p{N}_.-]{2,30})/u;
  return (
    <>
      {text.split(pattern).map((part, i) => {
        if (i % 2 === 0) return part;
        const typed = part.slice(1);
        const username = names.find((n) => n.toLowerCase() === typed.toLowerCase()) ?? typed.replace(/[.-]+$/, "");
        return (
          <Link key={i} href={`/u/${encodeURIComponent(username)}`} className="font-semibold text-primary hover:underline">
            {part}
          </Link>
        );
      })}
    </>
  );
}

/** L'extrait du message auquel on répond. Toucher ramène au message, s'il est encore dans la liste. */
export function ReplyQuote({ reply }: { reply: ReplyPreview }) {
  if (!reply.text) {
    return (
      <p className="mb-1 flex items-center gap-1 text-xs italic text-muted-foreground">
        <CornerDownRight className="h-3 w-3 shrink-0" />
        Message indisponible
      </p>
    );
  }
  function jump() {
    const element = document.getElementById(threadMessageId(reply.id));
    if (!element) return;
    element.scrollIntoView({ behavior: "smooth", block: "center" });
    element.animate([{ boxShadow: "0 0 0 2px var(--primary)" }, { boxShadow: "0 0 0 2px transparent" }], { duration: 1600 });
  }
  return (
    <button type="button" onClick={jump} className="mb-1 flex w-full min-w-0 items-center gap-1 rounded-md border-l-2 border-primary/60 bg-tint/[0.05] px-2 py-1 text-left text-xs text-muted-foreground hover:text-foreground">
      <CornerDownRight className="h-3 w-3 shrink-0" />
      <span className="shrink-0 font-semibold">{reply.username}</span>
      <span className="min-w-0 truncate">{reply.text}</span>
    </button>
  );
}

/**
 * La zone d'écriture des trois fils : réponse à un message et @pseudo proposé
 * pendant la frappe. Ceux qui écrivent dans le fil passent d'abord, c'est d'eux
 * qu'on parle ; la recherche complète ensuite.
 */
export function ThreadComposer({
  value,
  onChange,
  onSubmit,
  pending,
  placeholder,
  maxLength,
  leagueId,
  authors,
  replyTo,
  onCancelReply,
}: {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  pending: boolean;
  placeholder: string;
  maxLength: number;
  leagueId?: string;
  authors: MentionCandidate[];
  replyTo: ReplyTarget | null;
  onCancelReply: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [caret, setCaret] = useState(0);
  const [active, setActive] = useState(0);
  const [dismissed, setDismissed] = useState<string | null>(null);

  const before = value.slice(0, caret);
  const token = MENTION_AT_CARET.exec(before)?.[1] ?? null;
  const open = token !== null && dismissed !== before;

  // Attendre une courte pause : pas une requête par lettre.
  const [term, setTerm] = useState("");
  useEffect(() => {
    const id = setTimeout(() => setTerm(token ?? ""), 150);
    return () => clearTimeout(id);
  }, [token]);
  const search = useQuery({
    queryKey: ["mentions", leagueId ?? "tous", term],
    queryFn: () => fetchMentions(term, leagueId),
    enabled: open && (term.length > 0 || !!leagueId),
    staleTime: 60_000,
  });

  const prefix = (token ?? "").toLocaleLowerCase("fr");
  const seen = new Set<string>();
  const suggestions = !open
    ? []
    : [...authors.filter((a) => a.username.toLocaleLowerCase("fr").startsWith(prefix)), ...(term === token ? (search.data ?? []) : [])]
        .filter((c) => {
          const key = c.username.toLocaleLowerCase("fr");
          if (seen.has(key)) return false;
          seen.add(key);
          return true;
        })
        .slice(0, MAX_SUGGESTIONS);
  const highlighted = Math.min(active, Math.max(0, suggestions.length - 1));

  // « Répondre » est souvent loin de la zone d'écriture, tout en haut du fil.
  useEffect(() => {
    if (!replyTo) return;
    inputRef.current?.focus();
    inputRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [replyTo]);

  function pick(username: string) {
    const nextBefore = before.replace(MENTION_AT_CARET, `@${username} `);
    const next = (nextBefore + value.slice(caret)).slice(0, maxLength);
    onChange(next);
    setCaret(nextBefore.length);
    setActive(0);
    requestAnimationFrame(() => {
      inputRef.current?.focus();
      inputRef.current?.setSelectionRange(nextBefore.length, nextBefore.length);
    });
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (suggestions.length > 0) {
      if (e.key === "ArrowDown" || e.key === "ArrowUp") {
        e.preventDefault();
        const step = e.key === "ArrowDown" ? 1 : -1;
        setActive((highlighted + step + suggestions.length) % suggestions.length);
        return;
      }
      if (e.key === "Enter" || e.key === "Tab") {
        e.preventDefault();
        pick(suggestions[highlighted].username);
        return;
      }
      if (e.key === "Escape") {
        setDismissed(before);
        return;
      }
    }
    if (e.key === "Escape" && replyTo) onCancelReply();
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    if (value.trim() && !pending) onSubmit();
  }

  const trackCaret = (element: HTMLInputElement) => setCaret(element.selectionStart ?? element.value.length);

  return (
    <form onSubmit={submit} className="relative flex flex-col gap-1.5">
      {replyTo && (
        <div className="flex items-center gap-1.5 rounded-lg border-l-2 border-primary bg-tint/[0.05] px-2 py-1.5 text-xs">
          <CornerDownRight className="h-3 w-3 shrink-0 text-primary" />
          <span className="min-w-0 flex-1 truncate">
            <span className="font-semibold">Réponse à {replyTo.username}</span> <span className="text-muted-foreground">{replyTo.text}</span>
          </span>
          <button type="button" onClick={onCancelReply} aria-label="Annuler la réponse" className="shrink-0 rounded-full p-0.5 text-muted-foreground hover:text-foreground">
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      <div className="relative flex items-center gap-2">
        {suggestions.length > 0 && (
          <ul role="listbox" aria-label="Joueurs" className="absolute left-0 top-full z-30 mt-1 w-full max-w-xs overflow-hidden rounded-xl border border-tint/15 bg-popover shadow-lg">
            {suggestions.map((c, i) => (
              <li key={c.username} role="option" aria-selected={i === highlighted}>
                <button
                  type="button"
                  // Avant le blur : le champ doit garder le clavier ouvert sur téléphone.
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => pick(c.username)}
                  className={cn("flex w-full items-center gap-2 px-3 py-2 text-left text-sm font-medium", i === highlighted ? "bg-primary/15" : "hover:bg-tint/[0.06]")}
                >
                  <PlayerAvatar number={c.avatarNumber} position={c.avatarPosition} colorway={c.avatarColorway} icon={c.avatarIcon} size="xs" />
                  <span className="min-w-0 truncate">{c.username}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        <Input
          ref={inputRef}
          value={value}
          onChange={(e) => {
            onChange(e.target.value);
            trackCaret(e.target);
            setActive(0);
          }}
          onSelect={(e) => trackCaret(e.currentTarget)}
          onKeyDown={onKeyDown}
          maxLength={maxLength}
          placeholder={placeholder}
          aria-label={placeholder}
          autoComplete="off"
          className="min-w-0 flex-1"
        />
        <button type="submit" disabled={!value.trim() || pending} aria-label="Envoyer" className="glass-accent flex h-9 w-9 shrink-0 items-center justify-center rounded-full disabled:opacity-40">
          <Send className="h-4 w-4" />
        </button>
      </div>
    </form>
  );
}
