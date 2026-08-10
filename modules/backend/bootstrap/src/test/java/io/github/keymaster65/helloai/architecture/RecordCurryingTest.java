package io.github.keymaster65.helloai.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Enforces the currying convention of ADR 0021: every record with more than
 * {@value #CURRYING_THRESHOLD} components offers a curried factory whose named steps mirror the
 * record components in declaration order.
 *
 * <p>The rule checks the shape of the chain, not just the presence of a method: a
 * {@code curried()} that returns something other than a step chain ending in the record would
 * satisfy the name but not the intent.
 */
@AnalyzeClasses(
        packages = LayeredArchitectureTest.ROOT_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class RecordCurryingTest {

    /** Up to this many components the canonical constructor stays readable enough on its own. */
    static final int CURRYING_THRESHOLD = 2;

    private static final String FACTORY_NAME = "curried";

    @ArchTest
    static final ArchRule records_with_more_than_two_components_are_curried = ArchRuleDefinition.classes()
            .that(haveMoreComponentsThanTheThreshold())
            .should(offerACurriedFactory())
            .as("Records mit mehr als " + CURRYING_THRESHOLD + " Komponenten bieten eine curried()-Factory");

    private static DescribedPredicate<JavaClass> haveMoreComponentsThanTheThreshold() {
        return new DescribedPredicate<>("Records mit mehr als %d Komponenten", CURRYING_THRESHOLD) {

            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isRecord()
                        && javaClass.reflect().getRecordComponents().length > CURRYING_THRESHOLD;
            }
        };
    }

    private static ArchCondition<JavaClass> offerACurriedFactory() {
        return new ArchCondition<>("eine statische %s()-Factory bieten, deren Schritte die Komponenten abbilden",
                FACTORY_NAME) {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Class<?> recordClass = javaClass.reflect();
                Optional<Method> factory = curriedFactory(recordClass);
                if (factory.isEmpty()) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            "%s hat keine öffentliche statische Methode %s() ohne Parameter"
                                    .formatted(recordClass.getName(), FACTORY_NAME)));
                    return;
                }

                Optional<String> problem = chainProblem(recordClass, factory.get().getReturnType());
                events.add(problem
                        .map(description -> SimpleConditionEvent.violated(javaClass, description))
                        .orElseGet(() -> SimpleConditionEvent.satisfied(javaClass,
                                "%s ist curried".formatted(recordClass.getName()))));
            }
        };
    }

    private static Optional<Method> curriedFactory(Class<?> recordClass) {
        return Arrays.stream(recordClass.getDeclaredMethods())
                .filter(method -> FACTORY_NAME.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .findFirst();
    }

    /**
     * Walks the step chain against the record components.
     *
     * @param recordClass the record under test
     * @param firstStep   return type of its curried factory
     * @return a description of the first deviation, or empty if the chain matches
     */
    private static Optional<String> chainProblem(Class<?> recordClass, Class<?> firstStep) {
        RecordComponent[] components = recordClass.getRecordComponents();
        Class<?> currentStep = firstStep;

        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            Optional<Method> stepMethod = singleAbstractMethod(currentStep);
            if (stepMethod.isEmpty()) {
                return Optional.of("%s: Schritt %d ist mit %s kein Interface mit genau einer Methode"
                        .formatted(recordClass.getName(), index + 1, currentStep.getName()));
            }

            Method method = stepMethod.get();
            if (!method.getName().equals(component.getName())) {
                return Optional.of("%s: Schritt %d heißt %s(), erwartet war die Komponente %s()"
                        .formatted(recordClass.getName(), index + 1, method.getName(), component.getName()));
            }
            if (method.getParameterCount() != 1 || !method.getParameterTypes()[0].equals(component.getType())) {
                return Optional.of("%s: Schritt %s() nimmt nicht genau ein Argument vom Typ %s"
                        .formatted(recordClass.getName(), method.getName(), component.getType().getName()));
            }
            currentStep = method.getReturnType();
        }

        if (!currentStep.equals(recordClass)) {
            return Optional.of("%s: der letzte Schritt liefert %s statt des Records"
                    .formatted(recordClass.getName(), currentStep.getName()));
        }
        return Optional.empty();
    }

    private static Optional<Method> singleAbstractMethod(Class<?> candidate) {
        if (!candidate.isInterface()) {
            return Optional.empty();
        }
        List<Method> abstractMethods = Arrays.stream(candidate.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .toList();
        return abstractMethods.size() == 1 ? Optional.of(abstractMethods.getFirst()) : Optional.empty();
    }
}
