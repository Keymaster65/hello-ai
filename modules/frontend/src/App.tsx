import { BrowserRouter, Link, Route, Routes } from 'react-router'
import { RecipeDetailPage } from './pages/RecipeDetailPage'
import { RecipeFormPage } from './pages/RecipeFormPage'
import { RecipeListPage } from './pages/RecipeListPage'

/**
 * Routing der Anwendung (docs/prompt/frontend.adoc). `basename` stammt aus `import.meta.env.BASE_URL` und
 * spiegelt damit den Context-Path des Backends (docs/prompt/frontend.adoc) – die Routen darunter kennen ihn
 * nicht und bleiben lesbar.
 *
 * <pre>
 *   /            Liste          → /recipes/
 *   /new         Anlegen        → /recipes/new
 *   /{id}        Detailansicht  → /recipes/3       (teilbar)
 *   /{id}/edit   Bearbeiten     → /recipes/3/edit
 * </pre>
 *
 * <p>Damit diese Adressen auch beim direkten Aufruf funktionieren, leitet das Backend sie
 * auf die `index.html` weiter (SPA-Fallback).
 */
export function App() {
  return (
    <BrowserRouter basename={import.meta.env.BASE_URL}>
      <main className="app">
        <Routes>
          <Route path="/" element={<RecipeListPage />} />
          <Route path="/new" element={<RecipeFormPage mode="create" />} />
          <Route path="/:id" element={<RecipeDetailPage />} />
          <Route path="/:id/edit" element={<RecipeFormPage mode="edit" />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}

function NotFound() {
  return (
    <>
      <header>
        <h1>Rezepte</h1>
      </header>
      <p role="alert" className="error">
        Diese Seite gibt es nicht.
      </p>
      <Link to="/">Zur Liste</Link>
    </>
  )
}
