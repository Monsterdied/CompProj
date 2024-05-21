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
        boolean optimize1 = semanticsResult.getConfig().containsKey("optimize");
        //Jmm optimization
        if(optimize1){
            //TODO OPTIMIZE HERE
            if(semanticsResult.getConfig().get("optimize").equals("true")){
                boolean optimized = false;
                ConstantFold constantFold = new ConstantFold();
                constantFold.buildVisitor();

                do {
                    constantFold.setOptimized(false);
                    constantFold.setRootNode(semanticsResult.getRootNode());
                    constantFold.run();
                    optimized = constantFold.isOptimized();
                } while (optimized);

                //TODO OPTIMIZE HERE
            }
        }



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

        }
        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }
}
