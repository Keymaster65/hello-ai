# ADR 0020: Die Domäne in `model` und `services` teilen

## Status
Akzeptiert

## Kontext
Das Modul `:modules:backend:domain` hatte genau ein Package,
`io.github.keymaster65.helloai.domain`, und darin ausschließlich Modelltypen:
`Recipe`, `Ingredient`, `PreparationStep`, `Difficulty`.

Solange die Domäne nur aus Records besteht, trägt ein einzelnes Package. Sobald aber die
erste Regel auftaucht, die zu *keinem* einzelnen Typ gehört – etwa eine Prüfung über
Rezept **und** Zutatenliste hinweg –, gibt es keinen vorgesehenen Ort dafür. Erfahrungsgemäß
landet solche Logik dann im Application-Service, weil dort Platz ist. Damit wandert
Domänenwissen nach außen, und die innerste Schicht wird zur reinen Datenhaltung.

Auch die Architekturprüfung ist an dieser Stelle stumpfer als nötig: `OnionArchitectureTest`
kennt in ArchUnit die beiden getrennten Ringe `domainModels` und `domainServices`, nutzte
aber nur den ersten. Die Regel „ein Domain-Service darf das Modell verwenden, das Modell
aber nicht den Service" ließ sich mit einem einzigen Package gar nicht formulieren.

## Optionen
1. **Alles in einem Package `domain` lassen.** Nichts zu tun; die Frage stellt sich erst
   wieder beim ersten Domain-Service, dann aber unter Zeitdruck.
2. **`domain.model` anlegen, `domain.services` erst bei Bedarf.** Halber Schritt: der Ort
   für Domänenlogik bleibt unbenannt, und der nächste Autor entscheidet ihn neu.
3. **Beide Packages jetzt anlegen**, `domain.services` zunächst leer und mit
   `package-info.java` dokumentiert.
4. **Ein eigenes Gradle-Modul je Package.** Erzwingt die Richtung durch den Compiler,
   kostet aber ein weiteres Modul für aktuell null Klassen.

## Entscheidung
Option 3. Die vier Modelltypen liegen in `io.github.keymaster65.helloai.domain.model`,
daneben steht `io.github.keymaster65.helloai.domain.services`.

Das leere Package ist kein Platzhalter „auf Verdacht", sondern trägt eine Aussage: sein
`package-info.java` nennt die drei Bedingungen, unter denen eine Klasse dorthin gehört
(Domänenregel statt Anwendungsfall, mehr als ein Aggregat betroffen, frei von Frameworks).
Damit ist die Frage „wohin mit dieser Logik?" beantwortet, bevor sie gestellt wird.

Gegen Option 4 spricht der Schnitt der Module: sie bilden **Schichten** ab
(ADR 0013), nicht Packages innerhalb einer Schicht. Model und Services sind eine Schicht.

Zwei ArchUnit-Regeln sichern die Teilung ab:

- `OnionArchitectureTest` benennt beide Ringe (`domainModels` / `domainServices`) und prüft
  damit ab dem ersten Service die Richtung zwischen ihnen. `withOptionalLayers(true)` bleibt
  nötig, solange der Service-Ring leer ist.
- `LayeredArchitectureTest.domain_is_split_into_model_and_services` verhindert, dass eine
  Klasse direkt in `..domain` abgelegt wird und die Teilung damit stillschweigend aufhebt.

## Konsequenzen
**Vorteile**
- Domänenlogik hat einen benannten Ort; sie muss nicht mangels Alternative in den
  Application-Service ausweichen.
- Die Onion-Regel prüft ab sofort auch das Verhältnis Modell ↔ Domain-Service.
- Der Import zeigt die Rolle: `domain.model.Recipe` ist erkennbar ein Modelltyp.

**Nachteile**
- Ein leeres Package im Repository. Ohne das `package-info.java` wäre es Ballast – es ist
  die Dokumentation, die es rechtfertigt.
- Einmaliger Import-Bruch in allen Schichten (12 Dateien angepasst).
- `..domain..` in Regeln und Doku meint jetzt zwei Packages; wer nur `domain` liest, muss
  einmal genauer hinsehen.

## Referenzen
- [ADR 0012](0012-archunit-fuer-die-schichtenregel.md) – ArchUnit für die Schichtenregel
- [ADR 0013](0013-ein-gradle-modul-je-schicht.md) – ein Gradle-Modul je Schicht
- [ADR 0019](0019-onion-architecture-regel-in-archunit.md) – Onion-Architecture-Regel
