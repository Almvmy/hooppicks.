import { mergeProps } from "@base-ui/react/merge-props"
import { useRender } from "@base-ui/react/use-render"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const badgeVariants = cva(
  // border → shadow inset : sur du verre, une bordure 1px sombre fait une
  // rayure ; un inset blanc lit comme une arête. rounded-4xl conservé.
  "group/badge inline-flex h-5 w-fit shrink-0 items-center justify-center gap-1 overflow-hidden rounded-4xl border-0 px-2 py-0.5 text-xs font-medium whitespace-nowrap transition-all focus-visible:ring-[3px] focus-visible:ring-ring/50 has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&>svg]:pointer-events-none [&>svg]:size-3!",
  {
    variants: {
      variant: {
        // L'aplat orange est conservé UNIQUEMENT ici : c'est le badge le plus
        // fort du système (compteur de notifications), il doit rester opaque.
        default: "bg-primary text-primary-foreground [a]:hover:bg-primary/80",
        secondary:
          "bg-white/[0.06] text-secondary-foreground shadow-[inset_0_0_0_1px_rgba(255,255,255,0.12)] [a]:hover:bg-white/[0.10]",
        destructive:
          "bg-destructive/10 text-destructive shadow-[inset_0_0_0_1px_rgba(239,68,68,0.35)] focus-visible:ring-destructive/20 [a]:hover:bg-destructive/20",
        outline:
          "text-foreground shadow-[inset_0_0_0_1px_rgba(255,255,255,0.14)] [a]:hover:bg-white/[0.06]",
        ghost: "hover:bg-white/[0.06] hover:text-muted-foreground",
        link: "text-primary underline-offset-4 hover:underline",
        // Nouvelles variantes sémantiques, alignées sur les tokens existants :
        // elles remplacent les `bg-*/10 border-*/30` recopiés à la main dans
        // match-status-badge.tsx et recent-activity.tsx.
        lit: "glass-accent",
        success:
          "bg-success/10 text-success shadow-[inset_0_0_0_1px_color-mix(in_oklch,var(--success),transparent_60%)]",
        live: "bg-live/10 text-live shadow-[inset_0_0_0_1px_color-mix(in_oklch,var(--live),transparent_60%)]",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

function Badge({
  className,
  variant = "default",
  render,
  ...props
}: useRender.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return useRender({
    defaultTagName: "span",
    props: mergeProps<"span">(
      {
        className: cn(badgeVariants({ variant }), className),
      },
      props
    ),
    render,
    state: {
      slot: "badge",
      variant,
    },
  })
}

export { Badge, badgeVariants }
