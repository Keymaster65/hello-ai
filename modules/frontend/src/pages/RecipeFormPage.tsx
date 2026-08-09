import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ApiError, recipeApi, type Recipe, type RecipeRequest } from '../api/recipes'
import { RecipeForm } from '../components/RecipeForm'

interface RecipeFormPageProps {
  /** `edit` lädt das Rezept aus der Adresse, `create` startet mit einem leeren Formular. */
  mode: 'create' | 'edit'
}

/** Anlegen unter `/new`, Bearbeiten unter `/{id}/edit` (ADR 0017). */
export function RecipeFormPage({ mode }: RecipeFormPageProps) {
  const { id } = useParams()
  const navigate = useNavigate()
  const recipeId = Number(id)

  const [initial, setInitial] = useState<Recipe | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<ApiError | null>(null)

  useEffect(() => {
    if (mode !== 'edit') {
      return
    }
    let active = true
    recipeApi
      .get(recipeId)
      .then((loaded) => {
        if (active) {
          setInitial(loaded)
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setLoadError(
            cause instanceof ApiError && cause.isNotFound
              ? 'Dieses Rezept existiert nicht (mehr).'
              : 'Das Rezept konnte nicht geladen werden.',
          )
        }
      })
    return () => {
      active = false
    }
  }, [mode, recipeId])

  const save = async (request: RecipeRequest) => {
    setSubmitting(true)
    setSubmitError(null)
    try {
      if (mode === 'edit') {
        await recipeApi.update(recipeId, request)
      } else {
        await recipeApi.create(request)
      }
      void navigate('/')
    } catch (cause: unknown) {
      setSubmitError(
        cause instanceof ApiError ? cause : new ApiError(0, undefined, 'Speichern fehlgeschlagen.'),
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <header>
        <h1>Rezepte</h1>
      </header>

      {loadError ? (
        <>
          <p role="alert" className="error">
            {loadError}
          </p>
          <button type="button" onClick={() => void navigate('/')}>
            Zur Liste
          </button>
        </>
      ) : mode === 'edit' && !initial ? (
        <p>Rezept wird geladen…</p>
      ) : (
        <RecipeForm
          initial={initial ?? undefined}
          submitting={submitting}
          apiError={submitError}
          onSubmit={save}
          onCancel={() => void navigate('/')}
        />
      )}
    </>
  )
}
