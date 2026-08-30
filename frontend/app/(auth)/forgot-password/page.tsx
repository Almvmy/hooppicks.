"use client";

import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { requestPasswordReset } from "@/lib/api/auth";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await requestPasswordReset(email);
    } finally {
      // Toujours le même message, que le compte existe ou non : pas d'énumération.
      setIsSubmitting(false);
      setSubmitted(true);
    }
  }

  return (
    <Card className="w-full max-w-sm border-border bg-card">
      <CardHeader>
        <CardTitle className="font-heading text-xl">Mot de passe oublié</CardTitle>
      </CardHeader>
      <CardContent>
        {submitted ? (
          <p className="text-sm text-muted-foreground">
            Si un compte existe avec cette adresse, un e-mail avec un lien de
            réinitialisation vient d&apos;être envoyé. Vérifie ta boîte de réception (et
            les spams).
          </p>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
            <div className="flex flex-col gap-2">
              <Label htmlFor="email">Adresse e-mail</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="toi@exemple.com"
                required
              />
            </div>
            <Button type="submit" disabled={isSubmitting} className="mt-2 w-full">
              {isSubmitting ? "Envoi..." : "Envoyer le lien"}
            </Button>
          </form>
        )}
        <p className="mt-4 text-center text-sm text-muted-foreground">
          <Link href="/login" className="text-primary hover:underline">
            Retour à la connexion
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
