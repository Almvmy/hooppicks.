import Image from "next/image";
import Link from "next/link";
import { LogoSymbol } from "@/app/LogoSymbol";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    // app-field ajouté : les écrans d'auth sont HORS de l'AppShell, donc ils
    // n'héritaient pas du champ lumineux. Sans lui, la carte de login en verre
    // (Card → .glass) floute un fond uni et rend gris.
    // On garde la photo de terrain : elle passe AU-DESSUS du champ, et c'est
    // elle que le verre de la carte va flouter — l'effet y est plus lisible
    // que partout ailleurs dans le produit.
    <div className="app-field relative flex min-h-screen flex-col items-center justify-center overflow-hidden px-4">
      <Image
        src="/images/auth-court-lines.jpg"
        alt=""
        fill
        preload
        sizes="100vw"
        className="object-cover opacity-60 [filter:brightness(1.6)_contrast(1.15)]"
      />
      {/* Dégradé de lisibilité allégé (55 % → 40 % au centre) : la carte porte
          maintenant son propre verre, le fond n'a plus besoin d'être aussi
          couvrant. */}
      <div className="absolute inset-0 bg-gradient-to-b from-[rgba(10,15,28,0.3)] via-[rgba(10,15,28,0.4)] to-[rgba(10,15,28,0.75)]" />

      <Link
        href="/"
        className="relative z-10 mb-8 flex items-center gap-2.5 font-heading text-2xl font-bold"
      >
        <LogoSymbol className="h-9 w-9 shrink-0" />
        <span>
          Hoop<span className="text-primary">Picks</span>
        </span>
      </Link>
      <div className="relative z-10 flex w-full flex-col items-center">{children}</div>
    </div>
  );
}
