const API_URL = process.env.NEXT_PUBLIC_API_URL;

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });

  if (!res.ok) {
    throw new Error(`Erreur API (${res.status}) sur ${path}`);
  }

  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}