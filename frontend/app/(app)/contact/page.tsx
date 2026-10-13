"use client";

import { use, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { MessageSquare, MessageSquareReply } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { fetchMyFeedback, sendFeedback } from "@/lib/api/feedback";
import { FEEDBACK_CATEGORIES } from "@/lib/feedback";
import { isStandalone } from "@/lib/install-prompt";
import { Feedback, FeedbackCategory } from "@/lib/types";
import { cn, formatShortDateTime } from "@/lib/utils";

const MAX_LENGTH = 1000;


const STATUS_LABEL: Record<Feedback["status"], string> = {
  OPEN: "En attente",
  ANSWERED: "Répondu",
  CLOSED: "Traité",
};

export default function ContactPage({ searchParams }: { searchParams: Promise<{ depuis?: string; type?: string }> }) {
  // Page d'où vient le joueur (« Signaler un problème » sur un écran d'erreur…) : de quoi reproduire un bug.
  const { depuis, type } = use(searchParams);
  const queryClient = useQueryClient();
  const [category, setCategory] = useState<FeedbackCategory | null>(
    FEEDBACK_CATEGORIES.find((c) => c.value === type?.toUpperCase())?.value ?? null
  );
  const [message, setMessage] = useState("");

  const historyQuery = useQuery({ queryKey: ["my-feedback"], queryFn: fetchMyFeedback });

  const mutation = useMutation({
    mutationFn: () =>
      sendFeedback({
        category: category!,
        message: message.trim(),
        page: depuis?.startsWith("/") ? depuis : undefined,
        appInstalled: isStandalone(),
      }),
    onSuccess: () => {
      toast.success("Message envoyé, merci !");
      setMessage("");
      setCategory(null);
      queryClient.invalidateQueries({ queryKey: ["my-feedback"] });
    },
    onError: (err: Error) => toast.error(err.message || "Envoi impossible, réessaie dans un instant."),
  });

  const length = message.trim().length;
  const valid = category !== null && length >= 5 && length <= MAX_LENGTH;
  const selected = FEEDBACK_CATEGORIES.find((c) => c.value === category);

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Nous écrire</h1>
        <p className="mt-1 text-muted-foreground">
          Un bug, une question, une idée ou une remarque : réponse dans ta cloche.
        </p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-4 pt-6">
          <div role="radiogroup" aria-label="Type de message" className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {FEEDBACK_CATEGORIES.map(({ value, label, icon: Icon }) => (
              <button
                key={value}
                type="button"
                role="radio"
                aria-checked={category === value}
                onClick={() => setCategory(value)}
                className={cn(
                  "flex flex-col items-center gap-1.5 rounded-xl px-3 py-3 text-sm transition-colors",
                  category === value ? "glass-accent font-medium" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
                )}
              >
                <Icon className="h-5 w-5" />
                {label}
              </button>
            ))}
          </div>

          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            maxLength={MAX_LENGTH + 50}
            rows={6}
            placeholder={selected?.placeholder ?? "Ton message"}
            className="glass-inset w-full resize-y rounded-xl px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
            aria-label="Ton message"
          />

          <div className="flex justify-end">
            <span className={cn("font-mono text-xs", length > MAX_LENGTH ? "text-destructive" : "text-muted-foreground")}>
              {length}/{MAX_LENGTH}
            </span>
          </div>

          <Button className="w-full sm:w-fit" disabled={!valid || mutation.isPending} onClick={() => mutation.mutate()}>
            {mutation.isPending ? "Envoi…" : "Envoyer"}
          </Button>
        </CardContent>
      </Card>

      <section className="flex flex-col gap-3">
        <h2 className="font-heading text-lg font-semibold">Mes messages</h2>
        {historyQuery.isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
        {historyQuery.isError && (
          <p className="text-sm text-destructive">Impossible de charger tes messages. Vérifie ta connexion et recharge la page.</p>
        )}
        {historyQuery.data?.length === 0 && (
          <p className="text-sm text-muted-foreground">Tu ne nous as encore rien envoyé.</p>
        )}
        {historyQuery.data?.map((f) => (
          <FeedbackThread key={f.id} feedback={f} />
        ))}
      </section>
    </div>
  );
}

function FeedbackThread({ feedback }: { feedback: Feedback }) {
  const category = FEEDBACK_CATEGORIES.find((c) => c.value === feedback.category);
  const Icon = category?.icon ?? MessageSquare;
  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5">
            <Icon className="h-3.5 w-3.5" />
            {category?.label} · {formatShortDateTime(new Date(feedback.createdAt))}
          </span>
          <span
            className={cn(
              "rounded-full px-2 py-0.5",
              feedback.status === "ANSWERED" ? "bg-primary/15 text-primary" : "glass-inset-quiet"
            )}
          >
            {STATUS_LABEL[feedback.status]}
          </span>
        </div>
        <p className="whitespace-pre-wrap text-sm">{feedback.message}</p>
        {feedback.reply && (
          <div className="glass-inset-quiet flex flex-col gap-1 rounded-xl border-l-2 border-primary p-3">
            <p className="flex items-center gap-1.5 text-xs font-medium text-primary">
              <MessageSquareReply className="h-3.5 w-3.5" />
              Réponse de l&apos;équipe HoopPicks
              {feedback.repliedAt && (
                <span className="font-normal text-muted-foreground">· {formatShortDateTime(new Date(feedback.repliedAt))}</span>
              )}
            </p>
            <p className="whitespace-pre-wrap text-sm">{feedback.reply}</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
