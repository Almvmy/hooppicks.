"use client";

import { useEffect, useState } from "react";
import { Lightbulb } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { BASKETBALL_FACTS, pickRandomFact } from "@/lib/basketball-facts";

const ROTATE_MS = 12000;

export function FunFactCard() {
  const [fact, setFact] = useState(BASKETBALL_FACTS[0]);

  useEffect(() => {
    const interval = setInterval(() => {
      setFact((current) => pickRandomFact(current));
    }, ROTATE_MS);
    return () => clearInterval(interval);
  }, []);

  return (
    <Card>
      <CardContent className="flex items-start gap-3 pt-6">
        <Lightbulb className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Le savais-tu ?</p>
          <p className="mt-1 text-sm text-foreground/90">{fact}</p>
        </div>
      </CardContent>
    </Card>
  );
}
