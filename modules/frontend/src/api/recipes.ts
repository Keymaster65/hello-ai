import type { components } from './schema'

export type Recipe = components['schemas']['RecipeResponse']
export type RecipeRequest = components['schemas']['RecipeRequest']
export type Ingredient = components['schemas']['Ingredient']
export type PreparationStep = components['schemas']['PreparationStep']
export type PreparationStepResponse = components['schemas']['PreparationStepResponse']
export type ProblemDetail = components['schemas']['ProblemDetail']
export type Difficulty = RecipeRequest['difficulty']

export const DIFFICULTIES: readonly Difficulty[] = ['EASY', 'MEDIUM', 'HARD']

/**
 * A non-2xx answer from the BFF. Carries the backend's RFC 9457 problem detail so that
 * validation messages can be shown on the field that caused them (ADR 0046).
 */
export class ApiError extends Error {
  readonly status: number
  readonly payload: ProblemDetail | undefined

  constructor(status: number, payload?: ProblemDetail, message?: string) {
    super(message ?? payload?.detail ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }

  /**
   * Validation message for a single field, if the backend reported one.
   *
   * `fieldErrors` is our extension of the RFC; the problem details Spring itself produces for
   * framework errors (405, 415, …) do not carry it, hence the optional access.
   */
  fieldError(field: string): string | undefined {
    return this.payload?.fieldErrors?.find((error) => error.field === field)?.message
  }

  get isNotFound(): boolean {
    return this.status === 404
  }
}

// Derived from the bundle's base URL, which mirrors the backend's context path (ADR 0016):
// `/recipes/` in production and during development, `/` in the jsdom unit tests. The app
// therefore never carries an absolute origin – only a path relative to where it is served.
const BASE_PATH = `${import.meta.env.BASE_URL}api/recipes`

async function send<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: init?.body ? { 'Content-Type': 'application/json', ...init.headers } : init?.headers,
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorPayload(response))
  }

  return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
}

async function readErrorPayload(response: Response): Promise<ProblemDetail | undefined> {
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    // A proxy or gateway may answer with something other than a problem detail.
    return undefined
  }
}

export const recipeApi = {
  list: (): Promise<Recipe[]> => send<Recipe[]>(BASE_PATH),

  get: (id: number): Promise<Recipe> => send<Recipe>(`${BASE_PATH}/${id}`),

  create: (recipe: RecipeRequest): Promise<Recipe> =>
    send<Recipe>(BASE_PATH, { method: 'POST', body: JSON.stringify(recipe) }),

  update: (id: number, recipe: RecipeRequest): Promise<Recipe> =>
    send<Recipe>(`${BASE_PATH}/${id}`, { method: 'PUT', body: JSON.stringify(recipe) }),

  remove: (id: number): Promise<void> =>
    send<void>(`${BASE_PATH}/${id}`, { method: 'DELETE' }),
}
