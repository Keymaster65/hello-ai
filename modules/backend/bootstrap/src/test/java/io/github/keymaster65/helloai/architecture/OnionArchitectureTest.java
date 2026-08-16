package io.github.keymaster65.helloai.architecture;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Describes the same structure as {@link LayeredArchitectureTest}, but as the rings of an onion
 * instead of a stack of layers (see ADR 0019).
 *
 * <p>The stacked view says which layer may be accessed by which. The onion view adds what a stack
 * cannot express: the outermost ring is not a layer but a set of <em>adapters</em> that are
 * mutually unaware. REST and persistence both sit outside the application ring, and neither may
 * reach for the other &ndash; a fact that {@code layeredArchitecture()} has no vocabulary for,
 * because both would be the same layer {@code ..adapter..}.
 *
 * <p>Ring assignment:
 *
 * <pre>
 * domain model        ..domain.model..              (records and enums)
 * domain service      ..domain.services..           (currently empty, see ADR 0020)
 * application service ..application..               (ports and their implementations)
 * adapter "rest"      ..adapter.in.rest..
 * adapter "mcp"       ..adapter.in.mcp..            (tools and resources, see ADR 0049)
 * adapter "persist"   ..adapter.out.persistence..   (incl. generated jOOQ code)
 * adapter "docs"      ..adapter.out.documentation.. (the delivered documentation, ADR 0049)
 * adapter "bootstrap" ..bootstrap..                 (composition root, outermost ring)
 * </pre>
 *
 * <p>The two rings added for the MCP server are where the onion view earns its keep: the MCP
 * adapter must reach the recipes through the {@code RecipeService} port like the REST adapter does
 * &ndash; it may neither borrow that adapter's DTOs nor read the classpath itself.
 *
 * <p>Splitting the domain into the two innermost rings buys a rule that a single {@code ..domain..}
 * ring could not state: a domain service may use the model, but the model may not reach back for a
 * service. ArchUnit enforces that direction as soon as the first service exists.
 *
 * <p>{@code withOptionalLayers(true)} is required because the domain service ring is still empty:
 * all behaviour lives in the application ring. Without the flag ArchUnit would fail the empty ring
 * itself, which would say nothing about the dependencies.
 *
 * <p>Tests are excluded from the analysis for the same reason as in {@link
 * LayeredArchitectureTest}: a test may legitimately reach into any ring.
 */
@AnalyzeClasses(
        packages = LayeredArchitectureTest.ROOT_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class OnionArchitectureTest {

    @ArchTest
    static final ArchRule rings_are_respected = onionArchitecture()
            .domainModels("..domain.model..")
            .domainServices("..domain.services..")
            .applicationServices("..application..")
            .adapter("rest", "..adapter.in.rest..")
            .adapter("mcp", "..adapter.in.mcp..")
            .adapter("persistence", "..adapter.out.persistence..")
            .adapter("documentation", "..adapter.out.documentation..")
            .adapter("bootstrap", "..bootstrap..")
            .withOptionalLayers(true)
            .as("Zwiebelschalen: Adapter kennen die Anwendung, die Anwendung kennt nur die Domäne "
                    + "und Adapter kennen einander nicht");
}
