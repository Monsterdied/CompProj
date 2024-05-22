package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;
import java.util.HashMap;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        int numReg = ollirResult.getConfig().containsKey("registerAllocation") ? Integer.parseInt(ollirResult.getConfig().get("registerAllocation")) : -1;
        boolean optimize1 = ollirResult.getConfig().containsKey("optimize");

        if (numReg >= 0) { // Register Allocation
            for (Method method : ollirResult.getOllirClass().getMethods()) {
                method.buildCFG();
                method.buildVarTable();

                DataFlowAnalysis dataFlowAnalysis = new DataFlowAnalysis(method);
                dataFlowAnalysis.run();

                HashMap<String, Descriptor> old_varTable = method.getVarTable();



                System.out.println("Hello world");
            }
        }

        if (optimize1) {
            if (ollirResult.getConfig().get("optimize").equals("true")) {
                //TODO OPTIMIZE HERE
            }
        }
        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }
}
