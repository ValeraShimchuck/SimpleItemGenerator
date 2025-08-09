package ua.valeriishymchuk.test.simpleitemgenerator.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.Test;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesBukkit;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class ArchRulesTest {

    private static final String BASE_PACKAGE = "ua.valeriishymchuk.simpleitemgenerator";

    private JavaClasses getClasses() {
        return new ClassFileImporter().importPackages(BASE_PACKAGE);
    }

    private static final DescribedPredicate<JavaClass> MAIN_CLASS_PREDICATE = DescribedPredicate
            .describe(
                    "Main class",
                    clazz -> {
                        return clazz.getFullName().startsWith("ua.valeriishymchuk.simpleitemgenerator.SimpleItemGeneratorPlugin");
                    }
            );

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
                        .or(annotatedWith(UsesBukkit.class))
                        .or(MAIN_CLASS_PREDICATE)
                        .or(JavaClass.Predicates.assignableTo("org.bukkit.event.Event"));

        ArchRule rule = classes()
                .that().resideOutsideOfPackage(BASE_PACKAGE + ".controller")
                .and().areNotAnnotatedWith(UsesBukkit.class)
                .and().areNotAssignableFrom(MAIN_CLASS_PREDICATE)
                .and().areNotAssignableTo("org.bukkit.event.Event")
                .should().onlyAccessClassesThat(not(minecraftClasses));
        rule.check(jc);
    }

}
