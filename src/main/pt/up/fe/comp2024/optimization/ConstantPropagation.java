package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ConstantPropagation extends AJmmVisitor<Void,Void> {
    private JmmNode rootNode;
    private boolean optimized = false;
    private boolean changeVariables = true;
    HashMap<String,String> ConstantToValues = new HashMap<>(); //maps the variable name to its value
    Set<String> DeclarationsToNotBeDeleted = new HashSet<>(); //maps the variable name to the declaration node
    Set<JmmNode> AssignmentsToBeDeleted = new HashSet<>(); //maps the variable name to the assignment node
    Map<String,JmmNode> LatestAssignments = new HashMap<>(); //maps the variable name to the latest assignment node
    Map<String,JmmNode> NameToDeclarationNode = new HashMap<>(); // given the name of the variable, returns the declaration node

    public void buildVisitor(){
        addVisit(ASSIGN_STMT, this::visitWithAssign);
        addVisit(VAR_DECL, this::visitWithVarDecl);
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(WHILE_CONDITION, this::visitWhileStmt);
        addVisit(METHOD_DECL, this::visitMethod);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        setDefaultVisit(this::defaultVisitor);
    }
    public void setOptimized(boolean optimized){
        this.optimized = optimized;
    }
    public boolean isOptimized(){
        return this.optimized;
    }
    public void setRootNode(JmmNode rootNode){
        this.rootNode = rootNode;
    }

    public Void defaultVisitor(JmmNode node , Void unused){
        for(JmmNode node1 : node.getChildren()){
            visit(node1);
        }
        return unused;
    }
    public Void visitMethod(JmmNode node , Void unused){
        this.ConstantToValues.clear();
        this.DeclarationsToNotBeDeleted.clear();
        this.AssignmentsToBeDeleted.clear();
        this.LatestAssignments.clear();
        this.NameToDeclarationNode.clear();
        for(JmmNode node1 : node.getChildren()){
            visit(node1);
        }
        for(JmmNode n : this.AssignmentsToBeDeleted){
            n.getParent().removeChild(n);
        }
        for(JmmNode n : this.NameToDeclarationNode.values()){
            if(! this.DeclarationsToNotBeDeleted.contains(n.get("name"))){
                n.getParent().removeChild(n);
            }
        }
        return unused;
    }
    public Void visitWithVarDecl(JmmNode node , Void unused){
        String var = node.get("name");
        this.NameToDeclarationNode.put(var,node);
        return unused;
    }
    public Void visitWhileStmt(JmmNode node , Void unused){
        var condition = node.getChildren().get(0).getChildren().get(0);
        var body = node.getChildren().get(0).getChildren().get(1);
        if(! this.changeVariables){
            visit(body); // if we visit the code only once to get the view of the variables before the while
            return unused;
        }
        HashMap<String,String> constantToValuesTmp  = new HashMap<>();
        constantToValuesTmp.putAll(this.ConstantToValues);
        this.changeVariables = false;
        visit(body);
        this.changeVariables = true;
        for(Map.Entry<String,String> entry : constantToValuesTmp.entrySet()){
            String VarName = entry.getKey();
            String value = entry.getValue();
            boolean removeCondition =
                    this.ConstantToValues.containsKey(VarName) && ! this.ConstantToValues.get(VarName).equals(value)
                            || ! this.ConstantToValues.containsKey(VarName);
            if(removeCondition){
                // se o valor da variavel mudou durante o while nos não podemos garantir que o valor da variavel é constante durante o while
                this.AssignmentsToBeDeleted.remove(this.LatestAssignments.get(VarName));//garantir que não apagamos a ultima atribuição da variavel porque vamos precisar dela
                this.ConstantToValues.remove(entry.getKey());
            }
        }
        //depois de termos a certeza que o valor da variavel é constante durante o while, podemos propagar o valor da variavel
        visit(condition);
        visit(body);
        return unused;
    }
    public Void visitVarRef(JmmNode node , Void unused){
        String var = node.get("name");
        var parent = node.getParent();// TODO EXPAND THIS FOR LOOP conditions
        if(this.ConstantToValues.containsKey(var) && this.changeVariables){
            String valueType = NameToDeclarationNode.get(var).getChild(0).get("name");
            if(valueType.equals("int") ){//TODO expand this for other types
                JmmNode newNode = new JmmNodeImpl("IntegerLiteral");
                newNode.put("value",this.ConstantToValues.get(var));
                int childIndex = parent.removeChild(node);
                parent.add(newNode,childIndex);
                this.optimized = true;
            }
            if(valueType.equals("boolean")){//TODO expand this for other types
                JmmNode newNode = new JmmNodeImpl("BooleanLiteral");
                newNode.put("value",this.ConstantToValues.get(var));
                int childIndex = parent.removeChild(node);
                parent.add(newNode,childIndex);
                this.optimized = true;
            }
        }
        return unused;
    }
    public Void visitIfElseStmt(JmmNode node ,Void unused){
        var condition = node.getChildren().get(0).getChildren().get(0);
        var ifBody = node.getChildren().get(0).getChildren().get(1);
        var elseBody = node.getChildren().get(0).getChildren().get(2);
        if(! this.changeVariables){
            visit(ifBody);
            visit(elseBody);
            return unused;
        }
        visit(condition);
        HashMap<String,String> constantToValuesTmp  = new HashMap<>();//constant values before the if statement
        constantToValuesTmp.putAll(this.ConstantToValues);
        HashMap<String,JmmNode> latestAssigmentsTmp  = new HashMap<>();//constant values before the if statement
        latestAssigmentsTmp.putAll(this.LatestAssignments);
        visit(ifBody);
        HashMap<String,String> constantToValuesTmpThen  = new HashMap<>();
        constantToValuesTmpThen.putAll(this.ConstantToValues);
        HashMap<String,JmmNode> latestAssigmentsThen  = new HashMap<>();//constant values before the if statement
        latestAssigmentsThen.putAll(this.LatestAssignments);
        this.LatestAssignments.clear();
        this.LatestAssignments.putAll(latestAssigmentsTmp);
        this.ConstantToValues.clear();
        this.ConstantToValues.putAll(constantToValuesTmp);
        visit(elseBody);
        // the constant values that are being iterated are the ones from the else statement
        for(Map.Entry<String,String> entry : constantToValuesTmp.entrySet()){
            String VarName = entry.getKey();
            String value = entry.getValue();
            boolean removeCondition = // case where the value changes in the else statement
                    this.ConstantToValues.containsKey(VarName) && ! this.ConstantToValues.get(VarName).equals(value)
                            // case where the value changes in the then statement
                    || (constantToValuesTmpThen.containsKey(VarName) && ! constantToValuesTmpThen.get(VarName).equals(value) )
                    // case where the value is not constant in the else statement
                    || ! this.ConstantToValues.containsKey(VarName)
                    // case where the value is not constant in the then statement
                    ||  !constantToValuesTmpThen.containsKey(VarName);
            if(removeCondition){
                // se o valor da variavel mudou durante o while nos não podemos garantir que o valor da variavel é constante durante o while
                this.AssignmentsToBeDeleted.remove(this.LatestAssignments.get(VarName));//garantir que não apagamos a ultima atribuição da variavel porque vamos precisar dela
                this.ConstantToValues.remove(entry.getKey());
            }
            constantToValuesTmpThen.remove(entry.getKey());
            this.ConstantToValues.remove(entry.getKey());
        }
        for(Map.Entry<String,String> entry : constantToValuesTmpThen.entrySet()){
            this.AssignmentsToBeDeleted.remove(latestAssigmentsThen.get(entry.getKey()));
        }
        for(Map.Entry<String,String> entry : this.ConstantToValues.entrySet()){
            this.AssignmentsToBeDeleted.remove(this.LatestAssignments.get(entry.getKey()));
        }
        this.LatestAssignments.clear();
        this.LatestAssignments.putAll(latestAssigmentsTmp);
        this.ConstantToValues.clear();
        this.ConstantToValues.putAll(constantToValuesTmp);
        return unused;
    }
    public Void visitWithAssign(JmmNode node , Void unused){
        JmmNode lhs = node.getChildren().get(0);
        JmmNode rhs = node.getChildren().get(1);
        visit(rhs);
        if(lhs.getKind().equals("VarRefExpr") && rhs.getKind().equals("IntegerLiteral") && node.getChildren().size() == 2){
            //this.DeclarationsToBeDeleted.add(lhs.get("name"));
            this.ConstantToValues.put(lhs.get("name"),rhs.get("value"));
            this.AssignmentsToBeDeleted.add(node);
            //TODO continue this
            //this.optimized = true;
        }else{
            this.ConstantToValues.remove(lhs.get("name"));
            this.DeclarationsToNotBeDeleted.add(lhs.get("name"));
        }
        if(lhs.getKind().equals("VarRefExpr") ){
            this.LatestAssignments.put(lhs.get("name"),node);
        }else{
            visit(lhs);
        }

        return unused;
    }
    public void run(){
        visit(rootNode);
    }
}
