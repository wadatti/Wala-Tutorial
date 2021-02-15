import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.viz.DotUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

public class CallGraphOutput {
    public static void main(String[] args) throws IOException, WalaException, CallGraphBuilderCancelException {
        // input file path
        String inputFile = "./input/sampleMain.class";
        // exclusion text file path
        File exclusionsFile = new File("./Exclusions.txt");
        // make AnalysisScope
        AnalysisScope scope = AnalysisScopeReader
                .makeJavaBinaryAnalysisScope(inputFile, exclusionsFile);
        // make ClassHierarchy
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);
        Iterable<Entrypoint> entryPoints = Util.makeMainEntrypoints(scope, cha);
        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
        AnalysisCache cache = new AnalysisCacheImpl();
        CallGraphBuilder<InstanceKey> builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
        CallGraph cg = builder.makeCallGraph(options, null);

        Graph<CGNode> g = pruneForAppLoader(cg);

        String dotFile = File.createTempFile("out", ".dot").getAbsolutePath();
        String pdfFile = "./prunedCallGraph.pdf";
        String dotExe = "dot";
        DotUtil.dotify(g, null, dotFile, pdfFile, dotExe);
    }

    public static Graph<CGNode> pruneForAppLoader(CallGraph g) {
        return pruneGraph(g, new ApplicationLoaderFilter());
    }

    public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) {
        Collection<T> slice = GraphSlicer.slice(g, f);
        return GraphSlicer.prune(g, new CollectionFilter<>(slice));
    }


    private static class ApplicationLoaderFilter implements Predicate<CGNode> {
        @Override
        public boolean test(CGNode cgNode) {
            if (cgNode == null) return false;
            return cgNode.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
        }
    }

}
