package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;

import java.util.*;

public class DataFlowAnalysis {

    private Method method;
    private Map<Node, Set<Operand>> def;
    private Map<Node, Set<Operand>> use;
    private Map<Node, Set<Operand>> in;
    private Map<Node, Set<Operand>> out;

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
            //use.put(instruction, computeUse(instruction, method.getVarTable()));
            in.put(instruction, new HashSet<>());
            out.put(instruction, new HashSet<>());
        }

        boolean stable = false;

        while (!stable) {
            Stack<Node> stack = new Stack<>();
            stack.push(method.getEndNode());
            boolean isVisited[] = new boolean[instructions.size() + 1];
            Arrays.fill(isVisited, false);

            while (!stack.isEmpty()) {
                Node node = stack.pop();
                isVisited[node.getId()] = true;

                List<Operand> inSet = new ArrayList<>();
                List<Operand> outSet = new ArrayList<>();

                // OUT(B) = ∪ IN(s)
                for (Node successor : node.getSuccessors()) {
                    outSet.addAll(in.get(successor));
                }

                // IN(B) = Use(B) ∪ (OUT(B) - Def(B))
                inSet.addAll(use.get(node));
                List<Operand> inTmp = new ArrayList<>();
                inTmp.addAll(out.get(node));
                inTmp.removeAll(def.get(node));
                inSet.addAll(inTmp);

                for (Node predecessor : node.getPredecessors()) {
                    if (!isVisited[predecessor.getId()]) {
                        stack.push(predecessor);
                    }
                }
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

