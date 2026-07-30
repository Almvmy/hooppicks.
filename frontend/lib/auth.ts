"use client";

export function fakeLogin() {
  // TEMPORAIRE : simule une connexion en posant un cookie.
  // Sera remplacé par le vrai appel API du backend (NestJS + JWT) en semaine 2 côté backend.
  document.cookie = "hp_session=fake-token; path=/; max-age=86400";
}

export function fakeLogout() {
  document.cookie = "hp_session=; path=/; max-age=0";
}