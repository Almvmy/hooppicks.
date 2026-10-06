"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Megaphone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { fetchAdminAudit, sendAnnouncement } from "@/lib/api/admin";
import { cn, formatShortDateTime } from "@/lib/utils";

const MAX_LENGTH = 280;

export function AdminAnnouncements() {
  const queryClient = useQueryClient();
  const [message, setMessage] = useState("");
  const [push, setPush] = useState(false);
  const [confirming, setConfirming] = useState(false);

  // Historique tiré du journal d'audit : pas de table à part pour ça.
  const { data: audit } = useQuery({ queryKey: ["admin-audit"], queryFn: () => fetchAdminAudit(200) });
  const history = (audit ?? []).filter((e) => e.action === "ANNOUNCE").slice(0, 10);

  const mutation = useMutation({
    mutationFn: () => sendAnnouncement(message.trim(), push),
    onSuccess: (r) => {
      toast.success(`Annonce envoyée à ${r.recipients} joueur(s).`);
      setMessage("");
      setPush(false);
      queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
    onError: (err: Error) => toast.error(err.message || "Envoi impossible."),
  });

  const length = message.trim().length;
  const valid = length > 0 && length <= MAX_LENGTH;

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="flex items-center gap-2 font-medium">
            <Megaphone className="h-4 w-4 text-primary" />
            Nouvelle annonce
          </p>
          <p className="text-xs text-muted-foreground">
            Arrive dans la cloche de tous les joueurs. Maintenance, nouveauté, début des playoffs… Reste bref.
          </p>
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            maxLength={MAX_LENGTH + 20}
            rows={4}
            placeholder="Ex. : Les playoffs commencent samedi : les cotes des séries sont en ligne !"
            className="glass-inset w-full resize-y rounded-xl px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
            aria-label="Message de l'annonce"
          />
          <div className="flex flex-wrap items-center justify-between gap-2">
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={push} onChange={(e) => setPush(e.target.checked)} className="h-4 w-4 accent-[var(--primary)]" />
              Envoyer aussi en notification push
            </label>
            <span className={cn("font-mono text-xs", length > MAX_LENGTH ? "text-destructive" : "text-muted-foreground")}>
              {length}/{MAX_LENGTH}
            </span>
          </div>
          <Button className="w-fit" disabled={!valid || mutation.isPending} onClick={() => setConfirming(true)}>
            {mutation.isPending ? "Envoi…" : "Envoyer à tous"}
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Dernières annonces</p>
          {history.length === 0 && <p className="text-sm text-muted-foreground">Aucune annonce envoyée pour l&apos;instant.</p>}
          {history.map((e) => (
            <div key={e.id} className="glass-inset-quiet flex flex-col gap-1 rounded-xl p-3">
              <p className="text-sm">{e.details}</p>
              <p className="text-xs text-muted-foreground">
                {formatShortDateTime(new Date(e.date))} · par @{e.adminUsername} · {e.target}
              </p>
            </div>
          ))}
        </CardContent>
      </Card>

      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogTitle>Envoyer à tous les joueurs ?</AlertDialogTitle>
          <AlertDialogDescription>
            « {message.trim()} »{push ? " · avec notification push" : ""}. Une annonce envoyée ne peut pas être retirée.
          </AlertDialogDescription>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction onClick={() => mutation.mutate()}>Envoyer</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
