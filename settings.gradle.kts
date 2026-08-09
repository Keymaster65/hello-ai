rootProject.name = "recipe-backend"

// Alle Bausteine liegen unter `modules/` (siehe ADR 0015): die Anwendung als :modules:backend
// mit einem Untermodul je Schicht, daneben `modules/frontend` als npm-Projekt, das
// :modules:backend:bootstrap ins Boot-Jar packt.
//
// Die Reihenfolge hier dokumentiert die Abhängigkeitsrichtung; erzwungen wird sie durch die
// project()-Abhängigkeiten der Module.
include("modules:backend:domain")
include("modules:backend:application")
include("modules:backend:adapter")
include("modules:backend:bootstrap")
