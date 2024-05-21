package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import org.specs.comp.ollir.OllirErrorException;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        boolean optimize1 = ollirResult.getConfig().containsKey("optimize");
        var method = ollirResult.getOllirClass().getMethods().get(0);
        method.buildCFG();

        if(optimize1){
            if(ollirResult.getConfig().get("optimize").equals("true")){
                boolean optimized = false;
                ConstantFold constantFold = new ConstantFold();
                do {

                    constantFold.run(ollirResult.getOllirClass());
                    optimized = constantFold.isOptimized();
                } while (optimized);

                //TODO OPTIMIZE HERE
            }
        }
        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }
}
