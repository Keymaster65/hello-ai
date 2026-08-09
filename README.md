# Recipe Backend

Rezeptverwaltung: Spring-Boot-Backend mit REST-API und PostgreSQL, dazu ein
React-Frontend, das dieses Backend als **BFF** (Backend for Frontend) nutzt –
beides wird als **ein** Artefakt aus derselben Origin ausgeliefert.

## Tech-Stack

**Backend**
- Java 25, Spring Boot 4.1
- jOOQ (typsicherer SQL-Zugriff), Liquibase (Migrationen), PostgreSQL
- springdoc-openapi 3.1 (OpenAPI 3.1 + Swagger UI)
- Tests: JUnit 5, Mockito, AssertJ, jqwik, JaCoCo

**Frontend** (`frontend/`)
- TypeScript 5.9, React 19, Vite 8
- Tests: Vitest 4 + Testing Library (Komponenten), Playwright 1.62 (E2E)
- API-Typen aus dem OpenAPI-Contract generiert (`openapi-typescript`)

## Architektur
Hexagonal / Clean Architecture (`io.github.keymaster65.helloai`):

| Package                     | Verantwortung                                   |
|-----------------------------|-------------------------------------------------|
| `adapter.in.rest`           | REST-Controller, DTOs, Mapper, Fehlerbehandlung |
| `adapter.out.persistence`   | jOOQ-Repository (+ generierter jOOQ-Code)       |
| `application.port.in/out`   | Use-Case- und Repository-Interfaces (Ports)     |
| `application.service`       | Geschäftslogik                                  |
| `domain`                    | Domänenmodell (Records)                         |
| `bootstrap`                 | Spring-Boot-Einstiegspunkt                      |

Der jOOQ-Code wird beim Build aus dem Liquibase-Changelog generiert
(`org.jooq.meta.extensions.liquibase.LiquibaseDatabase`) – es wird dafür **keine
laufende Datenbank** benötigt.

## Frontend (BFF-Setup)

Das React-Frontend liegt in `frontend/` und spricht ausschließlich **relative**
`/api`-Pfade an. Gradle baut es (`frontendBuild`) und packt das Bundle nach
`static/` ins Boot-Jar – Spring Boot liefert damit SPA und API aus derselben
Origin aus: kein CORS, ein Deployable. Siehe
[ADR 0007](docs/adr/0007-react-frontend-mit-backend-als-bff.md).

| Modus       | URL                     | Womit                                        |
|-------------|-------------------------|----------------------------------------------|
| Entwicklung | <http://localhost:5173> | `npm run dev` (proxyt `/api` auf Port 8080)  |
| Produktion  | <http://localhost:8080> | `java -jar build/libs/recipe-backend-*.jar`  |

Die TypeScript-Typen stammen aus dem OpenAPI-Contract und liegen eingecheckt in
`frontend/src/api/schema.d.ts`. Nach API-Änderungen neu erzeugen:

```bash
cd frontend && npm run generate:api   # benötigt ein laufendes Backend auf :8080
```

Die Response-DTOs markieren die vom Domänenmodell garantierten Felder mit
`@Schema(requiredMode = REQUIRED)`. Dadurch typisiert der Generator sie als
nicht-optional (`id: number` statt `id?: number`), während echte Nullable-Felder
(`description`, `servings`, `prepTimeMinutes`) optional bleiben.

## REST-API

Basis-URL: `/api/recipes`

| Methode | Pfad                | Beschreibung             | Erfolg |
|---------|---------------------|--------------------------|--------|
| POST    | `/api/recipes`      | Rezept anlegen           | 201    |
| GET     | `/api/recipes`      | Alle Rezepte auflisten   | 200    |
| GET     | `/api/recipes/{id}` | Einzelnes Rezept lesen   | 200    |
| PUT     | `/api/recipes/{id}` | Rezept ersetzen          | 200    |
| DELETE  | `/api/recipes/{id}` | Rezept löschen           | 204    |

Nicht gefundene Rezepte liefern `404`, Validierungsfehler `400` (jeweils als
`ErrorResponse`).

### API-Dokumentation (OpenAPI / Swagger)
Bei laufender Anwendung (Port `80`):

| Zweck               | URL                                        |
|---------------------|--------------------------------------------|
| Swagger UI          | <http://localhost/swagger-ui.html>         |
| OpenAPI 3.1 (JSON)  | <http://localhost/v3/api-docs>             |
| OpenAPI 3.1 (YAML)  | <http://localhost/v3/api-docs.yaml>        |

Der Contract wird zur Laufzeit aus Controller-Signaturen, Bean-Validation und den
`@Operation`/`@Schema`-Annotationen erzeugt – siehe
[ADR 0005](docs/adr/0005-springdoc-openapi-und-swagger-ui.md).

