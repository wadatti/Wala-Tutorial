import com.ibm.wala.classLoader.Language;
import com.ibm.wala.demandpa.alg.ContextSensitiveStateMachine;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.refinepolicy.*;
import com.ibm.wala.demandpa.alg.statemachine.DummyStateMachine;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.PABasedMemoryAccessMap;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class DemandPtrAnalysis {
    public static void main(String[] args) throws ClassHierarchyException, IOException, CallGraphBuilderCancelException {
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

        CallGraphBuilder<InstanceKey> cgBuilder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);

        CallGraph cg = cgBuilder.makeCallGraph(options, null);
        PointerAnalysis<InstanceKey> pa = cgBuilder.getPointerAnalysis();

        MemoryAccessMap mam = new PABasedMemoryAccessMap(cg, pa);

        SSAPropagationCallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, cache, cha, scope);

        DemandRefinementPointsTo dmp = DemandRefinementPointsTo.makeWithDefaultFlowGraph(cg, builder, mam, cha, options, new ContextSensitiveStateMachine.Factory());

        dmp.setRefinementPolicyFactory(new SinglePassRefinementPolicy.Factory(new AlwaysRefineFieldsPolicy(), new AlwaysRefineCGPolicy()));

        CGNode mainMethod = findMainMethod(dmp.getBaseCallGraph());

        PointerKey keyToQuery = getParam(mainMethod, "foo", dmp.getHeapModel(), 0);

        Collection<InstanceKey> pointsTo = dmp.getPointsTo(keyToQuery);

        for (InstanceKey ik : pointsTo) {
            System.out.println(ik);
        }
    }

    private static CGNode findMainMethod(CallGraph cg) {
        return cg.stream()
                .parallel()
                .filter(i -> i.getMethod().getName().toString().equals("main"))
                .filter(i -> i.getMethod().getDescriptor().toString().equals("([Ljava/lang/String;)V"))
                .findAny()
                .orElse(null);
    }

    public static PointerKey getParam(CGNode cgNode, String methodName, HeapModel heapModel, int paramNum) {
        IR ir = cgNode.getIR();
        for (SSAInstruction inst : ir.getInstructions()) {
            if (inst instanceof SSAInvokeInstruction) {
                SSAInvokeInstruction call = (SSAInvokeInstruction) inst;
                if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
                    return heapModel.getPointerKeyForLocal(cgNode, call.getUse(paramNum));
                }
            }
        }
        return null;
    }
}
