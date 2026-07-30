import { UserProfile } from "@/lib/types";

const MOCK_PROFILE: UserProfile = {
  username: "Almamy",
  email: "almamy@exemple.com",
  winRate: 58,
  totalBets: 24,
  favoriteTeam: "Los Angeles Lakers",
};

// TEMPORAIRE : sera remplacé par fetch("/api/users/me").
export async function fetchProfile(): Promise<UserProfile> {
  await new Promise((r) => setTimeout(r, 500));
  return MOCK_PROFILE;
}