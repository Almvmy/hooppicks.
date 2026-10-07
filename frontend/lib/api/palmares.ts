import { apiFetch } from "@/lib/api/http";
import { Palmares } from "@/lib/types";

export async function fetchMyPalmares(): Promise<Palmares> {
  return apiFetch<Palmares>("/palmares/me");
}
