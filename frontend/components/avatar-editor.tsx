"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { updateProfile } from "@/lib/api/auth";
import { AvatarColorway, AvatarIcon, AvatarPosition, UserProfile } from "@/lib/types";
import {
  AVATAR_COLORWAYS,
  AVATAR_ICONS,
  AVATAR_POSITIONS,
} from "@/components/player-avatar";
import { PlayerCard } from "@/components/player-card";

export function AvatarEditor({ profile }: { profile: UserProfile }) {
  const queryClient = useQueryClient();

  const [number, setNumber] = useState(profile.avatarNumber);
  const [position, setPosition] = useState<AvatarPosition>(profile.avatarPosition);
  const [colorway, setColorway] = useState<AvatarColorway>(profile.avatarColorway);
  const [icon, setIcon] = useState<AvatarIcon>(profile.avatarIcon);

  const mutation = useMutation({
    mutationFn: () =>
      updateProfile({ avatarNumber: number, avatarPosition: position, avatarColorway: colorway, avatarIcon: icon }),
    onSuccess: (updated) => {
      queryClient.setQueryData(["profile"], updated);
      toast.success("Carte joueur mise à jour !");
    },
    onError: () => {
      toast.error("Impossible d'enregistrer ta carte. Réessaie.");
    },
  });

  const hasChanges =
    number !== profile.avatarNumber ||
    position !== profile.avatarPosition ||
    colorway !== profile.avatarColorway ||
    icon !== profile.avatarIcon;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="font-heading text-lg">Ta carte joueur</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-5">
        <div className="flex items-center gap-5">
          <PlayerCard
            username={profile.username}
            number={number}
            position={position}
            colorway={colorway}
            icon={icon}
          />
          <div className="flex flex-col gap-1">
            <label htmlFor="avatar-number" className="text-xs uppercase tracking-wide text-muted-foreground">
              Numéro de maillot
            </label>
            <Input
              id="avatar-number"
              type="number"
              min={0}
              max={99}
              value={number}
              onChange={(e) => {
                const v = Number(e.target.value);
                setNumber(Number.isFinite(v) ? Math.max(0, Math.min(99, v)) : 0);
              }}
              className="w-20"
            />
          </div>
        </div>

        <div>
          <p className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">Poste</p>
          <div className="flex flex-wrap gap-2">
            {AVATAR_POSITIONS.map((pos) => (
              <button
                key={pos}
                type="button"
                onClick={() => setPosition(pos)}
                aria-pressed={position === pos}
                className={cn(
                  "rounded-xl px-3 py-1.5 font-mono text-sm font-bold transition-colors",
                  position === pos ? "glass-accent" : "glass-inset text-muted-foreground hover:text-foreground"
                )}
              >
                {pos}
              </button>
            ))}
          </div>
        </div>

        <div>
          <p className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">Palette</p>
          <div className="flex flex-wrap gap-2">
            {(Object.keys(AVATAR_COLORWAYS) as AvatarColorway[]).map((key) => {
              const palette = AVATAR_COLORWAYS[key];
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => setColorway(key)}
                  title={palette.label}
                  aria-label={palette.label}
                  aria-pressed={colorway === key}
                  className={cn(
                    "h-8 w-8 rounded-full border-2 transition-transform",
                    colorway === key ? "scale-110 border-foreground" : "border-transparent hover:scale-105"
                  )}
                  style={{
                    background: `linear-gradient(135deg, ${palette.from}, ${palette.to})`,
                  }}
                />
              );
            })}
          </div>
        </div>

        <div>
          <p className="mb-2 text-xs uppercase tracking-wide text-muted-foreground">Style de jeu</p>
          <div className="flex flex-wrap gap-2">
            {(Object.keys(AVATAR_ICONS) as AvatarIcon[]).map((key) => {
              const { icon: Icon, label } = AVATAR_ICONS[key];
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => setIcon(key)}
                  aria-pressed={icon === key}
                  className={cn(
                    "flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-sm transition-colors",
                    icon === key ? "glass-accent" : "glass-inset text-muted-foreground hover:text-foreground"
                  )}
                >
                  <Icon className="h-3.5 w-3.5" />
                  {label}
                </button>
              );
            })}
          </div>
        </div>

        <Button
          onClick={() => mutation.mutate()}
          disabled={!hasChanges || mutation.isPending}
          className="w-fit"
        >
          {mutation.isPending ? "Enregistrement..." : "Enregistrer ma carte"}
        </Button>
      </CardContent>
    </Card>
  );
}
