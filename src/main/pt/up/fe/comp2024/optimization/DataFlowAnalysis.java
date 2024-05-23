package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;

import java.util.*;

public class DataFlowAnalysis {

    private Method method;
    private Map<Node, List<Operand>> def;
    private Map<Node, List<Operand>> use;
    private Map<Node, List<Operand>> in;
    private Map<Node, List<Operand>> out;

    public DataFlowAnalysis(Method method) {
        this.method = method;
        this.def = new HashMap<>();
        this.use = new HashMap<>();
        this.in = new HashMap<>();
        this.out = new HashMap<>();
    }

    public void run() {
        List<Instruction> instructions = method.getInstructions();
        //instructions = Collections.reverse(instructions.clone());

        for (Instruction instruction : instructions) {
            def.put(instruction, computeDef(instruction, method.getVarTable()));
            //use.put(instruction, computeUse(instruction, method.getVarTable()));
            in.put(instruction, new ArrayList<>());
            out.put(instruction, new ArrayList<>());
        }

        /*boolean stable = false;
        while (!stable) {

        }*/
    }

    private List<Operand> computeDef(Instruction instruction, Map<String, Descriptor> varTable) {
        List<Operand> def = new ArrayList<>();

        if (instruction.getInstType() == instruction.getInstType().ASSIGN) {
            AssignInstruction assignInstruction = (AssignInstruction) instruction;
            Element dest = assignInstruction.getDest();
            //Type type = dest.getType();

            if (dest.isLiteral()) {
                return null;
            }

            def.add((Operand) dest);
        }

        return def;
    }

    /*private List<Operand> computeUse(Instruction instruction) {
        List<Operand> use = new ArrayList<>();

        /*switch (instruction.getInstType()) {
            case ASSIGN ->
            case CALL ->
            case GOTO ->
            case BRANCH ->
            case RETURN ->
            case PUTFIELD ->
            case GETFIELD ->
            case UNARYOPER ->
            case BINARYOPER ->
            case NOPER ->
        }
    }*/
}

