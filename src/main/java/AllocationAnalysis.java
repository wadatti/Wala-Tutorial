import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class AllocationAnalysis {
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
        CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, cache, cha, scope);

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
                            for (Iterator<Pair<CGNode, NewSiteReference>> it = ik.getCreationSites(cg); it.hasNext(); ) {
                                Pair<CGNode, NewSiteReference> pair = it.next();
                                CGNode cgNode = pair.fst;
                                System.out.println("\tClass: " + cgNode.getMethod().getDeclaringClass().getName());
                                System.out.println("\tMethod: " + cgNode.getMethod().getName());
                                System.out.println("\tAllocationSite: " + pair.snd.toString());
                            }
                        }
                    }
                }
            }
        }
    }
}
