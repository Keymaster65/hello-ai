import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'
import type { Recipe } from './api/recipes'

const pancakes: Recipe = {
  id: 1,
  title: 'Pfannkuchen',
  description: 'Fluffig',
  difficulty: 'EASY',
  ingredients: [{ name: 'Mehl', quantity: 300, unit: 'g' }],
  steps: [{ position: 1, instruction: 'Teig rühren' }],
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('App', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockReset()
    // BrowserRouter arbeitet auf der echten window.history, die jsdom über alle Tests einer
    // Datei teilt. Ohne Zurücksetzen startet der nächste Test auf der zuletzt besuchten Route.
    window.history.pushState({}, '', '/')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('should load and display the recipes on start', async () => {
    fetchMock.mockResolvedValue(jsonResponse([pancakes]))

    render(<App />)

    expect(await screen.findByRole('link', { name: 'Pfannkuchen' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes', expect.anything())
  })

  it('should report a failing backend instead of rendering an empty list', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 503 }))

    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Die Rezepte konnten nicht geladen werden.',
    )
  })

  it('should load the single recipe when one is opened', async () => {
    fetchMock.mockImplementation((path: string) =>
      Promise.resolve(jsonResponse(path === '/api/recipes' ? [pancakes] : pancakes)),
    )

    render(<App />)
    await userEvent.click(await screen.findByRole('link', { name: 'Pfannkuchen' }))

    expect(await screen.findByRole('heading', { name: 'Pfannkuchen' })).toBeInTheDocument()
    expect(screen.getByText('300 g Mehl')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/1', expect.anything())
  })

  it('should create a recipe and reload the list afterwards', async () => {
    // A Response body can only be read once, so every call needs a fresh one.
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse([])))

    render(<App />)
    await userEvent.click(await screen.findByRole('link', { name: 'Neues Rezept' }))
    await userEvent.type(screen.getByLabelText('Titel'), 'Suppe')
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    await waitFor(() => {
      const post = fetchMock.mock.calls.find(
        ([, init]) => (init as RequestInit | undefined)?.method === 'POST',
      )
      expect(post?.[0]).toBe('/api/recipes')
      expect(JSON.parse((post?.[1] as RequestInit).body as string)).toMatchObject({
        title: 'Suppe',
        difficulty: 'MEDIUM',
      })
    })
    // Back on the list, which was fetched again after the successful create.
    expect(await screen.findByText('Noch keine Rezepte vorhanden.')).toBeInTheDocument()
  })

  it('should keep the form open and show field errors when the backend rejects the input', async () => {
    fetchMock.mockImplementation((_path: string, init?: RequestInit) =>
      Promise.resolve(
        init?.method === 'POST'
          ? jsonResponse(
              {
                status: 400,
                error: 'VALIDATION_FAILED',
                message: 'Request validation failed',
                fieldErrors: [{ field: 'title', message: 'must not be blank' }],
              },
              400,
            )
          : jsonResponse([]),
      ),
    )

    render(<App />)
    await userEvent.click(await screen.findByRole('link', { name: 'Neues Rezept' }))
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(await screen.findByText('must not be blank')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Neues Rezept' })).toBeInTheDocument()
  })

  it('should delete a recipe only after confirmation', async () => {
    fetchMock.mockResolvedValue(jsonResponse([pancakes]))
    const confirmMock = vi.fn().mockReturnValue(false)
    vi.stubGlobal('confirm', confirmMock)

    render(<App />)
    await userEvent.click(await screen.findByRole('button', { name: 'Pfannkuchen löschen' }))

    expect(confirmMock).toHaveBeenCalled()
    expect(
      fetchMock.mock.calls.some(([, init]) => (init as RequestInit | undefined)?.method === 'DELETE'),
    ).toBe(false)
  })
})
