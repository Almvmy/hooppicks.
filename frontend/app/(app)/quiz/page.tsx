"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Crown, X } from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { PlayerAvatar } from "@/components/player-avatar";
import { QuizArena } from "@/components/quiz-arena";
import { ShareButton } from "@/components/share-button";
import { answerQuiz, fetchQuiz, fetchQuizBoard, shotsCount, shotsLine, startQuiz, type QuizQuestion, type QuizState } from "@/lib/api/quiz";
import { cn } from "@/lib/utils";

const LIMIT_SECONDS = 15;
// Doit rester sous QuizService.FEEDBACK : le chrono suivant ne démarre qu'après.
const FEEDBACK_MS = 1500;

interface Feedback {
  question: QuizQuestion;
  choice: number | null;
  correct: number;
  hit: boolean;
}

export default function QuizPage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ["quiz", "state"], queryFn: fetchQuiz });
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => () => {
    if (timer.current) clearTimeout(timer.current);
  }, []);

  const update = (state: QuizState) => queryClient.setQueryData(["quiz", "state"], state);
  const onError = (e: Error) => {
    toast.error(e.message || "Action impossible. Réessaie.");
    queryClient.invalidateQueries({ queryKey: ["quiz", "state"] });
  };

  const start = useMutation({ mutationFn: startQuiz, onSuccess: update, onError });
  const answer = useMutation({
    mutationFn: ({ question, choice }: { question: QuizQuestion; choice: number | null }) => answerQuiz(question.index, choice),
    onSuccess: (result, { question, choice }) => {
      setFeedback({ question, choice, correct: result.correct, hit: result.hit });
      // La question reste à l'écran le temps de voir la réponse, puis on passe à la suite.
      timer.current = setTimeout(() => {
        setFeedback(null);
        update(result.state);
        if (result.state.status === "done") queryClient.invalidateQueries({ queryKey: ["quiz", "board"] });
      }, FEEDBACK_MS);
    },
    onError,
  });

  const state = query.data;
  const question = feedback?.question ?? state?.question ?? null;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      <h1 className="font-heading text-2xl font-bold">Le quiz du jour</h1>

      {query.isLoading && <Skeleton className="h-64 w-full rounded-2xl" />}
      {query.isError && <p className="text-sm text-destructive">Impossible de charger le quiz.</p>}

      {state?.status === "ready" && <QuizArena pending={start.isPending} onPlay={() => start.mutate()} />}

      {question && (
        <QuestionCard
          key={question.index}
          question={question}
          shots={state?.shots ?? ""}
          feedback={feedback}
          disabled={answer.isPending || !!feedback}
          onAnswer={(choice) => answer.mutate({ question, choice })}
        />
      )}

      {state?.status === "done" && !feedback && <QuizResult state={state} />}

      {state && state.status !== "playing" && !feedback && <QuizBoardSection />}
    </div>
  );
}

