# ADR 0021: Currying für Records mit mehr als zwei Komponenten

## Status
Akzeptiert – 2026-08-10

## Kontext
Das Projekt baut seine Datentypen konsequent als `record` (ADR 0003, bewusst ohne Lombok).
Damit bleibt als Konstruktionsweg der kanonische Konstruktor – eine rein **positionsbasierte**
Argumentliste.

Das trägt bei zwei Komponenten. Bei `Recipe` sind es acht:

```java
new Recipe(id, title, description, servings, prepTimeMinutes, difficulty, ingredients, steps);
```

`title` und `description` sind beide `String`, `servings` und `prepTimeMinutes` beide `Integer`
– und sie stehen jeweils nebeneinander. Wer die beiden vertauscht, bekommt **keinen**
Compilerfehler: Ein Rezept mit 25 Portionen und 4 Minuten Zubereitungszeit ist ein reiner
Laufzeitfehler, den erst ein Test oder ein Nutzer findet. Dasselbe gilt für `RecipeRequest`,
`RecipeResponse` und `ErrorResponse` (dort `error` und `message`, beide `String`).

Hinzu kommt die Lesbarkeit an der Aufrufstelle: `new Recipe(null, "Carbonara", null, 4, 25, …)`
zwingt zum Nachschlagen der Deklaration, weil die Namen der Komponenten im Aufruf nicht
vorkommen.

## Optionen
1. **Nichts ändern.** Der kanonische Konstruktor bleibt der einzige Weg. Kostenlos, aber die
   Vertauschung gleichartiger Nachbarn bleibt unentdeckbar für den Compiler.
2. **Klassischer Builder** (`Recipe.builder().title(…).build()`). Lesbar und verbreitet, aber
   `build()` kann jederzeit mit halb gefülltem Zustand aufgerufen werden – die Vollständigkeit
   prüft erst die Laufzeit. Außerdem ein veränderlicher Zwischenzustand in einem Entwurf, der
   sonst durchgängig unveränderlich ist.
3. **Klassisches Currying über `java.util.function.Function`**, also
   `Function<Long, Function<String, …>>` und `apply(…).apply(…)`. Erzwingt Reihenfolge und
   Vollständigkeit über Typen, überträgt aber die Namen der Komponenten nicht an die
   Aufrufstelle: `.apply("Carbonara").apply(null)` liest sich nicht besser als der Konstruktor.
   Bei acht Komponenten wird zusätzlich der Rückgabetyp unlesbar.
4. **Currying mit benannten Step-Interfaces**: pro Komponente ein `@FunctionalInterface` mit
   genau einer Methode, die nach der Komponente heißt und den nächsten Schritt liefert.
5. **Mehrere statische `of(…)`-Factories** je Anwendungsfall. Löst das Namensproblem nicht und
   vervielfacht die Überladungen.

## Entscheidung
**Option 4 für jeden Record mit mehr als zwei Komponenten** – im gesamten Backend, also Domäne
*und* Adapter-DTOs.

```java
public static NameStep curried() {
    return name -> quantity -> unit -> new Ingredient(name, quantity, unit);
}

Ingredient ingredient = Ingredient.curried()
        .name("Spaghetti")
        .quantity(BigDecimal.valueOf(500))
        .unit("g");
```

Es ist echtes Currying: eine Kette einstelliger Funktionen. Die Step-Interfaces geben ihr nur
sprechende Methodennamen statt `apply`. Daraus folgen drei Eigenschaften, die Option 2 und 3
nicht zusammen haben:

- **Der Compiler erzwingt Reihenfolge und Vollständigkeit.** Jeder Schritt hat genau einen
  Nachfolgetyp; ein `Recipe` entsteht ausschließlich am Ende der vollständigen Kette. Ein
  vergessener Schritt ist ein Compilerfehler, kein Laufzeitverhalten.
- **Die Namen stehen im Aufruf.** `.servings(4).prepTimeMinutes(25)` ist an der Aufrufstelle
  prüfbar, `new Recipe(…, 4, 25, …)` nicht.
- **Teilanwendung bleibt möglich.** Ein Zwischenschritt ist ein Wert und mehrfach verwendbar –
  eine bis zur `difficulty` gefüllte Kette erzeugt beliebig viele Rezepte, ohne kopierten Code.

