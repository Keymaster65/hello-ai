rootProject.name = "recipe-backend"

// The BFF lives in its own module (see ADR 0011); `frontend/` stays an npm project that
// :backend builds and packages into the Boot jar.
include("backend")
