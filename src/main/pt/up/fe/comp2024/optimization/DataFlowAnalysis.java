package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;

import java.util.*;

public class DataFlowAnalysis {

    private Method method;
    private Map<Instruction, Set<String>> def;
    private Map<Instruction, Set<String>> use;
    private Map<Instruction, Set<String>> in;
    private Map<Instruction, Set<String>> out;

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
            stable = true;
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
                    if (!isVisited[predecessor.getId()] && !stack.contains(predecessor) && predecessor.getId() != 0) {
                        stack.push((Instruction) predecessor);
                    }
                }

                if (instruction.getId() == 0) { // If END node or BEGIN node
                    continue;
                }

                Set<String> inSet = new HashSet<>();
                Set<String> outSet = new HashSet<>(out.get(instruction));

                // OUT(B) = ∪ IN(s)
                for (Node successor : instruction.getSuccessors()) {
                    if (successor.getId() != 0) { // If not BEGIN or END node
                        Set<String> tmpOutSet = new HashSet<>(in.get((Instruction) successor));
                        outSet.addAll(tmpOutSet);
                    }
                }

                // IN(B) = Use(B) ∪ (OUT(B) - Def(B))
                Set<String> useSet = new HashSet<>(use.get(instruction));
                Set<String> defSet = new HashSet<>(def.get(instruction));

                inSet.addAll(outSet);
                inSet.removeAll(defSet);
                inSet.addAll(useSet);

                if (!in.get(instruction).equals(inSet) || !out.get(instruction).equals(outSet)) {
                    stable = false;
                }

                in.put(instruction, inSet);
                out.put(instruction, outSet);
            }
        }
    }

    private Set<String> computeDef(Instruction instruction, Map<String, Descriptor> varTable) {
        Set<String> def = new HashSet<>();

        if (instruction.getInstType() == instruction.getInstType().ASSIGN) {
            AssignInstruction assignInstruction = (AssignInstruction) instruction;
            Element dest = assignInstruction.getDest();
            //Type type = dest.getType();

            if (dest.isLiteral()) {
                return null;
            }

            def.add(((Operand) dest).getName());
        }

        return def;
    }

    private Set<String> computeUse(Instruction instruction) {
        Set<String> use = new HashSet<>();
        switch (instruction.getInstType()) {
            case ASSIGN:
                use.addAll(assignUses((AssignInstruction) instruction));
                break;
            case CALL:
                use.addAll(callUses((CallInstruction) instruction));
                break;
            case BINARYOPER:
                use.addAll(binaryOperUses((BinaryOpInstruction) instruction));
                break;
            case BRANCH:
                use.addAll(branchOpUses((SingleOpCondInstruction) instruction));
                break;
            case RETURN:
                use.addAll(returnUses((ReturnInstruction) instruction));
                break;
            /*case PUTFIELD ->*/
            /*case GETFIELD:
                use.addAll(getFieldUses( instruction));
                break;*/
            case UNARYOPER:
                use.addAll(noperUses((SingleOpInstruction) instruction));
                break;
            case NOPER:
                use.addAll(noperUses((SingleOpInstruction) instruction));
                break;

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

        if(binaryOperInstruction.getLeftOperand() instanceof Operand)
            use.add((Operand) binaryOperInstruction.getLeftOperand());
        if(binaryOperInstruction.getRightOperand() instanceof Operand)
            use.add((Operand) binaryOperInstruction.getRightOperand());
        return use;
    }
    private Set<Operand> branchOpUses(SingleOpCondInstruction Branch) {
        return computeUse(Branch.getCondition());
    }
    private Set<Operand> returnUses(ReturnInstruction returnInstruction) {
        Set<Operand> use = new HashSet<>();
        if(returnInstruction.getOperand() instanceof Operand)
            use.add((Operand) returnInstruction.getOperand());
        return use;
    }
    private Set<Operand> noperUses(SingleOpInstruction instruction) {
        if(instruction.getSingleOperand() instanceof Operand)
            return Set.of((Operand) instruction.getSingleOperand());
        return new HashSet<>();
    }
    private Set<Operand> getFieldUses(Instruction instruction) {
        /*if(instruction.getSingleOperand() instanceof Operand)
            return Set.of((Operand) instruction.getSingleOperand());*/
        return new HashSet<>();
    }

    public Map<Instruction, Set<String>> getIn() {
        return in;
    }

    public Map<Instruction, Set<String>> getOut() {
        return out;
    }
}

