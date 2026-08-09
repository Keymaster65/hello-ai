import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { recipeApi, type Recipe } from '../api/recipes'
import { RecipeList } from '../components/RecipeList'

/** Startseite unter `/` – die Liste aller Rezepte. */
export function RecipeListPage() {
  const navigate = useNavigate()
  const [recipes, setRecipes] = useState<Recipe[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setRecipes(await recipeApi.list())
    } catch {
      setError('Die Rezepte konnten nicht geladen werden.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  const remove = async (recipe: Recipe) => {
    if (!window.confirm(`„${recipe.title}“ wirklich löschen?`)) {
      return
    }
    try {
      await recipeApi.remove(recipe.id)
      await reload()
    } catch {
      setError('Das Rezept konnte nicht gelöscht werden.')
    }
  }

  return (
    <>
      <header>
        <h1>Rezepte</h1>
        <Link className="button-link" to="/new">
          Neues Rezept
        </Link>
      </header>

      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}

      {loading ? (
        <p>Rezepte werden geladen…</p>
      ) : (
        <RecipeList
          recipes={recipes}
          onEdit={(recipe) => void navigate(`/${recipe.id}/edit`)}
          onDelete={remove}
        />
      )}
    </>
  )
}
