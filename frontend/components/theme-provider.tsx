"use client";

import * as React from "react";
import { ThemeProvider as NextThemesProvider, useTheme } from "next-themes";

const THEME_COLORS = { dark: "#0B1120", light: "#ECE3D3" } as const;

// La barre du navigateur mobile (et la barre de titre de l'app installée)
// suit le thème réellement affiché, y compris un choix manuel qui contredit
// le réglage système : la media query du <meta> seule ne le saurait pas.
function ThemeColorSync() {
  const { resolvedTheme } = useTheme();
  React.useEffect(() => {
    if (resolvedTheme !== "dark" && resolvedTheme !== "light") return;
    document.querySelectorAll('meta[name="theme-color"]').forEach((meta) => {
      meta.setAttribute("content", THEME_COLORS[resolvedTheme]);
    });
  }, [resolvedTheme]);
  return null;
}

export function ThemeProvider({
  children,
  ...props
}: React.ComponentProps<typeof NextThemesProvider>) {
  return (
    <NextThemesProvider {...props}>
      <ThemeColorSync />
      {children}
    </NextThemesProvider>
  );
}
