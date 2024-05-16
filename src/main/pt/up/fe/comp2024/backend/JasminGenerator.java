package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.lang.invoke.SwitchPoint;
import java.text.BreakIterator;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Map;
import java.util.stream.Collectors;

import static org.specs.comp.ollir.ElementType.ARRAYREF;
import static org.specs.comp.ollir.ElementType.VOID;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    Integer stackSize;
    String code;
    Integer labelCounterBranchs = 0;
    Integer maxStackSize = 0;

    Method currentMethod;
    //Map<String,Descriptor> varTable = new HashMap<>();
    ClassUnit currentClass;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;
        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpCondInstruction.class,this::generateSingleOpCondInstruction);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(PutFieldInstruction.class,this::PutFieldInstruction );
        generators.put(GetFieldInstruction.class,this::GetFieldInstruction );
        generators.put(UnaryOpInstruction.class,this::generateUnaryOpInstruction);
        generators.put(GotoInstruction.class,this::dealWithGoTo);
        generators.put(OpCondInstruction.class,this::generateOpCondInstruction);
        generators.put(ArrayOperand.class,this::generateArrayOperand);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {
        currentClass = classUnit;
        var code = new StringBuilder();
        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
        var superClass = classUnit.getSuperClass()== null ? "java/lang/Object" : classUnit.getSuperClass();
        code.append(".super ").append(superClass).append(NL);
        //code.append(".super java/lang/Object").append(NL);



        // generate code for all other methods
        for (var field : classUnit.getFields()){
            AccessModifier acces = field.getFieldAccessModifier();
            String accesType = "";
            switch (acces){
                case DEFAULT -> accesType = "";
                case PROTECTED -> accesType = "protected ";
                case PUBLIC -> accesType = "public ";
                case PRIVATE -> accesType = "private ";
            }
            code.append(".field ").append(accesType).append(field.getFieldName()).append(" ")
                    .append(field_to_jasmin(field.getFieldType()))
                    .append(NL);
        }
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                code.append(".method public <init>()V").append(NL)
                        .append(TAB).append("aload_0").append(NL)
                        .append(TAB).append("invokespecial ").append(superClass).append("/<init>(");
                        for (var param : method.getParams()) {
                            code.append(field_to_jasmin(param.getType()));
                        }
                                code.append(")V").append(NL)
                        .append(TAB).append("return").append(NL)
                        .append(".end method").append(NL);
                continue;
            }
            code.append(generators.apply(method));
        }
        return code.toString();
    }
    private String TypeToJasmin(Type type){
        switch (type.getTypeOfElement()){
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case VOID:
                return "V";
            case STRING:
                return "Ljava/lang/String;";
            case OBJECTREF:
                return "L"+getClass(((ClassType) type).getName())+";";
            case THIS:
                return "L" + getClass(((ClassType) type).getName()) + ";";
            default:
                return null;
        }
    }
    private String TypeToJasminArrayType(Type type){
        switch (type.getTypeOfElement()){
            case INT32:
                return "int";
            case BOOLEAN:
                return "boolean";
            case STRING:
                return "Ljava/lang/String;";
            case OBJECTREF:
                return getClass(((ClassType) type).getName());
            default:
                return null;
        }
    }
    // carefull here!!!! with probably wrong types or bugs
    private String field_to_jasmin(Type type){
        switch (type.getTypeOfElement()){
            case ARRAYREF:
                ArrayType t = (ArrayType) type;
                return "[" + field_to_jasmin(t.getElementType());
            default:
                return TypeToJasmin(type);
        }
    }
    private String PutFieldInstruction(PutFieldInstruction fieldInstruction){
        var code = new StringBuilder();
        Operand firstElement = (Operand) fieldInstruction.getChildren().get(0);
        Operand secondElement = (Operand) fieldInstruction.getChildren().get(1);
        Element thridElement = (Element)fieldInstruction.getChildren().get(2);
        code.append(generators.apply(firstElement)).append(NL).append(generators.apply(thridElement));
        code.append("putfield ").append(getClass(firstElement.getName())).append("/").append(secondElement.getName()).append(" ").append(field_to_jasmin(secondElement.getType()));
        subStackSize(2);//pops first and second operand
        return code.toString();
    }
    private String getClass(String className){

        if (className.equals("this")){
            return currentClass.getClassName();
        }
        for (String name : currentClass.getImports()){
            if (name.endsWith(className)){
                var result = name.replace(".","/");
                return result;
            }
        }
        return className;
    }
    private String GetFieldInstruction(GetFieldInstruction getFieldInstruction){
        var code = new StringBuilder();
        Operand firstOperand = (Operand) getFieldInstruction.getChildren().get(0);
        Operand secondOperand = (Operand) getFieldInstruction.getChildren().get(1);
        code.append(generators.apply(firstOperand)).append(NL);
        code.append("getfield ").append(getClass(firstOperand.getName())).append("/").append(secondOperand.getName()).append(" ");
        code.append(field_to_jasmin(secondOperand.getType())).append(NL);
        return code.toString();
    }
    private String generateMethod(Method method) {

        // set method
        currentMethod = method;
        var code = new StringBuilder();
        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded
        code.append("\n.method ").append(modifier);
        if (method.isStaticMethod() ){
            code.append("static ");
        }
        code.append(methodName).append("(");
        //build params
        for (var param : method.getParams()) {
            code.append(field_to_jasmin(param.getType()));
        }
        code.append(")").append(field_to_jasmin(method.getReturnType())).append(NL);
        // Add limits
        this.stackSize = 0;
        this.maxStackSize = 0;
        var codeTmp = new StringBuilder();
        for (var inst : method.getInstructions()) {
            for (var label : method.getLabels().entrySet()) {
                if (label.getValue().equals(inst)){
                    codeTmp.append(label.getKey()).append(":").append(NL);
                }
            }
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            codeTmp.append(instCode);
        }
        //code.append(TAB).append(".limit stack ").append(99).append(NL);
        code.append(TAB).append(".limit stack ").append(this.maxStackSize).append(NL);
        code.append(TAB).append(".limit locals ").append(this.getLocalLimits(method)).append(NL);
        if(this.stackSize > 1 || this.stackSize < 0){
            throw new NotImplementedException("");
        }
        code.append(codeTmp);
        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }
    private int getLocalLimits(Method method){
        HashSet<Integer> registers = new HashSet<>();
        registers.add(0);//Register 0 is this it can always be used even when it is not on the var table
        for (var local : method.getVarTable().values()){
            registers.add(local.getVirtualReg());
        }
        return registers.size();
    }
    private String checkIINC(AssignInstruction assign){
        var rhs = assign.getRhs();
        Operand lhs = (Operand)assign.getDest();
        if(rhs instanceof BinaryOpInstruction){
            var binary = (BinaryOpInstruction) rhs;
            var op = binary.getOperation().getOpType();
            if (op == OperationType.ADD || op == OperationType.SUB){
                var right = binary.getRightOperand();
                var left = binary.getLeftOperand();
                if((right.isLiteral() && !left.isLiteral()) || (!right.isLiteral() && left.isLiteral())){
                    LiteralElement literal = (LiteralElement)(right.isLiteral() ? right : left);
                    Operand operand = (Operand) (right.isLiteral() ? left : right);
                    String name = operand.getName();
                    if (!name.equals(lhs.getName())){
                        return null;
                    }
                    Integer reg = currentMethod.getVarTable().get(name).getVirtualReg();
                    Integer value = Integer.valueOf(literal.getLiteral());
                    if(op == OperationType.SUB){
                        value = -value;
                    }
                    if (value >= -128 && value <= 127){
                        return "iinc " + reg + " "+value + NL;
                    }
                }
            }
        }
        return null;
    }
    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();
        String iincString = this.checkIINC(assign);
        if (iincString != null){
            return iincString;
        }
        // generate code for loading what's on the right
        if (assign.getRhs() instanceof CallInstruction){
            code.append(generateCallInstructionFromAssign((CallInstruction) assign.getRhs()));
        }else{
            code.append(generators.apply(assign.getRhs()));
        }
        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        ElementType type = operand.getType().getTypeOfElement();
        String fill = reg > 3 ? " " : "_";
        //deal with array store
        if(operand instanceof ArrayOperand){
            var operand1 = (ArrayOperand) operand;
            var result = new StringBuilder();
            addStackSize(1);
            result.append("aload").append(fill).append(reg).append(NL);
            result.append(generators.apply(operand1.getIndexOperands().get(0)));
            result.append(code);
            result.append("iastore");
            subStackSize(3);
            code = result;
        }else {
            this.subStackSize(1);
            switch (type) {
                case INT32, BOOLEAN -> code.append("istore");
                case STRING, ARRAYREF, OBJECTREF -> code.append("astore");
                default -> throw new NotImplementedException(type);
            }
            code.append(fill).append(reg);
        }
        return code.toString();
    }
    //TODO merge this with generateCallInstruction and generateCallInstructionFromAssign
    private String generateCallInstruction(CallInstruction call){
        var code = new StringBuilder();
        // load arguments
        switch (call.getInvocationType()){
            case invokestatic -> code.append(DealWithInvokeStatic(call));
            case invokespecial -> code.append(DealWithInvokeSpecial(call,false));
            case invokevirtual -> code.append(DealWithInvokeVirtual(call,false));
            case NEW -> code.append(DealWithNew(call));
            case arraylength -> code.append(generators.apply(call.getCaller())).append("arraylength").append(NL);
        }
        //code.append(generators.apply(call.getCaller()));
        return code.toString();
        // invoke method
    }
    private String generateCallInstructionFromAssign(CallInstruction call){
        var code = new StringBuilder();
        // load arguments
        switch (call.getInvocationType()){
            case invokestatic -> code.append(DealWithInvokeStatic(call));
            case invokespecial -> code.append(DealWithInvokeSpecial(call,true));
            case invokevirtual -> code.append(DealWithInvokeVirtual(call,true));
            case NEW -> code.append(DealWithNew(call));
            case arraylength -> code.append(generators.apply(call.getCaller())).append("arraylength").append(NL);
        }
        return code.toString();
        // invoke method
    }
    public static String removeQuotes(String input) {
        return input.replaceAll("^\"|\"$", "");
    }
    private String DealWithInvokeVirtual(CallInstruction call,boolean assignCalled){
        var code = new StringBuilder();
        //probably wrong
        code.append(generators.apply(call.getCaller()));
        int argumentsNumber = 1;
        for (var arg : call.getArguments()) {
            argumentsNumber++;
            code.append(generators.apply(arg));
        }
        var className= ((ClassType)call.getCaller().getType()).getName();
        code.append("invokevirtual ").append(getClass(className));
        code.append("/").append(removeQuotes(((LiteralElement) call.getMethodName()).getLiteral()));
        code.append("(");
        for (var arg : call.getArguments()) {
            code.append(field_to_jasmin(arg.getType()));
        }
        code.append(")").append(field_to_jasmin(call.getReturnType())).append(NL);
        subStackSize(argumentsNumber);
        if(call.getReturnType().getTypeOfElement() != VOID){
            addStackSize(1);
        }
        if ( ! assignCalled && call.getReturnType().getTypeOfElement() != VOID ){
            code.append("pop").append(NL);
            subStackSize(1);
        }
        return code.toString();
    }
    private String DealWithInvokeStatic(CallInstruction call){
        var code = new StringBuilder();
        //probably wrong
        int argumentsNumber = 0;
        for (var arg : call.getArguments()) {
            argumentsNumber++;
            code.append(generators.apply(arg));
        }
        code.append("invokestatic ").append(getClass(((Operand)call.getCaller()).getName()));
        code.append("/").append(removeQuotes(((LiteralElement) call.getMethodName()).getLiteral()));
        code.append("(");
        for (var arg : call.getArguments()) {
            code.append(field_to_jasmin(arg.getType()));
        }
        code.append(")").append(field_to_jasmin(call.getReturnType())).append(NL);
        subStackSize(argumentsNumber);
        if(call.getReturnType().getTypeOfElement() != VOID){
            addStackSize(1);
        }
        return code.toString();
    }
    private String DealWithInvokeSpecial(CallInstruction call,boolean assignedCalled){
        var code = new StringBuilder();
        code.append(generators.apply(call.getCaller()));
        int argumentsNumber = 1;
        for (var arg : call.getArguments()) {
            argumentsNumber++;
            code.append(generators.apply(arg));
        }
        //probably wrong
        var className= ((ClassType)call.getCaller().getType()).getName();
        code.append("invokespecial ").append(getClass(className)).append("/<init>(");
        for (var arg : call.getArguments()) {
            code.append(field_to_jasmin(arg.getType()));
        }
        code.append(")V").append(NL);
        subStackSize(argumentsNumber);
        if(call.getReturnType().getTypeOfElement() != VOID){
            addStackSize(1);
        }
        if ( ! assignedCalled && call.getReturnType().getTypeOfElement() != VOID ){
            code.append("pop").append(NL);
            subStackSize(1);
        }
        return code.toString();
    }
    private String DealWithNew(CallInstruction call){
        //TODO: probably need subStackSize and arguments of the construtor of a object
        var code = new StringBuilder();
        if (call.getReturnType().getTypeOfElement()  == ARRAYREF){
            code.append(generators.apply(call.getOperands().get(1)));
            ArrayType array = (ArrayType) call.getReturnType();

            code.append("newarray ").append(TypeToJasminArrayType(array.getElementType())).append(NL);
            subStackSize(1);//test fails carefull TODO REMOVE THIS LINE FOR 97% OF THE TESTS
        }else {
            code.append("new ").append(((Operand) call.getCaller()).getName()).append(NL);
        }
        addStackSize(1);
        return code.toString();
    }
    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        addStackSize(1);
        if (literal.getType().getTypeOfElement() != ElementType.INT32 && literal.getType().getTypeOfElement() != ElementType.BOOLEAN) {
            return "ldc " + literal.getLiteral() + NL;
        }
        Integer value = Integer.valueOf(literal.getLiteral());
        if(value == -1){
            return "iconst_m1" + NL;
        }else if( value >= 0 && value <= 5){
            return "iconst_" + value + NL;
        } else if (value >= -128 && value <= 127) {
            return "bipush " + value + NL;
        } else if (value >= -32768 && value <= 32767) {
            return "sipush " + value + NL;
        }
        return "ldc " + literal.getLiteral() + NL;
    }
    private String generateOperand(Operand operand) {
        // get register
        addStackSize(1);
        return switch (operand.getType().getTypeOfElement()){
            case THIS -> "aload_0\n";
            case STRING ,ARRAYREF , OBJECTREF -> "aload" + getIndexOfTheReg(operand.getName());
            case INT32,BOOLEAN -> "iload" +  getIndexOfTheReg(operand.getName());
            default -> null;
        };
    }
    private String generateArrayOperand(ArrayOperand arrayOperand){
        var code = new StringBuilder();
        addStackSize(1);
        code.append("aload").append(getIndexOfTheReg(arrayOperand.getName()));
        code.append(generators.apply(arrayOperand.getIndexOperands().get(0)));
        code.append("iaload").append(NL);
        subStackSize(1);
        return code.toString();
    }
    private String getIndexOfTheReg(String name){
        var reg = currentMethod.getVarTable().get(name).getVirtualReg();
        return (reg < 4 ? "_": " ") +reg + NL;
    }
    private String dealWithCondicionalBranch(BinaryOpInstruction binaryOp){
        Element leftOperand = binaryOp.getLeftOperand();
        Element rightOperand = binaryOp.getRightOperand();
        boolean isZeroRight = false;
        boolean hasZero = false;
        if (rightOperand instanceof LiteralElement) {
            if(((LiteralElement) rightOperand).getLiteral().equals("0")) {
                isZeroRight = true;
                hasZero = true;
            }
        }
        if(leftOperand instanceof LiteralElement){
            if(((LiteralElement) leftOperand).getLiteral().equals("0")){
                hasZero = true;
            }
        }
        switch(binaryOp.getOperation().getOpType()){
            case LTH -> {
                if (isZeroRight){
                    var result = generators.apply(leftOperand) + "iflt ";
                    subStackSize(1);
                    return result;
                }
                if (hasZero){
                    var result = generators.apply(rightOperand) + "ifge ";
                    subStackSize(1);
                    return result;
                }
                var result =generators.apply(leftOperand) + generators.apply(rightOperand) + "if_icmplt ";
                subStackSize(2);
                return result;
            }
            case GTE -> {
                if (isZeroRight){
                    var result = generators.apply(leftOperand) + "ifge ";
                    subStackSize(1);
                    return result;
                }
                if (hasZero){
                    var result = generators.apply(rightOperand) + "iflt ";
                    subStackSize(1);
                    return result;
                }
                var result = generators.apply(leftOperand) + generators.apply(rightOperand) + "if_icmpge ";
                subStackSize(2);
                return result;
            }
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }
    }
    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        ArrayList<OperationType> operationsWithTags = new ArrayList<>();
        operationsWithTags.add(OperationType.LTH);
        operationsWithTags.add(OperationType.GTE);
        if(operationsWithTags.contains(binaryOp.getOperation().getOpType())){
            return this.dealWithCondicionalBranch(binaryOp);
        }
        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));
        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            case ANDB -> "iand";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };
        code.append(op);
        subStackSize(1);
        if(! operationsWithTags.contains(binaryOp.getOperation().getOpType())) {
            code.append(NL);
        }


        return code.toString();
    }
    private String generateSingleOpCondInstruction(SingleOpCondInstruction singleOpCond){
        var code = new StringBuilder();
        code.append(generators.apply(singleOpCond.getCondition()));
        code.append("ifne ").append(singleOpCond.getLabel()).append(NL);
        subStackSize(1);
        return code.toString();
    }
    private String generateOpCondInstruction(OpCondInstruction OpCondInstruction){
        var code = new StringBuilder();
        code.append(generators.apply(OpCondInstruction.getCondition())).append(OpCondInstruction.getLabel()).append(NL);
        return code.toString();

    }
    private String generateBooleansBranchs(){
        var code = new StringBuilder();
        code.append("true__").append(labelCounterBranchs).append(NL);
        code.append("iconst_0").append(NL);
        code.append("goto false__").append(labelCounterBranchs).append(NL);
        code.append("true__").append(labelCounterBranchs).append(":").append(NL);
        code.append("iconst_1").append(NL);
        code.append("false__").append(labelCounterBranchs).append(":").append(NL);
        labelCounterBranchs++;
        return code.toString();
    }
    private String dealWithGoTo(GotoInstruction goTo){
        var code = new StringBuilder();
        code.append("goto ").append(goTo.getLabel()).append(NL);
        return code.toString();
    }

    private String generateUnaryOpInstruction(UnaryOpInstruction Op){
        var code = new StringBuilder();
        code.append(generators.apply(Op.getOperand())).append(NL);
        switch (Op.getOperation().getOpType()) {
            case NOTB -> code.append("ifeq ").append(this.generateBooleansBranchs()).append(NL);
            default -> throw new NotImplementedException(Op.getOperation().getOpType());
        }
        return code.toString();
    }
    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        ElementType type = returnInst.getReturnType().getTypeOfElement();
        if (type == VOID){
            code.append("return").append(NL);
            return code.toString();
        }
        code.append(generators.apply(returnInst.getOperand()));
        switch (type) {
            case INT32 -> code.append("ireturn").append(NL);
            case BOOLEAN -> code.append("ireturn").append(NL);
            case ARRAYREF -> code.append("areturn").append(NL);
            case OBJECTREF -> code.append("areturn").append(NL);
            default -> throw new NotImplementedException(returnInst.getReturnType().getTypeOfElement());
        }
        //code.append(generators.apply(returnInst.getOperand()));
        //code.append("ireturn").append(NL);

        return code.toString();
    }
    private void addStackSize(int size){
        this.stackSize += size;
        if (this.stackSize > this.maxStackSize){
            this.maxStackSize = this.stackSize;
        }
    }
    private void subStackSize(int size){
        this.stackSize -= size;
    }

}
