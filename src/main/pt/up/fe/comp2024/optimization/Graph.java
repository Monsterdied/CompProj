package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;

import java.util.*;

public class Graph {
    Set<String> nodes;
    Map<String, Set<String>> edges;
    Map<Instruction, Set<String>> in;
    Map<Instruction, Set<String>> out;
    HashMap<String, Descriptor> old_varTable;
    Method method;

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

        System.out.println("Hello world");
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

}
