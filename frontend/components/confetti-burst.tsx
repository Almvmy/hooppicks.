"use client";

import { useEffect, useState } from "react";

interface ConfettiPiece {
  id: number;
  left: number; // vw
  delay: number; // s
  duration: number; // s
  color: string;
  rotation: number;
}

const COLORS = ["#FF7A1A", "#FFB380", "#22C55E", "#3B82F6", "#F1F5F9"];

function generatePieces(count: number): ConfettiPiece[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i,
    left: Math.random() * 100,
    delay: Math.random() * 0.3,
    duration: 1.6 + Math.random() * 0.8,
    color: COLORS[i % COLORS.length],
    rotation: Math.random() * 360,
  }));
}

/**
 * Confettis "swish" en CSS pur (pas de canvas, pas de dépendance). Piloté
 * entièrement par le montage/démontage : le parent affiche ce composant le
 * temps de la célébration, chaque montage génère une nouvelle salve, et il
 * se referme seul après l'animation via `onDone`.
 */
export function ConfettiBurst({ onDone }: { onDone?: () => void }) {
  const [pieces] = useState<ConfettiPiece[]>(() => generatePieces(28));

  useEffect(() => {
    const timeout = setTimeout(() => {
      onDone?.();
    }, 2200);
    return () => clearTimeout(timeout);
  }, [onDone]);

  return (
    <div className="pointer-events-none fixed inset-0 z-[200] overflow-hidden">
      {pieces.map((p) => (
        <span
          key={p.id}
          className="absolute top-[-10px] h-2.5 w-1.5 rounded-sm"
          style={{
            left: `${p.left}vw`,
            backgroundColor: p.color,
            animation: `confetti-fall ${p.duration}s ease-in ${p.delay}s forwards`,
            transform: `rotate(${p.rotation}deg)`,
          }}
        />
      ))}
    </div>
  );
}
