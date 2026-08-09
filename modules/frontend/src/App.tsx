import { useCallback, useEffect, useState } from 'react'
import { ApiError, recipeApi, type Recipe, type RecipeRequest } from './api/recipes'
import { RecipeDetail } from './components/RecipeDetail'
import { RecipeForm } from './components/RecipeForm'
import { RecipeList } from './components/RecipeList'

type View =
  | { mode: 'list' }
  | { mode: 'detail'; id: number }
  | { mode: 'create' }
  | { mode: 'edit'; recipe: Recipe }

export function App() {
  const [view, setView] = useState<View>({ mode: 'list' })
  const [recipes, setRecipes] = useState<Recipe[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<ApiError | null>(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      setRecipes(await recipeApi.list())
    } catch {
      setLoadError('Die Rezepte konnten nicht geladen werden.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const save = async (request: RecipeRequest) => {
    setSubmitting(true)
    setSubmitError(null)
    try {
      if (view.mode === 'edit') {
        await recipeApi.update(view.recipe.id, request)
      } else {
        await recipeApi.create(request)
      }
      setView({ mode: 'list' })
      await reload()
    } catch (cause: unknown) {
      setSubmitError(
        cause instanceof ApiError
          ? cause
          : new ApiError(0, undefined, 'Speichern fehlgeschlagen.'),
      )
    } finally {
      setSubmitting(false)
    }
  }

  const remove = async (recipe: Recipe) => {
    if (!window.confirm(`„${recipe.title}“ wirklich löschen?`)) {
      return
    }
    try {
      await recipeApi.remove(recipe.id)
      await reload()
    } catch {
      setLoadError('Das Rezept konnte nicht gelöscht werden.')
    }
  }

  const openForm = (next: View) => {
    setSubmitError(null)
    setView(next)
  }

  return (
    <main className="app">
      <header>
        <h1>Rezepte</h1>
        {view.mode === 'list' && (
          <button type="button" onClick={() => openForm({ mode: 'create' })}>
            Neues Rezept
          </button>
        )}
      </header>

      {loadError && (
        <p role="alert" className="error">
          {loadError}
        </p>
      )}

      {view.mode === 'list' &&
        (loading ? (
          <p>Rezepte werden geladen…</p>
        ) : (
          <RecipeList
            recipes={recipes}
            onOpen={(id) => setView({ mode: 'detail', id })}
            onEdit={(recipe) => openForm({ mode: 'edit', recipe })}
            onDelete={remove}
          />
        ))}

      {view.mode === 'detail' && (
        <RecipeDetail
          id={view.id}
          onBack={() => setView({ mode: 'list' })}
          onEdit={(recipe) => openForm({ mode: 'edit', recipe })}
        />
      )}

      {(view.mode === 'create' || view.mode === 'edit') && (
        <RecipeForm
          initial={view.mode === 'edit' ? view.recipe : undefined}
          submitting={submitting}
          apiError={submitError}
          onSubmit={save}
          onCancel={() => setView({ mode: 'list' })}
        />
      )}
    </main>
  )
}
