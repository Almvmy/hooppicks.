import type { Metadata, Viewport } from "next";
import { Inter, JetBrains_Mono, Space_Grotesk } from "next/font/google";
import { ThemeProvider } from "@/components/theme-provider";
import { QueryProvider } from "@/components/query-provider";
import { Toaster } from "@/components/ui/sonner";
import "./globals.css";

const inter = Inter({
  variable: "--font-sans",
  subsets: ["latin"],
});

const spaceGrotesk = Space_Grotesk({
  variable: "--font-heading",
  subsets: ["latin"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-mono-num",
  subsets: ["latin"],
});

export const metadata: Metadata = {
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
    title: "HoopPicks — Pronostics NBA",
    description:
      "Pronostique sur chaque match de la saison NBA en points virtuels, grimpe dans le classement.",
  },
};

export const viewport: Viewport = {
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
      className={`dark ${inter.variable} ${spaceGrotesk.variable} ${jetbrainsMono.variable}`}
      suppressHydrationWarning
    >
      <body className="antialiased" suppressHydrationWarning>
        <ThemeProvider attribute="class" defaultTheme="dark" forcedTheme="dark">
          <QueryProvider>
            {children}
            <Toaster />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}