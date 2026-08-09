import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { CONTEXT_PATH } from '../playwright.config'

/** Basis-URL der Anwendung inklusive Context-Path, mit Schrägstrich am Ende (ADR 0016). */
const APP = `${CONTEXT_PATH}/`

/**
 * User flows through the browser (see ADR 0008). These tests assert on what a user sees –
 * status codes, JSON fields and contract details stay in the Java system tests.
 *
 * <p>The database is shared, so every test works on its own uniquely named recipe and
 * removes it again.
 */

/** Unique per run so parallel or repeated runs never collide on titles. */
const run = `e2e-${Date.now()}`

function uniqueTitle(name: string): string {
  return `${name} ${run}`
}

async function createRecipe(request: APIRequestContext, title: string): Promise<number> {
  const response = await request.post(`${APP}api/recipes`, {
    data: {
      title,
      description: 'Vom E2E-Test angelegt',
      servings: 2,
      prepTimeMinutes: 10,
      difficulty: 'EASY',
      ingredients: [{ name: 'Mehl', quantity: 300, unit: 'g' }],
      steps: [{ instruction: 'Teig rühren' }],
    },
  })
  expect(response.status()).toBe(201)
  return (await response.json()).id as number
}

async function deleteRecipe(request: APIRequestContext, id: number): Promise<void> {
  await request.delete(`${APP}api/recipes/${id}`)
}

/** Card of a recipe in the list, addressed via its title. */
function card(page: Page, title: string) {
  return page.locator('li.recipe-card').filter({ hasText: title })
}

/**
 * The title link of a card. `exact` is required: accessible names match as substrings, and the
 * delete button carries the title in its aria-label ("<title> löschen").
 */
function titleLink(page: Page, title: string) {
  return page.getByRole('button', { name: title, exact: true })
}

test.describe('Rezeptverwaltung', () => {
  test('legt ein Rezept an, bearbeitet und löscht es wieder', async ({ page }) => {
    const title = uniqueTitle('Pfannkuchen')
    // Deliberately not a superstring of `title`: card lookup matches on contained text.
    const renamed = uniqueTitle('Crepes')

    await page.goto(APP)

    // Anlegen
    await page.getByRole('button', { name: 'Neues Rezept' }).click()
    await page.getByLabel('Titel').fill(title)
    await page.getByLabel('Beschreibung').fill('Fluffig und schnell')
    await page.getByLabel('Portionen').fill('2')
    await page.getByLabel('Schwierigkeit').selectOption('EASY')

    await page.getByRole('button', { name: 'Zutat hinzufügen' }).click()
    await page.getByLabel('Zutat 1 Name').fill('Mehl')
    await page.getByLabel('Zutat 1 Menge').fill('300')
    await page.getByLabel('Zutat 1 Einheit').fill('g')

    await page.getByRole('button', { name: 'Schritt hinzufügen' }).click()
    // `exact`, sonst trifft der Query auch den Button "Schritt 1 entfernen".
    await page.getByLabel('Schritt 1', { exact: true }).fill('Teig anrühren')

    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(card(page, title)).toBeVisible()
    await expect(card(page, title)).toContainText('2 Portionen')

    // Bearbeiten
    await card(page, title).getByRole('button', { name: 'Bearbeiten' }).click()
    await expect(page.getByRole('heading', { name: 'Rezept bearbeiten' })).toBeVisible()
    await expect(page.getByLabel('Zutat 1 Name')).toHaveValue('Mehl')

    await page.getByLabel('Titel').fill(renamed)
    await page.getByLabel('Schwierigkeit').selectOption('HARD')
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(card(page, renamed)).toBeVisible()
    await expect(card(page, renamed)).toContainText('HARD')
    await expect(card(page, title)).toHaveCount(0)

    // Löschen – die Bestätigung ist ein window.confirm
    page.once('dialog', (dialog) => dialog.accept())
    await card(page, renamed).getByRole('button', { name: `${renamed} löschen` }).click()

    await expect(card(page, renamed)).toHaveCount(0)
  })

  test('bricht das Löschen ab, wenn die Bestätigung verneint wird', async ({ page, request }) => {
    const title = uniqueTitle('Bleibt bestehen')
    const id = await createRecipe(request, title)

    try {
      await page.goto(APP)
      page.once('dialog', (dialog) => dialog.dismiss())
      await card(page, title).getByRole('button', { name: `${title} löschen` }).click()

      await expect(card(page, title)).toBeVisible()
    } finally {
      await deleteRecipe(request, id)
    }
  })

  test('zeigt den Validierungsfehler des Backends am betroffenen Feld', async ({ page }) => {
    await page.goto(APP)
    await page.getByRole('button', { name: 'Neues Rezept' }).click()

    // Leerer Titel: die Meldung stammt aus der Bean-Validation des Backends.
    await page.getByRole('button', { name: 'Speichern' }).click()

    await expect(page.getByText('must not be blank')).toBeVisible()
    await expect(page.getByLabel('Titel')).toHaveAttribute('aria-invalid', 'true')
    // Das Formular bleibt offen, die Eingaben gehen nicht verloren.
    await expect(page.getByRole('heading', { name: 'Neues Rezept' })).toBeVisible()
  })

  test('navigiert von der Liste in die Detailansicht und zurück', async ({ page, request }) => {
    const title = uniqueTitle('Carbonara')
    const id = await createRecipe(request, title)

    try {
      await page.goto(APP)
      await titleLink(page, title).click()

      await expect(page.getByRole('heading', { name: title })).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Zutaten' })).toBeVisible()
      await expect(page.getByText('300 g Mehl')).toBeVisible()
      await expect(page.getByRole('listitem').filter({ hasText: 'Teig rühren' })).toBeVisible()

      await page.getByRole('button', { name: 'Zurück' }).click()

      await expect(card(page, title)).toBeVisible()
    } finally {
      await deleteRecipe(request, id)
    }
  })

  test('liefert SPA und API aus derselben Origin aus', async ({ page }) => {
    // Der BFF-Kern aus ADR 0007: der Browser lädt die Seite und ruft die API ohne CORS.
    const apiCalls: string[] = []
    page.on('request', (request) => {
      if (request.url().includes('/api/')) {
        apiCalls.push(new URL(request.url()).origin)
      }
    })

    await page.goto(APP)
    await expect(page.getByRole('heading', { name: 'Rezepte' })).toBeVisible()

    const pageOrigin = new URL(page.url()).origin
    expect(apiCalls.length).toBeGreaterThan(0)
    expect(new Set(apiCalls)).toEqual(new Set([pageOrigin]))
  })
})
