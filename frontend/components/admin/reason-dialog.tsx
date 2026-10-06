"use client";

import { useState } from "react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Input } from "@/components/ui/input";

/**
 * Confirmation avec motif obligatoire, pour les actions qui touchent aux
 * points d'un joueur : le motif lui est envoyé et part dans le journal.
 * children : champs en plus du motif (montant d'un ajustement…).
 */
export function ReasonDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  destructive = false,
  canConfirm = true,
  onConfirm,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: React.ReactNode;
  confirmLabel: string;
  destructive?: boolean;
  canConfirm?: boolean;
  onConfirm: (reason: string) => void;
  children?: React.ReactNode;
}) {
  const [reason, setReason] = useState("");
  const valid = reason.trim().length >= 3 && canConfirm;

  function close(next: boolean) {
    if (!next) setReason("");
    onOpenChange(next);
  }

  return (
    <AlertDialog open={open} onOpenChange={close}>
      <AlertDialogContent>
        <AlertDialogTitle>{title}</AlertDialogTitle>
        <AlertDialogDescription>{description}</AlertDialogDescription>
        {children}
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium">Motif (envoyé au joueur et gardé au journal)</span>
          <Input
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            maxLength={200}
            placeholder="Ex. : match reporté, bug de synchro…"
            autoFocus
          />
        </label>
        <AlertDialogFooter>
          <AlertDialogCancel>Annuler</AlertDialogCancel>
          <AlertDialogAction
            variant={destructive ? "destructive" : "default"}
            disabled={!valid}
            onClick={() => {
              onConfirm(reason.trim());
              setReason("");
            }}
          >
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
