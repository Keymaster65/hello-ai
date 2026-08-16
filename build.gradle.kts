import org.asciidoctor.gradle.jvm.AsciidoctorTask
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask

// Root project: no sources of its own – it aggregates, and it renders the documentation that
// belongs to the repository as a whole.
//
// Alle Bausteine liegen unter `modules/` (docs/prompt/architektur.adoc):
//
//   :modules:backend   – die Anwendung, ein Untermodul je Schicht
//   modules/frontend   – npm-Projekt, von :modules:backend:bootstrap ins Boot-Jar gepackt
//
// Die gemeinsame Konfiguration der Schichtmodule steht in `modules/backend/build.gradle.kts`,
// also dort, wo sie gilt. Die Spring-Plugins stehen hier nur auf dem Buildscript-Classpath;
// angewendet werden sie in den Modulen.
//
// Die gewohnten Kommandos funktionieren unverändert, weil Gradle einen Task-Namen an jedes
// Projekt weiterleitet, das ihn kennt: ./gradlew clean build, test, systemtest, e2eTest.
//
// Versions and coordinates live solely in `gradle/libs.versions.toml`
// (docs/prompt/build.adoc); the build scripts reference them as `libs.<alias>`.
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false

    // Die Systemdokumentation beschreibt das Repository, nicht eine Schicht – sie liegt in
    // `docs/system` und gehört deshalb hierher und in kein Modul
    // (docs/prompt/systemdokumentation.adoc). Das ist das einzige Plugin, das im Wurzelprojekt
    // auch angewendet wird.
    alias(libs.plugins.asciidoctor.convert)

    // Liest die git-Historie und schreibt daraus die Tabelle für den gleichnamigen Anhang
    // (docs/prompt/systemdokumentation.adoc). Gehört aus demselben Grund hierher: Die Historie
    // beschreibt das Repository als Ganzes, nicht eine Schicht.
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
    // Diagramme entstehen beim Rendern aus ihrer Quelle im Dokument, nicht aus eingecheckten
    // Bilddateien (docs/prompt/systemdokumentation.adoc). Die Erweiterung bringt PlantUML
    // mit; sie gilt für beide Asciidoctor-Tasks, damit ein Diagramm überall stehen darf, wo
    // AsciiDoc steht.
    modules {
        diagram.use()
        diagram.setVersion(libs.versions.asciidoctorjDiagram.get())
    }

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
// `docs/prompt` und im Anhang die Quelldateien selbst. Ohne die Deklaration unten
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

