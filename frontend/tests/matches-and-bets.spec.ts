import { test, expect } from "@playwright/test";

test.describe("Matchs et paris", () => {
  test.beforeEach(async ({ page, context }) => {
    await context.clearCookies();
    await page.goto("/login");
    await page.getByLabel("Adresse e-mail").fill("test@exemple.com");
    await page.getByLabel("Mot de passe").fill("motdepasse");
    await page.getByRole("button", { name: "Se connecter" }).click();
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test("affiche la liste des matchs et permet de filtrer par conférence", async ({ page }) => {
    await page.goto("/matches");
    await expect(page.getByText("Lakers").first()).toBeVisible();

    await page.getByText("Est", { exact: true }).click();
    await expect(page.getByText("Ouest", { exact: true })).toBeVisible();
  });

  test("ajoute une sélection au ticket et affiche le panneau flottant", async ({ page }) => {
    await page.goto("/matches");

    const oddsButtons = page.locator("button", { hasText: /\d\.\d{2}/ });
    await oddsButtons.first().click();

    await expect(page.getByText(/Ticket \(1\)/)).toBeVisible();
  });

  test("bloque la validation si la mise dépasse le solde", async ({ page }) => {
    await page.goto("/matches");

    const oddsButtons = page.locator("button", { hasText: /\d\.\d{2}/ });
    await oddsButtons.first().click();

    await page.getByPlaceholder("Mise en points").fill("999999");
    await expect(page.getByText(/Solde insuffisant/)).toBeVisible();
    await expect(page.getByRole("button", { name: "Valider le ticket" })).toBeDisabled();
  });
});