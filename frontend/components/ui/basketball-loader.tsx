import { cn } from "@/lib/utils";

/**
 * Loader "ballon qui rebondit" pour les états de chargement pleine page :
 * plus fun que des skeletons génériques pour un site NBA, à réserver aux
 * chargements de listes/pages entières (les skeletons en shimmer restent
 * préférables pour prévisualiser la forme d'un contenu précis).
 */
export function BasketballLoader({
  label,
  className,
}: {
  label?: string;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col items-center justify-center gap-3 py-10", className)}>
      <div className="relative flex h-16 w-10 flex-col items-center justify-end">
        <svg viewBox="0 0 40 40" width="32" height="32" className="animate-bball-bounce">
          <circle cx="20" cy="20" r="18" fill="var(--primary)" />
          <g stroke="rgba(0,0,0,0.35)" strokeWidth="1.6" fill="none">
            <path d="M2 20 H38" />
            <path d="M20 2 V38" />
            <path d="M7 7 Q20 20 7 33" />
            <path d="M33 7 Q20 20 33 33" />
          </g>
        </svg>
        <div className="animate-bball-shadow h-2 w-8 rounded-full bg-foreground" />
      </div>
      {label && <p className="text-sm text-muted-foreground">{label}</p>}
    </div>
  );
}
