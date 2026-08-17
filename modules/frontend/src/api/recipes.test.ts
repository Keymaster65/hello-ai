import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, recipeApi, type Recipe, type RecipeRequest } from './recipes'

const recipe: Recipe = {
  id: 1,
  title: 'Pfannkuchen',
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

describe('recipeApi', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockReset()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('should request the relative BFF path when listing recipes', async () => {
    fetchMock.mockResolvedValue(jsonResponse([recipe]))

    const result = await recipeApi.list()

    expect(fetchMock).toHaveBeenCalledWith('/api/gitdata/recipes', expect.anything())
    expect(result).toEqual([recipe])
  })

  it('should send the recipe as JSON when creating', async () => {
    fetchMock.mockResolvedValue(jsonResponse(recipe, 201))
    const request: RecipeRequest = { title: 'Pfannkuchen', difficulty: 'EASY' }

    await recipeApi.create(request)

    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(path).toBe('/api/gitdata/recipes')
    expect(init.method).toBe('POST')
    expect(init.body).toBe(JSON.stringify(request))
    expect(new Headers(init.headers).get('Content-Type')).toBe('application/json')
  })

  it('should address the single-recipe endpoint when updating', async () => {
    fetchMock.mockResolvedValue(jsonResponse(recipe))

    await recipeApi.update(7, { title: 'Neu', difficulty: 'HARD' })

    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(path).toBe('/api/gitdata/recipes/7')
    expect(init.method).toBe('PUT')
  })

  it('should tolerate the empty 204 body when deleting', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(recipeApi.remove(3)).resolves.toBeUndefined()
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/gitdata/recipes/3')
  })

  it('should raise an ApiError carrying the backend payload on 404', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        {
          type: '/recipes/docs/#problem-not-found',
          title: 'Recipe not found',
          status: 404,
          detail: 'Recipe not found: 9',
          instance: '/recipes/api/recipes/9',
          fieldErrors: [],
        },
        404,
      ),
    )

    const error = await recipeApi.get(9).catch((cause: unknown) => cause)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).isNotFound).toBe(true)
    expect((error as ApiError).message).toBe('Recipe not found: 9')
  })

  it('should expose per-field validation messages from a 400', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        {
          type: '/recipes/docs/#problem-validation-failed',
          title: 'Request validation failed',
          status: 400,
          detail: '1 field(s) of the request body are invalid',
          instance: '/recipes/api/recipes',
          fieldErrors: [{ field: 'title', message: 'must not be blank' }],
        },
        400,
      ),
    )

    const error = (await recipeApi
      .create({ title: '', difficulty: 'EASY' })
      .catch((cause: unknown) => cause)) as ApiError

    expect(error.fieldError('title')).toBe('must not be blank')
    expect(error.fieldError('difficulty')).toBeUndefined()
  })

  it('should tolerate a problem detail without our fieldErrors extension', async () => {
    // Spring produces this shape for the errors of the framework itself (ADR 0046).
    fetchMock.mockResolvedValue(
      jsonResponse(
        { type: 'about:blank', title: 'Method Not Allowed', status: 405, detail: 'Method PATCH is not supported.' },
        405,
      ),
    )

    const error = (await recipeApi.list().catch((cause: unknown) => cause)) as ApiError

    expect(error.message).toBe('Method PATCH is not supported.')
    expect(error.fieldError('title')).toBeUndefined()
  })

  it('should still fail cleanly when the error body is not a problem detail', async () => {
    fetchMock.mockResolvedValue(new Response('<html>gateway timeout</html>', { status: 504 }))

    const error = (await recipeApi.list().catch((cause: unknown) => cause)) as ApiError

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(504)
    expect(error.payload).toBeUndefined()
  })
})
