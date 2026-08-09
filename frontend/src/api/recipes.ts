import type { components } from './schema'

export type Recipe = components['schemas']['RecipeResponse']
export type RecipeRequest = components['schemas']['RecipeRequest']
export type Ingredient = components['schemas']['Ingredient']
export type PreparationStep = components['schemas']['PreparationStep']
export type PreparationStepResponse = components['schemas']['PreparationStepResponse']
export type ErrorResponse = components['schemas']['ErrorResponse']
export type Difficulty = RecipeRequest['difficulty']

export const DIFFICULTIES: readonly Difficulty[] = ['EASY', 'MEDIUM', 'HARD']

/**
 * A non-2xx answer from the BFF. Carries the backend's `ErrorResponse` payload so that
 * validation messages can be shown on the field that caused them.
 */
export class ApiError extends Error {
  readonly status: number
  readonly payload: ErrorResponse | undefined

  constructor(status: number, payload?: ErrorResponse) {
    super(payload?.message ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }

  /** Validation message for a single field, if the backend reported one. */
  fieldError(field: string): string | undefined {
    return this.payload?.fieldErrors?.find((error) => error.field === field)?.message ?? undefined
  }

  get isNotFound(): boolean {
    return this.status === 404
  }
}

// Relative on purpose: in production the Spring Boot application serves this bundle itself,
// during development Vite proxies the path to it. Same code, no origin configuration.
const BASE_PATH = '/api/recipes'

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

async function readErrorPayload(response: Response): Promise<ErrorResponse | undefined> {
  try {
    return (await response.json()) as ErrorResponse
  } catch {
    // A proxy or gateway may answer with something other than our ErrorResponse.
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
