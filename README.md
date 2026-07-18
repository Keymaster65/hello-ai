# Recipe Backend

Backend zur Verwaltung von Rezepten über eine REST-API mit Persistenz in PostgreSQL.

## Tech-Stack
- Java 25, Spring Boot 4.1
- jOOQ (typsicherer SQL-Zugriff), Flyway (Migrationen), PostgreSQL
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

Der jOOQ-Code wird beim Build aus den Flyway-Skripten generiert
(`org.jooq.meta.extensions.ddl.DDLDatabase`) – es wird dafür **keine laufende
Datenbank** benötigt.

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
Flyway-Migrationen in `src/main/resources/db/migration`. Zutaten und Schritte
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
./gradlew bootRun       # Anwendung starten (benötigt laufende PostgreSQL)
```

## Tests
- **Unit** (`RecipeServiceImplTest`): Geschäftslogik mit Mockito/AssertJ.
- **Property-based** (`RecipeRestMapperPropertyTest`): Mapper-Invarianten mit jqwik.
- **Web-Slice** (`RecipeControllerTest`): REST-Schicht mit `@WebMvcTest`.
- **Integration** (`RecipeIntegrationTest`): kompletter Stack gegen ein echtes
  PostgreSQL. Genutzt wird **embedded PostgreSQL (Zonky)**, das ohne Docker
  auskommt; in Umgebungen mit Docker lässt sich stattdessen Testcontainers
  einsetzen. Kann das native Binary nicht starten, wird die Klasse automatisch
  übersprungen.
