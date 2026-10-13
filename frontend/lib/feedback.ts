import { Bug, CircleHelp, Lightbulb, MessageSquare, type LucideIcon } from "lucide-react";
import { FeedbackCategory } from "@/lib/types";

/** Types de message de « Nous écrire », partagés avec la console admin. */
export const FEEDBACK_CATEGORIES: { value: FeedbackCategory; label: string; icon: LucideIcon; placeholder: string }[] = [
  { value: "BUG", label: "Un bug", icon: Bug, placeholder: "Ce que tu faisais, ce qui s'est passé, ce que tu attendais…" },
  { value: "QUESTION", label: "Une question", icon: CircleHelp, placeholder: "Ta question" },
  { value: "IDEA", label: "Une idée", icon: Lightbulb, placeholder: "Ton idée" },
  { value: "OTHER", label: "Autre chose", icon: MessageSquare, placeholder: "Ton message" },
];
