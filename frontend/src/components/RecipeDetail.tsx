import { useEffect, useState } from 'react'
import { ApiError, recipeApi, type Recipe } from '../api/recipes'

interface RecipeDetailProps {
  id: number
  onBack: () => void
  onEdit: (recipe: Recipe) => void
}

export function RecipeDetail({ id, onBack, onEdit }: RecipeDetailProps) {
  const [recipe, setRecipe] = useState<Recipe | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setRecipe(null)
    setError(null)

    recipeApi
      .get(id)
      .then((loaded) => {
        if (active) {
          setRecipe(loaded)
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof ApiError && cause.isNotFound
              ? 'Dieses Rezept existiert nicht (mehr).'
              : 'Das Rezept konnte nicht geladen werden.',
          )
        }
      })

    return () => {
      active = false
    }
  }, [id])

  if (error) {
    return (
      <section>
        <p role="alert" className="error">
          {error}
        </p>
        <button type="button" onClick={onBack}>
          Zurück
        </button>
      </section>
    )
  }

  if (!recipe) {
    return <p>Rezept wird geladen…</p>
  }

  return (
    <section className="detail">
      <h2>{recipe.title}</h2>
      {recipe.description && <p>{recipe.description}</p>}
      <p className="meta">
        {recipe.difficulty}
        {recipe.servings != null && <> · {recipe.servings} Portionen</>}
        {recipe.prepTimeMinutes != null && <> · {recipe.prepTimeMinutes} min</>}
      </p>

      <h3>Zutaten</h3>
      {recipe.ingredients && recipe.ingredients.length > 0 ? (
        <ul>
          {recipe.ingredients.map((ingredient, index) => (
            <li key={`${ingredient.name}-${index}`}>
              {[ingredient.quantity, ingredient.unit, ingredient.name].filter(Boolean).join(' ')}
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">Keine Zutaten erfasst.</p>
      )}

      <h3>Zubereitung</h3>
      {recipe.steps && recipe.steps.length > 0 ? (
        <ol>
          {recipe.steps.map((step) => (
            <li key={step.position}>{step.instruction}</li>
          ))}
        </ol>
      ) : (
        <p className="muted">Keine Schritte erfasst.</p>
      )}

      <div className="actions">
        <button type="button" onClick={onBack}>
          Zurück
        </button>
        <button type="button" onClick={() => onEdit(recipe)}>
          Bearbeiten
        </button>
      </div>
    </section>
  )
}
