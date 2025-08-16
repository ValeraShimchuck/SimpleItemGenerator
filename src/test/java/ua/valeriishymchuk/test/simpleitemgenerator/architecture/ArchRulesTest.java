package ua.valeriishymchuk.test.simpleitemgenerator.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import org.junit.Test;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class ArchRulesTest {

    private static final String BASE_PACKAGE = "ua.valeriishymchuk.simpleitemgenerator";
    private static final DescribedPredicate<JavaClass> MAIN_CLASS_PREDICATE = DescribedPredicate
            .describe(
                    "Main class",
                    clazz -> {
                        return clazz.getFullName().startsWith("ua.valeriishymchuk.simpleitemgenerator.SimpleItemGeneratorPlugin");
                    }
            );

    private JavaClasses getClasses() {
        return new ClassFileImporter().importPackages(BASE_PACKAGE);
    }

    @Test
    public void layeredArchitectureConstraint() {
        JavaClasses jc = getClasses();
        Architectures.LayeredArchitecture arch = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Main").definedBy(MAIN_CLASS_PREDICATE)
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")
                .whereLayer("Main").mayNotBeAccessedByAnyLayer()
                .whereLayer("Controller").mayOnlyBeAccessedByLayers("Main")
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Main")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Main");
        arch.check(jc);
    }

    @Test
    public void minecraftFrameworkUsageConstraints() {
        JavaClasses jc = getClasses();
        DescribedPredicate<JavaClass> minecraftClasses = DescribedPredicate.describe("Minecraft classes", clazz -> {
            String packageName = clazz.getPackageName();
            return packageName.startsWith("org.bukkit") ||
                    packageName.startsWith("net.minecraft") ||
                    packageName.startsWith("org.spigotmc") ||
                    packageName.startsWith("de.tr7zw") ||
                    packageName.startsWith("com.github.retrooper") ||
                    packageName.startsWith("com.destroystokyo.paper") ||
                    packageName.startsWith("io.papermc");
        });

        DescribedPredicate<JavaClass> allowedClasses =
                JavaClass.Predicates.resideInAPackage(BASE_PACKAGE + ".controller")
                        .or(annotatedWith(UsesMinecraft.class))
                        .or(MAIN_CLASS_PREDICATE)
                        .or(JavaClass.Predicates.assignableTo("org.bukkit.event.Event"));

        DescribedPredicate<JavaMethod> allowedMethods = DescribedPredicate.describe(
                "Methods in allowed classes",
                method -> method.isAnnotatedWith(UsesMinecraft.class) ||
                        allowedClasses.test(method.getOwner())
        );

        ArchRule classDependRule = classes()
                .that(not(allowedClasses))
                .should(new ArchCondition<JavaClass>("Depend on fields or constructors") {
                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                        for (JavaAccess<?> access : item.getAccessesFromSelf()) {
                            if (access instanceof JavaMethodCall) continue;
                            if (minecraftClasses.test(access.getTargetOwner())) {
                                events.add(SimpleConditionEvent.violated(
                                        item, String.format(
                                                "%s uses Minecraft classes without @UsesMinecraft annotation. Class: %s location: %s",
                                                item.getDescription(),
                                                access.getTargetOwner(),
                                                access.getSourceCodeLocation()
                                        )
                                ));
                            }
                        }
                    }
                });

        ArchRule methodRule = methods()
                .that(not(allowedMethods)).should(new ArchCondition<>("Access Minecraft classes") {
                    @Override
                    public void check(JavaMethod item, ConditionEvents events) {
                        for (JavaAccess<?> access : item.getAccessesFromSelf()) {
                            boolean annotationCheckFailed;
                            if (access instanceof JavaMethodCall call) {
                                JavaMethod method = call.getTarget().resolveMember().orElse(null);
                                annotationCheckFailed = method != null && method.isAnnotatedWith(UsesMinecraft.class);
                            } else annotationCheckFailed = false;
                            if (minecraftClasses.test(access.getTarget().getOwner()) || annotationCheckFailed) {
                                events.add(SimpleConditionEvent.violated(
                                        item, String.format(
                                                "%s uses Minecraft classes without @UsesMinecraft annotation. Class: %s location: %s",
                                                item.getDescription(),
                                                access.getTargetOwner(),
                                                access.getSourceCodeLocation()
                                        )
                                ));

                            }
                        }
                    }
                });
        methodRule.check(jc);
        classDependRule.check(jc);
    }

}
