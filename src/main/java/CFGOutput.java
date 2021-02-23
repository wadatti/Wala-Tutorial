import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.util.CallGraphSearchUtil;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

import java.io.File;
import java.io.IOException;

public class CFGOutput {
    public static void main(String[] args) throws CallGraphBuilderCancelException, WalaException, IOException {
        // input file path
        String inputFile = "./input/CFGSample.class";
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
        // Get main method CGNode
        CGNode mainNode = CallGraphSearchUtil.findMainMethod(cg);

        SSACFG mainCFG = mainNode.getIR().getControlFlowGraph();

        String dotFile = File.createTempFile("out", ".dot").getAbsolutePath();
        String pdfFile = "./ControlFlowGraph.pdf";
        String dotExe = "dot";
        DotUtil.dotify(mainCFG, PDFViewUtil.makeIRDecorator(mainNode.getIR()), dotFile, pdfFile, dotExe);

    }
}
