import { LogoMark } from "@/app/logo-mark";

type LogoSymbolProps = {
  className?: string;
  /** "compact" (défaut) : version simplifiée, sans pointillés, pour les
   *  petites tailles (≤ 32 px).
   *  "mid" : version moyenne du kit, pour les en-têtes (36 à 72 px).
   *  "full" : avec le tir en pointillés fins, à partir de 72 px. */
  variant?: "full" | "mid" | "compact";
};

// Symbole de marque : un tir qui entre dans le panier. La jante et le filet
// prennent la couleur du texte (--foreground), donc blanc en Nuit, marine
// sur le sable ; le ballon reste orange de marque dans les deux thèmes.
export const LogoSymbol = ({ className = "w-10 h-10", variant = "compact" }: LogoSymbolProps) => (
  <LogoMark
    variant={variant === "full" ? "full" : variant === "mid" ? "medium" : "small"}
    className={`text-foreground ${className}`}
    arcClassName="fill-[#00E5FF] light:fill-[#0A6577]"
  />
);
