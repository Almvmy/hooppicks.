import * as React from "react"
import { Input as InputPrimitive } from "@base-ui/react/input"

import { cn } from "@/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <InputPrimitive
      type={type}
      data-slot="input"
      // Deux changements :
      // - border-input bg-transparent → glass-inset (le champ posé sur du verre
      //   a besoin de sa propre strate, sinon il disparaît).
      // - h-8 → h-11 : 44px, cible tactile. Ce champ est la saisie de mise du
      //   bet slip sur mobile ; 32px était trop petit pour un pouce.
      className={cn(
        "glass-inset h-11 w-full min-w-0 rounded-xl border-0 bg-transparent px-3 py-1 text-base transition-colors outline-none file:inline-flex file:h-6 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm",
        className
      )}
      {...props}
    />
  )
}

export { Input }
