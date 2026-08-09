// Root project: it carries no sources of its own. The application lives in :backend
// (see ADR 0011), the web client in frontend/ as an npm project that :backend bundles.
//
// The usual commands keep working unchanged, because Gradle forwards a task name to every
// project that has it: ./gradlew clean build, test, systemtest, e2eTest, frontendTest.
