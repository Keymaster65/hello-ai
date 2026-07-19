# 0001 – jOOQ-Codegenerierung aus Flyway-Skripten (DDLDatabase)

Status: abgelöst durch [ADR 0004](0004-liquibase-statt-flyway.md) (2026-07-18)
Datum: 2026-07-18

> **Hinweis:** Das Grundprinzip (Codegen ohne laufende DB, Migrationsskripte als
> Single Source of Truth) gilt weiterhin. Ausschließlich das eingesetzte
> Migrationswerkzeug wurde von Flyway/`DDLDatabase` auf Liquibase/`LiquibaseDatabase`
> umgestellt – siehe ADR 0004.

| Abschnitt    | Inhalt |
|--------------|--------|
| **Kontext**  | jOOQ benötigt für seine typsicheren Zugriffsklassen ein Schema als Quelle. Üblich sind (a) Generierung gegen eine laufende Datenbank oder (b) gegen ein aus Flyway-Migrationen abgeleitetes Schema. Der Build soll reproduzierbar sein und darf beim reinen Kompilieren keine laufende PostgreSQL-Instanz oder Docker voraussetzen. |
| **Optionen** | 1. Codegen gegen eine live PostgreSQL (lokal/CI). 2. Codegen gegen eine zur Build-Zeit hochgefahrene Container-/Embedded-DB. 3. Codegen aus den Flyway-`.sql`-Skripten via `org.jooq.meta.extensions.ddl.DDLDatabase` (jOOQ interpretiert die DDL, ohne DB). |
| **Entscheidung** | Option 3. `DDLDatabase` liest `src/main/resources/db/migration/*.sql` (Sortierung `flyway`) und erzeugt die Klassen unter `build/generated-src/jooq/main`. jOOQ-Version ist explizit auf 3.21.6 gepinnt (`ext['jooq.version']`), passend zum Plugin `nu.studer.jooq` 10.2.1. |
| **Konsequenzen** | **+** Kein DB-/Docker-Zwang zur Build-Zeit, deterministisch, schnell. **+** Flyway-Skripte sind einzige Schemaquelle (Single Source of Truth), Migration und Codegen können nicht auseinanderlaufen. **−** DDL muss vom jOOQ-Parser verstanden werden (PostgreSQL-Spezialitäten ggf. eingeschränkt). **−** Feinheiten des echten PostgreSQL-Typmappings werden erst im Integrationstest ([ADR 0002](0002-embedded-postgres-statt-testcontainers.md)) verifiziert. |
