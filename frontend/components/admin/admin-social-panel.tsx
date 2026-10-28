"use client";

import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Copy, Download, Flame, Trophy } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchAdminInsights } from "@/lib/api/admin";
import { formatRelativeTime } from "@/lib/utils";

// Images déjà générées par l'app, prêtes à poster (404 tant que le titre n'existe pas).
const CARDS = [
  { src: "/podium/carte?periode=week", label: "Podium de la semaine" },
  { src: "/podium/carte?periode=season", label: "Podium de la saison" },
  { src: "/champion/carte", label: "Champion de la semaine passée" },
  { src: "/champion/carte?phase=month", label: "Champion du mois passé" },
  { src: "/saison/revelation/carte", label: "Révélation des pronostics de saison" },
];

const odds = (n: number) => n.toLocaleString("fr-FR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function copy(text: string) {
  navigator.clipboard?.writeText(text).then(
    () => toast.success("Copié."),
    () => toast.error("Copie impossible.")
  );
}

/** Onglet « Réseaux » : de quoi alimenter les posts de la semaine. */
export function AdminSocialPanel() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["admin-insights"], queryFn: fetchAdminInsights, refetchInterval: 5 * 60_000 });
  const origin = typeof window === "undefined" ? "" : window.location.origin;

  if (isLoading) return <Skeleton className="h-72 w-full rounded-2xl" />;
  if (isError || !data) return <p className="text-sm text-destructive">Impossible de charger les chiffres.</p>;

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="flex items-center gap-2 text-sm font-medium">
            <Trophy className="h-4 w-4 text-primary" />
            Plus gros gains · 7 jours
          </p>
          {data.bigWins.length === 0 && <p className="text-sm text-muted-foreground">Aucun ticket gagné cette semaine.</p>}
          <div className="flex flex-col divide-y divide-tint/10">
            {data.bigWins.map((w) => (
              <div key={w.betId} className="flex items-center gap-3 py-2.5">
                <div className="min-w-0 flex-1">
                  <p className="text-sm">
                    <span className="font-semibold">@{w.username}</span>{" "}
                    <span className="font-mono font-bold text-success">+{w.profit.toLocaleString("fr-FR")} pts</span>
                  </p>
                  <p className="truncate text-xs text-muted-foreground">
                    {w.labels} · cote {odds(w.odds)} · mise {w.stake} · {formatRelativeTime(w.resolvedAt)}
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    copy(`🔥 @${w.username} encaisse +${w.profit.toLocaleString("fr-FR")} pts sur HoopPicks (cote ${odds(w.odds)}) : ${w.labels}\n${origin}/t/${w.betId}`)
                  }
                >
                  <Copy className="h-3.5 w-3.5" />
                  Texte
                </Button>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="flex items-center gap-2 text-sm font-medium">
            <Flame className="h-4 w-4 text-primary" />
            Paris les plus joués · 7 jours
          </p>
          {data.popular.length === 0 && <p className="text-sm text-muted-foreground">Pas encore de pari cette semaine.</p>}
          {data.popular.map((p, i) => (
            <div key={`${p.label}-${p.matchLabel}`} className="flex items-center gap-3 text-sm">
              <span className="w-5 font-mono text-xs text-muted-foreground">{i + 1}</span>
              <span className="min-w-0 flex-1 truncate">
                <span className="font-medium">{p.label}</span> <span className="text-muted-foreground">· {p.matchLabel}</span>
              </span>
              <span className="font-mono text-xs font-bold">{p.tickets}</span>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="text-sm font-medium">Images prêtes à poster</p>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            {CARDS.map((c) => (
              <a key={c.src} href={c.src} target="_blank" rel="noreferrer" className="group flex flex-col gap-1.5">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={c.src} alt={c.label} loading="lazy" className="aspect-square w-full rounded-xl bg-tint/10 object-cover transition-transform group-hover:scale-[1.02]" onError={(e) => (e.currentTarget.style.visibility = "hidden")} />
                <span className="flex items-center gap-1 text-xs text-muted-foreground group-hover:text-foreground">
                  <Download className="h-3 w-3" />
                  {c.label}
                </span>
              </a>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
