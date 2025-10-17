package network.bane.scripts.deploy;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;

import static network.bane.utils.deployment.TokenCompilation.compileTokenContract;
import static network.bane.utils.deployment.TokenDeployment.deployContract;
import static network.bane.utils.env.EnvVariables.NODE;

public class DeployToken {

    public static void main(String[] args) throws Throwable {
        Neow3j neow3j = Neow3j.build(new HttpService(NODE, true));

        CompilationUnit compUnit = compileTokenContract();
        deployContract(neow3j, compUnit);
    }

}
