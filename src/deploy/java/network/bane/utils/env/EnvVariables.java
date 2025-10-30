package network.bane.utils.env;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.types.Hash160;

import java.math.BigInteger;

public class EnvVariables {

    public static String getEnvVariable(String variableName) {
        String value = System.getenv(variableName);
        if (value == null) {
            throw new IllegalArgumentException("Environment variable " + variableName + " is not set.");
        }
        return value;
    }

    // For local use: "http://127.0.0.1:40332"
    public static final String N3_JSON_RPC = getEnvVariable("N3_JSON_RPC");

    // Wallets
    public static final String WALLET_FILEPATH_PERSONAL = getEnvVariable("WALLET_FILEPATH_PERSONAL");
    public static final String WALLET_PASSWORD_PERSONAL = getEnvVariable("WALLET_PASSWORD_PERSONAL");

    public static final String WALLET_FILEPATH_DEPLOYER = getEnvVariable("WALLET_FILEPATH_DEPLOYER");
    public static final String WALLET_PASSWORD_DEPLOYER = getEnvVariable("WALLET_PASSWORD_DEPLOYER");

    public static final String WALLET_FILEPATH_OWNER = getEnvVariable("WALLET_FILEPATH_OWNER");
    public static final String WALLET_PASSWORD_OWNER = getEnvVariable("WALLET_PASSWORD_OWNER");

    public static final String WALLET_FILEPATH_GOVERNOR = getEnvVariable("WALLET_FILEPATH_GOVERNOR");
    public static final String WALLET_PASSWORD_GOVERNOR = getEnvVariable("WALLET_PASSWORD_GOVERNOR");

    // Contract Addresses
    public static final Hash160 BRIDGE_HASH = new Hash160(getEnvVariable("BRIDGE_HASH"));
    public static final Hash160 MESSAGE_BRIDGE_HASH = new Hash160(getEnvVariable("MESSAGE_BRIDGE_HASH"));

    // Role Addresses and Validator Public Keys
    public static final Hash160 ROLE_OWNER_ADDRESS = Hash160.fromAddress(getEnvVariable("ROLE_OWNER_ADDRESS"));
    public static final Hash160 ROLE_RELAYER_ADDRESS = Hash160.fromAddress(getEnvVariable("ROLE_RELAYER_ADDRESS"));
    public static final Hash160 ROLE_GOVERNOR_ADDRESS = Hash160.fromAddress(getEnvVariable("ROLE_GOVERNOR_ADDRESS"));
    public static final Hash160 ROLE_SECURITY_GUARD_ADDRESS =
            Hash160.fromAddress(getEnvVariable("ROLE_SECURITY_GUARD_ADDRESS"));
    public static final ECKeyPair.ECPublicKey ROLE_VALIDATOR_01_PUBLIC_KEY =
            new ECKeyPair.ECPublicKey(getEnvVariable("ROLE_VALIDATOR_01_PUBLIC_KEY"));
    public static final ECKeyPair.ECPublicKey ROLE_VALIDATOR_02_PUBLIC_KEY =
            new ECKeyPair.ECPublicKey(getEnvVariable("ROLE_VALIDATOR_02_PUBLIC_KEY"));
    public static final ECKeyPair.ECPublicKey ROLE_VALIDATOR_03_PUBLIC_KEY =
            new ECKeyPair.ECPublicKey(getEnvVariable("ROLE_VALIDATOR_03_PUBLIC_KEY"));
    public static final ECKeyPair.ECPublicKey ROLE_VALIDATOR_04_PUBLIC_KEY =
            new ECKeyPair.ECPublicKey(getEnvVariable("ROLE_VALIDATOR_04_PUBLIC_KEY"));
    public static final ECKeyPair.ECPublicKey ROLE_VALIDATOR_05_PUBLIC_KEY =
            new ECKeyPair.ECPublicKey(getEnvVariable("ROLE_VALIDATOR_05_PUBLIC_KEY"));
    public static final ECKeyPair.ECPublicKey ROLE_VALIDATOR_06_PUBLIC_KEY =
            new ECKeyPair.ECPublicKey(getEnvVariable("ROLE_VALIDATOR_06_PUBLIC_KEY"));
    public static final ECKeyPair.ECPublicKey ROLE_VALIDATOR_07_PUBLIC_KEY =
            new ECKeyPair.ECPublicKey(getEnvVariable("ROLE_VALIDATOR_07_PUBLIC_KEY"));

    // Deployment - Management
    public static final String MANAGEMENT_CONTRACT_NAME = getEnvVariable("MANAGEMENT_CONTRACT_NAME");
    // The first n validators are taken into account when deploying the management contract.
    // For example, if this value is 2, validator 01, 02, and 03 will be used as the validators in the deployment of
    // the management contract.
    public static final int MANAGEMENT_NUMBER_OF_VALIDATORS =
            Integer.parseInt(getEnvVariable("MANAGEMENT_NUMBER_OF_VALIDATORS"));
    public static final int MANAGEMENT_VALIDATOR_THRESHOLD =
            Integer.parseInt(getEnvVariable("MANAGEMENT_VALIDATOR_THRESHOLD"));

    // Deployment - Bridge
    public static final String BRIDGE_CONTRACT_NAME = getEnvVariable("BRIDGE_CONTRACT_NAME");
    public static final BigInteger LINKED_CHAIN_ID = new BigInteger(getEnvVariable("LINKED_CHAIN_ID"));

