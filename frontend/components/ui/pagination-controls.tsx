"use client";

import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * Pagine un tableau côté client. Revient automatiquement à la page 1 si la
 * liste source change de taille (nouveau filtre, refetch avec moins
 * d'éléments...) pour éviter de se retrouver sur une page vide.
 */
export function usePagination<T>(items: T[] | undefined, pageSize: number) {
  const [page, setPage] = useState(1);
  const safeItems = items ?? [];
  const pageCount = Math.max(1, Math.ceil(safeItems.length / pageSize));

  // Revient à la page 1 si la taille de la liste change (nouveau filtre,
  // refetch avec moins d'éléments...), pour éviter une page vide.
  // Ajustement pendant le rendu plutôt que dans un effet, cf. recommandation React.
  const [lastLength, setLastLength] = useState(safeItems.length);
  if (safeItems.length !== lastLength) {
    setLastLength(safeItems.length);
    if (page !== 1) setPage(1);
  }

  const clampedPage = Math.min(page, pageCount);
  const start = (clampedPage - 1) * pageSize;
  const pageItems = safeItems.slice(start, start + pageSize);

  return {
    page: clampedPage,
    pageCount,
    pageItems,
    setPage,
    totalCount: safeItems.length,
  };
}

export function PaginationControls({
  page,
  pageCount,
  onPageChange,
}: {
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
}) {
  if (pageCount <= 1) return null;

  return (
    <div className="flex items-center justify-between border-t border-border pt-4">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 1}
        className="gap-1"
      >
        <ChevronLeft className="h-4 w-4" />
        Précédent
      </Button>

      <span className="font-mono text-xs text-muted-foreground">
        Page {page} / {pageCount}
      </span>

      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= pageCount}
        className="gap-1"
      >
        Suivant
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  );
}
