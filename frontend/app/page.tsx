import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function HomePage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 text-center">
      <span className="mb-4 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 font-mono text-xs text-primary">
        Saison NBA 2026 — Points virtuels
      </span>
      <h1 className="font-heading text-4xl font-bold tracking-tight sm:text-5xl">
        Hoop<span className="text-primary">Picks</span>
      </h1>
      <p className="mt-4 max-w-md text-muted-foreground">
        Pronostique sur chaque match de la saison NBA, grimpe dans le
        classement, aucun argent réel en jeu.
      </p>
      <div className="mt-8 flex gap-3">
        <Link href="/register" className={cn(buttonVariants({ size: "lg" }))}>
          Créer un compte
        </Link>
        <Link
          href="/login"
          className={cn(buttonVariants({ size: "lg", variant: "outline" }))}
        >
          Se connecter
        </Link>
      </div>
    </div>
  );
}