"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Copy, Shield, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { createLeague, fetchMyLeagues, joinLeague, previewLeague } from "@/lib/api/leagues";
import { LeaguePreview } from "@/lib/types";

export default function LeaguesPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [preview, setPreview] = useState<LeaguePreview | null>(null);

  const leaguesQuery = useQuery({ queryKey: ["leagues"], queryFn: fetchMyLeagues });

  const createMutation = useMutation({
    mutationFn: () => createLeague(name.trim()),
    onSuccess: (league) => {
      queryClient.setQueryData(["leagues"], (prev: typeof leaguesQuery.data) => [
        ...(prev ?? []),
        league,
      ]);
      setName("");
      toast.success(`Ligue "${league.name}" créée — code ${league.inviteCode}`);
    },
    onError: () => toast.error("Impossible de créer la ligue. Réessaie."),
  });

  const previewMutation = useMutation({
    mutationFn: () => previewLeague(code.trim()),
    onSuccess: (result) => setPreview(result),
    onError: () => toast.error("Code d'invitation invalide."),
  });

  const joinMutation = useMutation({
    mutationFn: () => joinLeague(code.trim()),
    onSuccess: (league) => {
      queryClient.invalidateQueries({ queryKey: ["leagues"] });
      setCode("");
      setPreview(null);
      toast.success(`Tu as rejoint "${league.name}" !`);
    },
    onError: () => toast.error("Impossible de rejoindre cette ligue. Réessaie."),
  });

  function copyCode(inviteCode: string) {
    navigator.clipboard.writeText(inviteCode);
    toast.success("Code copié !");
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Ligues</h1>
        <p className="mt-1 text-muted-foreground">
          Crée une ligue privée ou rejoins celle de tes potes pour comparer vos pronostics.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Créer une ligue</p>
            <div className="flex gap-2">
              <Input
                placeholder="Nom de la ligue"
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && name.trim() && createMutation.mutate()}
              />
              <Button
                onClick={() => createMutation.mutate()}
                disabled={createMutation.isPending || !name.trim()}
              >
                Créer
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Rejoindre avec un code</p>

            {preview ? (
              <div className="flex flex-col gap-2.5">
                <p className="text-sm">
                  Rejoindre <span className="font-semibold">{preview.name}</span> (
                  {preview.memberCount} membre{preview.memberCount > 1 ? "s" : ""}) ?
                </p>
                <div className="flex gap-2">
                  <Button
                    onClick={() => joinMutation.mutate()}
                    disabled={joinMutation.isPending}
                  >
                    Confirmer
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => setPreview(null)}
                    disabled={joinMutation.isPending}
                  >
                    Annuler
                  </Button>
                </div>
              </div>
            ) : (
              <div className="flex gap-2">
                <Input
                  placeholder="Ex. K7P2QX"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  onKeyDown={(e) => e.key === "Enter" && code.trim() && previewMutation.mutate()}
                  className="font-mono uppercase"
                  maxLength={6}
                />
                <Button
                  variant="outline"
                  onClick={() => previewMutation.mutate()}
                  disabled={previewMutation.isPending || !code.trim()}
                >
                  Rechercher
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-col gap-3">
        {leaguesQuery.isLoading && (
          <>
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-20 w-full" />
          </>
        )}

        {!leaguesQuery.isLoading && leaguesQuery.data?.length === 0 && (
          <Card>
            <CardContent className="flex flex-col items-center gap-2 py-10 text-center text-muted-foreground">
              <Shield className="h-8 w-8" />
              Pas encore de ligue. Crées-en une ou rejoins celle d&apos;un ami.
            </CardContent>
          </Card>
        )}

        {leaguesQuery.data?.map((league) => (
          <Card key={league.id}>
            <CardContent className="flex items-center justify-between gap-4 pt-6">
              <Link href={`/leagues/${league.id}`} className="flex flex-1 items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <Shield className="h-5 w-5" />
                </div>
                <div>
                  <p className="font-medium">
                    {league.name}
                    {league.isOwner && (
                      <span className="ml-2 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-primary">
                        Créateur
                      </span>
                    )}
                  </p>
                  <p className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Users className="h-3 w-3" />
                    {league.memberCount} membre{league.memberCount > 1 ? "s" : ""}
                  </p>
                </div>
              </Link>
              <button
                type="button"
                onClick={() => copyCode(league.inviteCode)}
                className="glass-inset flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 font-mono text-xs font-bold text-muted-foreground transition-colors hover:text-foreground"
              >
                {league.inviteCode}
                <Copy className="h-3 w-3" />
              </button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
