# Recipe Backend

Backend zur Verwaltung von Rezepten über eine REST-API mit Persistenz in PostgreSQL.

## Tech-Stack
- Java 25, Spring Boot 4.1
- jOOQ (typsicherer SQL-Zugriff), Liquibase (Migrationen), PostgreSQL
- springdoc-openapi 3.1 (OpenAPI 3.1 + Swagger UI)
- Tests: JUnit 5, Mockito, AssertJ, jqwik, JaCoCo

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
./gradlew clean build   # kompilieren + alle Tests + JaCoCo-Report
./gradlew test          # nur Tests
./gradlew systemtest    # Systemtests gegen die laufende Anwendung
./gradlew bootRun       # Anwendung starten (benötigt laufende PostgreSQL)

# Systemtests gegen eine bereits laufende/deployte Instanz:
./gradlew systemtest -Psystemtest.baseUrl=http://localhost:8080
```

## Tests
- **Unit** (`RecipeServiceImplTest`): Geschäftslogik mit Mockito/AssertJ.
- **Property-based** (`RecipeRestMapperPropertyTest`): Mapper-Invarianten mit jqwik.
- **Web-Slice** (`RecipeControllerTest`): REST-Schicht mit `@WebMvcTest`.
- **Contract** (`OpenApiDocumentationTest`): prüft, dass `/v3/api-docs` alle
  Operationen und Schemata beschreibt und die Swagger UI ausgeliefert wird.
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
