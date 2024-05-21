package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import static pt.up.fe.comp2024.ast.Kind.BINARY_EXPR;

public class ConstantFold extends AJmmVisitor<Void,Void> {
    private JmmNode rootNode;
    private boolean optimized = false;

    public void buildVisitor(){
        addVisit(BINARY_EXPR, this::dealWithBinnaryOp);
        /*addVisit(PAREN_EXPR,this::parenExprVisit);
        addVisit(THIS_EXPR,this::visitThisExpr);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCall);
        addVisit(NEW_CLASS_EXPR, this::visitNewClass);
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(NOT_EXPR, this::visitNotExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(ARRAY_ACCESS_EXPR, this::visitVarRef);
        addVisit(NEW_ARRAY_EXPR,this::visitNewArray);
        addVisit(ARRAY_LENGTH_EXPR, this::visitArrayLength);
        addVisit(ARRAY_INIT_EXPRESSION,this::visitArrayInit);*/
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
        for(JmmNode node1 : node.getDescendants()){
            if(node1.getKind().equals("BinaryExpr")){
                var test = "ok";
            }
            visit(node1);
        }
    return unused;
    }
    public Void dealWithBinnaryOp(JmmNode node , Void unused){
        visit(node.getChildren().get(0));
        visit(node.getChildren().get(1));
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);
        if(left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")){
            int leftValue = Integer.parseInt(left.get("value"));
            int rightValue = Integer.parseInt(right.get("value"));
            int result = 0;
            switch(node.get("op")){
                case "+":
                    result = leftValue + rightValue;
                    break;
                case "-":
                    result = leftValue - rightValue;
                    break;
                case "*":
                    result = leftValue * rightValue;
                    break;
                case "/":
                    result = leftValue / rightValue;
                    break;
            }
            JmmNode prev = node.getParent();
            int index = prev.removeChild(node);
            JmmNodeImpl newNode = new JmmNodeImpl("IntegerLiteral");
            newNode.put("value",Integer.toString(result));
            prev.add(newNode,index);
            this.optimized = true;
        }

        return unused;
    }
    public void run(){
        visit(rootNode);
    }

}

/*
public class ConstantFold {
        private ClassUnit classUnit;
        private boolean optimized = false;
        Method currentMethod;
        private final FunctionClassMap<TreeNode, Boolean> generators = new FunctionClassMap<>();
        public ConstantFold() {
            this.currentMethod = null;
            generators.put(AssignInstruction.class, this::generateAssignBinarryOpInstruction);
        }
        private boolean apply(TreeNode node){
            var result = generators.applyTry(node);
            if(! result.isPresent()){
                return everthingElse(node);
            }
            return result.get();
        }
        public void run(ClassUnit classUnit) {
            this.optimized = false;
            this.classUnit = classUnit;
            for (var method : this.classUnit.getMethods()) {
                this.currentMethod = method;
                for (var instruction : method.getInstructions()) {
                    apply(instruction);
                }
            }
        }
        public boolean isOptimized(){
            return this.optimized;
        }
        private boolean everthingElse(TreeNode node){
            var children = node.getChildren();
            for (var child : children) {
                apply(child);
            }
            return false;
        }
        private boolean generateAssignBinarryOpInstruction(AssignInstruction instruction1){
            var rhs = instruction1.getRhs();
            var lhs = instruction1.getDest();
            if( ! (rhs instanceof BinaryOpInstruction)){
                return false;
            }
            BinaryOpInstruction instruction = (BinaryOpInstruction) rhs;
            var left = instruction.getLeftOperand();
            var right = instruction.getRightOperand();
            if(left.isLiteral() && right.isLiteral()){
                int leftLiteral = Integer.parseInt(((LiteralElement) left).getLiteral());
                int rightLiteral = Integer.parseInt(((LiteralElement) right).getLiteral());
                int temp = 0;
                switch (instruction.getOperation().getOpType()){
                    case ADD:
                        temp = leftLiteral + rightLiteral;
                        break;
                    case SUB:
                        temp = leftLiteral - rightLiteral;
                        break;
                    case MUL:
                        temp = leftLiteral * rightLiteral;
                        break;
                    case DIV:
                        temp = leftLiteral / rightLiteral;
                        break;
                }
                SingleOpInstruction newNode = new SingleOpInstruction(new LiteralElement(Integer.toString(temp),new Type(ElementType.INT32)));
                instruction1.addSucc(newNode);
                return true;
            }
            return false;
        }

}*/
