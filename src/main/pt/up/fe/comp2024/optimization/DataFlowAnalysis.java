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

