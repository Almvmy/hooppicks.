import Image from "next/image";
import Link from "next/link";
import { LogoSymbol } from "@/app/LogoSymbol";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-background px-4">
      <Image
        src="/images/auth-court-lines.jpg"
        alt=""
        fill
        preload
        sizes="100vw"
        className="object-cover opacity-60 [filter:brightness(1.6)_contrast(1.15)]"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-background/40 via-background/55 to-background" />

      <Link
        href="/"
        className="relative z-10 mb-8 flex items-center gap-2.5 font-heading text-2xl font-bold"
      >
        <LogoSymbol className="h-9 w-9 shrink-0" />
        Hoop<span className="text-primary">Picks</span>
      </Link>
      <div className="relative z-10 flex w-full flex-col items-center">{children}</div>
    </div>
  );
}