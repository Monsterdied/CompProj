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
            use.put(instruction, computeUse(instruction));
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

    private Set<Operand> computeUse(Instruction instruction) {
        Set<Operand> use = new HashSet<>();
        return use;
        switch (instruction.getInstType()) {
            case ASSIGN:
                use.addAll(assignUses((AssignInstruction) instruction));
                break;
            case CALL:
                use.addAll(callUses((CallInstruction) instruction));
                break;
            case BINARYOPER:
                //use.addAll(binaryOperUses((BinaryOperInstruction) instruction));
                break;
            /*case BRANCH ->
            case RETURN ->
            case PUTFIELD ->
            case GETFIELD ->
            case UNARYOPER ->
            case NOPER ->*/
        }
        return use;
    }
    private Set<Operand> assignUses(AssignInstruction assignInstruction) {
        return computeUse(assignInstruction.getRhs());
    }
    private Set<Operand> callUses(CallInstruction callInstruction) {
        Set<Operand> use = new HashSet<>();
        for (Element arg : callInstruction.getArguments()) {
            if(arg instanceof Operand)
                use.add((Operand) arg);
        }
        return use;
    }
    private Set<Operand> binaryOperUses(BinaryOpInstruction binaryOperInstruction) {
        Set<Operand> use = new HashSet<>();
        //use.addAll(computeUse(binaryOperInstruction.getLhs()));
        //use.addAll(computeUse(binaryOperInstruction.getRhs()));
        return use;
    }
}