// Die git-Historie als Tabelle für den gleichnamigen Anhang. Sie wird bei jedem Lauf
// neu erzeugt und *nicht* eingecheckt – anders als die Kennzahlen-Tabelle, deren
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
    // In einer Commit-Message steht beliebiger Text, und der darf den Build nicht brechen.
    //
    // Die Vorlage legt die *Gestalt* der Tabelle fest, auch die vierte Spalte „Prompt".
    // Deren Inhalt bleibt hier leer: Handlebars sieht nur die Commits, nicht das Session-Protokoll.
    // Gefüllt wird die Spalte – zusammen mit dem Anker je Commit – im `doLast` unten. Die Spalte
    // ist *nicht* literal, sonst blieben die Querverweise darin Text.
    templateContent.set(
        """
        // Erzeugt vom Gradle-Task `gitChangelog` aus der git-Historie – nicht von Hand ändern,
        // nicht eingecheckt. Eingebunden in docs/system/anhang-git-historie.adoc.

        [[git-historie-tabelle]]
        .Commits, neuester zuerst
        [cols="2,1m,6l,1", options="header"]
        |===
        | Datum | Commit | Betreff | Prompt
        {{#commits}}
        | {{commitTime}} | {{hash}} | {{{messageTitle}}} |
        {{/commits}}
        |===
        """.trimIndent(),
    )

    // Querverweise zwischen den Anhängen „Session-Protokoll" und „Git-Historie"
    // (docs/prompt/butterfly.adoc): Jede Zeile
    // bekommt den Anker `+commit-<kurzhash>+`, auf den das Protokoll zeigt, und in der Spalte
    // „Prompt" die Verweise zurück auf die Log-Einträge, die diesen Commit nennen.
    //
    // Die Zuordnung steht *im Protokoll*, nicht hier: Ein Log-Eintrag, aus dem ein Commit
    // hervorgegangen ist, nennt ihn in einer eigenen Zeile `+_Commit: …_+` (Butterfly-Skill).
    // Sie wird daraus abgeleitet und nirgends zweitgeführt – wie die Kennzahlen-Tabelle aus
    // denselben Einträgen entsteht.
    //
    // Gelesen wird *ausschließlich* diese Zeile, nicht der Fließtext. Ein Eintrag nennt Hashes
    // auch aus anderen Gründen – einen Pull-Bereich (`+e94f8f9..09d4158+`), den Stand eines
    // Remote-Branches, einen fremden Commit als Nebenbefund. Über den Fließtext gelesen behauptete
    // die Spalte, dieser Prompt habe jene Commits erzeugt; sie wäre überwiegend falsch. Erraten
    // wird nichts: Ein Commit, den keine solche Zeile nennt, bekommt `–`.
    val protokoll = file("docs/log/claudeLog.adoc")
    val ziel = gitChangelogAdoc.get().asFile
    inputs.file(protokoll).withPropertyName("sessionProtokoll")

    doLast {
        // `+== 131. Prompt: „ja, commit"+` – die Nummer ist der Anker `+prompt-131+`. Die vier
        // ältesten Einträge tragen eine negative Nummer, deshalb `+-?+`.
        val ueberschrift = Regex("""^== (-?\d+)\. """)
        // `+_Commit: <<commit-66a5ae4,66a5ae4>>_+`, bei mehreren durch Komma getrennt.
        val commitzeile = Regex("""^_Commit: (.+)_$""")
        val hashes = Regex("""\b[0-9a-f]{7,40}\b""")

        val zuordnung = linkedMapOf<String, MutableList<String>>()
        var eintrag: String? = null
        protokoll.forEachLine { zeile ->
            ueberschrift.find(zeile)?.let { eintrag = it.groupValues[1] }
            val nummer = eintrag ?: return@forEachLine
            val treffer = commitzeile.matchEntire(zeile) ?: return@forEachLine
            hashes.findAll(treffer.groupValues[1])
                .map { it.value.take(7) }
                .distinct()
                .forEach { kurz ->
                    val nummern = zuordnung.getOrPut(kurz) { mutableListOf() }
                    if (nummer !in nummern) nummern.add(nummer)
                }
        }

        // Die Kopfzeile passt nicht: „Commit" ist keine Hexadezimalzahl.
        val datenzeile = Regex("""^\| (.+?) \| ([0-9a-f]{7,40}) \| (.*) \|$""")
        var commits = 0
        var verknuepft = 0
        val zeilen = ziel.readLines().map { zeile ->
            val treffer = datenzeile.matchEntire(zeile) ?: return@map zeile
            val (zeit, hash, betreff) = treffer.destructured
            val kurz = hash.take(7)
            val prompts = zuordnung[kurz].orEmpty()
            commits++
            if (prompts.isNotEmpty()) verknuepft++
            // Die vier ältesten Einträge tragen eine negative Nummer. Ihr Anker heißt
            // `+prompt-minus1+`, nicht `+prompt--1+`: Asciidoctor ersetzt `+--+` durch einen
            // Geviertstrich, und der Verweis wird dann *still* zu einem Link auf ein anderes
            // Dokument – ohne Warnung, weil ein Dokumentverweis nicht geprüft wird.
            val spalte =
                if (prompts.isEmpty()) {
                    "–"
                } else {
                    prompts.joinToString(", ") { nr ->
                        "<<prompt-${nr.replace("-", "minus")},$nr>>"
                    }
                }
            "| $zeit | [[commit-$kurz]]$hash | $betreff | $spalte"
        }
        ziel.writeText(zeilen.joinToString("\n", postfix = "\n"))

        logger.lifecycle(
            "gitChangelog: $commits Commits, davon $verknuepft einem Log-Eintrag zugeordnet.",
        )
    }
}

