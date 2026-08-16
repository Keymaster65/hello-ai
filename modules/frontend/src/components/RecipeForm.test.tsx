import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError, type Recipe, type RecipeRequest } from '../api/recipes'
import { RecipeForm } from './RecipeForm'

function renderForm(overrides: Partial<Parameters<typeof RecipeForm>[0]> = {}) {
  const props = {
    submitting: false,
    apiError: null,
    onSubmit: vi.fn(),
    onCancel: vi.fn(),
    ...overrides,
  }
  render(<RecipeForm {...props} />)
  return props
}

describe('RecipeForm', () => {
  it('should submit only the filled fields', async () => {
    const { onSubmit } = renderForm()

    await userEvent.type(screen.getByLabelText('Titel'), 'Pfannkuchen')
    await userEvent.selectOptions(screen.getByLabelText('Schwierigkeit'), 'EASY')
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Pfannkuchen',
      difficulty: 'EASY',
      ingredients: [],
      steps: [],
    } satisfies RecipeRequest)
  })

  it('should prefill the form when editing an existing recipe', () => {
    const recipe: Recipe = {
      id: 4,
      title: 'Carbonara',
      description: 'Klassisch',
      servings: 4,
      prepTimeMinutes: 25,
      difficulty: 'HARD',
      ingredients: [{ name: 'Spaghetti', quantity: 500, unit: 'g' }],
      steps: [{ position: 1, instruction: 'Nudeln kochen' }],
    }

    renderForm({ initial: recipe })

    expect(screen.getByRole('heading', { name: 'Rezept bearbeiten' })).toBeInTheDocument()
    expect(screen.getByLabelText('Titel')).toHaveValue('Carbonara')
    expect(screen.getByLabelText('Portionen')).toHaveValue(4)
    expect(screen.getByLabelText('Schwierigkeit')).toHaveValue('HARD')
    expect(screen.getByLabelText('Zutat 1 Name')).toHaveValue('Spaghetti')
    expect(screen.getByLabelText('Schritt 1')).toHaveValue('Nudeln kochen')
  })

  it('should add and remove ingredients and steps', async () => {
    const { onSubmit } = renderForm()

    await userEvent.type(screen.getByLabelText('Titel'), 'Suppe')
    await userEvent.click(screen.getByRole('button', { name: 'Zutat hinzufügen' }))
    await userEvent.type(screen.getByLabelText('Zutat 1 Name'), 'Salz')
    await userEvent.type(screen.getByLabelText('Zutat 1 Menge'), '5')

    await userEvent.click(screen.getByRole('button', { name: 'Schritt hinzufügen' }))
    await userEvent.click(screen.getByRole('button', { name: 'Schritt hinzufügen' }))
    await userEvent.type(screen.getByLabelText('Schritt 1'), 'Wasser kochen')
    await userEvent.click(screen.getByRole('button', { name: 'Schritt 2 entfernen' }))

    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        ingredients: [{ name: 'Salz', quantity: 5 }],
        steps: [{ instruction: 'Wasser kochen' }],
      }),
    )
  })

  it('should render the backend validation message at the offending field', () => {
    const apiError = new ApiError(400, {
      type: '/recipes/docs/#problem-validation-failed',
      title: 'Request validation failed',
      status: 400,
      detail: '1 field(s) of the request body are invalid',
      instance: '/recipes/api/recipes',
      fieldErrors: [{ field: 'title', message: 'must not be blank' }],
    })

    renderForm({ apiError })

    expect(screen.getByText('must not be blank')).toBeInTheDocument()
    expect(screen.getByLabelText('Titel')).toHaveAttribute('aria-invalid', 'true')
  })

  it('should show a general alert when the backend reports no field errors', () => {
    renderForm({ apiError: new ApiError(500, undefined, 'Boom') })

    expect(screen.getByRole('alert')).toHaveTextContent('Boom')
  })

  it('should disable the submit button while saving', () => {
    renderForm({ submitting: true })

    expect(screen.getByRole('button', { name: 'Speichern…' })).toBeDisabled()
  })
})
