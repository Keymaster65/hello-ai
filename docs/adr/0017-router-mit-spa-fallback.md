# 0017 – Router mit SPA-Fallback für teilbare Rezept-Adressen

Status: akzeptiert und umgesetzt
Datum: 2026-08-09

| Abschnitt        | Inhalt |
|------------------|--------|
| **Kontext**      | Die Ansicht wurde bisher über Komponenten-State umgeschaltet ([ADR 0007](0007-react-frontend-mit-backend-als-bff.md), Option ii). Das war bewusst schlicht gewählt, hat aber einen Preis: Die Adresszeile ändert sich nie. Ein Rezept lässt sich nicht verlinken, ein Lesezeichen führt immer zur Liste, die Zurück-Taste verlässt die Anwendung, und ein Neuladen wirft den Nutzer aus der Detailansicht. ADR 0007 hatte den Nachzug bereits vorgesehen: „Kommt `react-router` dazu, braucht das Backend einen Forwarding-Controller für unbekannte Pfade." |
| **Optionen**     | **Routing:** 1. `react-router` mit echten Adressen. 2. Hash-Routing (`/#/3`) – bräuchte keinen Server-Fallback, erzeugt aber unschöne Adressen und schwächt Server-Rendering und Analytik. 3. Beim State-Umschalten bleiben. **Server-Fallback:** a) Die Routen **explizit** weiterleiten. b) Catch-all-Muster mit Ausschlussliste. c) Resource-Resolver, der bei fehlender Datei auf `index.html` fällt. |
| **Entscheidung** | **1 + a.** `react-router` 8 mit `basename={import.meta.env.BASE_URL}` – der Router erbt damit den Context-Path aus [ADR 0016](0016-context-path-recipes.md), ohne ihn zu kennen. Routen: `/` (Liste), `/new` (Anlegen), `/{id}` (Detail, **teilbar**), `/{id}/edit` (Bearbeiten). Serverseitig leitet `SpaForwardingController` **genau diese** Pfade auf `forward:/index.html` weiter – kein Catch-all. |
| **Konsequenzen** | **+** Rezepte sind verlinkbar: Adresse kopieren, in neuem Tab öffnen, Lesezeichen setzen, Zurück-Taste – alles funktioniert. **+** Der Rezepttitel ist jetzt ein echtes `<a href>`; Mittelklick und „Link in neuem Tab öffnen" gehen ohne Zusatzarbeit. **+** Der explizite Fallback kann die API, das OpenAPI-Dokument und die Swagger-UI-Ressourcen **nicht** verschatten – ein Catch-all hätte genau dieses Risiko. Ein Systemtest prüft beides. **+** Unbekannte Pfade bleiben ein ehrlicher 404 statt still die SPA auszuliefern. **−** Jede neue Frontend-Route braucht eine Zeile im `SpaForwardingController`. Wird sie vergessen, funktioniert die Route in der Anwendung, aber nicht beim direkten Aufruf – der Systemtest `shouldForwardSpaRoutesToTheIndexPage` schlägt dann fehl. **−** `App.tsx` verliert seinen State-Schalter; die Ladelogik wandert in drei Seiten unter `src/pages/`. **−** Die Detailseite lädt das Rezept jetzt immer selbst, auch wenn es aus der Liste kommt – ein zusätzlicher Request im Tausch gegen die Teilbarkeit. |

## Zwei Fallen, in die ich gelaufen bin

**Die jsdom-Historie ist über Tests hinweg gemeinsam.** `BrowserRouter` arbeitet auf der echten
`window.history`, und jsdom teilt sie innerhalb einer Testdatei. Nach dem ersten Test, der zur
Detailansicht navigierte, starteten die folgenden Tests dort – mit leerem Rendering und einem
Absturz in `RecipeDetail`. Abhilfe im `beforeEach`:

```ts
window.history.pushState({}, '', '/')
```

**`reuseExistingServer` testet stillschweigend die alte Anwendung.** Die Playwright-Konfiguration
verwendet lokal einen bereits laufenden Server wieder. Weil aus einem früheren Schritt noch eine
Instanz auf Port 8080 lief, prüften alle E2E-Tests das **alte** Bundle – sieben Fehlschläge, die
wie ein Router-Problem aussahen, aber keines waren. Vor einem E2E-Lauf gehört Port 8080
freigeräumt; der Screenshot aus dem Fehlerbericht (Titel als `button` statt `link`) war der
entscheidende Hinweis.

## Nachweis

- E2E: `öffnet eine geteilte Rezept-Adresse direkt` ruft `/recipes/{id}` und `/recipes/{id}/edit`
  **ohne** Umweg über die Liste auf und prüft Status 200 und Inhalt; `navigiert von der Liste …`
  prüft zusätzlich, dass die Adresse mitwandert (`toHaveURL`).
- System: `shouldForwardSpaRoutesToTheIndexPage` prüft `/new`, `/{id}`, `/{id}/edit`;
  `shouldNotLetTheSpaForwardShadowTheBackend` prüft, dass API, OpenAPI und Swagger UI
  unverändert antworten und ein unbekannter Pfad weiterhin 404 liefert.
- `./gradlew clean build` grün (30 Java, 24 Vitest), `systemtest` 15/15, `e2eTest` 9/9.

## Regel für die Zukunft

- Neue Route im Frontend? **Immer** die passende Zeile im `SpaForwardingController` ergänzen
  und den Systemtest um den Pfad erweitern.
- Keine Catch-all-Weiterleitung einführen. Der Preis der expliziten Liste ist eine Zeile pro
  Route; der Preis eines Catch-alls wäre ein stiller 200 auf Pfade, die eigentlich 404 sind.
- Vor `e2eTest` sicherstellen, dass auf dem Zielport keine Altinstanz läuft.
