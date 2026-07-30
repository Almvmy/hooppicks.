import { Badge } from "@/components/ui/badge";
import { MatchStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

const STATUS_CONFIG: Record<MatchStatus, { label: string; className: string }> = {
  scheduled: { label: "À venir", className: "border-border bg-secondary text-muted-foreground" },
  live: { label: "En direct", className: "border-destructive/30 bg-destructive/10 text-destructive" },
  finished: { label: "Terminé", className: "border-success/30 bg-success/10 text-success" },
};

export function MatchStatusBadge({ status }: { status: MatchStatus }) {
  const config = STATUS_CONFIG[status];
  return (
    <Badge variant="outline" className={cn("font-mono text-xs", config.className)}>
      {status === "live" && (
        <span className="mr-1.5 inline-block h-1.5 w-1.5 animate-pulse rounded-full bg-destructive" />
      )}
      {config.label}
    </Badge>
  );
}