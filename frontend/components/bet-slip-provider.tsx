"use client";

import { createContext, useContext, useMemo, useState } from "react";
import { BetSelection } from "@/lib/types";

interface BetSlipContextValue {
  selections: BetSelection[];
  stake: number;
  setStake: (value: number) => void;
  toggleSelection: (selection: BetSelection) => void;
  clear: () => void;
  totalOdds: number;
  potentialPayout: number;
}

const BetSlipContext = createContext<BetSlipContextValue | null>(null);

export function BetSlipProvider({ children }: { children: React.ReactNode }) {
  const [selections, setSelections] = useState<BetSelection[]>([]);
  const [stake, setStake] = useState(0);

  function toggleSelection(selection: BetSelection) {
    setSelections((prev) => {
      const exists = prev.find((s) => s.id === selection.id);
      if (exists) return prev.filter((s) => s.id !== selection.id);
      // Une seule sélection active par match, pour éviter les combinaisons contradictoires (ex: pari sur les deux équipes gagnantes du même match).
      const withoutSameMatch = prev.filter((s) => s.matchId !== selection.matchId);
      return [...withoutSameMatch, selection];
    });
  }

  function clear() {
    setSelections([]);
    setStake(0);
  }

  const totalOdds = useMemo(
    () => selections.reduce((acc, s) => acc * s.odds, 1),
    [selections]
  );

  const potentialPayout = useMemo(
    () => Math.round(stake * totalOdds),
    [stake, totalOdds]
  );

  return (
    <BetSlipContext.Provider
      value={{ selections, stake, setStake, toggleSelection, clear, totalOdds, potentialPayout }}
    >
      {children}
    </BetSlipContext.Provider>
  );
}

export function useBetSlip() {
  const ctx = useContext(BetSlipContext);
  if (!ctx) throw new Error("useBetSlip doit être utilisé dans un BetSlipProvider");
  return ctx;
}