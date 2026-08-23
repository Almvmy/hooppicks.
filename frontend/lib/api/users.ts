import { apiFetch } from "@/lib/api/http";
import { PublicProfile } from "@/lib/types";

export async function fetchPublicProfile(username: string): Promise<PublicProfile> {
  return apiFetch<PublicProfile>(`/users/${encodeURIComponent(username)}`);
}