function QuestionCard({
  question,
  shots,
  feedback,
  disabled,
  onAnswer,
}: {
  question: QuizQuestion;
  shots: string;
  feedback: Feedback | null;
  disabled: boolean;
  onAnswer: (choice: number | null) => void;
}) {
  const [left, setLeft] = useState(question.secondsLeft);
  const answered = useRef(false);
  // Marqué dès le tap : sur une connexion lente, la correction met un moment à revenir.
  const [picked, setPicked] = useState<number | null>(null);
  const stopped = disabled || !!feedback;

  useEffect(() => {
    if (stopped) return;
    const deadline = Date.now() + question.secondsLeft * 1000;
    const id = setInterval(() => {
      const remaining = Math.max(0, (deadline - Date.now()) / 1000);
      setLeft(remaining);
      if (remaining === 0 && !answered.current) {
        answered.current = true;
        clearInterval(id);
        onAnswer(null);
      }
    }, 100);
    return () => clearInterval(id);
    // Le chrono ne dépend que de la question : `onAnswer` change à chaque affichage.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [question.index, stopped]);

  function pick(choice: number) {
    if (answered.current || disabled) return;
    answered.current = true;
    setPicked(choice);
    onAnswer(choice);
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-5">
        <div className="flex items-center justify-between gap-3 text-sm">
          <span className="font-mono font-bold">
            {question.index + 1}/{question.total}
          </span>
          <span aria-label="Tirs déjà joués">{shotsLine(shots)}</span>
          <span className={cn("font-mono font-bold tabular-nums", left <= 5 && !feedback && "text-destructive")}>{Math.ceil(left)} s</span>
        </div>
        <div className="h-1.5 overflow-hidden rounded-full bg-tint/10" aria-hidden>
          <div className={cn("h-full rounded-full", left <= 5 ? "bg-destructive" : "bg-primary")} style={{ width: `${(left / LIMIT_SECONDS) * 100}%` }} />
        </div>

        {question.imageUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={question.imageUrl} alt="" className="mx-auto h-36 w-44 rounded-xl object-cover object-top" />
        )}
        <p className="font-heading text-lg font-bold leading-snug">{question.text}</p>

        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {question.choices.map((choice, i) => {
            const isRight = feedback?.correct === i;
            const isWrongPick = feedback && feedback.choice === i && !feedback.hit;
            return (
              <button
                key={choice}
                type="button"
                disabled={disabled}
                onClick={() => pick(i)}
                className={cn(
                  "glass-inset-quiet flex min-h-12 items-center justify-between gap-2 rounded-xl px-3 py-2 text-left text-sm font-semibold transition-colors",
                  !feedback && "hover:text-primary",
                  !feedback && picked === i && "ring-1 ring-primary",
                  isRight && "bg-success/20 text-success ring-1 ring-success",
                  isWrongPick && "bg-destructive/15 text-destructive ring-1 ring-destructive"
                )}
              >
                <span className="min-w-0">{choice}</span>
                {isRight && <Check className="h-4 w-4 shrink-0" />}
                {isWrongPick && <X className="h-4 w-4 shrink-0" />}
              </button>
            );
          })}
        </div>
        {feedback && feedback.choice === null && <p className="text-center text-sm font-semibold text-destructive">Temps écoulé</p>}
      </CardContent>
    </Card>
  );
}

function QuizResult({ state }: { state: QuizState }) {
  const count = shotsCount(state.shots);
  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-3 pt-6 text-center">
        <p className="text-3xl">{shotsLine(state.shots)}</p>
        <p className="font-heading text-2xl font-bold">
          {count}/5 panier{count > 1 ? "s" : ""}
        </p>
        <ShareButton path="/quiz" text={`Quiz HoopPicks ${shotsLine(state.shots)} ${count}/5`} label="Partager mon score" />
        <ul className="mt-2 flex w-full flex-col gap-2 text-left">
          {state.review?.map((r) => (
            <li key={r.text} className="flex items-start gap-2 text-sm">
              {r.hit ? <Check className="mt-0.5 h-4 w-4 shrink-0 text-success" /> : <X className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />}
              <span className="min-w-0">
                <span className="text-muted-foreground">{r.text}</span> <span className="font-semibold">{r.answer}</span>
              </span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}

function QuizBoardSection() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["quiz", "board"], queryFn: fetchQuizBoard });
  const meShown = data?.entries.some((e) => e.me);
  const rows = data ? (data.me && !meShown ? [...data.entries, data.me] : data.entries) : [];

  return (
    <section className="flex flex-col gap-3">
      <div>
        <h2 className="font-heading text-lg font-bold">Classement de la semaine</h2>
        <p className="text-xs text-muted-foreground">Tes 5 meilleurs jours comptent. Remise à zéro le lundi à 8h GMT.</p>
      </div>
      {data?.lastChampion && (
        <p className="glass flex items-center gap-2 rounded-2xl px-4 py-3 text-sm">
          <Crown className="h-4 w-4 shrink-0 text-amber-400" />
          <span className="min-w-0">
            <Link href={`/u/${encodeURIComponent(data.lastChampion.username)}`} className="font-semibold hover:underline">
              {data.lastChampion.username}
            </Link>
            , champion du quiz la semaine dernière avec {data.lastChampion.baskets} paniers
          </span>
        </p>
      )}
      {isLoading && <Skeleton className="h-32 w-full rounded-2xl" />}
      {isError && <p className="text-sm text-destructive">Impossible de charger le classement.</p>}
      {data && rows.length === 0 && <p className="py-4 text-center text-sm text-muted-foreground">Personne n&apos;a encore joué cette semaine.</p>}
      <div className="flex flex-col gap-1.5">
        {rows.map((e) => (
          <Link
            key={e.username}
            href={`/u/${encodeURIComponent(e.username)}`}
            className={cn("glass flex items-center gap-2.5 rounded-xl px-3 py-2 text-sm", e.me && "ring-1 ring-primary")}
          >
            <span className="w-6 shrink-0 text-center font-mono text-xs font-bold text-muted-foreground">{e.rank}</span>
            <PlayerAvatar number={e.avatarNumber} position={e.avatarPosition} colorway={e.avatarColorway} icon={e.avatarIcon} size="xs" />
            <span className="min-w-0 flex-1 truncate font-medium">{e.username}</span>
            <span className="shrink-0 rounded-full bg-primary/10 px-1.5 text-[10px] font-semibold text-primary">{e.level}</span>
            <span className="shrink-0 text-right font-mono text-sm font-bold">{e.baskets} 🏀</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
