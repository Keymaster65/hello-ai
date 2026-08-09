rootProject.name = "recipe-backend"

// Die Schichten liegen als Untermodule im Modul :backend (siehe ADR 0014); daneben steht
// `frontend/` als npm-Projekt, das :backend:bootstrap ins Boot-Jar packt.
// Die Reihenfolge hier ist Dokumentation der Abhängigkeitsrichtung; erzwungen wird sie
// durch die project()-Abhängigkeiten der Module.
include("backend:domain")
include("backend:application")
include("backend:adapter")
include("backend:bootstrap")