**Die Grenze liegt bei zwei Komponenten.** Bis dahin ist die Argumentliste kurz genug, dass die
Deklaration beim Lesen präsent ist, und eine Vertauschung betrifft höchstens ein Paar. Ab drei
wächst die Zahl der möglichen Vertauschungen schneller als die Aufmerksamkeit. Betroffen sind
damit `Recipe` (8), `RecipeResponse` (8), `RecipeRequest` (7), `ErrorResponse` (4),
`Ingredient` (3) und `IngredientDto` (3); nicht betroffen sind `PreparationStep`,
`PreparationStepResponse`, `ErrorResponse.FieldError` (je 2) und `PreparationStepDto` (1).

Die Regel gilt bewusst **auch für die DTOs**, obwohl Jackson und springdoc dort weiter den
kanonischen Konstruktor verwenden. Eine Konvention, die je Schicht anders lautet, muss bei jeder
neuen Klasse neu entschieden werden; eine, die überall gilt, nicht. Den Nutzen zieht der Code,
der die DTOs von Hand baut – vor allem die Tests.

Der kanonische Konstruktor bleibt öffentlich und bleibt der einzige Ort der Validierung: Die
Kette ruft ihn im letzten Schritt auf, `curried()` ist damit kein zweiter Konstruktionsweg mit
eigenen Regeln, sondern eine typisierte Fassade davor.

Abgesichert wird das durch `RecordCurryingTest` (ArchUnit), passend zu ADR 0012 und ADR 0019.
Die Regel prüft nicht nur, dass eine Methode `curried()` existiert, sondern die **Form der
Kette**: Schritt *n* ist ein Interface mit genau einer Methode, diese heißt wie Komponente *n*,
nimmt genau ein Argument von deren Typ, und der letzte Schritt liefert den Record. Eine
`curried()`-Methode, die den Namen trägt, aber nicht das Versprechen einlöst, fällt damit auf.

## Konsequenzen
**Vorteile**
- Vertauschte Argumente gleichen Typs werden zum Compilerfehler statt zum Testfall.
- Die Aufrufstelle nennt die Komponenten; die Deklaration muss man nicht danebenlegen.
- Teilweise angewandte Ketten sind wiederverwendbare Werte – nützlich für Testdaten.
- Kein veränderlicher Zwischenzustand, anders als beim Builder; die Immutability-Regel aus
  `CLAUDE.md` bleibt unangetastet.
- Die Konvention ist maschinell geprüft, nicht nur dokumentiert.

**Nachteile**
- Deutlich mehr Code im Record: `Recipe` trägt acht Step-Interfaces. Das ist der Preis dafür,
  dass die Namen im Typsystem stehen – ohne Codegenerierung gibt es ihn nicht billiger.
- Die Schrittfolge ist starr. Wer nur `title` und `difficulty` setzen will, muss die optionalen
  Komponenten explizit mit `null` durchreichen. Das ist gewollt (Vollständigkeit statt
  Vergessen), aber unbequemer als ein Builder.
- Bei einer neuen Record-Komponente sind Kette **und** Interfaces mitzuziehen. Vergisst man es,
  schlägt der Compiler zu; ändert sich nur die Reihenfolge, die ArchUnit-Regel.
- Die DTOs tragen Steps, die im Produktionspfad niemand aufruft – Jackson deserialisiert über
  den kanonischen Konstruktor.
- Die Grenze „mehr als zwei" ist an ihrem Rand willkürlich: `Ingredient` mit drei Komponenten
  bekommt die Kette, `PreparationStep` mit zweien nicht.

**Bewusst nicht getan**
- Bestehende Aufrufstellen wurden **nicht** umgestellt (`CLAUDE.md`, Regel 4: keine ungefragten
  Refactorings). `curried()` steht additiv neben dem Konstruktor; die Migration von Mappern und
  Tests ist eine eigene Entscheidung.
- Es gibt keine Codegenerierung für die Step-Interfaces. Bei der aktuellen Zahl von Records
  wiegt ein Annotation-Processor schwerer als der Boilerplate, den er spart – siehe ADR 0003.

## Referenzen
- [ADR 0003](0003-kein-lombok-records-und-konstruktor-injektion.md) – kein Lombok, Records
- [ADR 0012](0012-archunit-fuer-die-schichtenregel.md) – ArchUnit für die Schichtenregel
- [ADR 0019](0019-onion-architecture-regel-in-archunit.md) – Onion-Architecture-Regel
- [ADR 0020](0020-domain-in-model-und-services-teilen.md) – Domäne in model und services teilen
