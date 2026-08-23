"use client";

import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { verifyEmail } from "@/lib/api/auth";

type Status = "loading" | "success" | "error";

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [status, setStatus] = useState<Status>(token ? "loading" : "error");

  useEffect(() => {
    if (!token) return;
    verifyEmail(token)
      .then(() => setStatus("success"))
      .catch(() => setStatus("error"));
  }, [token]);

  if (status === "loading") {
    return <p className="text-sm text-muted-foreground">Vérification en cours...</p>;
  }

  if (status === "success") {
    return (
      <div className="flex flex-col gap-4">
        <p className="text-sm">Ton adresse e-mail est confirmée. Merci !</p>
        <Link href="/dashboard" className={buttonVariants({ className: "w-full" })}>
          Retour au tableau de bord
        </Link>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-destructive">
        Lien invalide ou expiré. Tu peux en redemander un depuis tes paramètres, une fois connecté.
      </p>
      <Link
        href="/dashboard"
        className={buttonVariants({ variant: "outline", className: "w-full" })}
      >
        Retour au tableau de bord
      </Link>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Card className="w-full max-w-sm border-border bg-card">
      <CardHeader>
        <CardTitle className="font-heading text-xl">Confirmation d&apos;e-mail</CardTitle>
      </CardHeader>
      <CardContent>
        <Suspense fallback={null}>
          <VerifyEmailContent />
        </Suspense>
      </CardContent>
    </Card>
  );
}
