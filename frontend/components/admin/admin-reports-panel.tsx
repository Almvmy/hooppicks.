"use client";

import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  adminAddWord,
  adminDeleteMessage,
  adminMutePlayer,
  adminRemoveWord,
  adminRestoreMessage,
  adminUnmutePlayer,
  fetchChatModeration,
  type ReportedMessage,
} from "@/lib/api/night-thread";
import { formatShortDateTime } from "@/lib/utils";

const MUTE_DAYS = [1, 7, 30];

function useModerationAction<T>(action: (value: T) => Promise<unknown>, done?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: action,
    onSuccess: () => {
      if (done) toast.success(done);
      queryClient.invalidateQueries({ queryKey: ["chat-moderation"] });
      queryClient.invalidateQueries({ queryKey: ["night-thread"] });
    },
    onError: (e: Error) => toast.error(e.message || "Action impossible."),
  });
}

export function AdminReportsPanel() {
  const query = useQuery({ queryKey: ["chat-moderation"], queryFn: fetchChatModeration });
  const [word, setWord] = useState("");
  const addWord = useModerationAction(adminAddWord);
  const removeWord = useModerationAction(adminRemoveWord);
  const unmute = useModerationAction(adminUnmutePlayer, "Parole rendue");

  function submitWord(e: FormEvent) {
    e.preventDefault();
    if (!word.trim()) return;
    addWord.mutate(word.trim());
    setWord("");
  }

  if (query.isLoading) return <p className="text-sm text-muted-foreground">Chargement…</p>;
  if (query.isError || !query.data) return <p className="text-sm text-destructive">Impossible de charger les signalements.</p>;
  const { reported, words, muted } = query.data;

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3">
        <h2 className="font-heading text-lg font-bold">Messages signalés</h2>
        {reported.length === 0 && <p className="text-sm text-muted-foreground">Aucun signalement.</p>}
        {reported.map((m) => (
          <ReportedItem key={m.id} message={m} />
        ))}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="font-heading text-lg font-bold">Mots refusés</h2>
        <form onSubmit={submitWord} className="flex gap-2">
          <Input value={word} onChange={(e) => setWord(e.target.value)} placeholder="Ajouter un mot" aria-label="Mot refusé" className="max-w-xs" />
          <Button type="submit" size="sm" disabled={!word.trim() || addWord.isPending}>
            Ajouter
          </Button>
        </form>
        <div className="flex flex-wrap gap-1.5">
          {words.map((w) => (
            <button
              key={w}
              type="button"
              onClick={() => removeWord.mutate(w)}
              aria-label={`Retirer ${w}`}
              className="glass-inset-quiet flex items-center gap-1 rounded-full px-2.5 py-1 text-xs hover:text-destructive"
            >
              {w}
              <X className="h-3 w-3" />
            </button>
          ))}
        </div>
      </section>

      {muted.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="font-heading text-lg font-bold">Parole coupée</h2>
          {muted.map((u) => (
            <div key={u.username} className="flex items-center justify-between gap-2 text-sm">
              <span className="min-w-0 truncate">
                <span className="font-medium">{u.username}</span>
                <span className="text-muted-foreground"> · jusqu&apos;au {formatShortDateTime(new Date(u.until))}</span>
              </span>
              <Button size="sm" variant="outline" onClick={() => unmute.mutate(u.username)}>
                Rendre la parole
              </Button>
            </div>
          ))}
        </section>
      )}
    </div>
  );
}

function ReportedItem({ message: m }: { message: ReportedMessage }) {
  const remove = useModerationAction(adminDeleteMessage, "Message supprimé");
  const restore = useModerationAction(adminRestoreMessage, "Message rétabli");
  const mute = useModerationAction((days: number) => adminMutePlayer(m.username ?? "", days), "Parole coupée");

  return (
    <Card>
      <CardContent className="flex flex-col gap-2 pt-4">
        <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span className="font-semibold text-foreground">{m.username}</span>
          <span>{formatShortDateTime(new Date(m.createdAt))}</span>
          <span>
            {m.reports} signalement{m.reports > 1 ? "s" : ""}
          </span>
          {m.hidden && <span className="rounded-full bg-destructive/15 px-1.5 font-semibold text-destructive">Masqué</span>}
        </div>
        <p className="break-words text-sm">{m.text}</p>
        <div className="flex flex-wrap items-center gap-2">
          <Button size="sm" variant="destructive" disabled={remove.isPending} onClick={() => remove.mutate(m.id)}>
            Supprimer
          </Button>
          <Button size="sm" variant="outline" disabled={restore.isPending} onClick={() => restore.mutate(m.id)}>
            Rétablir
          </Button>
          {m.username && (
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
              Couper la parole :
              {MUTE_DAYS.map((d) => (
                <button key={d} type="button" disabled={mute.isPending} onClick={() => mute.mutate(d)} className="glass-inset-quiet rounded-full px-2 py-1 font-semibold hover:text-foreground">
                  {d} j
                </button>
              ))}
            </span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
