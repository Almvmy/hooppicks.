import { apiFetch } from "@/lib/api/http";
import { NewsItem } from "@/lib/types";

export async function fetchNews(): Promise<NewsItem[]> {
  return apiFetch<NewsItem[]>("/news");
}
