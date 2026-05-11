package network.bane.utils.env;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;

import java.math.BigInteger;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

public class EnvVariables {

    public static Neow3j getNeow3jFromEnv() {
        return Neow3j.build(new HttpService(getEnvVariable(N3_JSON_RPC), true));
    }

    public static Hash160 getHash160FromEnvVar(String variableName) {
        return new Hash160(getEnvVariable(variableName));
    }

    public static Hash160 getAddressFromEnv(String variableName) {
        return Hash160.fromAddress(getEnvVariable(variableName));
    }

    public static ECKeyPair.ECPublicKey getPublicKeyFromEnvVar(String variableName) {
        return new ECKeyPair.ECPublicKey(getEnvVariable(variableName));
    }

    public static BigInteger getBigIntegerFromEnvVar(String variableName) {
        return new BigInteger(getEnvVariable(variableName));
    }

    public static int getIntegerFromEnvVar(String variableName) {
        return parseInt(getEnvVariable(variableName));
    }

    public static boolean getBooleanFromEnvVar(String variableName) {
        return parseBoolean(getEnvVariable(variableName));
    }

    public static String getEnvVariable(String variableName) {
        String value = System.getenv(variableName);
        if (value == null) {
            throw new IllegalArgumentException("Environment variable " + variableName + " is not set.");
        }
        return value;
    }

    // For local use: "http://127.0.0.1:40332"
    public static final String N3_JSON_RPC = "N3_JSON_RPC";

    // Wallets
    public static final String WALLET_FILEPATH_PERSONAL = "WALLET_FILEPATH_PERSONAL";
    public static final String WALLET_PASSWORD_PERSONAL = "WALLET_PASSWORD_PERSONAL";

    public static final String WALLET_FILEPATH_DEPLOYER = "WALLET_FILEPATH_DEPLOYER";
    public static final String WALLET_PASSWORD_DEPLOYER = "WALLET_PASSWORD_DEPLOYER";

    public static final String WALLET_FILEPATH_OWNER = "WALLET_FILEPATH_OWNER";
    public static final String WALLET_PASSWORD_OWNER = "WALLET_PASSWORD_OWNER";

    public static final String WALLET_FILEPATH_GOVERNOR = "WALLET_FILEPATH_GOVERNOR";
    public static final String WALLET_PASSWORD_GOVERNOR = "WALLET_PASSWORD_GOVERNOR";

    // Contract Addresses
    public static final String BRIDGE_HASH = "BRIDGE_HASH";
    public static final String MESSAGE_BRIDGE_HASH = "MESSAGE_BRIDGE_HASH";

    // Pausing/Unpausing
    public static final String PAUSE_TOKEN_HASH = "PAUSE_TOKEN_HASH";
    public static final String UNPAUSE_TOKEN_HASH = "UNPAUSE_TOKEN_HASH";

    // Role Addresses and Validator Public Keys
    public static final String ROLE_OWNER_ADDRESS = "ROLE_OWNER_ADDRESS";
    public static final String ROLE_RELAYER_ADDRESS = "ROLE_RELAYER_ADDRESS";
    public static final String ROLE_GOVERNOR_ADDRESS = "ROLE_GOVERNOR_ADDRESS";
    public static final String ROLE_SECURITY_GUARD_ADDRESS = "ROLE_SECURITY_GUARD_ADDRESS";
    public static final String ROLE_VALIDATOR_01_PUBLIC_KEY = "ROLE_VALIDATOR_01_PUBLIC_KEY";
    public static final String ROLE_VALIDATOR_02_PUBLIC_KEY = "ROLE_VALIDATOR_02_PUBLIC_KEY";
    public static final String ROLE_VALIDATOR_03_PUBLIC_KEY = "ROLE_VALIDATOR_03_PUBLIC_KEY";
    public static final String ROLE_VALIDATOR_04_PUBLIC_KEY = "ROLE_VALIDATOR_04_PUBLIC_KEY";
    public static final String ROLE_VALIDATOR_05_PUBLIC_KEY = "ROLE_VALIDATOR_05_PUBLIC_KEY";
    public static final String ROLE_VALIDATOR_06_PUBLIC_KEY = "ROLE_VALIDATOR_06_PUBLIC_KEY";
    public static final String ROLE_VALIDATOR_07_PUBLIC_KEY = "ROLE_VALIDATOR_07_PUBLIC_KEY";

    // Deployment - Management
    public static final String MANAGEMENT_CONTRACT_NAME = "MANAGEMENT_CONTRACT_NAME";
    // The first n validators are taken into account when deploying the management contract.
    // For example, if this value is 2, validator 01, 02, and 03 will be used as the validators in the deployment of
    // the management contract.
    public static final String MANAGEMENT_NUMBER_OF_VALIDATORS = "MANAGEMENT_NUMBER_OF_VALIDATORS";
    public static final String MANAGEMENT_VALIDATOR_THRESHOLD = "MANAGEMENT_VALIDATOR_THRESHOLD";

