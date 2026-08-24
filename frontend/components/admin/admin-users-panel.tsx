"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Search, ShieldCheck, ShieldOff, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { deleteAdminUser, fetchAdminUsers, toggleAdminStatus } from "@/lib/api/admin";
import { AdminUser } from "@/lib/types";

type PendingAction = { type: "toggle" | "delete"; user: AdminUser };

export function AdminUsersPanel() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [submittedSearch, setSubmittedSearch] = useState("");
  const [pending, setPending] = useState<PendingAction | null>(null);

  const { data: users, isLoading, isError } = useQuery({
    queryKey: ["admin-users", submittedSearch],
    queryFn: () => fetchAdminUsers(submittedSearch || undefined),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin-users"] });

  const toggleMutation = useMutation({
    mutationFn: toggleAdminStatus,
    onSuccess: (updated) => {
      toast.success(
        updated.isAdmin ? `${updated.username} est maintenant admin.` : `${updated.username} n'est plus admin.`
      );
      invalidate();
    },
    onError: (err: Error) => toast.error(err.message || "Action impossible."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAdminUser,
    onSuccess: () => {
      toast.success("Compte supprimé.");
      invalidate();
    },
    onError: (err: Error) => toast.error(err.message || "Suppression impossible."),
  });

  function confirmPending() {
    if (!pending) return;
    if (pending.type === "toggle") toggleMutation.mutate(pending.user.id);
    else deleteMutation.mutate(pending.user.id);
    setPending(null);
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <p className="text-xs uppercase tracking-wide text-muted-foreground">Utilisateurs</p>

        <form
          className="flex gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            setSubmittedSearch(search.trim());
          }}
        >
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Chercher par pseudo ou email..."
            className="max-w-xs"
          />
          <Button type="submit" variant="outline" size="icon" aria-label="Rechercher">
            <Search className="h-4 w-4" />
          </Button>
        </form>

        {isLoading && <BasketballLoader label="Chargement des utilisateurs..." />}
        {isError && <p className="text-sm text-destructive">Impossible de charger les utilisateurs.</p>}

        {users && (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Pseudo</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead className="text-right">Solde</TableHead>
                  <TableHead>Statut</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground">
                      Aucun utilisateur trouvé.
                    </TableCell>
                  </TableRow>
                )}
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">@{user.username}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {user.email}
                      {!user.emailVerified && (
                        <span className="ml-1.5 text-xs text-destructive">(non vérifié)</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right font-mono">
                      {user.walletBalance.toLocaleString("fr-FR")} pts
                    </TableCell>
                    <TableCell>
                      {user.isAdmin && (
                        <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-primary">
                          Admin
                        </span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1.5">
                        <Button
                          variant="outline"
                          size="icon"
                          title={user.isAdmin ? "Retirer les droits admin" : "Rendre admin"}
                          disabled={toggleMutation.isPending}
                          onClick={() => setPending({ type: "toggle", user })}
                        >
                          {user.isAdmin ? <ShieldOff className="h-4 w-4" /> : <ShieldCheck className="h-4 w-4" />}
                        </Button>
                        <Button
                          variant="outline"
                          size="icon"
                          title="Supprimer le compte"
                          className="text-destructive hover:text-destructive"
                          disabled={deleteMutation.isPending}
                          onClick={() => setPending({ type: "delete", user })}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>

      <AlertDialog open={pending !== null} onOpenChange={(open) => !open && setPending(null)}>
        <AlertDialogContent>
          <AlertDialogTitle>
            {pending?.type === "delete"
              ? "Supprimer ce compte ?"
              : pending?.user.isAdmin
                ? "Retirer les droits admin ?"
                : "Rendre admin ?"}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {pending?.type === "delete"
              ? `Le compte @${pending?.user.username} et toutes ses données seront supprimés définitivement. Cette action est irréversible.`
              : pending?.user.isAdmin
                ? `@${pending?.user.username} n'aura plus accès à la console admin.`
                : `@${pending?.user.username} aura accès à la console admin.`}
          </AlertDialogDescription>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction variant={pending?.type === "delete" ? "destructive" : "default"} onClick={confirmPending}>
              Confirmer
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
