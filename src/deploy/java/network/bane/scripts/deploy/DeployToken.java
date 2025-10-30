package network.bane.scripts.deploy;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.protocol.Neow3j;
import io.neow3j.wallet.Account;

import static io.neow3j.types.ContractParameter.hash160;
import static network.bane.utils.deployment.TokenCompilation.compileTokenContract;
import static network.bane.utils.deployment.TokenDeployment.deployContract;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPLOY_TOKEN_NAME;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPLOY_TOKEN_SYMBOL;
import static network.bane.utils.env.EnvVariables.WALLET_FILEPATH_DEPLOYER;
import static network.bane.utils.env.EnvVariables.WALLET_PASSWORD_DEPLOYER;
import static network.bane.utils.env.EnvVariables.getEnvVariable;
import static network.bane.utils.env.EnvVariables.getNeow3jFromEnvVar;
import static network.bane.utils.wallet.LoadWallet.getAccountFromWallet;

/**
 * This class is used to deploy a fungible token contract.
 * <p>
 * Requires the following environment variables to be set:
 * - N3_JSON_RPC: The RPC endpoint of the N3 node
 * - WALLET_FILEPATH_DEPLOYER: the filepath to the deployer wallet
 * - WALLET_PASSWORD_DEPLOYER: the password for the deployer wallet
 * - TOKEN_DEPLOY_TOKEN_NAME: The name of the token to deploy
 * - TOKEN_DEPLOY_TOKEN_SYMBOL: The symbol of the token to deploy
 * <p>
 * Run with: gradle run -PmainClass=network.bane.scripts.deploy.DeployToken
 */
public class DeployToken {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = getNeow3jFromEnvVar(N3_JSON_RPC);
        String deployerWalletFilepath = WALLET_FILEPATH_DEPLOYER;
        String deployerWalletPassword = WALLET_PASSWORD_DEPLOYER;
        String tokenName = getEnvVariable(TOKEN_DEPLOY_TOKEN_NAME);
        String tokenSymbol = getEnvVariable(TOKEN_DEPLOY_TOKEN_SYMBOL);
        Account deployerAcc = getAccountFromWallet(deployerWalletFilepath, deployerWalletPassword);

        CompilationUnit compUnit = compileTokenContract(tokenName, tokenSymbol);
        deployContract(neow3j, compUnit, deployerAcc, hash160(deployerAcc));
    }

}
