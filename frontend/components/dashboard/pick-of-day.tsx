import Link from "next/link";
import { Radio, Clock } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { MatchOddsRow } from "@/components/match-odds-row";
import { FaceOffTeams, TeamWatermarks, faceOffBackground } from "@/components/match-face-off";
import { NbaLogo } from "@/components/nba-logo";
import { Match } from "@/lib/types";
import { cn, formatMatchDate, formatMatchTime, isBettable } from "@/lib/utils";

export function PickOfDay({ match }: { match: Match | undefined }) {
  if (!match) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-1 py-10 text-center">
          <p className="font-heading text-base font-bold">Aucun match programmé</p>
          <p className="text-sm text-muted-foreground">
            Reviens plus tard, le calendrier NBA n&apos;est pas encore ouvert.
          </p>
        </CardContent>
      </Card>
    );
  }

  const isLive = match.status === "live";
  const date = new Date(match.date);

  return (
    // Le match phare du jour en grand : logos 72 px et filigranes plus présents.
    <Card style={faceOffBackground(match, 18)} className="relative overflow-hidden">
      <TeamWatermarks match={match} size={260} opacity={0.12} />
      <CardContent className="relative pt-6">
        <div className="flex items-center justify-between">
          <span
            className={cn(
              "flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide",
              isLive ? "text-destructive" : "text-primary"
            )}
          >
            {isLive ? (
              <>
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-destructive" />
                En direct
              </>
            ) : (
              <>
                <NbaLogo size={18} />
                <Radio className="h-3.5 w-3.5" />
                Prochain coup d&apos;envoi
              </>
            )}
          </span>
          <span className="flex items-center gap-1 font-mono text-xs text-muted-foreground">
            <Clock className="h-3.5 w-3.5" />
            {formatMatchDate(date)}
            {" · "}
            {formatMatchTime(date)}
          </span>
        </div>

        <Link href={`/matches/${match.id}`} className="mt-5 block">
          <FaceOffTeams match={match} logoSize={72} morph nameClassName="text-lg" scoreClassName="text-3xl" />
        </Link>

        {isBettable(match) && (
          <div className="mt-4">
            <MatchOddsRow match={match} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}
