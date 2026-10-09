import { Badge } from "@/components/ui/badge";
import { MatchStatus } from "@/lib/types";
import { LiveStatus, liveClockLabel } from "@/lib/live";

/**
 * Les trois états d'un match. Après le lot 2, plus aucune classe écrite à la
 * main : les variantes `secondary` / `live` / `success` du Badge portent les
 * liserés, donc un changement de grammaire visuelle se fait en un seul endroit.
 */
const STATUS_CONFIG: Record<
  MatchStatus,
  { label: string; variant: "secondary" | "live" | "success" }
> = {
  scheduled: { label: "À venir", variant: "secondary" },
  live: { label: "En direct", variant: "live" },
  finished: { label: "Terminé", variant: "success" },
};

/** `live` : quart-temps et chrono du direct à la place de « En direct », quand on les a. */
export function MatchStatusBadge({ status, live }: { status: MatchStatus; live?: LiveStatus }) {
  const config = STATUS_CONFIG[status] ?? STATUS_CONFIG.scheduled;
  const label = status === "live" && live ? liveClockLabel(live) : config.label;

  return (
    <Badge variant={config.variant} className="font-mono">
      {status === "live" && (
        <span
          aria-hidden
          className="mr-0.5 h-1.5 w-1.5 shrink-0 animate-pulse rounded-full bg-live"
        />
      )}
      {label}
    </Badge>
  );
}
