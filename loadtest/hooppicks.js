// Test de charge k6 de HoopPicks, contre l'app en ligne (relais /api de Vercel → Railway).
//
// Lancement (à une heure creuse, sans match NBA en cours) :
//   k6 run loadtest/hooppicks.js
//   k6 run -e MAX_VUS=600 loadtest/hooppicks.js           // monter plus haut
//   k6 run -e LOGIN_EMAIL=... -e LOGIN_PASSWORD=... loadtest/hooppicks.js
//
// Avec un compte de test (LOGIN_*), les joueurs simulés lisent aussi les pages
// connectées (profil, solde, mes paris, cloche). Aucun pari n'est posé : le
// test ne touche ni au classement ni aux soldes, et aucune inscription n'est
// faite (chaque inscription enverrait un e-mail de confirmation par Brevo).
//
// Garde-fou : arrêt automatique si plus de 10 % des requêtes échouent.

import http from "k6/http";
import { check, group, sleep } from "k6";

const BASE = __ENV.BASE_URL || "https://hooppicks.vercel.app";
const API = `${BASE}/api`;
const MAX = Number(__ENV.MAX_VUS || 400);

// Paliers : chacun tenu 2 minutes pour laisser le serveur se stabiliser.
const steps = [50, 100, 200, MAX].filter((v, i, a) => v <= MAX && a.indexOf(v) === i);
const stages = steps.flatMap((target) => [
  { duration: "1m", target },
  { duration: "2m", target },
]);
stages.push({ duration: "1m", target: 0 });

export const options = {
  scenarios: {
    joueurs: { executor: "ramping-vus", startVUs: 0, stages, gracefulRampDown: "30s" },
  },
  thresholds: {
    http_req_failed: [{ threshold: "rate<0.10", abortOnFail: true, delayAbortEval: "30s" }],
    http_req_duration: ["p(95)<2000"],
    "http_req_duration{page:matchs}": ["p(95)<2000"],
    "http_req_duration{page:direct}": ["p(95)<1500"],
  },
  summaryTrendStats: ["avg", "med", "p(90)", "p(95)", "max"],
};

export function setup() {
  let cookie = null;
  if (__ENV.LOGIN_EMAIL && __ENV.LOGIN_PASSWORD) {
    const res = http.post(
      `${API}/auth/login`,
      JSON.stringify({ email: __ENV.LOGIN_EMAIL, password: __ENV.LOGIN_PASSWORD }),
      { headers: { "Content-Type": "application/json" } }
    );
    const set = res.cookies.hp_session;
    if (res.status !== 200 || !set || !set.length) throw new Error(`Connexion du compte de test refusée (${res.status})`);
    cookie = `hp_session=${set[0].value}`;
  }

  // Quelques vrais matchs, pour ouvrir des pages de match comme un joueur.
  const matches = http.get(`${API}/matches`).json() || [];
  const ids = matches
    .filter((m) => m.status === "live" || m.status === "scheduled" || m.status === "finished")
    .sort((a, b) => Math.abs(Date.parse(a.date) - Date.now()) - Math.abs(Date.parse(b.date) - Date.now()))
    .slice(0, 10)
    .map((m) => m.id);
  return { cookie, ids };
}

function get(path, page, cookie) {
  const params = { tags: { page } };
  if (cookie) params.headers = { Cookie: cookie };
  const res = http.get(`${API}${path}`, params);
  check(res, { [`${page} ok`]: (r) => r.status === 200 || r.status === 204 });
  return res;
}

// Un tour = ce que fait un joueur en ouvrant l'app : accueil, matchs, un match, classement, actus.
export default function (data) {
  const { cookie, ids } = data;
  const matchId = ids.length ? ids[Math.floor(Math.random() * ids.length)] : null;

  group("accueil", () => {
    http.get(BASE + "/", { tags: { page: "html" } });
    get("/matches/live", "direct", cookie);
    get("/boost", "boost", cookie);
    if (cookie) {
      get("/auth/me", "profil", cookie);
      get("/wallet", "solde", cookie);
      get("/leaderboard/me", "rang", cookie);
      get("/bets", "mes-paris", cookie);
      get("/notifications/unread-count", "cloche", cookie);
    }
  });
  sleep(2 + Math.random() * 3);

  group("matchs", () => {
    get("/matches", "matchs", cookie);
  });
  sleep(2 + Math.random() * 3);

  if (matchId) {
    group("page d'un match", () => {
      get(`/matches/${matchId}`, "match", cookie);
      get(`/matches/${matchId}/live`, "direct", cookie);
      get(`/matches/${matchId}/props`, "paris-joueurs", cookie);
    });
    sleep(3 + Math.random() * 4);
  }

  group("classement et actus", () => {
    get("/leaderboard?period=week", "classement", cookie);
    if (Math.random() < 0.5) get("/news", "actus", cookie);
  });
  sleep(3 + Math.random() * 5);
}
