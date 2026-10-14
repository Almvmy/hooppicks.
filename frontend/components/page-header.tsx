import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface PageHeaderProps {
  badge?: ReactNode;
  title: ReactNode;
  subtitle?: ReactNode;
  className?: string;
}

export function PageHeader({ badge, title, subtitle, className }: PageHeaderProps) {
  return (
    <div className={cn("border-l-4 border-primary bg-primary/[0.04] rounded-r-xl px-4 py-3", className)}>
      {badge && (
        <p className="mb-1 font-condensed text-[10px] font-bold uppercase tracking-[.14em] text-primary">
          {badge}
        </p>
      )}
      <h1 className="font-condensed text-[28px] font-black uppercase leading-none tracking-[.01em] text-foreground">
        {title}
      </h1>
      {subtitle && (
        <p className="mt-1.5 font-condensed text-[13px] font-bold tracking-[.04em] text-muted-foreground">
          {subtitle}
        </p>
      )}
    </div>
  );
}
