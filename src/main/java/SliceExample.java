import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class SliceExample {
    public static void main(String[] args) throws IOException, ClassHierarchyException, CancelException {
// input file path
        String inputFile = "./input/SliceSample.class";
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

        PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();

        CGNode cgNode = findMainMethod(cg);

        // find println statement in Main method
        Statement statement = findStatement(cgNode, "println");

        Collection<Statement> slice = Slicer.computeBackwardSlice(statement, cg, pointerAnalysis, Slicer.DataDependenceOptions.NO_BASE_PTRS, Slicer.ControlDependenceOptions.NONE);

        dumpSlice(slice);
    }

    private static CGNode findMainMethod(CallGraph cg) {
        return cg.stream()
                .parallel()
                .filter(i -> i.getMethod().getName().toString().equals("main"))
                .filter(i -> i.getMethod().getDescriptor().toString().equals("([Ljava/lang/String;)V"))
                .findAny()
                .orElse(null);
    }

    public static Statement findStatement(CGNode cgNode, String methodName) {
        IR ir = cgNode.getIR();
        for (SSAInstruction inst : ir.getInstructions()) {
            if (inst instanceof SSAAbstractInvokeInstruction) {
                SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) inst;
                if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName))
                    return new NormalStatement(cgNode, call.iIndex());
            }
        }
        return null;
    }

    public static void dumpSlice(Collection<Statement> slice) {
        System.out.println("dump slice:");
        for (Statement s : slice) {
            if (!s.toString().contains("wala"))
                System.out.println(s);
        }
    }
}
