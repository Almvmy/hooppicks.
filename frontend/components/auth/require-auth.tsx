"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { fetchProfile } from "@/lib/api/auth";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [status, setStatus] = useState<"checking" | "authenticated">("checking");

  useEffect(() => {
    let cancelled = false;

    fetchProfile()
      .then(() => {
        if (!cancelled) setStatus("authenticated");
      })
      .catch(() => {
        if (!cancelled) {
          router.replace(`/login?from=${encodeURIComponent(pathname)}`);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  if (status === "checking") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-sm text-muted-foreground">Chargement...</p>
      </div>
    );
  }

  return <>{children}</>;
}