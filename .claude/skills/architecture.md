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
├── domain                     → Entities, Domain-Modelle, Value Objects
├── bootstrap                  → Spring Anwendung (main)
```

## Schichten & Abhängigkeitsregel
```
bootstrap → adapter  →  application  →  domain
```
Abhängigkeiten zeigen nach innen. Controller kennt keine Entities direkt.

Diese Regel ist **doppelt abgesichert**:

1. **Vom Build erzwungen** – jede Schicht ist ein eigenes Gradle-Modul
   (`:bootstrap` → `:adapter` → `:application` → `:domain`). Ein Import aus einer äußeren
   Schicht **compiliert nicht**; siehe
   [ADR 0013](../../docs/adr/0013-ein-gradle-modul-je-schicht.md).
2. **Von ArchUnit geprüft** – `LayeredArchitectureTest` sichert zusätzlich ab, was
   Modulgrenzen nicht sehen: Framework-Freiheit des Domänenmodells und „Ports sind
   Interfaces"; siehe
   [ADR 0012](../../docs/adr/0012-archunit-fuer-die-schichtenregel.md).

Fehlt einer Klasse eine Abhängigkeit, ist das ein Hinweis auf einen Schichtverstoß –
nicht die Aufforderung, sie im Buildskript zu ergänzen.

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