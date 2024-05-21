package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.classmap.FunctionClassMap;

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
                rhs= new SingleOpInstruction(new Element(new Type(ElementType.INT32)));
                return true;
            }
            return false;
        }

}
