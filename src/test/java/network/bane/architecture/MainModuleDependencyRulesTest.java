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
 * This test suite enforces package-level boundaries between SDK-based off-chain DTOs and
 * devpack-based on-chain contract code.
 */
public class MainModuleDependencyRulesTest {

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("network.bane");

    /**
     * DTO models must stay SDK-only and must not pull in devpack dependencies.
     */
    private static final ArchRule DTO_MUST_NOT_DEPEND_ON_DEVPACK =
            noClasses()
                    .that().resideInAPackage("network.bane.dto..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.neow3j.devpack..");

    /**
     * Non-DTO main packages must stay contract-focused and must not use SDK runtime classes.
     */
    private static final ArchRule NON_DTO_MUST_NOT_DEPEND_ON_SDK =
            noClasses()
                    .that().resideOutsideOfPackage("network.bane.dto..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.neow3j.protocol..",
                            "io.neow3j.types..",
                            "io.neow3j.contract..");

    /**
     * Verifies that DTO packages do not depend on devpack classes.
     */
    @Test
    void dto_must_not_depend_on_devpack() {
        DTO_MUST_NOT_DEPEND_ON_DEVPACK.check(MAIN_CLASSES);
    }

    /**
     * Verifies that non-DTO main packages do not depend on SDK classes.
     */
    @Test
    void non_dto_must_not_depend_on_sdk() {
        NON_DTO_MUST_NOT_DEPEND_ON_SDK.check(MAIN_CLASSES);
    }

}
