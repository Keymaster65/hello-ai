import org.asciidoctor.gradle.jvm.AsciidoctorTask
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask

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

    // Liest die git-Historie und schreibt daraus die Tabelle für den Anhang „Änderungen"
    // (ADR 0036). Gehört aus demselben Grund hierher: Die Historie beschreibt das Repository
    // als Ganzes, nicht eine Schicht.
    alias(libs.plugins.git.changelog)

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

// Die ADRs sind seit ADR 0026 ebenfalls AsciiDoc und werden gerendert, statt als Quelltext
// mitzureisen. Eigener Task, weil sie ein eigenes Quellverzeichnis haben und – anders als
// Systemdoku und Arbeitsgrundlage – *jede* Datei ein Eingangspunkt ist: 25 Dokumente, kein
// Masterdokument. Das Ergebnis wird vom `asciidoctor`-Task unten neben das HTML kopiert.
val asciidoctorAdr by tasks.registering(AsciidoctorTask::class) {
    group = "documentation"
    description = "Rendert die Architekturentscheidungen aus docs/adr nach HTML."

    setSourceDir(file("docs/adr"))
    baseDirFollowsSourceFile()
    // Kein `sources`-Filter: Jedes ADR ist ein eigenständiges Dokument und damit ein eigener
    // Eingangspunkt.
    setOutputDir(layout.buildDirectory.dir("docs/adr").get().asFile)
    outputOptions { backends("html5") }

    attributes(
        mapOf(
            // Die ADRs verweisen untereinander als `link:NNNN-titel{adrext}[…]`. Im Repository
            // steht `.adoc` (GitHub rendert es), im Ergebnis liegen die HTML-Dateien
            // nebeneinander – deshalb hier `.html`.
            "adrext" to ".html",
            // Ein Stylesheet neben 25 Dokumenten statt 25-mal eingebettet; sonst wüchse das
            // Boot-Jar um ein Vielfaches der eigentlichen Texte.
            "linkcss" to true,
            "copycss" to true,
            "attribute-missing" to "warn",
        ),
    )
}

// Die git-Historie als Tabelle für den Anhang „Änderungen" (ADR 0036). Sie wird bei jedem Lauf
// neu erzeugt und *nicht* eingecheckt – anders als die Kennzahlen-Tabelle (ADR 0027), deren
// Quelle im Repository liegt. Hier ist die Quelle das Repository selbst: Ein Commit kann sich
// nicht enthalten, eine eingecheckte Fassung wäre ab ihrem Entstehen um genau einen Commit alt.
val gitChangelogAdoc = layout.buildDirectory.file("docs/changelog/gitchangelog.adoc")

tasks.named<GitChangelogTask>("gitChangelog") {
    group = "documentation"
    description = "Schreibt die git-Historie als AsciiDoc-Tabelle für den Anhang „Änderungen\"."

    file.set(gitChangelogAdoc.get().asFile)
    outputs.file(gitChangelogAdoc)
    // Die Eingabe ist die git-Historie, nicht eine Datei, die Gradle beobachten könnte: Ein
    // `commit` ändert weder das Buildskript noch das Arbeitsverzeichnis. Der Task läuft deshalb
    // immer – er kostet Sekundenbruchteile. Ob das Ergebnis *neu* ist, entscheidet danach der
    // Asciidoctor-Task an seinem Eingabe-Hash.
    outputs.upToDateWhen { false }

    dateFormat.set("yyyy-MM-dd HH:mm")
    timeZone.set("Europe/Berlin")
    // Voreinstellung des Plugins ist `^Merge.*` – das verschluckt auch Commits, die nur mit dem
    // Wort „Merge" beginnen. Ausgelassen werden sollen die von git selbst erzeugten
    // Zusammenführungen, sonst nichts.
    ignoreCommitsIfMessageMatches.set(
        "^Merge branch .*|^Merge remote-tracking branch .*|^Merge pull request .*",
    )

    // Handlebars-Vorlage. `commits` ist die flache Liste aller Commits, neuester zuerst; das
    // Projekt kennt keine Tags, nach denen sich gruppieren ließe. Die Spalte „Betreff" ist
    // *literal* (`l`) – aus demselben Grund wie die Prompt-Spalte der Kennzahlen-Tabelle
    // (ADR 0027): In einer Commit-Message steht beliebiger Text, und der darf den Build nicht
    // brechen.
    templateContent.set(
        """
        // Erzeugt vom Gradle-Task `gitChangelog` aus der git-Historie – nicht von Hand ändern,
        // nicht eingecheckt. Eingebunden in docs/system/anhang-aenderungen.adoc (ADR 0036).

        [[git-historie-tabelle]]
        .Commits, neuester zuerst
        [cols="2,1m,6l", options="header"]
        |===
        | Datum | Commit | Betreff
        {{#commits}}
        | {{commitTime}} | {{hash}} | {{{messageTitle}}}
        {{/commits}}
        |===
        """.trimIndent(),
    )
}

tasks.named<AsciidoctorTask>("asciidoctor") {
    group = "documentation"
    description = "Rendert die Systemdokumentation aus docs/system nach HTML."

    inputs.files(provider { includesAusserhalb(file("docs/system")) })
        .withPropertyName("eingebundeneDateienAusserhalb")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // Der Anhang „Änderungen" bindet die erzeugte Tabelle ein; ohne diese Kante stünde
    // Asciidoctor vor einem fehlenden `include` – und der ist als Build-Fehler konfiguriert.
    dependsOn(tasks.named("gitChangelog"))

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
    // in sich geschlossen und überall gültig, auch im Boot-Jar. Kopiert wird seit ADR 0026
    // das *gerenderte* HTML aus `asciidoctorAdr`, nicht mehr der Quelltext.
    dependsOn(asciidoctorAdr)
    resources {
        from(asciidoctorAdr.map { it.outputDir })
        into("adr")
    }

    attributes(
        mapOf(
            // Verweise auf die ADRs. In `docs/system` gilt `../adr`; im Ergebnis liegen sie
            // dank des `resources`-Blocks oben direkt daneben – deshalb wird das Attribut hier
            // überschrieben, statt es im Dokument zu verbiegen.
            "adr" to "adr",
            // Dort liegen sie als HTML (ADR 0026); im Repository ist es `.adoc`.
            "adrext" to ".html",
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
            // Dieser Pfad zeigt ins Repository, nicht in ein Ausgabeverzeichnis – dort liegen
            // die ADRs als Quelltext. Der Wert ist der Vorgabewert der Dateien; er steht hier,
            // damit die Wahl sichtbar ist und nicht nur implizit gilt.
            "adrext" to ".adoc",
            "attribute-missing" to "warn",
        ),
    )
}

tasks.named("build") {
    dependsOn(tasks.named("asciidoctor"), asciidoctorPrompt)
}
