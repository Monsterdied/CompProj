package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;

import java.util.*;

public class Graph {
    private Set<String> nodes;
    private Map<String, Set<String>> edges;
    private Map<Instruction, Set<String>> in;
    private Map<Instruction, Set<String>> out;
    private HashMap<String, Descriptor> old_varTable;
    private Method method;
    private int minReg = 0;

    public Graph(Map<Instruction, Set<String>> in, Map<Instruction, Set<String>> out, Method method) {
        this.nodes = new HashSet<>();
        this.edges = new HashMap<>();
        this.old_varTable = method.getVarTable();
        this.in = in;
        this.out = out;
        this.method = method;
    }

    public void run() {
        for (Set<String> vars : in.values()) {
            nodes.addAll(vars);
        }


        Set<String> firstIns = in.get(((Instruction) method.getBeginNode().getSuccessors().get(0)));
        Collection<Set<String>> firstInsList = new HashSet<>();
        firstInsList.add(firstIns);

        addEdges(firstInsList);
        addEdges(out.values());
        if(nodes.isEmpty()){
            //System.out.println("Empty");
            return;
        }
        DSaturAlgorithm();
        System.out.println(this.minReg);
    }

    private void addEdges(Collection<Set<String>> in) {
        for (Set<String> ins : in) {
            for(String var1 : ins){
                if(! this.edges.containsKey(var1)){
                    this.edges.put(var1,new HashSet<>());
                }
                for(String var2 : ins){
                    if(! var1.equals(var2))
                        this.edges.get(var1).add(var2);
                }
            }

        }
    }
    public int getMinReg(){
        return this.minReg;
    }
    private void DSaturAlgorithm(){
        Comparator<String> comparator = new Comparator<>() {
            @Override
            public int compare(String o1, String o2) {
                return edges.get(o2).size() - edges.get(o1).size();
            }
        };
        List<String> nodesList = new ArrayList<>(nodes);
        nodesList.sort(comparator);// to have the priority on the nodes with more connections
        HashMap<String,Integer> colors = new HashMap<>();// VarName -> Register
        Set<Integer> neighboorsColors = new HashSet<>();
        for(String node : nodesList){
            neighboorsColors.clear();
            for(String neighbor : edges.get(node)){
                if(colors.containsKey(neighbor)){
                    neighboorsColors.add(colors.get(neighbor));
                }
            }
            for(int i = 0; i < nodes.size(); i++) {
                if (!neighboorsColors.contains(i)) {
                    colors.put(node, i);
                    break;
                }
            }
        }
        this.minReg = colors.values().stream().max(Integer::compareTo).get();
        for(String var : colors.keySet()){
            old_varTable.get(var).setVirtualReg(colors.get(var));
        }
    }

}
