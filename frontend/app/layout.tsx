import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import { ThemeProvider } from "@/components/theme-provider";
import { QueryProvider } from "@/components/query-provider";
import { Toaster } from "@/components/ui/sonner";
import { InstallPromptCapture } from "@/components/install-app";
import { AppInstallReporter } from "@/components/app-nudges";
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

// Barlow Condensed : typo condensée pour les îles marines (recap card, stats
// importantes). Partagée avec les images d'aperçu OG (lib/share-fonts.ts).
const barlowCondensed = localFont({
  src: [
    { path: "./fonts/share/barlow-condensed-latin-700-normal.woff", weight: "700", style: "normal" },
    { path: "./fonts/share/barlow-condensed-latin-800-normal.woff", weight: "800", style: "normal" },
    { path: "./fonts/share/barlow-condensed-latin-900-normal.woff", weight: "900", style: "normal" },
    { path: "./fonts/share/barlow-condensed-latin-900-italic.woff", weight: "900", style: "italic" },
  ],
  variable: "--font-condensed",
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

const RELOAD_ON_STALE_BUILD = `(function () {
  var KEY = "hp-stale-build-reload";
  function reloadOnce() {
    try {
      if (Date.now() - Number(sessionStorage.getItem(KEY) || 0) < 30000) return;
      sessionStorage.setItem(KEY, String(Date.now()));
    } catch (e) {}
    location.reload();
  }
  window.addEventListener("error", function (e) {
    var t = e.target, src = t && (t.src || t.href);
    if (src && String(src).indexOf("/_next/static/") !== -1) reloadOnce();
  }, true);
  window.addEventListener("unhandledrejection", function (e) {
    var r = e.reason, m = r ? String(r.name) + " " + String(r.message) : "";
    if (/ChunkLoadError|Loading chunk|dynamically imported module|Importing a module script failed/.test(m)) reloadOnce();
  });
})();`;

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="fr"
      className={`${inter.variable} ${spaceGrotesk.variable} ${jetbrainsMono.variable} ${barlowCondensed.variable}`}
      suppressHydrationWarning
    >
      <head>
        {/* App installée restée ouverte pendant un déploiement : sa page réclame
            les fichiers de l'ancienne version, que Vercel ne sert plus, et
            l'écran restait blanc (React n'a même pas démarré, aucune page
            d'erreur possible). Script en ligne, actif avant tout le reste :
            un fichier de l'app introuvable = un rechargement, qui récupère la
            nouvelle version. Une fois par 30 s au plus, jamais en boucle. */}
        <script dangerouslySetInnerHTML={{ __html: RELOAD_ON_STALE_BUILD }} />
      </head>
      <body className="antialiased" suppressHydrationWarning>
        {/* Sombre par défaut (l'identité de l'app), clair ou système au choix
            dans Paramètres. disableTransitionOnChange : sans ça, chaque
            élément animerait sa couleur pendant la bascule. */}
        <ThemeProvider attribute="class" defaultTheme="dark" enableSystem disableTransitionOnChange>
          <QueryProvider>
            {children}
            <Toaster />
            <InstallPromptCapture />
            <AppInstallReporter />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
