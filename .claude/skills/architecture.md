# Skill: Java-Architektur

## Zweck
Aktiviere diesen Skill bei Design-Entscheidungen, neuen Modulen,
Refactorings oder Struktur-Fragen.

## Prinzipien
- **Layered / Clean Architecture**
- **SOLID-Prinzipien**
- **Dependency Injection** (Konstruktor-Injektion)
- **Separation of Concerns**

## Package-Struktur
```
io.github.keymaster65.helloai
├── adapter.in.rest            → REST-Endpoints (Controller)
├── adapter.out.persistenz     → Datenzugriff (Infrastructure)
├── application.port.in        → Business-Logik eingehende ports
├── application.port.out       → Business-Logik ausgehende ports
├── application.service        → Business-Logik (Application)
├── domain.model               → Entities, Domain-Modelle, Value Objects
├── domain.services            → Domänenlogik ohne eigenen Modelltyp (ADR 0020)
├── bootstrap                  → Spring Anwendung (main)
```

## Schichten & Abhängigkeitsregel
```
bootstrap → adapter  →  application  →  domain
```
Abhängigkeiten zeigen nach innen. Controller kennt keine Entities direkt.

Diese Regel ist **doppelt abgesichert**:

1. **Vom Build erzwungen** – jede Schicht ist ein eigenes Gradle-Modul unter `:modules:backend`
   (`:modules:backend:bootstrap` → `:modules:backend:adapter` → `:modules:backend:application` →
   `:modules:backend:domain`). Ein Import aus einer äußeren Schicht **compiliert nicht**; siehe
   [ADR 0013](../../docs/adr/0013-ein-gradle-modul-je-schicht.md) und
   [ADR 0014](../../docs/adr/0014-schichtmodule-unter-backend.md) sowie
   [ADR 0015](../../docs/adr/0015-alle-bausteine-unter-modules.md).
2. **Von ArchUnit geprüft** – `LayeredArchitectureTest` sichert zusätzlich ab, was
   Modulgrenzen nicht sehen: Framework-Freiheit des Domänenmodells und „Ports sind
   Interfaces"; siehe
   [ADR 0012](../../docs/adr/0012-archunit-fuer-die-schichtenregel.md).
   `OnionArchitectureTest` beschreibt dieselbe Struktur als Zwiebelschalen und schließt
   die Lücke der Schichtensicht: **Adapter kennen einander nicht** – der REST-Adapter
   greift nicht am Port vorbei auf die Persistenz zu. Siehe
   [ADR 0019](../../docs/adr/0019-onion-architecture-regel-in-archunit.md).

Fehlt einer Klasse eine Abhängigkeit, ist das ein Hinweis auf einen Schichtverstoß –
nicht die Aufforderung, sie im Buildskript zu ergänzen.

## Records: Currying ab drei Komponenten
Jeder `record` mit **mehr als zwei** Komponenten bietet eine curried Factory
`curried()` – eine Kette einstelliger Schritte, je einer pro Komponente, benannt
wie die Komponente. Damit erzwingt der Compiler Reihenfolge und Vollständigkeit,
und die Namen stehen an der Aufrufstelle statt nur in der Deklaration.

```java
public record Ingredient(String name, BigDecimal quantity, String unit) {

    public static NameStep curried() {
        return name -> quantity -> unit -> new Ingredient(name, quantity, unit);
    }

    @FunctionalInterface
    public interface NameStep {
        QuantityStep name(String name);
    }
    // QuantityStep → UnitStep → Ingredient
}

Ingredient ingredient = Ingredient.curried()
        .name("Spaghetti")
        .quantity(BigDecimal.valueOf(500))
        .unit("g");
```

Der kanonische Konstruktor bleibt der einzige Ort der Validierung; die Kette ruft
ihn im letzten Schritt auf. `RecordCurryingTest` prüft die Form der Kette – nicht
nur, dass es eine Methode `curried()` gibt. Begründung und Grenzen:
[ADR 0021](../../docs/adr/0021-currying-fuer-records-mit-mehr-als-zwei-komponenten.md).

## Checkliste vor Implementierung
1. Welche Schicht ist betroffen?
2. Gibt es ein Interface für den Service/Repository?
3. Ist die Komponente per Mock testbar?
4. Werden Entities über DTOs von der API entkoppelt?
5. Ist Exception-Handling zentral (@ControllerAdvice)?

## Beispiel: sauberer Service
```java
@Service
@RequiredArgsConstructor   // Lombok: Konstruktor-Injektion
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDto findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

## Beispiel: Controller (dünn, ohne Business-Logik)
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

## Beispiel: zentrales Exception-Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            UserNotFoundException ex) {
        var error = new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                                      ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

## Anti-Patterns vermeiden
- Business-Logik im Controller
- @Autowired auf Feldern (nutze Konstruktor-Injektion)
- Entities direkt als API-Response zurückgeben
- Fat Services / God-Classes
- Zirkuläre Abhängigkeiten zwischen Komponenten
- Feld-Injektion erschwert Testbarkeit

## Bei größeren Entscheidungen
Kurzes **ADR (Architecture Decision Record)** erstellen:

| Abschnitt      | Inhalt                                   |
|----------------|------------------------------------------|
| Kontext        | Warum ist die Entscheidung nötig?        |
| Optionen       | Welche Alternativen gibt es?             |
| Entscheidung   | Was wird gewählt und warum?              |
| Konsequenzen   | Vor- und Nachteile der Wahl              |

Speicherort: `docs/adr/NNNN-titel.md`