package network.bane.utils.deployment;

import io.neow3j.compiler.CompilationUnit;
import network.bane.testhelper.TestFungibleToken;

import java.io.IOException;
import java.util.HashMap;

import static network.bane.utils.env.GetEnv.getEnvVariable;

public class TokenCompilation {

    public static CompilationUnit compileTokenContract() throws IOException {
        HashMap<String, String> substitutions = new HashMap<>();
        substitutions.put("TokenName", getEnvVariable("TOKEN_NAME"));
        substitutions.put("TokenSymbol", getEnvVariable("TOKEN_SYMBOL"));
        return new io.neow3j.compiler.Compiler().compile(TestFungibleToken.class.getCanonicalName(), substitutions);
    }

}
