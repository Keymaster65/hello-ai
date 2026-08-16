import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Recipe } from '../api/recipes'
import { RecipeList } from './RecipeList'

const recipes: Recipe[] = [
  {
    id: 1,
    title: 'Pfannkuchen',
    description: 'Fluffig',
    servings: 2,
    prepTimeMinutes: 15,
    difficulty: 'EASY',
    ingredients: [],
    steps: [],
  },
  { id: 2, title: 'Carbonara', difficulty: 'MEDIUM', ingredients: [], steps: [] },
]

function renderList(overrides: Partial<Parameters<typeof RecipeList>[0]> = {}) {
  const props = {
    recipes,
    onEdit: vi.fn(),
    onDelete: vi.fn(),
    ...overrides,
  }
  // Der Titel ist jetzt ein Link – das braucht einen Router-Kontext (docs/prompt/frontend.adoc).
  render(
    <MemoryRouter>
      <RecipeList {...props} />
    </MemoryRouter>,
  )
  return props
}

describe('RecipeList', () => {
  it('should show a hint instead of an empty list when there are no recipes', () => {
    renderList({ recipes: [] })

    expect(screen.getByText('Noch keine Rezepte vorhanden.')).toBeInTheDocument()
  })

  it('should render every recipe with its metadata', () => {
    renderList()

    expect(screen.getByRole('link', { name: 'Pfannkuchen' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Carbonara' })).toBeInTheDocument()
    expect(screen.getByText(/2 Portionen/)).toBeInTheDocument()
    expect(screen.getByText(/15 min/)).toBeInTheDocument()
  })

  it('should link to the shareable detail address', () => {
    renderList()

    expect(screen.getByRole('link', { name: 'Pfannkuchen' })).toHaveAttribute('href', '/1')
    expect(screen.getByRole('link', { name: 'Carbonara' })).toHaveAttribute('href', '/2')
  })

  it('should hand the whole recipe to the edit callback', async () => {
    const { onEdit } = renderList()

    await userEvent.click(screen.getAllByRole('button', { name: 'Bearbeiten' })[1]!)

    expect(onEdit).toHaveBeenCalledWith(recipes[1])
  })

  it('should request deletion for the selected recipe', async () => {
    const { onDelete } = renderList()

    await userEvent.click(screen.getByRole('button', { name: 'Pfannkuchen löschen' }))

    expect(onDelete).toHaveBeenCalledWith(recipes[0])
  })
})
