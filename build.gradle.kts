import org.asciidoctor.gradle.jvm.AsciidoctorTask

// Root project: no sources of its own – it aggregates, and it renders the documentation that
// belongs to the repository as a whole.
//
// Alle Bausteine liegen unter `modules/` (ADR 0015):
//
//   :modules:backend   – die Anwendung, ein Untermodul je Schicht (ADR 0013, ADR 0014)
//   modules/frontend   – npm-Projekt, von :modules:backend:bootstrap ins Boot-Jar gepackt
//
// Die gemeinsame Konfiguration der Schichtmodule steht in `modules/backend/build.gradle.kts`,
// also dort, wo sie gilt. Die Spring-Plugins stehen hier nur auf dem Buildscript-Classpath;
// angewendet werden sie in den Modulen.
//
// Die gewohnten Kommandos funktionieren unverändert, weil Gradle einen Task-Namen an jedes
// Projekt weiterleitet, das ihn kennt: ./gradlew clean build, test, systemtest, e2eTest.
//
// Versions and coordinates live solely in `gradle/libs.versions.toml` (ADR 0018); the build
// scripts reference them as `libs.<alias>`.
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false

    // Die Systemdokumentation beschreibt das Repository, nicht eine Schicht – sie liegt in
    // `docs/system` und gehört deshalb hierher und in kein Modul (ADR 0022). Das ist das
    // einzige Plugin, das im Wurzelprojekt auch angewendet wird.
    alias(libs.plugins.asciidoctor.convert)

    // Liefert `clean` und `build` im Wurzelprojekt, damit die Doku am gewohnten Befehl hängt
    // und `./gradlew clean` das gerenderte Ergebnis wieder entfernt.
    base
}

// Das Wurzelprojekt brauchte bisher keine Repositories, weil es keine Abhängigkeiten auflöste
// (die der Module stehen in `modules/backend/build.gradle.kts`). Asciidoctor lädt seine
// Laufzeit selbst nach – deshalb hier, und nur deshalb.
repositories {
    mavenCentral()
}

asciidoctorj {
    // Asciidoctor meldet einen kaputten Querverweis nur als INFO. Ohne diese Stufe würde die
    // Meldung nicht erfasst und die Regel unten nie greifen.
    logLevel = LogLevel.INFO

    // Ein fehlender `include` und ein ins Leere zeigender Querverweis sind Build-Fehler, keine
    // Log-Zeilen: sonst rendert die Doku weiter und verliert still ein Kapitel oder einen
    // Verweis. Genau das ist der Verfall, den eine mitgepflegte Doku vermeiden soll.
    fatalWarnings(missingIncludes(), java.util.regex.Pattern.compile("possible invalid reference"))
}

// Asciidoctor löst `include::` selbst auf – Gradle sieht davon nichts. Für Includes innerhalb des
// Quellverzeichnisses genügt das, weil der Task es ohnehin als Eingabe kennt. Die
// Systemdokumentation bindet aber auch Dateien von *außerhalb* ein: die Arbeitsgrundlage aus
// `docs/prompt` (ADR 0023) und im Anhang die Quelldateien selbst. Ohne die Deklaration unten
// bliebe der Task nach einer Codeänderung UP-TO-DATE und der Anhang zeigte einen alten Stand –
// nachgemessen an einem `touch` auf `Recipe.java`.
fun includesAusserhalb(quellverzeichnis: File): List<File> =
    quellverzeichnis.walkTopDown()
        .filter { it.extension == "adoc" }
        .flatMap { datei ->
            Regex("""^include::([^\[\n]+)\[""", RegexOption.MULTILINE)
                .findAll(datei.readText())
                .map { treffer -> datei.parentFile.resolve(treffer.groupValues[1]).normalize() }
        }
        .filter { !it.startsWith(quellverzeichnis) }
        .distinct()
        .toList()

tasks.named<AsciidoctorTask>("asciidoctor") {
    group = "documentation"
    description = "Rendert die Systemdokumentation aus docs/system nach HTML."

    inputs.files(provider { includesAusserhalb(file("docs/system")) })
        .withPropertyName("eingebundeneDateienAusserhalb")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    setSourceDir(file("docs/system"))
    // Ohne das löst Asciidoctor `include::` gegen das Projektverzeichnis auf, nicht gegen die
    // Datei mit der Direktive – die Kapitel würden dann in `/` gesucht.
    baseDirFollowsSourceFile()
    // Nur das Masterdokument ist ein Eingangspunkt; die Kapitel kommen über `include`.
    // Ohne diese Einschränkung würde jedes Kapitel zusätzlich als eigene Seite gerendert.
    sources { include("system.adoc") }
    outputOptions { backends("html5") }

    // Die Kapitel verlinken die ADRs, die ADRs selbst liegen aber außerhalb des Ergebnisses.
    // Bisher hieß das: Das HTML funktioniert nur an seinem Bauplatz, weil das Attribut `adr`
    // ins Repository zurückzeigte. Seit die Doku mit ausgeliefert wird (ADR 0024), kopiert der
    // Task sie in einen Unterordner `adr/` neben das HTML – damit ist das Ausgabeverzeichnis
    // in sich geschlossen und überall gültig, auch im Boot-Jar.
    resources {
        from(file("docs/adr")) {
            include("*.md")
        }
        into("adr")
    }

    attributes(
        mapOf(
            // Verweise auf die ADRs. In `docs/system` gilt `../adr`; im Ergebnis liegen sie
            // dank des `resources`-Blocks oben direkt daneben – deshalb wird das Attribut hier
            // überschrieben, statt es im Dokument zu verbiegen.
            "adr" to "adr",
            // Ohne diese Angabe würde eine offene Attribut-Referenz stumm im Text stehen.
            "attribute-missing" to "warn",
        ),
    )
}

// Die Arbeitsgrundlage – Master-Prompt und Skills – liegt seit ADR 0023 ebenfalls als AsciiDoc
// im Repository (`docs/prompt`) und bekommt deshalb dieselbe Zusicherung wie die
// Systemdokumentation: kaputte Includes und Querverweise brechen den Build. Die `asciidoctorj`-
// Konfiguration oben gilt für beide Tasks.
//
// Eigener Task statt eines zweiten Eingangspunkts im bestehenden `asciidoctor`: Die beiden
// Dokumente haben verschiedene Quellverzeichnisse, und getrennte Tasks lassen sich einzeln
// aufrufen. Das Ausgabeverzeichnis liegt bewusst *neben* `build/docs/asciidoc` und nicht darin –
// überlappende Task-Ausgaben verwirren Gradles Up-to-date-Prüfung.
val asciidoctorPrompt by tasks.registering(AsciidoctorTask::class) {
    group = "documentation"
    description = "Rendert Master-Prompt und Skills aus docs/prompt nach HTML."

    setSourceDir(file("docs/prompt"))
    baseDirFollowsSourceFile()
    // Nur das Masterdokument ist ein Eingangspunkt; Master-Prompt und Skills kommen über
    // `include`.
    sources { include("prompt.adoc") }
    setOutputDir(layout.buildDirectory.dir("docs/prompt").get().asFile)
    outputOptions { backends("html5") }

    attributes(
        mapOf(
            // In `docs/prompt` gilt `../adr`; im gerenderten HTML unter `build/docs/prompt` muss
            // der Pfad drei Ebenen zurück.
            "adr" to "../../../docs/adr",
            "attribute-missing" to "warn",
        ),
    )
}

tasks.named("build") {
    dependsOn(tasks.named("asciidoctor"), asciidoctorPrompt)
}
