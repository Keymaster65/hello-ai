import { useState, type FormEvent } from 'react'
import {
  DIFFICULTIES,
  type ApiError,
  type Difficulty,
  type Ingredient,
  type PreparationStep,
  type Recipe,
  type RecipeRequest,
} from '../api/recipes'

interface RecipeFormProps {
  initial?: Recipe
  submitting: boolean
  /** Set when the backend rejected the last submit; field errors are rendered inline. */
  apiError: ApiError | null
  onSubmit: (recipe: RecipeRequest) => void
  onCancel: () => void
}

interface FormState {
  title: string
  description: string
  servings: string
  prepTimeMinutes: string
  difficulty: Difficulty
  ingredients: Ingredient[]
  steps: PreparationStep[]
}

function toFormState(recipe?: Recipe): FormState {
  return {
    title: recipe?.title ?? '',
    description: recipe?.description ?? '',
    servings: recipe?.servings?.toString() ?? '',
    prepTimeMinutes: recipe?.prepTimeMinutes?.toString() ?? '',
    difficulty: recipe?.difficulty ?? 'MEDIUM',
    ingredients: recipe?.ingredients?.map((i) => ({ ...i })) ?? [],
    // Response fields are optional in the generated contract (see README): default them here.
    steps: recipe?.steps?.map((step) => ({ instruction: step.instruction ?? '' })) ?? [],
  }
}

/** Empty optional fields are omitted rather than sent as empty strings. */
function toRequest(form: FormState): RecipeRequest {
  return {
    title: form.title,
    ...(form.description.trim() ? { description: form.description } : {}),
    ...(form.servings.trim() ? { servings: Number(form.servings) } : {}),
    ...(form.prepTimeMinutes.trim() ? { prepTimeMinutes: Number(form.prepTimeMinutes) } : {}),
    difficulty: form.difficulty,
    ingredients: form.ingredients,
    steps: form.steps,
  }
}

export function RecipeForm({ initial, submitting, apiError, onSubmit, onCancel }: RecipeFormProps) {
  const [form, setForm] = useState<FormState>(() => toFormState(initial))

  const fieldError = (field: string): string | undefined => apiError?.fieldError(field)

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit(toRequest(form))
  }

  const updateIngredient = (index: number, patch: Partial<Ingredient>) => {
    setForm((current) => ({
      ...current,
      ingredients: current.ingredients.map((ingredient, i) =>
        i === index ? { ...ingredient, ...patch } : ingredient,
      ),
    }))
  }

  const updateStep = (index: number, instruction: string) => {
    setForm((current) => ({
      ...current,
      steps: current.steps.map((step, i) => (i === index ? { instruction } : step)),
    }))
  }

  return (
    <form onSubmit={handleSubmit} className="recipe-form" noValidate>
      <h2>{initial ? 'Rezept bearbeiten' : 'Neues Rezept'}</h2>

      {apiError && !apiError.payload?.fieldErrors?.length && (
        <p role="alert" className="error">
          {apiError.message}
        </p>
      )}

      <label>
        Titel
        <input
          name="title"
          value={form.title}
          onChange={(e) => setForm({ ...form, title: e.target.value })}
          aria-invalid={fieldError('title') !== undefined}
        />
      </label>
      {fieldError('title') && <p className="error field-error">{fieldError('title')}</p>}

      <label>
        Beschreibung
        <textarea
          name="description"
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
      </label>

      <label>
        Portionen
        <input
          name="servings"
          type="number"
          min="1"
          value={form.servings}
          onChange={(e) => setForm({ ...form, servings: e.target.value })}
          aria-invalid={fieldError('servings') !== undefined}
        />
      </label>
      {fieldError('servings') && <p className="error field-error">{fieldError('servings')}</p>}

      <label>
        Zubereitungszeit (min)
        <input
          name="prepTimeMinutes"
          type="number"
          min="1"
          value={form.prepTimeMinutes}
          onChange={(e) => setForm({ ...form, prepTimeMinutes: e.target.value })}
          aria-invalid={fieldError('prepTimeMinutes') !== undefined}
        />
      </label>
      {fieldError('prepTimeMinutes') && (
        <p className="error field-error">{fieldError('prepTimeMinutes')}</p>
      )}

      <label>
        Schwierigkeit
        <select
          name="difficulty"
          value={form.difficulty}
          onChange={(e) => setForm({ ...form, difficulty: e.target.value as Difficulty })}
        >
          {DIFFICULTIES.map((difficulty) => (
            <option key={difficulty} value={difficulty}>
              {difficulty}
            </option>
          ))}
        </select>
      </label>

      <fieldset>
        <legend>Zutaten</legend>
        {form.ingredients.map((ingredient, index) => (
          <div key={index} className="row">
            <input
              aria-label={`Zutat ${index + 1} Name`}
              value={ingredient.name}
              onChange={(e) => updateIngredient(index, { name: e.target.value })}
            />
            <input
              aria-label={`Zutat ${index + 1} Menge`}
              type="number"
              step="any"
              value={ingredient.quantity ?? ''}
              onChange={(e) =>
                updateIngredient(index, {
                  quantity: e.target.value === '' ? undefined : Number(e.target.value),
                })
              }
            />
            <input
              aria-label={`Zutat ${index + 1} Einheit`}
              value={ingredient.unit ?? ''}
              onChange={(e) => updateIngredient(index, { unit: e.target.value })}
            />
            <button
              type="button"
              className="danger"
              aria-label={`Zutat ${index + 1} entfernen`}
              onClick={() =>
                setForm({ ...form, ingredients: form.ingredients.filter((_, i) => i !== index) })
              }
            >
              ✕
            </button>
          </div>
        ))}
        <button
          type="button"
          onClick={() => setForm({ ...form, ingredients: [...form.ingredients, { name: '' }] })}
        >
          Zutat hinzufügen
        </button>
      </fieldset>

      <fieldset>
        <legend>Zubereitungsschritte</legend>
        {form.steps.map((step, index) => (
          <div key={index} className="row">
            <input
              aria-label={`Schritt ${index + 1}`}
              value={step.instruction}
              onChange={(e) => updateStep(index, e.target.value)}
            />
            <button
              type="button"
              className="danger"
              aria-label={`Schritt ${index + 1} entfernen`}
              onClick={() => setForm({ ...form, steps: form.steps.filter((_, i) => i !== index) })}
            >
              ✕
            </button>
          </div>
        ))}
        <button
          type="button"
          onClick={() => setForm({ ...form, steps: [...form.steps, { instruction: '' }] })}
        >
          Schritt hinzufügen
        </button>
      </fieldset>

      <div className="actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Speichern…' : 'Speichern'}
        </button>
        <button type="button" onClick={onCancel}>
          Abbrechen
        </button>
      </div>
    </form>
  )
}
