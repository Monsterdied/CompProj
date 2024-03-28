package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        code.append(".method public <init>()V").append(NL)
                .append(TAB).append("aload_0").append(NL)
                .append(TAB).append("invokespecial ").append(superClass).append("/<init>()V").append(NL)
                .append(TAB).append("return").append(NL)
                .append(".end method").append(NL);
        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
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
            case OBJECTREF:
                return "To be implemented";
            default:
                return TypeToJasmin(type);
        }
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
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        code.append("istore ").append(reg).append(NL);

        return code.toString();
    }
    private String generateCallInstruction(CallInstruction call){
        var code = new StringBuilder();
        return code.toString();
        // load arguments
        /*
        for (var arg : call.getArgs()) {
            code.append(generators.apply(arg));
        }
        // invoke method
        code.append("invokestatic ").append(call.getClassName()).append("/").append(call.getMethodName()).append("(");
        for (var arg : call.getArgs()) {
            code.append(field_to_jasmin(arg.getType()));
        }
        code.append(")").append(field_to_jasmin(call.getReturnType())).append(NL);
        return code.toString();*/
    }
    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return "iload " + reg + NL;
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
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
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
            default -> throw new NotImplementedException(returnInst.getReturnType().getTypeOfElement());
        }
        //code.append(generators.apply(returnInst.getOperand()));
        //code.append("ireturn").append(NL);

        return code.toString();
    }

}
