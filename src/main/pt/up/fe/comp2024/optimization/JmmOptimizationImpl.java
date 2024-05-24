package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Ollir;
import org.specs.comp.ollir.OllirErrorException;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.report.StageResult;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        boolean optimize1 = semanticsResult.getConfig().containsKey("optimize");
        //Jmm optimization
        if(optimize1){
            //TODO OPTIMIZE HERE
            if(semanticsResult.getConfig().get("optimize").equals("true")){
                boolean optimizedFold = false;
                boolean optimizedProp = false;
                ConstantFold constantFold = new ConstantFold();
                constantFold.buildVisitor();
                ConstantPropagation constantPropagation = new ConstantPropagation();
                constantPropagation.buildVisitor();

                do {
                    constantFold.setOptimized(false);
                    constantFold.setRootNode(semanticsResult.getRootNode());
                    constantFold.run();
                    optimizedFold = constantFold.isOptimized();
                    constantPropagation.setOptimized(false);
                    constantPropagation.setRootNode(semanticsResult.getRootNode());
                    constantPropagation.run();
                    optimizedProp = constantPropagation.isOptimized();
                    String checker = semanticsResult.getRootNode().toTree();
                    var delete = ";";

                } while (optimizedFold || optimizedProp);

                //TODO OPTIMIZE HERE
            }
        }

        String checker = semanticsResult.getRootNode().toTree();
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        int numReg = ollirResult.getConfig().containsKey("registerAllocation") ? Integer.parseInt(ollirResult.getConfig().get("registerAllocation")) : -1;
        if (numReg >= 0) { // Register Allocation
            for (Method method : ollirResult.getOllirClass().getMethods()) {
                method.buildCFG();
                method.buildVarTable();
                if(method.isConstructMethod()){
                    continue;
                }
                DataFlowAnalysis dataFlowAnalysis = new DataFlowAnalysis(method);
                dataFlowAnalysis.run();

                Map<Instruction, Set<String>> in = dataFlowAnalysis.getIn();
                Map<Instruction, Set<String>> out = dataFlowAnalysis.getOut();

                Graph graph = new Graph(in, out, method);
                graph.run();
                if(graph.getMinReg() > numReg && numReg > 0){
                    System.out.println("The number of registers is not enough");
                    ollirResult.getReports().add(Report.newError(Stage.OPTIMIZATION, -1, -1, "The number of registers is not enough the number needed is :" + graph.getMinReg(),null));
                    return ollirResult;
                }
            }
        }

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }
}
