package network.bane.scripts.compile;

import io.neow3j.compiler.CompilationUnit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static io.neow3j.contract.ContractUtils.writeContractManifestFile;
import static io.neow3j.contract.ContractUtils.writeNefFile;
import static java.lang.String.format;

public class CompilationHelper {

    static CompilationUnit compileContract(String contractToCompile) throws IOException {
        return new io.neow3j.compiler.Compiler().compile(contractToCompile);
    }

    static CompilationUnit compileContractWithSubstitutions(String contractToCompile,
            HashMap<String, String> replaceMap) throws IOException {
        return new io.neow3j.compiler.Compiler().compile(contractToCompile, replaceMap);
    }

    static Path writeNefAndManifestFiles(CompilationUnit compUnit) throws IOException {
        Path buildNeow3jPath = Paths.get("build", "neow3j");
        buildNeow3jPath.toFile().mkdirs();
        writeNefFile(compUnit.getNefFile(), compUnit.getManifest().getName(), buildNeow3jPath);

        // Write manifest to the disk
        writeContractManifestFile(compUnit.getManifest(), buildNeow3jPath);
        return buildNeow3jPath;
    }

    static void printWriteFilesResults(Path relativePath, String contractName) {
        String manifestPath = format("%s/%s.manifest.json", relativePath, contractName);
        String nefPath = format("%s/%s.nef", relativePath, contractName);
        System.out.printf("NEF File:      %s%n", nefPath);
        System.out.printf("Manifest File: %s%n", manifestPath);
    }

    static void printCompilationResults(CompilationUnit compUnit, String canonicalName) {
        String contractName = compUnit.getManifest().getName();
        System.out.println("Contract Compilation");
        System.out.printf("Class:         %s%n", canonicalName);
        System.out.printf("Contract Name: %s%n", contractName);
        System.out.printf("NEF Checksum:  %s%n", compUnit.getNefFile().getCheckSumAsInteger());
    }

}
