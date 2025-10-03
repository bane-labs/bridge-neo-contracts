package network.bane.utils.env;

import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.exceptions.CipherException;
import io.neow3j.crypto.exceptions.NEP2InvalidFormat;
import io.neow3j.crypto.exceptions.NEP2InvalidPassphrase;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import java.io.IOException;
import java.math.BigInteger;

import static network.bane.utils.env.GetEnv.getEnvVariableOrDefault;
import static network.bane.utils.wallet.LoadWallet.getDeployerAccountFromWallet;
import static network.bane.utils.wallet.LoadWallet.getOwnerAccountFromWallet;

public class EnvVariables {

    public static final String NODE = getEnvVariableOrDefault("NEON3_JSON_RPC", "http://127.0.0.1:40332");

    public static Account deployerAcc;
    public static Account ownerAcc;

    static {
        try {
            deployerAcc = getDeployerAccountFromWallet();
            ownerAcc = getOwnerAccountFromWallet();
        } catch (NEP2InvalidPassphrase | NEP2InvalidFormat | CipherException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Hash160 relayer = Hash160.fromAddress(
            getEnvVariableOrDefault("NEON3_RELAYER_ADDRESS", "NSsMzYzdLsXG6mjp4MHyGZHuKssakqpv3g"));

    public static final int MAX_NR_VALIDATORS = 7;
    // This env variable specifies the number of validators (n). The first n validators are used and remaining
    // validators will be ignored. If n is lower than 2 or higher than 7, an exception will be thrown.
    public static final int nrValidators = Integer.parseInt(getEnvVariableOrDefault("NEON3_NR_VALIDATORS", "7"));
    public static final ECKeyPair.ECPublicKey validator_1 = new ECKeyPair.ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR1_PUBKEY",
                    "021fed0d208f2c4b2fe425571c36aea1d465159c93af349581896fe38553dffae1"));
    public static final ECKeyPair.ECPublicKey validator_2 = new ECKeyPair.ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR2_PUBKEY",
                    "021986f90b2596322c9f681c42814237555732411e8c517100c702b91cd404f090"));
    public static final ECKeyPair.ECPublicKey validator_3 = new ECKeyPair.ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR3_PUBKEY",
                    "021387fa5748344658778d9a3de2bd99f2133b96a3fe5ab2cda007bce3531f4d9f"));
    public static final ECKeyPair.ECPublicKey validator_4 = new ECKeyPair.ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR4_PUBKEY",
                    "02108e50c32a9b4c12b90013b145370530a413a605ced93491ba2523d903295f45"));
    public static final ECKeyPair.ECPublicKey validator_5 = new ECKeyPair.ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR5_PUBKEY",
                    "030187e4b19cddfa93f282c0bba9759446094c006a9f91c5e80963b2d9f8c4b568"));
    public static final ECKeyPair.ECPublicKey validator_6 = new ECKeyPair.ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR6_PUBKEY",
                    "037c94e4ef2445f283ab7b02a1c26783ab39527404c157f4eb686edbdd0e651ade"));
    public static final ECKeyPair.ECPublicKey validator_7 = new ECKeyPair.ECPublicKey(
            getEnvVariableOrDefault("NEON3_VALIDATOR7_PUBKEY",
                    "02189efe8f9d4fc34cceaae9fa79346525810143d5f198070594c7da12facfd4ab"));

    public static final int validator_threshold =
            Integer.parseInt(getEnvVariableOrDefault("NEON3_VALIDATOR_THRESHOLD", "5"));

    public static final Hash160 governor = Hash160.fromAddress(
            getEnvVariableOrDefault("NEON3_GOVERNOR_ADDRESS", "NbdSPqc4iADXLT6XzFUtwhDKAfPU8JdT5b"));

    public static final Hash160 securityGuard = Hash160.fromAddress(
            getEnvVariableOrDefault("NEON3_SECURITYGUARD_ADDRESS", "NKybo9Fy5d84RFeWGMvqJgKtEwDsD1CrDD"));

    // Parameters

    public static final BigInteger depositFee =
            new BigInteger(getEnvVariableOrDefault("NEON3_DEPOSIT_FEE", "10000000"));
    public static final BigInteger minDepositAmount =
            new BigInteger(getEnvVariableOrDefault("NEON3_MIN_DEPOSIT_AMOUNT", "100000000"));
    public static final BigInteger maxDepositAmount =
            new BigInteger(getEnvVariableOrDefault("NEON3_MAX_DEPOSIT_AMOUNT", "1000000000000"));
    public static final BigInteger maxTotalDeposited =
            new BigInteger(getEnvVariableOrDefault("NEON3_MAX_TOTAL_DEPOSITED", "100000000000000"));

    public static final BigInteger linkedChainId =
            new BigInteger(getEnvVariableOrDefault("NEON3_LINKED_CHAIN_ID", "1"));

}