### Beispiel-Request (POST)
```json
{
  "title": "Spaghetti Carbonara",
  "description": "Klassische römische Pasta",
  "servings": 4,
  "prepTimeMinutes": 25,
  "difficulty": "MEDIUM",
  "ingredients": [
    { "name": "Spaghetti", "quantity": 500, "unit": "g" },
    { "name": "Eier", "quantity": 4, "unit": "Stk" }
  ],
  "steps": [
    { "instruction": "Nudeln kochen" },
    { "instruction": "Eier und Käse verrühren" }
  ]
}
```

## Datenbank
Drei Tabellen (`recipe`, `ingredient`, `preparation_step`), verwaltet über
Liquibase-Changelogs in `src/main/resources/db/changelog`. Zutaten und Schritte
werden mit `ON DELETE CASCADE` an das Rezept gebunden.

Konfiguration über Umgebungsvariablen (Defaults für lokale Entwicklung):
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` – siehe `application.yml`.

Lokale PostgreSQL z. B. so:
```bash
docker run --name recipes-db -e POSTGRES_DB=recipes \
  -e POSTGRES_USER=recipes -e POSTGRES_PASSWORD=recipes \
  -p 5432:5432 -d postgres:18
```

## Befehle
```bash
./gradlew clean build   # Frontend + Backend, alle Tests, JaCoCo-Report
./gradlew test          # nur Java-Tests
./gradlew frontendTest  # nur Vitest
./gradlew systemtest    # Systemtests gegen die laufende Anwendung
./gradlew e2eTest       # Playwright im echten Browser gegen das Boot-Jar
./gradlew bootRun       # Anwendung starten (benötigt laufende PostgreSQL)

# Ohne Node/npm bzw. für die schnelle Java-Schleife:
./gradlew build -PskipFrontend

# E2E gegen eine bereits laufende/deployte Instanz:
./gradlew e2eTest -Pe2e.baseUrl=http://localhost:8080

# Frontend-Entwicklung mit HMR (Backend muss auf :8080 laufen):
cd frontend && npm run dev

# Systemtests gegen eine bereits laufende/deployte Instanz:
./gradlew systemtest -Psystemtest.baseUrl=http://localhost:8080
```

## Tests
- **Unit** (`RecipeServiceImplTest`): Geschäftslogik mit Mockito/AssertJ.
- **Property-based** (`RecipeRestMapperPropertyTest`): Mapper-Invarianten mit jqwik.
- **Web-Slice** (`RecipeControllerTest`): REST-Schicht mit `@WebMvcTest`.
- **Contract** (`OpenApiDocumentationTest`): prüft, dass `/v3/api-docs` alle
  Operationen und Schemata beschreibt und die Swagger UI ausgeliefert wird.
- **Frontend** (`frontend/src/**/*.test.tsx`, Task `frontendTest`): Vitest +
  Testing Library für API-Client, Komponenten und die Zusammenschaltung in `App`.
- **E2E** (`frontend/e2e`, Task `e2eTest`): Playwright treibt einen echten Chromium
  gegen das **Boot-Jar** – also gegen das ausgelieferte Artefakt inklusive der darin
  verpackten SPA. Getestet werden ausschließlich Nutzer-Flows; Statuscodes und
  Contract-Details bleiben in den Systemtests. Benötigt eine erreichbare PostgreSQL.
  Siehe [ADR 0008](docs/adr/0008-playwright-fuer-e2e-tests.md).

  Alle Artefakte liegen unter **`build/e2e/`** und werden damit von `gradle clean` mit
  entfernt:

  | Artefakt | Ort | Wann |
  |---|---|---|
  | **Video der Durchführung** (WebM, 1280×800) | `build/e2e/test-results/<test>/video.webm` | jeder Test, jeder Lauf |
  | HTML-Report (verlinkt die Videos) | `build/e2e/report/index.html` | jeder Lauf |
  | Screenshot | `build/e2e/test-results/<test>/` | nur bei Fehlschlag |
  | Trace | `build/e2e/test-results/<test>/trace.zip` | nur bei Fehlschlag |

  ```bash
  cd frontend
  npx playwright show-report ../build/e2e/report      # Report inkl. eingebetteter Videos
  npx playwright show-trace ../build/e2e/test-results/<test>/trace.zip
  ```
- **System** (`src/systemtest/java`, Task `systemtest`): Black-Box-Tests gegen die
  **laufende** Anwendung, ausschließlich über HTTP. Ohne `-Psystemtest.baseUrl`
  startet die Suite die Anwendung selbst auf einem freien Port (embedded PostgreSQL),
  mit der Property läuft sie gegen eine deployte Instanz. Siehe
  [ADR 0006](docs/adr/0006-testsets-plugin-und-systemtests.md).
- **Integration** (`RecipeIntegrationTest`): kompletter Stack gegen ein echtes
  PostgreSQL. Genutzt wird **embedded PostgreSQL (Zonky)**, das ohne Docker
  auskommt; in Umgebungen mit Docker lässt sich stattdessen Testcontainers
  einsetzen. Kann das native Binary nicht starten, wird die Klasse automatisch
  übersprungen.
