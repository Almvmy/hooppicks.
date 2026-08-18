import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background px-4 text-center">
      <span className="font-mono text-sm text-muted-foreground">Erreur 404</span>
      <h1 className="font-heading text-4xl font-bold tracking-tight sm:text-5xl">
        Match <span className="text-primary">introuvable</span>
      </h1>
      <p className="max-w-md text-muted-foreground">
        Cette page n&apos;existe pas ou a été déplacée. Retourne au tableau de
        bord pour retrouver tes pronostics.
      </p>
      <Link href="/dashboard" className={cn(buttonVariants({ size: "lg" }), "mt-2")}>
        Retour au tableau de bord
      </Link>
    </div>
  );
}
