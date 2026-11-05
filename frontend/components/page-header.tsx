import type { ReactNode } from "react";

interface PageHeaderProps {
  title: ReactNode;
  subtitle?: ReactNode;
  className?: string;
}

/**
 * Le titre d'une page, le même partout. Pas de rappel de la rubrique au-dessus :
 * les onglets juste au-dessus la disent déjà.
 */
export function PageHeader({ title, subtitle, className }: PageHeaderProps) {
  return (
    <div className={className}>
      <h1 className="font-heading text-2xl font-bold leading-tight">{title}</h1>
      {subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}
    </div>
  );
}
