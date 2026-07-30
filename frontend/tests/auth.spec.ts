import { test, expect } from "@playwright/test";

test.describe("Authentification", () => {
  test("redirige vers /login si on accède à une page protégée sans être connecté", async ({ page, context }) => {
    await context.clearCookies();
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login/);
  });

  test("permet de se connecter et d'accéder au dashboard", async ({ page, context }) => {
    await context.clearCookies();
    await page.goto("/login");
    await page.getByLabel("Adresse e-mail").fill("test@exemple.com");
    await page.getByLabel("Mot de passe").fill("motdepasse");
    await page.getByRole("button", { name: "Se connecter" }).click();

    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByRole("heading", { name: "Tableau de bord" })).toBeVisible();
  });

  test("permet de se déconnecter", async ({ page, context }) => {
    await context.clearCookies();
    await page.goto("/login");
    await page.getByLabel("Adresse e-mail").fill("test@exemple.com");
    await page.getByLabel("Mot de passe").fill("motdepasse");
    await page.getByRole("button", { name: "Se connecter" }).click();
    await expect(page).toHaveURL(/\/dashboard/);

    await page.getByTestId("user-menu-trigger").click();
    await page.getByText("Se déconnecter").click();

    await expect(page).toHaveURL(/\/login/);
  });
});