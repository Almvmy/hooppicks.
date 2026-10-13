"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { MessageSquare, MessageSquareReply, Smartphone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { closeFeedback, fetchAdminFeedback, replyFeedback } from "@/lib/api/admin";
import { FEEDBACK_CATEGORIES } from "@/lib/feedback";
import { Feedback } from "@/lib/types";
import { cn, formatShortDateTime } from "@/lib/utils";

const MAX_LENGTH = 1000;

const FILTERS = [
  { value: "OPEN", label: "À traiter" },
  { value: "ANSWERED", label: "Répondus" },
  { value: "CLOSED", label: "Traités" },
  { value: "", label: "Tous" },
] as const;

/** Appareil lisible à partir du User-Agent : assez pour savoir où chercher un bug. */
function describeDevice(ua: string | null | undefined): string | null {
  if (!ua) return null;
  const os = /iPhone|iPad/.test(ua) ? "iPhone/iPad" : /Android/.test(ua) ? "Android" : /Windows/.test(ua) ? "Windows" : /Mac OS/.test(ua) ? "Mac" : /Linux/.test(ua) ? "Linux" : "Autre";
  const browser = /Edg\//.test(ua) ? "Edge" : /SamsungBrowser/.test(ua) ? "Samsung Internet" : /CriOS|Chrome\//.test(ua) ? "Chrome" : /FxiOS|Firefox\//.test(ua) ? "Firefox" : /Safari\//.test(ua) ? "Safari" : "navigateur inconnu";
  return `${os} · ${browser}`;
}

export function AdminFeedbackPanel() {
  const [filter, setFilter] = useState<(typeof FILTERS)[number]["value"]>("OPEN");
  const query = useQuery({
    queryKey: ["admin-feedback", filter],
    queryFn: () => fetchAdminFeedback(filter || undefined),
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => setFilter(f.value)}
            className={cn(
              "rounded-full px-3 py-1.5 text-sm transition-colors",
              filter === f.value ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
            )}
          >
            {f.label}
            {f.value === "OPEN" && query.data ? ` (${query.data.openCount})` : ""}
          </button>
        ))}
      </div>

      {query.isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
      {query.isError && <p className="text-sm text-destructive">Impossible de charger les messages.</p>}
      {query.data?.items.length === 0 && (
        <p className="text-sm text-muted-foreground">
          {filter === "OPEN" ? "Aucun message à traiter." : "Aucun message ici."}
        </p>
      )}
      {query.data?.items.map((f) => <FeedbackItem key={f.id} feedback={f} />)}
    </div>
  );
}

function FeedbackItem({ feedback }: { feedback: Feedback }) {
  const queryClient = useQueryClient();
  const [reply, setReply] = useState("");
  const [open, setOpen] = useState(false);
  const category = FEEDBACK_CATEGORIES.find((c) => c.value === feedback.category);
  const Icon = category?.icon ?? MessageSquare;
  const device = describeDevice(feedback.device);

  function refresh() {
    queryClient.invalidateQueries({ queryKey: ["admin-feedback"] });
    queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
  }

  const replyMutation = useMutation({
    mutationFn: () => replyFeedback(feedback.id, reply.trim()),
    onSuccess: () => {
      toast.success(`Réponse envoyée à @${feedback.username}.`);
      setReply("");
      setOpen(false);
      refresh();
    },
    onError: (err: Error) => toast.error(err.message || "Envoi impossible."),
  });

  const closeMutation = useMutation({
    mutationFn: (closed: boolean) => closeFeedback(feedback.id, closed),
    onSuccess: refresh,
    onError: (err: Error) => toast.error(err.message || "Action impossible."),
  });

  const length = reply.trim().length;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5 font-medium text-foreground">
            <Icon className="h-3.5 w-3.5 text-primary" />
            {category?.label}
          </span>
          <span>@{feedback.username ?? "compte supprimé"}</span>
          <span>{formatShortDateTime(new Date(feedback.createdAt))}</span>
          {feedback.page && <span className="font-mono">{feedback.page}</span>}
          {device && (
            <span className="flex items-center gap-1">
              <Smartphone className="h-3 w-3" />
              {device}
              {feedback.appInstalled ? " · app installée" : ""}
            </span>
          )}
        </div>

        <p className="whitespace-pre-wrap text-sm">{feedback.message}</p>

        {feedback.reply && (
          <div className="glass-inset-quiet rounded-xl border-l-2 border-primary p-3">
            <p className="flex items-center gap-1.5 text-xs font-medium text-primary">
              <MessageSquareReply className="h-3.5 w-3.5" />
              Réponse {feedback.repliedAt && <span className="font-normal text-muted-foreground">· {formatShortDateTime(new Date(feedback.repliedAt))}</span>}
            </p>
            <p className="mt-1 whitespace-pre-wrap text-sm">{feedback.reply}</p>
          </div>
        )}

        {open ? (
          <div className="flex flex-col gap-2">
            <textarea
              value={reply}
              onChange={(e) => setReply(e.target.value)}
              maxLength={MAX_LENGTH + 50}
              rows={4}
              autoFocus
              placeholder={feedback.reply ? "Nouvelle réponse (remplace la précédente)" : "Réponse"}
              className="glass-inset w-full resize-y rounded-xl px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              aria-label="Réponse au joueur"
            />
            <div className="flex flex-wrap items-center gap-2">
              <Button size="sm" disabled={length === 0 || length > MAX_LENGTH || replyMutation.isPending} onClick={() => replyMutation.mutate()}>
                {replyMutation.isPending ? "Envoi…" : "Envoyer la réponse"}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setOpen(false)}>
                Annuler
              </Button>
              <span className={cn("ml-auto font-mono text-xs", length > MAX_LENGTH ? "text-destructive" : "text-muted-foreground")}>
                {length}/{MAX_LENGTH}
              </span>
            </div>
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {feedback.username && (
              <Button size="sm" variant={feedback.reply ? "outline" : "default"} onClick={() => setOpen(true)}>
                {feedback.reply ? "Répondre à nouveau" : "Répondre"}
              </Button>
            )}
            <Button
              size="sm"
              variant="ghost"
              disabled={closeMutation.isPending}
              onClick={() => closeMutation.mutate(feedback.status !== "CLOSED")}
            >
              {feedback.status === "CLOSED" ? "Rouvrir" : "Marquer comme traité"}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
