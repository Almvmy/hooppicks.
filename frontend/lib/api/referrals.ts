import { apiFetch } from "@/lib/api/http";

export interface MyReferrals {
  recruits: number;
  goal: number;
}

export async function fetchMyReferrals(): Promise<MyReferrals> {
  return apiFetch<MyReferrals>("/referrals/me");
}

/** Juste après l'inscription : rattache le compte au joueur qui l'a invité. */
export async function claimReferral(ref: string): Promise<void> {
  return apiFetch<void>("/referrals/claim", { method: "POST", body: JSON.stringify({ ref }) });
}
