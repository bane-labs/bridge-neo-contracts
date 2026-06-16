package network.bane.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture guardrails for the main module.
 * <p>
 * This test suite enforces package-level boundaries between SDK-facing code and devpack-side code.
 */
public class MainModuleDependencyRulesTest {

    /**
     * {@code main} packages that are considered SDK-facing code.
     * <p>
     * These are the off-chain packages that are allowed to use the neow3j SDK layer.
     * They are the only packages checked against the devpack dependency rule.
     */
    private static final String[] SDK_PACKAGES = {
            "network.bane.dto..",
            "network.bane.client.."
    };

    /**
     * neow3j SDK packages used by the devpack-side code.
     * <p>
     * Any package outside {@link #SDK_PACKAGES} must not depend on these classes.
     */
    private static final String[] NEOW3J_SDK_PACKAGES = {
            "io.neow3j.protocol..",
            "io.neow3j.types..",
            "io.neow3j.contract.."
    };

    /**
     * neow3j devpack package used by the SDK-facing code check.
     * <p>
     * This rule exists so SDK-facing code cannot accidentally depend on on-chain devpack types.
     */
    private static final String NEOW3J_DEVPACK_PACKAGE = "io.neow3j.devpack..";

    /**
     * All classes under {@code network.bane} from {@code main}, excluding tests.
     */
    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("network.bane");

    /**
     * Ensures the SDK-facing packages do not pull in devpack dependencies.
     */
    private static final ArchRule SDK_PACKAGES_MUST_NOT_DEPEND_ON_DEVPACK =
            noClasses()
                    .that().resideInAnyPackage(SDK_PACKAGES)
                    .should().dependOnClassesThat().resideInAnyPackage(NEOW3J_DEVPACK_PACKAGE);

    /**
     * Ensures every non-SDK package stays away from neow3j SDK dependencies.
     * <p>
     * These packages represent the devpack-side code, so they must not use the SDK runtime layer.
     */
    private static final ArchRule NON_SDK_PACKAGES_MUST_NOT_DEPEND_ON_NEOW3J_SDK =
            noClasses()
                    .that().resideOutsideOfPackages(SDK_PACKAGES)
                    .should().dependOnClassesThat().resideInAnyPackage(NEOW3J_SDK_PACKAGES);

    /**
     * Verifies that the SDK-facing packages do not depend on devpack classes.
     */
    @Test
    void sdk_packages_must_not_depend_on_devpack() {
        SDK_PACKAGES_MUST_NOT_DEPEND_ON_DEVPACK.check(MAIN_CLASSES);
    }

    /**
     * Verifies that all non-SDK packages do not depend on neow3j SDK classes.
     */
    @Test
    void non_sdk_packages_must_not_depend_on_neow3j_sdk() {
        NON_SDK_PACKAGES_MUST_NOT_DEPEND_ON_NEOW3J_SDK.check(MAIN_CLASSES);
    }

}
