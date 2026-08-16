import { useNavigate, useParams } from 'react-router'
import { RecipeDetail } from '../components/RecipeDetail'

/**
 * Teilbare Detailansicht unter `/{id}`. Die Seite lädt das Rezept anhand der ID aus der
 * Adresse – ein Aufruf aus einem Lesezeichen oder einem geteilten Link funktioniert damit
 * genauso wie die Navigation aus der Liste (docs/prompt/frontend.adoc).
 */
export function RecipeDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const recipeId = Number(id)

  if (!Number.isInteger(recipeId) || recipeId <= 0) {
    return (
      <>
        <header>
          <h1>Rezepte</h1>
        </header>
        <p role="alert" className="error">
          „{id}“ ist keine gültige Rezept-Adresse.
        </p>
        <button type="button" onClick={() => void navigate('/')}>
          Zur Liste
        </button>
      </>
    )
  }

  return (
    <>
      <header>
        <h1>Rezepte</h1>
      </header>
      <RecipeDetail
        id={recipeId}
        onBack={() => void navigate('/')}
        onEdit={(recipe) => void navigate(`/${recipe.id}/edit`)}
      />
    </>
  )
}