    // Deployment - Bridge
    public static final String BRIDGE_CONTRACT_NAME = "BRIDGE_CONTRACT_NAME";
    public static final String LINKED_CHAIN_ID = "LINKED_CHAIN_ID";

    // Deployment - Message Bridge
    // Reuses the linked chain ID from Bridge deployment.
    // The current scripts always deploy the message bridge in connection to a management bridge as well as in
    // connection to an Execution Manager contract, so neither the address of the management contract, nor the
    // execution manager is needed here for now. Once standalone deployment scripts are added, appropriate env variables
    // for the management contract address and the execution manager should be added here.

    // Deployment - Token Contract
    public static final String TOKEN_DEPLOY_TOKEN_NAME = "TOKEN_DEPLOY_TOKEN_NAME";
    public static final String TOKEN_DEPLOY_TOKEN_SYMBOL = "TOKEN_DEPLOY_TOKEN_SYMBOL";

    // Native Bridge Setting
    public static final String NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE = "NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE";
    public static final String NATIVE_SET_DECIMALS_ON_LINKED_CHAIN = "NATIVE_SET_DECIMALS_ON_LINKED_CHAIN";
    public static final String NATIVE_SET_DEPOSIT_FEE = "NATIVE_SET_DEPOSIT_FEE";
    public static final String NATIVE_SET_MIN_AMOUNT = "NATIVE_SET_MIN_AMOUNT";
    public static final String NATIVE_SET_MAX_AMOUNT = "NATIVE_SET_MAX_AMOUNT";
    public static final String NATIVE_SET_MAX_WITHDRAWALS = "NATIVE_SET_MAX_WITHDRAWALS";
    public static final String NATIVE_SET_MAX_TOTAL_DEPOSITED = "NATIVE_SET_MAX_TOTAL_DEPOSITED";

    // Token Registration Setting
    public static final String TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3 =
            "TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3";
    public static final String TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM =
            "TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM";
    public static final String TOKEN_REGISTRATION_DEPOSIT_FEE = "TOKEN_REGISTRATION_DEPOSIT_FEE";
    public static final String TOKEN_REGISTRATION_MIN_AMOUNT = "TOKEN_REGISTRATION_MIN_AMOUNT";
    public static final String TOKEN_REGISTRATION_MAX_AMOUNT = "TOKEN_REGISTRATION_MAX_AMOUNT";
    public static final String TOKEN_REGISTRATION_MAX_WITHDRAWALS = "TOKEN_REGISTRATION_MAX_WITHDRAWALS";
    public static final String TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR = "TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR";

    // Distinct Settings
    public static final String SETTING_TOKEN_HASH = "SETTING_TOKEN_HASH";
    public static final String NATIVE_DEPOSIT_FEE = "NATIVE_DEPOSIT_FEE";
    public static final String NATIVE_DEPOSIT_MIN_AMOUNT = "NATIVE_DEPOSIT_MIN_AMOUNT";
    public static final String TOKEN_DEPOSIT_FEE = "TOKEN_DEPOSIT_FEE";
    public static final String TOKEN_DEPOSIT_MIN_AMOUNT = "TOKEN_DEPOSIT_MIN_AMOUNT";
    public static final String MESSAGE_DEPOSIT_FEE = "MESSAGE_DEPOSIT_FEE";

    // Native Bridge Deposit
    public static final String NATIVE_DEPOSIT_RECIPIENT_ON_EVM = "NATIVE_DEPOSIT_RECIPIENT_ON_EVM";
    public static final String NATIVE_DEPOSIT_AMOUNT = "NATIVE_DEPOSIT_AMOUNT";

    // Token Bridge Deposit
    public static final String TOKEN_DEPOSIT_TOKEN_HASH = "TOKEN_DEPOSIT_TOKEN_HASH";
    public static final String TOKEN_DEPOSIT_RECIPIENT_ON_EVM = "TOKEN_DEPOSIT_RECIPIENT_ON_EVM";
    public static final String TOKEN_DEPOSIT_AMOUNT = "TOKEN_DEPOSIT_AMOUNT";

    // Token Transfer
    public static final String TOKEN_TRANSFER_TOKEN_HASH = "TOKEN_TRANSFER_TOKEN_HASH";

    // Message Bridge Sending Store-Only Message
    public static final String MESSAGE_SEND_STORE_ONLY_MESSAGE = "MESSAGE_SEND_STORE_ONLY_MESSAGE";

    // Message Bridge Sending Executable Message
    public static final String MESSAGE_SEND_EXECUTABLE_MESSAGE = "MESSAGE_SEND_EXECUTABLE_MESSAGE";
    public static final String MESSAGE_SEND_EXECUTABLE_STORE_BOOL = "MESSAGE_SEND_EXECUTABLE_STORE_BOOL";

    // Message Bridge Executing/Reading Message Nonce
    public static final String MESSAGE_NONCE = "MESSAGE_NONCE";

}
