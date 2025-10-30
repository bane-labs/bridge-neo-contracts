package network.bane.utils.deployment;

import io.neow3j.compiler.CompilationUnit;
import network.bane.testhelper.TestFungibleToken;

import java.io.IOException;
import java.util.HashMap;

public class TokenCompilation {

    public static CompilationUnit compileTokenContract(String tokenName, String tokenSymbol) throws IOException {
        HashMap<String, String> substitutions = new HashMap<>();
        substitutions.put("TokenName", tokenName);
        substitutions.put("TokenSymbol", tokenSymbol);
        return new io.neow3j.compiler.Compiler().compile(TestFungibleToken.class.getCanonicalName(), substitutions);
    }

}
