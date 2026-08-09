rootProject.name = "recipe-backend"

// Ein Modul je Schicht, in Abhängigkeitsrichtung von innen nach außen (siehe ADR 0013).
// Die Reihenfolge hier ist Dokumentation; Gradle leitet die Richtung aus den
// project()-Abhängigkeiten der Module ab.
include("domain")
include("application")
include("adapter")
include("bootstrap")
