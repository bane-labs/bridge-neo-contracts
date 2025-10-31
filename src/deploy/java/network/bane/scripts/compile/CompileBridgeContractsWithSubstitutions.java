package network.bane.scripts.compile;

import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import network.bane.bridge.BridgeContract;
import network.bane.management.BridgeManagementContract;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static io.neow3j.contract.ContractUtils.writeContractManifestFile;
import static io.neow3j.contract.ContractUtils.writeNefFile;
import static network.bane.utils.env.EnvVariables.BRIDGE_CONTRACT_NAME;
import static network.bane.utils.env.EnvVariables.MANAGEMENT_CONTRACT_NAME;
import static network.bane.utils.env.EnvVariables.getEnvVariable;

public class CompileBridgeContractsWithSubstitutions {

    public static void main(String[] args) throws IOException {
        // Note: If you need to update existing contracts, the names must remain the same.
        String bridgeContractName = getEnvVariable(BRIDGE_CONTRACT_NAME);
        String bridgeManagementName = getEnvVariable(MANAGEMENT_CONTRACT_NAME);

        compileAndWriteNefAndManifestFilesBridge(bridgeContractName);
        compileAndWriteNefAndManifestFilesManagement(bridgeManagementName);
    }

    private static void compileAndWriteNefAndManifestFilesBridge(String bridgeContractName) throws IOException {
        HashMap<String, String> replaceMap = new HashMap<>();
        replaceMap.put("BridgeName", bridgeContractName);
        CompilationUnit compUnit = new Compiler().compile(BridgeContract.class.getCanonicalName(), replaceMap);
        writeNefAndManifestFiles(compUnit);
    }

    private static void compileAndWriteNefAndManifestFilesManagement(String managementContractName) throws IOException {
        HashMap<String, String> replaceMap = new HashMap<>();
        replaceMap.put("BridgeManagementName", managementContractName);
        CompilationUnit compUnit = new Compiler().compile(BridgeManagementContract.class.getCanonicalName(),
                replaceMap);
        writeNefAndManifestFiles(compUnit);
    }

    public static void writeNefAndManifestFiles(CompilationUnit compUnit) throws IOException {
        Path buildNeow3jPath = Paths.get("build", "neow3j");
        buildNeow3jPath.toFile().mkdirs();
        writeNefFile(compUnit.getNefFile(), compUnit.getManifest().getName(), buildNeow3jPath);

        // Write manifest to the disk
        writeContractManifestFile(compUnit.getManifest(), buildNeow3jPath);
    }

}
