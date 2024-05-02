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

    String code;

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
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(PutFieldInstruction.class,this::PutFieldInstruction );
        generators.put(GetFieldInstruction.class,this::GetFieldInstruction );
        generators.put(UnaryOpInstruction.class,this::generateUnaryOpInstruction);
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

        // generate a single constructor method
        /*var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);*/

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
        var code1 = new StringBuilder();
        var code2 = new StringBuilder();
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                code1.append(".method public <init>()V").append(NL)
                        .append(TAB).append("aload_0").append(NL)
                        .append(TAB).append("invokespecial ").append(superClass).append("/<init>(");
                        for (var param : method.getParams()) {
                            code1.append(field_to_jasmin(param.getType()));
                        }
                                code1.append(")V").append(NL)
                        .append(TAB).append("return").append(NL)
                        .append(".end method").append(NL);
                continue;
            }
            code2.append(generators.apply(method));
        }
        return code.append(code1).append(code2).toString();
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
        return code.toString();
    }
    private String getClass(String className){

        if (className.equals("this")){
            return currentClass.getClassName();
        }
        for (String name : currentClass.getImports()){
            if (name.endsWith(className)){
                return name;
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
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

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

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        ElementType type = operand.getType().getTypeOfElement();
        String fill = reg > 3 ? " " : "_";
        switch (type) {
            case INT32 -> code.append("istore");
            case BOOLEAN -> code.append("istore");
            case STRING -> code.append("astore");
            case ARRAYREF -> code.append("astore");
            case OBJECTREF -> code.append("astore");
            default -> throw new NotImplementedException(type);
        }
        code.append(fill).append(reg);

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
        }
        //code.append(generators.apply(call.getCaller()));
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
        for (var arg : call.getArguments()) {
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
        if ( ! assignCalled && call.getReturnType().getTypeOfElement() != VOID ){
            code.append("pop").append(NL);
        }
        return code.toString();
    }
    private String DealWithInvokeStatic(CallInstruction call){
        var code = new StringBuilder();
        //probably wrong
        for (var arg : call.getArguments()) {
            code.append(generators.apply(arg));
        }
        code.append("invokestatic ").append(getClass(((Operand)call.getCaller()).getName()));
        code.append("/").append(removeQuotes(((LiteralElement) call.getMethodName()).getLiteral()));
        code.append("(");
        for (var arg : call.getArguments()) {
            code.append(field_to_jasmin(arg.getType()));
        }
        code.append(")").append(field_to_jasmin(call.getReturnType())).append(NL);
        return code.toString();
    }
    private String DealWithInvokeSpecial(CallInstruction call,boolean assignedCalled){
        var code = new StringBuilder();
        code.append(generators.apply(call.getCaller()));
        for (var arg : call.getArguments()) {
            code.append(generators.apply(arg));
        }
        //probably wrong
        var className= ((ClassType)call.getCaller().getType()).getName();
        code.append("invokespecial ").append(getClass(className)).append("/<init>(");
        for (var arg : call.getArguments()) {
            code.append(field_to_jasmin(arg.getType()));
        }
        code.append(")V").append(NL);
        if ( ! assignedCalled && call.getReturnType().getTypeOfElement() != VOID ){
            code.append("pop").append(NL);
        }
        return code.toString();
    }
    private String DealWithNew(CallInstruction call){
        var code = new StringBuilder();
        if (call.getReturnType().getTypeOfElement()  == ARRAYREF){
            code.append(generators.apply(call.getOperands().get(1)));
            ArrayType array = (ArrayType) call.getReturnType();

            code.append("newarray ").append(TypeToJasminArrayType(array.getElementType())).append(NL);
        }else {
            code.append("new ").append(((Operand) call.getCaller()).getName()).append(NL);
        }
        return code.toString();
    }
    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }
    private String generateOperand(Operand operand) {
        // get register
        return switch (operand.getType().getTypeOfElement()){
            case THIS -> "aload_0\n";
            case STRING ,ARRAYREF , OBJECTREF -> "aload" + getIndexOfTheReg(operand.getName());
            case INT32,BOOLEAN -> "iload" +  getIndexOfTheReg(operand.getName());
            default -> null;
        };
    }
    private String getIndexOfTheReg(String name){
        var reg = currentMethod.getVarTable().get(name).getVirtualReg();
        return (reg < 4 ? "_": " ") +reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

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
            case NEQ -> "ineg";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }
    private String generateUnaryOpInstruction(UnaryOpInstruction Op){
        var code = new StringBuilder();
        code.append(generators.apply(Op.getOperand()));
        var op = switch (Op.getOperation().getOpType()) {
            case NOTB -> "ineg";
            default -> throw new NotImplementedException(Op.getOperation().getOpType());
        };
        code.append(op).append(NL);
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

}
