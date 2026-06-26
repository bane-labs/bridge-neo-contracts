package network.bane.scripts.compile;

import io.neow3j.compiler.CompilationUnit;
import network.bane.bridge.BridgeContract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

import static network.bane.scripts.compile.CompilationHelper.compileContractWithSubstitutions;
import static network.bane.scripts.compile.CompilationHelper.printCompilationResults;
import static network.bane.scripts.compile.CompilationHelper.printWriteFilesResults;
import static network.bane.scripts.compile.CompilationHelper.writeNefAndManifestFiles;
import static network.bane.utils.env.EnvVariables.BRIDGE_CONTRACT_NAME;
import static network.bane.utils.env.EnvVariables.getEnvVariable;

public class CompileBridgeToFiles {

    public static void main(String[] args) throws IOException {
        // Note: If you need to update existing contracts, the names must remain the same.
        String bridgeContractName = getEnvVariable(BRIDGE_CONTRACT_NAME);

        HashMap<String, String> replaceMap = new HashMap<>();
        replaceMap.put("BridgeName", bridgeContractName);

        String contractToCompile = BridgeContract.class.getCanonicalName();
        CompilationUnit compUnit = compileContractWithSubstitutions(contractToCompile, replaceMap);

        System.out.println("------------------------------");
        printCompilationResults(compUnit, contractToCompile);
        Path path = writeNefAndManifestFiles(compUnit);
        printWriteFilesResults(path, compUnit.getManifest().getName());
        System.out.println("------------------------------");
    }

}