    // Deployment - Message Bridge
    // Reuses the linked chain ID from Bridge deployment.
    // The current scripts always deploy the message bridge in connection to a management bridge as well as in
    // connection to an Execution Manager contract, so neither the address of the management contract, nor the
    // execution manager is needed here for now. Once standalone deployment scripts are added, appropriate env variables
    // for the management contract address and the execution manager should be added here.

    // Deployment - Token Contract
    public static final String TOKEN_DEPLOY_TOKEN_NAME = getEnvVariable("TOKEN_DEPLOY_TOKEN_NAME");
    public static final String TOKEN_DEPLOY_TOKEN_SYMBOL = getEnvVariable("TOKEN_DEPLOY_TOKEN_SYMBOL");

    // Native Bridge Setting
    public static final Hash160 NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE =
            new Hash160(getEnvVariable("NATIVE_SET_TOKEN_FOR_NATIVE_BRIDGE"));
    public static final BigInteger NATIVE_SET_DECIMALS_ON_LINKED_CHAIN =
            new BigInteger(getEnvVariable("NATIVE_SET_DECIMALS_ON_LINKED_CHAIN"));
    public static final BigInteger NATIVE_SET_DEPOSIT_FEE = new BigInteger(getEnvVariable("NATIVE_SET_DEPOSIT_FEE"));
    public static final BigInteger NATIVE_SET_MIN_AMOUNT = new BigInteger(getEnvVariable("NATIVE_SET_MIN_AMOUNT"));
    public static final BigInteger NATIVE_SET_MAX_AMOUNT = new BigInteger(getEnvVariable("NATIVE_SET_MAX_AMOUNT"));
    public static final BigInteger NATIVE_SET_MAX_WITHDRAWALS =
            new BigInteger(getEnvVariable("NATIVE_SET_MAX_WITHDRAWALS"));
    public static final BigInteger NATIVE_SET_MAX_TOTAL_DEPOSITED =
            new BigInteger(getEnvVariable("NATIVE_SET_MAX_TOTAL_DEPOSITED"));

    // Token Registration Setting
    public static final Hash160 TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3 =
            new Hash160(getEnvVariable("TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_N3"));
    public static final Hash160 TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM =
            new Hash160(getEnvVariable("TOKEN_REGISTRATION_TOKEN_CONTRACT_HASH_ON_EVM"));
    public static final BigInteger TOKEN_REGISTRATION_DEPOSIT_FEE =
            new BigInteger(getEnvVariable("TOKEN_REGISTRATION_DEPOSIT_FEE"));
    public static final BigInteger TOKEN_REGISTRATION_MIN_AMOUNT =
            new BigInteger(getEnvVariable("TOKEN_REGISTRATION_MIN_AMOUNT"));
    public static final BigInteger TOKEN_REGISTRATION_MAX_AMOUNT =
            new BigInteger(getEnvVariable("TOKEN_REGISTRATION_MAX_AMOUNT"));
    public static final BigInteger TOKEN_REGISTRATION_MAX_WITHDRAWALS =
            new BigInteger(getEnvVariable("TOKEN_REGISTRATION_MAX_WITHDRAWALS"));
    public static final BigInteger TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR =
            new BigInteger(getEnvVariable("TOKEN_REGISTRATION_DECIMAL_SCALING_FACTOR"));

    // Native Bridge Deposit
    public static final Hash160 NATIVE_DEPOSIT_RECIPIENT_ON_EVM =
            new Hash160(getEnvVariable("NATIVE_DEPOSIT_RECIPIENT_ON_EVM"));
    public static final BigInteger NATIVE_DEPOSIT_AMOUNT = new BigInteger(getEnvVariable("NATIVE_DEPOSIT_AMOUNT"));

    // Token Bridge Deposit
    public static final Hash160 TOKEN_DEPOSIT_TOKEN_HASH = new Hash160(getEnvVariable("TOKEN_DEPOSIT_TOKEN_HASH"));
    public static final Hash160 TOKEN_DEPOSIT_RECIPIENT_ON_EVM =
            new Hash160(getEnvVariable("TOKEN_DEPOSIT_RECIPIENT_ON_EVM"));
    public static final BigInteger TOKEN_DEPOSIT_AMOUNT = new BigInteger(getEnvVariable("TOKEN_DEPOSIT_AMOUNT"));

    // Token Transfer
    public static final Hash160 TOKEN_TRANSFER_TOKEN_HASH = new Hash160(getEnvVariable("TOKEN_TRANSFER_TOKEN_HASH"));

    // Message Bridge Sending Store-Only Message
    public static final String MESSAGE_SEND_STORE_ONLY_MESSAGE = getEnvVariable("MESSAGE_SEND_STORE_ONLY_MESSAGE");

    // Message Bridge Sending Executable Message
    public static final String MESSAGE_SEND_EXECUTABLE_MESSAGE = getEnvVariable("MESSAGE_SEND_EXECUTABLE_MESSAGE");
    public static final boolean MESSAGE_SEND_EXECUTABLE_STORE_BOOL =
            Boolean.parseBoolean(getEnvVariable("MESSAGE_SEND_EXECUTABLE_STORE_BOOL"));

}
