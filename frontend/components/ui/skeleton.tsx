import { cn } from "@/lib/utils"

function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      // bg-muted (#1E293B, opaque) → blanc translucide : un skeleton opaque sur
      // une carte en verre fait un trou noir dans la carte. rounded-lg pour
      // suivre les nouveaux rayons.
      className={cn("animate-pulse rounded-lg bg-white/[0.07]", className)}
      {...props}
    />
  )
}

export { Skeleton }