// Die Ergebnisse der Testabdeckung als Tabelle für das Kapitel „Abdeckung"
// (docs/prompt/tests.adoc). Anders
// als die git-Historie oben entsteht die Tabelle *nicht* beim Rendern, sondern in einem eigenen,
// ausdrücklich aufgerufenen Schritt – und ihr Ergebnis wird eingecheckt, wie die
// Kennzahlen-Tabelle. Der Grund ist ein Kreis: `:bootstrap` packt die gerenderte
// Dokumentation in sein Jar, sein `processResources` hängt also am `asciidoctor`-Task
// des Wurzelprojekts. Hinge dieser umgekehrt an den Tests, die die Messung erzeugen, wäre der
// Task-Graph zyklisch. Eine Messung des laufenden Builds kann deshalb nicht im selben Build
// ausgeliefert werden.
val abdeckungTabelle = file("docs/system/abdeckung-tabelle.adoc")

// Von innen nach außen, in der Reihenfolge der Schichten – nicht
// alphabetisch, wie `subprojects` sie lieferte. Die Tabelle liest sich damit wie das Kapitel
// „Bausteinsicht".
val schichtmodule = listOf("domain", "application", "adapter", "bootstrap")

/** Die JaCoCo-Zähler eines Moduls: Typ → (verfehlt, abgedeckt). */
fun jacocoZaehler(bericht: File): Map<String, Pair<Int, Int>> {
    val fabrik = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    // Der Bericht verweist im DOCTYPE auf `report.dtd`. Ohne diese Zeile lädt der Parser sie
    // über das Netz nach – der Build hinge an der Erreichbarkeit von jacoco.org.
    fabrik.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    fabrik.isValidating = false
    val wurzel = fabrik.newDocumentBuilder().parse(bericht).documentElement
    // Nur die *direkten* Kinder des Wurzelelements zählen: Dieselben `counter`-Elemente stehen
    // auch in jedem Paket und jeder Klasse, und die summierten sich sonst mehrfach auf.
    return (0 until wurzel.childNodes.length)
        .map { wurzel.childNodes.item(it) }
        .filterIsInstance<org.w3c.dom.Element>()
        .filter { it.tagName == "counter" }
        .associate {
            it.getAttribute("type") to
                (it.getAttribute("missed").toInt() to it.getAttribute("covered").toInt())
        }
}

/** `98,7 % (1.234/1.250)` – Anteil und absolute Zahlen, weil eine Quote allein die Größe verschweigt. */
fun zelle(zaehler: Pair<Int, Int>?): String {
    val (verfehlt, abgedeckt) = zaehler ?: return "–"
    val gesamt = verfehlt + abgedeckt
    if (gesamt == 0) return "–"
    return String.format(
        java.util.Locale.GERMANY,
        "%.1f %% (%,d/%,d)",
        100.0 * abgedeckt / gesamt,
        abgedeckt,
        gesamt,
    )
}

