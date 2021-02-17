import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;

public class PointerAnalysisExample {
    public static void main(String[] args) throws IOException, WalaException, CallGraphBuilderCancelException {
        // input file path
        String inputFile = "./input/PointerSample.class";
        // exclusion text file path
        File exclusionsFile = new File("./Exclusions.txt");
        // make AnalysisScope
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(inputFile, exclusionsFile);
        // make ClassHierarchy
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);
        Iterable<Entrypoint> entryPoints = Util.makeMainEntrypoints(scope, cha);
        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
        AnalysisCache cache = new AnalysisCacheImpl();
        CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);

        CallGraph cg = builder.makeCallGraph(options, null);

        PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();

        for (PointerKey pk : pointerAnalysis.getPointerKeys()) {
            if (pk instanceof LocalPointerKey) {
                LocalPointerKey lpk = (LocalPointerKey) pk;
                String className = lpk.getNode().getMethod().getDeclaringClass().getName().toString();
                String methodName = lpk.getNode().getMethod().getName().toString();
                ClassLoaderReference clr = lpk.getNode().getMethod().getDeclaringClass().getClassLoader().getReference();
                if (clr.equals(ClassLoaderReference.Application) && className.equals("LPointerSample") && methodName.equals("foo")) {
                    if (lpk.getValueNumber() == 1 || lpk.getValueNumber() == 2) {
                        System.out.println("Object o" + lpk.getValueNumber() + ": ");
                        for (InstanceKey ik : pointerAnalysis.getPointsToSet(lpk)) {
                            System.out.println(ik.toString());
                        }
                    }
                }
            }
        }
    }
}
