"use client";

import * as Sentry from "@sentry/nextjs";
import { useEffect } from "react";

// Filet de secours React : une erreur qui échappe à tous les error.tsx
// locaux (racine du layout, hydratation...) atterrit ici. Styles en ligne
// volontairement : ce composant remplace <html>/<body> en entier, pas sûr
// que globals.css soit chargé si l'app a planté avant.
export default function GlobalError({
  error,
}: {
  error: Error & { digest?: string };
}) {
  useEffect(() => {
    Sentry.captureException(error);
  }, [error]);

  return (
    <html lang="fr">
      <body
        style={{
          margin: 0,
          minHeight: "100dvh",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: "12px",
          background: "#0B1120",
          color: "#F1F5F9",
          fontFamily: "system-ui, -apple-system, 'Segoe UI', sans-serif",
          textAlign: "center",
          padding: "24px",
        }}
      >
        <p style={{ fontSize: "18px", fontWeight: 600, margin: 0 }}>
          Un problème est survenu.
        </p>
        <p style={{ fontSize: "14px", color: "#97A3BD", margin: 0 }}>
          L&apos;équipe HoopPicks a été prévenue automatiquement.
        </p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          style={{
            marginTop: "8px",
            padding: "8px 18px",
            borderRadius: "10px",
            border: "none",
            background: "#FF7A1A",
            color: "#0B1120",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          Recharger la page
        </button>
      </body>
    </html>
  );
}