tasks.register("coverageTable") {
    group = "documentation"
    description = "Schreibt die JaCoCo-Ergebnisse als Tabelle nach docs/system/abdeckung-tabelle.adoc."

    // Misst selbst, statt eine vorhandene Messung vorauszusetzen: Ein Aufruf, ein Stand. Die
    // Reports hängen ihrerseits am `test`-Task ihres Moduls (`modules/backend/build.gradle.kts`).
    dependsOn(schichtmodule.map { ":modules:backend:$it:jacocoTestReport" })

    // Die geschriebene Datei wird *nicht* als Ausgabe angemeldet, obwohl sie eine ist: Sie liegt
    // im Quellverzeichnis des `asciidoctor`-Tasks, und Gradle bräche jeden Lauf ab, in dem beide
    // Tasks vorkommen, ohne dass eine Kante zwischen ihnen deklariert ist. Genau die darf es
    // wegen des Kreises oben aber nicht geben. Der Task läuft dafür immer.
    outputs.upToDateWhen { false }

    val berichte = schichtmodule.associateWith { modul ->
        project(":modules:backend:$modul").layout.buildDirectory
            .file("reports/jacoco/test/jacocoTestReport.xml").get().asFile
    }
    val ziel = abdeckungTabelle

    doLast {
        // Vier Zähler von sechs: `COMPLEXITY` und `CLASS` sagen über die Prüfung wenig, was
        // Instruktionen und Zweige nicht schon sagen.
        val spalten = listOf(
            "INSTRUCTION" to "Instruktionen",
            "BRANCH" to "Zweige",
            "LINE" to "Zeilen",
            "METHOD" to "Methoden",
        )

        val gemessen = berichte.mapValues { (modul, bericht) ->
            if (!bericht.exists()) {
                throw GradleException(
                    "Kein JaCoCo-Bericht für :$modul unter ${bericht.path} – " +
                        "erst `./gradlew test` laufen lassen.",
                )
            }
            jacocoZaehler(bericht)
        }

        val summe = spalten.associate { (typ, _) ->
            typ to gemessen.values.fold(0 to 0) { stand, zaehler ->
                val (verfehlt, abgedeckt) = zaehler[typ] ?: (0 to 0)
                (stand.first + verfehlt) to (stand.second + abgedeckt)
            }
        }

        val zeilen = gemessen.map { (modul, zaehler) ->
            "| `:$modul` " + spalten.joinToString(" ") { (typ, _) -> "| ${zelle(zaehler[typ])}" }
        } + ("| *Summe* " + spalten.joinToString(" ") { (typ, _) -> "| ${zelle(summe[typ])}" })

        // Zeilenweise zusammengesetzt, nicht als `trimIndent()`-Block: Eine mehrzeilige
        // Einsetzung wird *vor* dem Trimmen aufgelöst, und ihre unbündigen Zeilen setzen die
        // gemeinsame Einrückung auf null – der Rahmen der Tabelle behielte seine Leerzeichen.
        val ausgabe = listOf(
            "// Erzeugt vom Gradle-Task `coverageTable` aus den JaCoCo-Berichten der",
            "// Backend-Module – nicht von Hand ändern. Eingebunden in",
            "// docs/system/qualitaetssicherung.adoc.",
            "",
            "[[abdeckung-tabelle]]",
            ".Abdeckung je Modul, gemessen beim letzten Lauf von `./gradlew coverageTable`",
            """[cols="1,>1,>1,>1,>1", options="header"]""",
            "|===",
            "| Modul | " + spalten.joinToString(" | ") { it.second },
            "",
        ) + zeilen + "|==="

        ziel.writeText(ausgabe.joinToString("\n", postfix = "\n"))

        logger.lifecycle("coverageTable: ${gemessen.size} Module nach ${ziel.path} geschrieben.")
    }
}

tasks.named<AsciidoctorTask>("asciidoctor") {
    group = "documentation"
    description = "Rendert die Systemdokumentation aus docs/system nach HTML."

    inputs.files(provider { includesAusserhalb(file("docs/system")) })
        .withPropertyName("eingebundeneDateienAusserhalb")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // Die Docinfo-Fußzeile macht das Inhaltsverzeichnis klappbar. Sie fällt durch
    // beide Netze: Der Task durchsucht `docs/system` nur nach AsciiDoc, und `includesAusserhalb`
    // sammelt ausdrücklich nur, was *außerhalb* liegt. Ohne diese Zeile bliebe er nach einer
    // Änderung an der Datei UP-TO-DATE und lieferte das alte Verzeichnis aus – nachgemessen.
    inputs.file(file("docs/system/docinfo-footer.html"))
        .withPropertyName("docinfoFusszeile")
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

    // Das Ausgabeverzeichnis ist in sich geschlossen: Alles, worauf das Dokument zeigt – die
    // erzeugten Diagramme –, liegt daneben und wird als Ganzes ins Boot-Jar kopiert. Deshalb
    // funktioniert das HTML auch außerhalb seines Bauplatzes.
    attributes(
        mapOf(
            // Ohne diese Angabe würde eine offene Attribut-Referenz stumm im Text stehen.
            "attribute-missing" to "warn",
        ),
    )
}

// Die Arbeitsgrundlage – Master-Prompt und Skills – liegt ebenfalls als AsciiDoc
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
            "attribute-missing" to "warn",
        ),
    )
}

tasks.named("build") {
    dependsOn(tasks.named("asciidoctor"), asciidoctorPrompt)
}
