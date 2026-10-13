import { apiFetch } from "@/lib/api/http";
import { Feedback, FeedbackCategory } from "@/lib/types";

export async function sendFeedback(body: {
  category: FeedbackCategory;
  message: string;
  page?: string;
  appInstalled?: boolean;
}): Promise<Feedback> {
  return apiFetch<Feedback>("/feedback", { method: "POST", body: JSON.stringify(body) });
}

export async function fetchMyFeedback(): Promise<Feedback[]> {
  return apiFetch<Feedback[]>("/feedback/mine");
}
