import { apiFetch } from "@/lib/api/http";
import { NotificationPreferences, UpdateProfileInput, UserProfile } from "@/lib/types";

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

export async function requestPasswordReset(email: string): Promise<void> {
  return apiFetch<void>("/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  return apiFetch<void>("/auth/reset-password", {
    method: "POST",
    body: JSON.stringify({ token, newPassword }),
  });
}

export async function verifyEmail(token: string): Promise<void> {
  return apiFetch<void>("/auth/verify-email", {
    method: "POST",
    body: JSON.stringify({ token }),
  });
}

export async function resendVerificationEmail(): Promise<void> {
  return apiFetch<void>("/auth/resend-verification", { method: "POST" });
}

export async function fetchProfile(): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/me");
}

export async function updateProfile(input: UpdateProfileInput): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/profile", {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

export async function updateNotificationPreferences(
  prefs: NotificationPreferences
): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/notification-preferences", {
    method: "PATCH",
    body: JSON.stringify(prefs),
  });
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiFetch<void>("/auth/change-password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export async function changeEmail(newEmail: string, currentPassword: string): Promise<UserProfile> {
  return apiFetch<UserProfile>("/auth/change-email", {
    method: "POST",
    body: JSON.stringify({ newEmail, currentPassword }),
  });
}

export async function deleteAccount(currentPassword: string): Promise<void> {
  return apiFetch<void>("/auth/delete-account", {
    method: "POST",
    body: JSON.stringify({ currentPassword }),
  });
}