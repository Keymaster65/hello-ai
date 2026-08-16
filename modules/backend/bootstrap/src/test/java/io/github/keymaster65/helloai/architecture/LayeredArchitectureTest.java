package io.github.keymaster65.helloai.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;

/**
 * Enforces the layering rule documented in {@code docs/prompt/architektur.adoc}:
 *
 * <pre>
 * bootstrap → adapter → application → domain
 * </pre>
 *
 * <p>Dependencies point inwards only. Until now this was a convention on paper; these rules
 * make a violation fail the build instead of surviving a review (see ADR 0012).
 *
 * <p>Tests are excluded from the analysis on purpose: a test may legitimately reach into any
 * layer, and the system tests deliberately boot the application from {@code bootstrap}.
 */
@AnalyzeClasses(
        packages = LayeredArchitectureTest.ROOT_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    static final String ROOT_PACKAGE = "io.github.keymaster65.helloai";

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String ADAPTER = "..adapter..";
    private static final String BOOTSTRAP = "..bootstrap..";

    /**
     * The frameworks the inner layers stay away from. Listed once because two rules use it: the
     * domain and, since ADR 0045, the application layer.
     */
    private static final String[] FRAMEWORKS = {
        "org.springframework..",
        "org.jooq..",
        "jakarta..",
        "io.swagger..",
        "com.fasterxml.jackson..",
        "tools.jackson..",
        "liquibase..",
        // Das MCP-SDK ist ein Rahmenwerk wie die übrigen: Werkzeuge und Transport gehören in den
        // Adapter bzw. den Composition Root, nicht in eine innere Schicht (ADR 0049).
        "io.modelcontextprotocol..",
    };

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Adapter").definedBy(ADAPTER)
            .layer("Bootstrap").definedBy(BOOTSTRAP)
            // Innermost layer: everyone may depend on it.
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Bootstrap")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Bootstrap")
            .whereLayer("Adapter").mayOnlyBeAccessedByLayers("Bootstrap")
            // Outermost layer: nothing may depend on it.
            .whereLayer("Bootstrap").mayNotBeAccessedByAnyLayer()
            .as("Abhängigkeiten zeigen nach innen: bootstrap → adapter → application → domain");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER, BOOTSTRAP)
            .as("Das Domänenmodell kennt keine äußere Schicht");

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(ADAPTER, BOOTSTRAP)
            .as("Die Anwendungsschicht kennt weder Adapter noch Bootstrap");

    /**
     * The domain is expressed in plain Java records. Framework types there would tie the innermost
     * layer to infrastructure and quietly turn it into an adapter.
     */
    @ArchTest
    static final ArchRule domain_is_free_of_frameworks = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .as("Das Domänenmodell bleibt frei von Framework-Abhängigkeiten");

    /**
     * The use cases are plain Java too (ADR 0045). A {@code @Service} or {@code @Transactional}
     * here would tie the business rules to the runtime that happens to host them today and make
     * the layer testable only with a container.
     *
     * <p>The build already keeps the frameworks off this module's compile classpath; the rule is
     * the second net, and it states the intent where the other layer rules are read.
     */
    @ArchTest
    static final ArchRule application_is_free_of_frameworks = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .as("Die Anwendungsschicht bleibt frei von Framework-Abhängigkeiten");

    /**
     * The domain is split into model and services (ADR 0020). A class directly in {@code ..domain}
     * would belong to neither and quietly undo the split.
     */
    @ArchTest
    static final ArchRule domain_is_split_into_model_and_services = ArchRuleDefinition.classes()
            .that().resideInAPackage(DOMAIN)
            .should().resideInAnyPackage("..domain.model..", "..domain.services..")
            .as("Die Domäne besteht aus model und services");

    /**
     * Ports are the contract between the layers; a class here would put implementation detail
     * into the boundary.
     */
    @ArchTest
    static final ArchRule ports_are_interfaces = ArchRuleDefinition.classes()
            .that().resideInAPackage("..application.port..")
            .should().beInterfaces()
            .as("Ports sind Interfaces");
}
