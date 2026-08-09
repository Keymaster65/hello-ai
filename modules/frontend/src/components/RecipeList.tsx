import { Link } from 'react-router'
import type { Recipe } from '../api/recipes'

interface RecipeListProps {
  recipes: Recipe[]
  onEdit: (recipe: Recipe) => void
  onDelete: (recipe: Recipe) => void
}

export function RecipeList({ recipes, onEdit, onDelete }: RecipeListProps) {
  if (recipes.length === 0) {
    return <p className="empty">Noch keine Rezepte vorhanden.</p>
  }

  return (
    <ul className="recipe-list">
      {recipes.map((recipe) => (
        <li key={recipe.id} className="recipe-card">
          <div className="recipe-card__main">
            {/* Ein echter Link, damit die Detailansicht teilbar ist: Adresse kopieren,
                neuer Tab und Zurück-Taste funktionieren wie erwartet (ADR 0017). */}
            <Link className="link" to={`/${recipe.id}`}>
              {recipe.title}
            </Link>
            {recipe.description && <p className="muted">{recipe.description}</p>}
            <p className="meta">
              {recipe.difficulty}
              {recipe.servings != null && <> · {recipe.servings} Portionen</>}
              {recipe.prepTimeMinutes != null && <> · {recipe.prepTimeMinutes} min</>}
            </p>
          </div>
          <div className="recipe-card__actions">
            <button type="button" onClick={() => onEdit(recipe)}>
              Bearbeiten
            </button>
            <button
              type="button"
              className="danger"
              onClick={() => onDelete(recipe)}
              aria-label={`${recipe.title} löschen`}
            >
              Löschen
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}
