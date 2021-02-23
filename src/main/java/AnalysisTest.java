import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;

public class AnalysisTest {
    public static void main(String[] args) throws IOException {
        String inputFile = "./input/test.jar";
        File exclusionsFile = new File("./Exclusions.txt");
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(inputFile, exclusionsFile);
    }
}
