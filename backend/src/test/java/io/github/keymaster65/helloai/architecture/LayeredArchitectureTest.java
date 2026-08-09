package io.github.keymaster65.helloai.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;

/**
 * Enforces the layering rule documented in {@code .claude/skills/architecture.md}:
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
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "org.jooq..",
                    "jakarta..",
                    "io.swagger..",
                    "com.fasterxml.jackson..",
                    "tools.jackson..",
                    "liquibase..")
            .as("Das Domänenmodell bleibt frei von Framework-Abhängigkeiten");

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
