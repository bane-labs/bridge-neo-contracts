package network.bane.scripts.compile;

import io.neow3j.compiler.CompilationUnit;
import network.bane.message.MessageBridgeContract;

import java.io.IOException;
import java.nio.file.Path;

import static network.bane.scripts.compile.CompilationHelper.compileContract;
import static network.bane.scripts.compile.CompilationHelper.printCompilationResults;
import static network.bane.scripts.compile.CompilationHelper.printWriteFilesResults;
import static network.bane.scripts.compile.CompilationHelper.writeNefAndManifestFiles;

public class CompileMessageBridgeToFiles {

    public static void main(String[] args) throws IOException {
        String contractToCompile = MessageBridgeContract.class.getCanonicalName();
        CompilationUnit compUnit = compileContract(contractToCompile);

        System.out.println("------------------------------");
        printCompilationResults(compUnit, contractToCompile);
        Path path = writeNefAndManifestFiles(compUnit);
        printWriteFilesResults(path, compUnit.getManifest().getName());
        System.out.println("------------------------------");
    }

}
