// Service worker HoopPicks : uniquement les notifications push pour l'instant.
// Pas de gestionnaire "fetch" : aucune mise en cache, le réseau reste la
// seule source (un cache hors-ligne mal invalidé afficherait des cotes ou
// des scores périmés, pire qu'une page qui ne charge pas).

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = { body: event.data ? event.data.text() : "" };
  }

  event.waitUntil(
    self.registration.showNotification(data.title || "HoopPicks", {
      body: data.body || "",
      icon: "/pwa-icon/192",
      lang: "fr",
      data: { url: data.url || "/dashboard" },
    })
  );
});

// Clic sur la notification : ramène sur l'onglet HoopPicks déjà ouvert s'il
// y en a un (plutôt que d'en empiler un nouveau), sinon en ouvre un.
self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  // Lien interne uniquement : une URL vers un autre site retombe sur le tableau de bord.
  let target = new URL("/dashboard", self.location.origin);
  try {
    const candidate = new URL(event.notification.data?.url || "/dashboard", self.location.origin);
    if (candidate.origin === self.location.origin) target = candidate;
  } catch {
    // URL illisible : tableau de bord.
  }

  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
      const existing = windows.find((client) => new URL(client.url).origin === self.location.origin);
      if (existing) {
        await existing.focus();
        return existing.navigate(target.href);
      }
      return self.clients.openWindow(target.href);
    })()
  );
});
