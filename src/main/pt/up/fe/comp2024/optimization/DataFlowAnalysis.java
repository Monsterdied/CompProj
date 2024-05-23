package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;

import java.util.*;

public class DataFlowAnalysis {

    private Method method;
    private Map<Instruction, Set<Operand>> def;
    private Map<Instruction, Set<Operand>> use;
    private Map<Instruction, Set<Operand>> in;
    private Map<Instruction, Set<Operand>> out;

    public DataFlowAnalysis(Method method) {
        this.method = method;
        this.def = new HashMap<>();
        this.use = new HashMap<>();
        this.in = new HashMap<>();
        this.out = new HashMap<>();
    }

    public void run() {
        List<Instruction> instructions = method.getInstructions();

        for (Instruction instruction : instructions) {
            def.put(instruction, computeDef(instruction, method.getVarTable()));
            use.put(instruction, computeUse(instruction));
            in.put(instruction, new HashSet<>());
            out.put(instruction, new HashSet<>());
        }

        boolean stable = false;

        while (!stable) {
            Stack<Instruction> stack = new Stack<>();
            for (Node pred : method.getEndNode().getPredecessors()) {
                stack.push((Instruction) pred);
            }

            boolean isVisited[] = new boolean[instructions.size() + 1];
            Arrays.fill(isVisited, false);

            while (!stack.isEmpty()) {
                Instruction instruction = stack.pop();
                isVisited[instruction.getId()] = true;

                for (Node predecessor : instruction.getPredecessors()) {
                    if (!isVisited[predecessor.getId()] || !stack.contains(predecessor) || predecessor.getId() != 0) {
                        stack.push((Instruction) predecessor);
                    }
                }

                if (instruction.getId() == 0) { // If END node or BEGIN node
                    continue;
                }

                Set<Operand> inSet = new HashSet<>();
                Set<Operand> outSet = new HashSet<>();

                // OUT(B) = ∪ IN(s)
                for (Node successor : instruction.getSuccessors()) {
                    if (successor.getId() != 0) { // If not BEGIN or END node
                        outSet.addAll(in.get((Instruction) successor));
                    }
                }

                // IN(B) = Use(B) ∪ (OUT(B) - Def(B))
                inSet.addAll(use.get(instruction));
                List<Operand> inTmp = new ArrayList<>();
                inTmp.addAll(out.get(instruction));
                inTmp.removeAll(def.get(instruction));
                inSet.addAll(inTmp);

                stable = in.equals(inSet) && out.equals(outSet);

                in.put(instruction, inSet);
                out.put(instruction, outSet);
            }
        }
    }

    private Set<Operand> computeDef(Instruction instruction, Map<String, Descriptor> varTable) {
        Set<Operand> def = new HashSet<>();

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
        /*switch (instruction.getInstType()) {
            case ASSIGN:
                use.addAll(assignUses((AssignInstruction) instruction));
                break;
            case CALL:
                use.addAll(callUses((CallInstruction) instruction));
                break;
            case BINARYOPER:
                //use.addAll(binaryOperUses((BinaryOperInstruction) instruction));
                break;*/
            /*case BRANCH ->
            case RETURN ->
            case PUTFIELD ->
            case GETFIELD ->
            case UNARYOPER ->
            case NOPER ->
        }*/
        //return use;
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

