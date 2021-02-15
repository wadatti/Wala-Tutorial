import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.viz.DotUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

public class ClassHierarchyOutput {
    public static void main(String[] args) throws IOException, WalaException {
        String inputFile = "./input/SampleMain.class";
        File exclusionsFile = new File("./Exclusions.txt");
        AnalysisScope scope = AnalysisScopeReader
                .makeJavaBinaryAnalysisScope(inputFile, exclusionsFile);
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);

        Graph<IClass> g = typeHierarchy2Graph(cha);
        g = pruneForAppLoader(g);


        String dotFile = File.createTempFile("out", ".dot").getAbsolutePath();
        String pdfFile = "./ClassHierarchy.pdf";
        String dotExe = "dot";
        DotUtil.dotify(g, null, dotFile, pdfFile, dotExe);
    }

    public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) {
        Collection<T> slice = GraphSlicer.slice(g, f);
        return GraphSlicer.prune(g, new CollectionFilter<>(slice));
    }

    public static Graph<IClass> pruneForAppLoader(Graph<IClass> g) {
        Predicate<IClass> f = new Predicate<IClass>() {
            @Override
            public boolean test(IClass iClass) {
                return (iClass.getClassLoader().getReference().equals(ClassLoaderReference.Application));
            }
        };
        return pruneGraph(g, f);
    }

    public static Graph<IClass> typeHierarchy2Graph(IClassHierarchy cha) {
        Graph<IClass> result = SlowSparseNumberedGraph.make();
        for (IClass c : cha) {
            result.addNode(c);
        }
        for (IClass c : cha) {
            for (IClass x : cha.getImmediateSubclasses(c)) {
                result.addEdge(c, x);
            }
            if (c.isInterface()) {
                for (IClass x : cha.getImplementors(c.getReference())) {
                    result.addEdge(c, x);
                }
            }
        }
        return result;
    }
}
