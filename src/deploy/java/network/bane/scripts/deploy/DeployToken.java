package network.bane.scripts.deploy;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;

import static network.bane.utils.deployment.TokenCompilation.compileTokenContract;
import static network.bane.utils.deployment.TokenDeployment.deployContract;
import static network.bane.utils.env.EnvVariables.N3_JSON_RPC;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPLOY_TOKEN_NAME;
import static network.bane.utils.env.EnvVariables.TOKEN_DEPLOY_TOKEN_SYMBOL;

public class DeployToken {

    // The following are the required env variables for running this script
    private static final Neow3j neow3j = Neow3j.build(new HttpService(N3_JSON_RPC, true));
    private static final String tokenName = TOKEN_DEPLOY_TOKEN_NAME;
    private static final String tokenSymbol = TOKEN_DEPLOY_TOKEN_SYMBOL;

    public static void main(String[] args) throws Throwable {
        CompilationUnit compUnit = compileTokenContract(tokenName, tokenSymbol);
        deployContract(neow3j, compUnit);
    }

}
