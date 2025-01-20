package network.bane.management;

import io.neow3j.devpack.ECPoint;

import static io.neow3j.devpack.Helper.abort;
import static network.bane.management.BridgeManagementContract.baseMap;
import static network.bane.management.BridgeManagementContract.isValidator;
import static network.bane.management.BridgeManagementContract.key_validator_threshold;
import static network.bane.management.BridgeManagementContract.validatorMap;
import static network.bane.management.BridgeManagementContract.validatorThreshold;
import static network.bane.management.BridgeManagementContract.validators;

public class ManagementImpl {

    private static final int MIN_VALIDATOR_THRESHOLD = 2;
    private static final int MIN_NR_VALIDATORS = 2;

    static void addValidator(ECPoint validator) {
        if (validator == null || !ECPoint.isValid(validator)) {
            abort("Invalid public key");
        }
        if (isValidator(validator)) {
            abort("Already a validator");
        }
        validatorMap.put(validator, true);
    }

    static void removeValidator(ECPoint validator) {
        if (validator == null || !ECPoint.isValid(validator)) {
            abort("Invalid public key");
        }
        if (!isValidator(validator)) {
            abort("Not a validator");
        }
        int nrValidators = validators().size();
        if (nrValidators == MIN_NR_VALIDATORS) {
            abort("Min number of validators reached.");
        }
        // If the threshold is equal to the number of validators, we can't remove any more validators.
        // The greater than or equal to check is just a safety measure.
        if (validatorThreshold() >= nrValidators) {
            abort("Threshold too high");
        }
        validatorMap.delete(validator);
    }

    static int incrementValidatorThreshold() {
        int threshold = validatorThreshold();
        int newThreshold = threshold + 1;
        baseMap.put(key_validator_threshold, newThreshold);
        return newThreshold;
    }

    static int decrementValidatorThreshold() {
        int threshold = validatorThreshold();
        if (threshold == MIN_VALIDATOR_THRESHOLD) {
            abort("Min validator threshold reached.");
        }
        int newThreshold = baseMap.getInt(key_validator_threshold) - 1;
        baseMap.put(key_validator_threshold, newThreshold);
        return newThreshold;
    }

    static void replaceValidator(ECPoint oldValidator, ECPoint newValidator) {
        if (oldValidator == null || !ECPoint.isValid(oldValidator) || newValidator == null || !ECPoint.isValid(newValidator)) {
            abort("Invalid public key");
        }
        if (oldValidator.equals(newValidator)) {
            abort("Public keys must differ.");
        }
        if (!isValidator(oldValidator)) {
            abort("Old public key is not a validator.");
        }
        if (isValidator(newValidator)) {
            abort("New public key is already a validator.");
        }
        validatorMap.delete(oldValidator);
        validatorMap.put(newValidator, true);
    }

    static void setValidatorThreshold(int newThreshold) {
        if (newThreshold < MIN_VALIDATOR_THRESHOLD) abort("Threshold too low");
        if (newThreshold > validators().size()) abort("Threshold too high");
        baseMap.put(key_validator_threshold, newThreshold);
    }
}
