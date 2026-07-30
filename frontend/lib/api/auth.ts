import { apiFetch } from "@/lib/api/http";
import { UserProfile } from "@/lib/types";

export async function registerUser(username: string, email: string, password: string): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password }),
  });
}

export async function loginUser(email: string, password: string): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export async function logoutUser(): Promise<void> {
  return apiFetch<void>("/auth/logout", { method: "POST" });
}

export async function fetchProfile(): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/me");
}