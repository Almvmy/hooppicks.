"use client";

import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MessagesSquare } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { debateDayLabel, fetchDebatePlanning, planDebate, resetDebate, type PlannedDebate } from "@/lib/api/debate";

/** Les 7 prochains débats. Sans intervention, les questions de la liste tournent toutes seules. */
export function AdminDebatesCard() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["admin-debates"], queryFn: fetchDebatePlanning });

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <p className="flex items-center gap-2 text-sm font-medium">
          <MessagesSquare className="h-4 w-4 text-primary" />
          Débats à venir
        </p>
        {isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
        {isError && <p className="text-sm text-destructive">Impossible de charger les débats.</p>}
        <div className="flex flex-col divide-y divide-tint/10">
          {data?.map((d) => <DebateRow key={d.day} debate={d} />)}
        </div>
      </CardContent>
    </Card>
  );
}

function DebateRow({ debate }: { debate: PlannedDebate }) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [question, setQuestion] = useState(debate.question);
  const [optionA, setOptionA] = useState(debate.optionA);
  const [optionB, setOptionB] = useState(debate.optionB);

  const done = (fresh: PlannedDebate[]) => {
    queryClient.setQueryData(["admin-debates"], fresh);
    queryClient.invalidateQueries({ queryKey: ["debate"] });
    setEditing(false);
  };
  const fail = (e: Error) => toast.error(e.message || "Changement impossible.");
  const save = useMutation({ mutationFn: planDebate, onSuccess: done, onError: fail });
  const reset = useMutation({ mutationFn: () => resetDebate(debate.day), onSuccess: done, onError: fail });

  function submit(e: FormEvent) {
    e.preventDefault();
    save.mutate({ day: debate.day, question, optionA, optionB });
  }

  return (
    <div className="flex flex-col gap-2 py-2.5">
      <div className="flex items-start gap-2">
        <div className="min-w-0 flex-1">
          <p className="text-xs text-muted-foreground">
            {debateDayLabel(debate.day)}
            {debate.planned && " · prévu à la main"}
            {debate.votes > 0 && ` · ${debate.votes} vote${debate.votes > 1 ? "s" : ""}`}
          </p>
          <p className="text-sm font-medium">{debate.question}</p>
          <p className="text-xs text-muted-foreground">
            {debate.optionA} / {debate.optionB}
          </p>
        </div>
        {debate.votes === 0 && (
          <div className="flex shrink-0 gap-1.5">
            {debate.planned && (
              <Button size="sm" variant="outline" disabled={reset.isPending} onClick={() => reset.mutate()}>
                Retirer
              </Button>
            )}
            <Button size="sm" variant="outline" onClick={() => setEditing((v) => !v)}>
              Modifier
            </Button>
          </div>
        )}
      </div>
      {editing && (
        <form onSubmit={submit} className="flex flex-col gap-2">
          <Input value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={140} placeholder="Question" aria-label="Question" />
          <div className="flex gap-2">
            <Input value={optionA} onChange={(e) => setOptionA(e.target.value)} maxLength={40} placeholder="Camp 1" aria-label="Camp 1" />
            <Input value={optionB} onChange={(e) => setOptionB(e.target.value)} maxLength={40} placeholder="Camp 2" aria-label="Camp 2" />
          </div>
          <Button type="submit" size="sm" className="self-start" disabled={save.isPending}>
            Enregistrer
          </Button>
        </form>
      )}
    </div>
  );
}
