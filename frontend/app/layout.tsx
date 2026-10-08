import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import { ThemeProvider } from "@/components/theme-provider";
import { QueryProvider } from "@/components/query-provider";
import { Toaster } from "@/components/ui/sonner";
import { InstallPromptCapture } from "@/components/install-app";
import "./globals.css";

// Polices dans le dépôt plutôt que next/font/google : celui-ci les retélécharge
// chez Google au démarrage du serveur et casse toute la page (« Can't resolve
// .../font/google/font ») à la moindre coupure réseau. Variables, sous-ensemble latin.
const inter = localFont({
  src: "./fonts/inter-latin.woff2",
  variable: "--font-sans",
  weight: "100 900",
});

const spaceGrotesk = localFont({
  src: "./fonts/space-grotesk-latin.woff2",
  variable: "--font-heading",
  weight: "300 700",
});

const jetbrainsMono = localFont({
  src: "./fonts/jetbrains-mono-latin.woff2",
  variable: "--font-mono-num",
  weight: "100 800",
});

export const metadata: Metadata = {
  // Adresse absolue des images d'aperçu (liens partagés sur WhatsApp & co.).
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "https://hooppicks.vercel.app"),
  title: {
    default: "HoopPicks - Pronostics NBA",
    template: "%s · HoopPicks",
  },
  description:
    "Pronostique sur chaque match de la saison NBA en points virtuels, grimpe dans le classement, aucun argent réel en jeu.",
  openGraph: {
    title: "HoopPicks - Pronostics NBA",
    description:
      "Pronostique sur chaque match de la saison NBA en points virtuels, grimpe dans le classement.",
    siteName: "HoopPicks",
    locale: "fr_FR",
    type: "website",
  },
  twitter: {
    card: "summary",
    title: "HoopPicks - Pronostics NBA",
    description:
      "Pronostique sur chaque match de la saison NBA en points virtuels, grimpe dans le classement.",
  },
};

export const viewport: Viewport = {
  // Valeur initiale avant que le JS ne prenne la main (ThemeColorSync).
  themeColor: "#0B1120",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="fr"
      className={`${inter.variable} ${spaceGrotesk.variable} ${jetbrainsMono.variable}`}
      suppressHydrationWarning
    >
      <body className="antialiased" suppressHydrationWarning>
        {/* Sombre par défaut (l'identité de l'app), clair ou système au choix
            dans Paramètres. disableTransitionOnChange : sans ça, chaque
            élément animerait sa couleur pendant la bascule. */}
        <ThemeProvider attribute="class" defaultTheme="dark" enableSystem disableTransitionOnChange>
          <QueryProvider>
            {children}
            <Toaster />
            <InstallPromptCapture />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}