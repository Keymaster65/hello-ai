import { expect, test, type APIRequestContext } from '@playwright/test'

import { CONTEXT_PATH } from '../playwright.config'

/** Basis-URL der Anwendung inklusive Context-Path, mit Schrägstrich am Ende (ADR 0016). */
const APP = `${CONTEXT_PATH}/`

/**
 * Structural regression tests: the accessibility tree of a view is compared against a committed
 * baseline (see ADR 0008).
 *
 * <p>Unlike the recorded videos – which are diagnostics and not reproducible byte-for-byte – an
 * aria snapshot is deterministic text: it survives font, GPU and platform differences and shows
 * up as a readable diff in code review. It also guards exactly what the other tests rely on:
 * the roles and accessible names used by `getByRole`/`getByLabel`.
 *
 * <p>Baselines live in `aria-snapshots.spec.ts-snapshots/` and are updated deliberately with
 * `npx playwright test --update-snapshots`.
 */

/**
 * Fixed – the title is part of the snapshot, so it must not vary between runs. Any leftover from
 * an aborted run is removed before the test creates its own.
 */
const SNAPSHOT_TITLE = 'Snapshot-Rezept'

async function removeByTitle(request: APIRequestContext, title: string): Promise<void> {
  const response = await request.get(`${APP}api/recipes`)
  const recipes = (await response.json()) as Array<{ id: number; title: string }>
  for (const recipe of recipes.filter((candidate) => candidate.title === title)) {
    await request.delete(`${APP}api/recipes/${recipe.id}`)
  }
}

test.describe('Struktur der Oberfläche', () => {
  test('das leere Anlegeformular entspricht der Baseline', async ({ page }) => {
    await page.goto(APP)
    await page.getByRole('link', { name: 'Neues Rezept' }).click()

    // Data-independent: an empty form looks the same no matter what is in the database.
    await expect(page.locator('form.recipe-form')).toMatchAriaSnapshot({
      name: 'empty-form.aria.yml',
    })
  })

  test('die Detailansicht entspricht der Baseline', async ({ page, request }) => {
    await removeByTitle(request, SNAPSHOT_TITLE)
    const created = await request.post(`${APP}api/recipes`, {
      data: {
        title: SNAPSHOT_TITLE,
        description: 'Fester Datensatz für den Struktur-Vergleich',
        servings: 4,
        prepTimeMinutes: 25,
        difficulty: 'MEDIUM',
        ingredients: [
          { name: 'Mehl', quantity: 300, unit: 'g' },
          { name: 'Milch', quantity: 500, unit: 'ml' },
        ],
        steps: [{ instruction: 'Teig anrühren' }, { instruction: 'Ausbacken' }],
      },
    })
    expect(created.status()).toBe(201)
    const id = (await created.json()).id as number

    try {
      await page.goto(APP)
      await page.getByRole('link', { name: SNAPSHOT_TITLE, exact: true }).click()
      await expect(page.getByRole('heading', { name: SNAPSHOT_TITLE })).toBeVisible()

      // Scoped to the detail section: the surrounding list depends on the database content.
      await expect(page.locator('section.detail')).toMatchAriaSnapshot({
        name: 'recipe-detail.aria.yml',
      })
    } finally {
      await request.delete(`${APP}api/recipes/${id}`)
    }
  })
})
