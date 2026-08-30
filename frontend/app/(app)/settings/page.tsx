"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Mail, ShieldAlert, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  changeEmail,
  changePassword,
  changeUsername,
  deleteAccount,
  fetchProfile,
  logoutUser,
  updateNotificationPreferences,
} from "@/lib/api/auth";
import { NotificationPreferences } from "@/lib/types";

const NOTIFICATION_ROWS: { key: keyof NotificationPreferences; label: string; description: string }[] = [
  {
    key: "notifyMatchStarting",
    label: "Coup d'envoi des matchs",
    description: "Un match sur lequel tu as un pari en attente démarre.",
  },
  {
    key: "notifyBetResults",
    label: "Résultats de paris",
    description: "Un de tes tickets est gagné, perdu, ou remboursé.",
  },
  {
    key: "notifyLeagueActivity",
    label: "Activité de ligue",
    description: "Quelqu'un rejoint une ligue dont tu es membre.",
  },
];

function NotificationsCard() {
  const queryClient = useQueryClient();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  const mutation = useMutation({
    mutationFn: updateNotificationPreferences,
    onSuccess: (updated) => {
      queryClient.setQueryData(["profile"], updated);
      toast.success("Préférences enregistrées.");
    },
    onError: () => toast.error("Impossible d'enregistrer. Réessaie."),
  });

  function toggle(key: keyof NotificationPreferences, value: boolean) {
    if (!profile) return;
    mutation.mutate({
      notifyMatchStarting: profile.notifyMatchStarting,
      notifyBetResults: profile.notifyBetResults,
      notifyLeagueActivity: profile.notifyLeagueActivity,
      [key]: value,
    });
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <p className="text-xs uppercase tracking-wide text-muted-foreground">Notifications</p>
        <div className="flex flex-col gap-4">
          {NOTIFICATION_ROWS.map((row) => (
            <div key={row.key} className="flex items-center justify-between gap-4">
              <div>
                <p className="text-sm font-medium">{row.label}</p>
                <p className="text-xs text-muted-foreground">{row.description}</p>
              </div>
              <Switch
                checked={profile?.[row.key] ?? true}
                onCheckedChange={(checked) => toggle(row.key, checked)}
                disabled={!profile || mutation.isPending}
              />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function ChangeUsernameForm() {
  const queryClient = useQueryClient();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const [newUsername, setNewUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => changeUsername(newUsername, password),
    onSuccess: (updated) => {
      queryClient.setQueryData(["profile"], updated);
      setNewUsername("");
      setPassword("");
      setError(null);
      toast.success("Pseudo mis à jour.");
    },
    onError: (err: Error) => setError(err.message || "Impossible de changer le pseudo."),
  });

  return (
    <div className="flex flex-col gap-3">
      <div>
        <p className="text-sm font-medium">Pseudo</p>
        <p className="text-xs text-muted-foreground">@{profile?.username}</p>
      </div>
      <form
        className="flex flex-col gap-2 sm:flex-row sm:items-end"
        onSubmit={(e) => {
          e.preventDefault();
          setError(null);
          mutation.mutate();
        }}
      >
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="new-username">Nouveau pseudo</Label>
          <Input
            id="new-username"
            type="text"
            minLength={3}
            maxLength={20}
            value={newUsername}
            onChange={(e) => setNewUsername(e.target.value)}
            placeholder="TonPseudo"
            required
          />
        </div>
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="username-password">Mot de passe actuel</Label>
          <Input
            id="username-password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </div>
        <Button type="submit" variant="outline" disabled={mutation.isPending}>
          {mutation.isPending ? "..." : "Changer"}
        </Button>
      </form>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

function ChangeEmailForm() {
  const queryClient = useQueryClient();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const [newEmail, setNewEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => changeEmail(newEmail, password),
    onSuccess: (updated) => {
      queryClient.setQueryData(["profile"], updated);
      setNewEmail("");
      setPassword("");
      setError(null);
      toast.success("Email mis à jour.");
    },
    onError: (err: Error) => setError(err.message || "Impossible de changer l'email."),
  });

  return (
    <div className="flex flex-col gap-3">
      <div>
        <p className="text-sm font-medium">Email</p>
        <p className="text-xs text-muted-foreground">{profile?.email}</p>
      </div>
      <form
        className="flex flex-col gap-2 sm:flex-row sm:items-end"
        onSubmit={(e) => {
          e.preventDefault();
          setError(null);
          mutation.mutate();
        }}
      >
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="new-email">Nouvel email</Label>
          <Input
            id="new-email"
            type="email"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            placeholder="toi@exemple.com"
            required
          />
        </div>
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="email-password">Mot de passe actuel</Label>
          <Input
            id="email-password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </div>
        <Button type="submit" variant="outline" disabled={mutation.isPending}>
          {mutation.isPending ? "..." : "Changer"}
        </Button>
      </form>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

function ChangePasswordForm() {
  const router = useRouter();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => changePassword(currentPassword, newPassword),
    onSuccess: async () => {
      toast.success("Mot de passe changé. Reconnecte-toi.");
      await logoutUser();
      router.push("/login");
    },
    onError: (err: Error) => setError(err.message || "Impossible de changer le mot de passe."),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (newPassword !== confirmPassword) {
      setError("Les mots de passe ne correspondent pas.");
      return;
    }
    if (newPassword.length < 6) {
      setError("Le mot de passe doit faire au moins 6 caractères.");
      return;
    }
    mutation.mutate();
  }

  return (
    <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
      <p className="text-sm font-medium">Mot de passe</p>
      <div className="grid gap-3 sm:grid-cols-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="current-password">Actuel</Label>
          <Input
            id="current-password"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="new-password">Nouveau</Label>
          <Input
            id="new-password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="confirm-new-password">Confirmer</Label>
          <Input
            id="confirm-new-password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </div>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      <Button type="submit" variant="outline" className="w-fit" disabled={mutation.isPending}>
        {mutation.isPending ? "..." : "Changer le mot de passe"}
      </Button>
    </form>
  );
}

function DeleteAccountZone() {
  const router = useRouter();
  const [armed, setArmed] = useState(false);
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => deleteAccount(password),
    onSuccess: () => {
      toast.success("Ton compte a été supprimé.");
      router.push("/");
    },
    onError: (err: Error) => setError(err.message || "Impossible de supprimer le compte."),
  });

  if (!armed) {
    return (
      <Button
        variant="destructive"
        className="w-fit gap-1.5"
        onClick={() => setArmed(true)}
      >
        <Trash2 className="h-4 w-4" />
        Supprimer mon compte
      </Button>
    );
  }

  return (
    <form
      className="flex flex-col gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        setError(null);
        mutation.mutate();
      }}
    >
      <p className="text-sm text-muted-foreground">
        Cette action est irréversible : tickets, historique et ligues possédées seront
        définitivement supprimés. Confirme avec ton mot de passe.
      </p>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Mot de passe"
          required
          className="sm:max-w-xs"
        />
        <Button type="submit" variant="destructive" disabled={mutation.isPending}>
          {mutation.isPending ? "Suppression..." : "Confirmer la suppression"}
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={() => {
            setArmed(false);
            setPassword("");
            setError(null);
          }}
          disabled={mutation.isPending}
        >
          Annuler
        </Button>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </form>
  );
}

export default function SettingsPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Paramètres</h1>
        <p className="mt-1 text-muted-foreground">Compte et notifications.</p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-6 pt-6">
          <p className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-muted-foreground">
            <Mail className="h-3.5 w-3.5" />
            Compte
          </p>
          <ChangeUsernameForm />
          <ChangeEmailForm />
          <ChangePasswordForm />
        </CardContent>
      </Card>

      <NotificationsCard />

      <Card className="shadow-[inset_0_0_0_1px_rgba(239,68,68,0.3)]">
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-destructive">
            <ShieldAlert className="h-3.5 w-3.5" />
            Zone dangereuse
          </p>
          <DeleteAccountZone />
        </CardContent>
      </Card>
    </div>
  );
}
