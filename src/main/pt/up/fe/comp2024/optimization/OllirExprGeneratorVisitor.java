package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";



    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(PAREN_EXPR,this::parenExprVisit);
        addVisit(THIS_EXPR,this::visitThisExpr);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCall);
        addVisit(NEW_CLASS_EXPR, this::visitNewClass);
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(NOT_EXPR, this::visitNotExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(ARRAY_ACCESS_EXPR, this::visitVarRef);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitThisExpr(JmmNode nodeThis, Void unused){
        var type = TypeUtils.getExprType(nodeThis,table);
        String code = "this" +  OptUtils.toOllirType(type);
        return new OllirExprResult(code,"");
    }

    private OllirExprResult visitMethodCall(JmmNode nodeMethodCall, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        var object = visit(nodeMethodCall.getJmmChild(0));
        computation.append(object.getComputation());
        var objectName = object.getCode();
        List<String> imports = table.getImports();

        boolean isStatic = OptUtils.isStatic(objectName, imports);

        String type_virtual = "";


        if(isStatic){
            code.append(String.format("invokestatic(%s, ", objectName));
            code.append("\"").append(nodeMethodCall.get("name")).append("\"");
        }
        else{
            var type = TypeUtils.getExprType(nodeMethodCall.getJmmChild(0),table);
            type_virtual = OptUtils.toOllirType(type);
            code.append(String.format("invokevirtual(%s, %s", objectName, "\"" + nodeMethodCall.get("name") + "\""));

        }

        String arg_type ="";
        var args_node_list = nodeMethodCall.getChildren("Args");
        if (!args_node_list.isEmpty()) {
            var args_nodes = args_node_list.get(0).getChildren();
            code.append(", ");
            for (int i = 0; i < args_nodes.size(); i++) {
                JmmNode arg = args_nodes.get(i);
                //Variable Case
                if (arg.isInstance(VAR_REF_EXPR)) {
                    var result = visit(arg);
                    arg_type = result.getCode();
                    computation.append(result.getComputation());
                    code.append(arg_type);
                }
                //Other case
                else {
                    var expr = visit(arg);

                    computation.append(expr.getComputation());

                    if(arg.isInstance(METHOD_CALL_EXPR)){

                        Type resType = TypeUtils.getExprType(arg, table);
                        String resOllirType = OptUtils.toOllirType(resType);
                        String tempVar = OptUtils.getTemp() + resOllirType;
                        computation.append(tempVar).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(expr.getCode());
                        code.append(tempVar);

                    }
                    else {
                        code.append(expr.getCode());
                    }
                }
                if (i < args_nodes.size() - 1) {
                    code.append(", ");
                }
            }
        }
        if(nodeMethodCall.getParent().isInstance(ASSIGN_STMT)){
            var type = TypeUtils.getExprType(nodeMethodCall.getParent().getChildren(VAR_REF_EXPR).get(0),table);
            String resOllirType = OptUtils.toOllirType(type);
            code.append(String.format(")%s",resOllirType)).append(END_STMT);
            return new OllirExprResult(code.toString(), computation);
        }
        if(isStatic){
                code.append(").V").append(END_STMT);
        }
        else{
            if(table.getMethods().contains(nodeMethodCall.get("name"))){
                var wtype_virtual = table.getReturnType(nodeMethodCall.get("name")).getName();
                type_virtual = OptUtils.toOllirType(wtype_virtual);
            }
            if(nodeMethodCall.getAncestor("AssignStmt").isPresent()){
                code.append(")").append(type_virtual).append(END_STMT);
            }
            else{
                var type = TypeUtils.getExprType(nodeMethodCall.getJmmChild(0),table);
                if(type.getName().equals(table.getClassName())){
                    code.append(")").append(type_virtual).append(END_STMT);
                }
                else{
                    code.append(").V").append(END_STMT);
                }
            }

        }
        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitNewClass(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        var resOllirType = node.get("name");
        var tempVar = OptUtils.getTemp() + "." +resOllirType;
        computation.append(tempVar).append(SPACE).append(ASSIGN).append(".").append(resOllirType).append(SPACE).append("new(").append(resOllirType).append(")").append(".").append(resOllirType).append(END_STMT);
        computation.append(String.format("invokespecial(%s, \"<init>\").V", tempVar)).append(END_STMT);
        code.append(tempVar);
        return new OllirExprResult(code.toString(),computation.toString());
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        String code;


        if(node.getKind().equals("ArrayAccessExpr")) {
            String id = node.getChild(0).get("name");
            var index = visit(node.getChildren().get(1)).getCode();
            code = id + "[" + index + "]" + OptUtils.toOllirType(TypeUtils.getExprType(node,table));
            return new OllirExprResult(code);
        }

        //Check if local or param
        var scope = TypeUtils.getVariableScope(node,table);


        if(scope.equals("field")) {
            for (var field : table.getFields()) {
                if (field.getName().equals(node.get("name"))) {
                    if (node.getParent().getKind().equals("AssignStmt")) {
                        String name = node.get("name");
                        String type = OptUtils.toOllirType(field.getType().getName());
                        var computation = visit(node.getAncestor("AssignStmt").orElseThrow().getChild(1));
                        code = String.format("putfield(this, %s, %s).V;", name + type, computation.getCode());
                        return new OllirExprResult(computation.getComputation(), code);
                    } else {
                        Type fieldType = TypeUtils.getExprType(node, table);
                        String fieldOllirType = OptUtils.toOllirType(fieldType);
                        String tempVar = OptUtils.getTemp() + fieldOllirType;
                        String fieldName = node.get("name");

                        code = tempVar + SPACE + ASSIGN + fieldOllirType + SPACE + String.format("getfield(this, %s)", fieldName + fieldOllirType) + fieldOllirType + END_STMT;
                        return new OllirExprResult(tempVar, code);
                    }
                }
            }
        }

        String id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self

        if (node.getJmmChild(0).isInstance(METHOD_CALL_EXPR) && node.getJmmChild(1).isInstance(METHOD_CALL_EXPR)) {
            Type resType = TypeUtils.getExprType(node, table);
            String resOllirType = OptUtils.toOllirType(resType);

            // Generate temporary variables for both results
            String code1 = OptUtils.getTemp() + resOllirType;
            String code2 = OptUtils.getTemp() + resOllirType;
            String code3 = OptUtils.getTemp() + resOllirType;

            computation.append(code1).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(lhs.getCode());

            computation.append(code2).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(rhs.getCode());

            computation.append(code3).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE).append(code1).append(SPACE)
                    .append(node.get("op")).append(OptUtils.toOllirType(resType)).append(SPACE)
                    .append(code2).append(END_STMT);

            return new OllirExprResult(code1, computation.toString());

        } else if(node.getJmmChild(0).isInstance(METHOD_CALL_EXPR)){

            Type resType = TypeUtils.getExprType(node, table);
            String resOllirType = OptUtils.toOllirType(resType);
            String code = OptUtils.getTemp() + resOllirType;
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(lhs.getCode());

            Type resType2 = TypeUtils.getExprType(node, table);
            String resOllirType2 = OptUtils.toOllirType(resType);
            String code2 = OptUtils.getTemp() + resOllirType;

            computation.append(code2).append(SPACE)
                    .append(ASSIGN).append(resOllirType2).append(SPACE)
                    .append(code).append(SPACE)
                    .append(node.get("op")).append(OptUtils.toOllirType(resType)).append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
            return new OllirExprResult(code2,computation.toString());

        }else if(node.getJmmChild(1).isInstance(METHOD_CALL_EXPR)){

            Type resType = TypeUtils.getExprType(node, table);
            String resOllirType = OptUtils.toOllirType(resType);
            String code = OptUtils.getTemp() + resOllirType;
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(rhs.getCode());

            Type resType2 = TypeUtils.getExprType(node, table);
            String resOllirType2 = OptUtils.toOllirType(resType);
            String code2 = OptUtils.getTemp() + resOllirType;

            computation.append(code2).append(SPACE)
                    .append(ASSIGN).append(resOllirType2).append(SPACE)
                    .append(lhs.getCode()).append(SPACE)
                    .append(node.get("op")).append(OptUtils.toOllirType(resType)).append(SPACE)
                    .append(code).append(END_STMT);

            return new OllirExprResult(code2,computation.toString());
        }

        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE)
                .append(node.get("op")).append(OptUtils.toOllirType(resType)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNotExpr(JmmNode node, Void unused){
        var expr  = visit(node.getJmmChild(0));
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        computation.append(expr.getComputation());

        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String tempVar = OptUtils.getTemp() + resOllirType;
        computation.append(tempVar).append(SPACE).append(ASSIGN).append(resOllirType)
                .append(SPACE).append("!").append(resOllirType).append(SPACE)
                .append(expr.getCode()).append(END_STMT);
        code.append(tempVar);
        return new OllirExprResult(code.toString(),computation);
    }
    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type(TypeUtils.getBooleanTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
            String value = node.get("value").equals("true") ? "1" : "0";
        String code = value + ollirBoolType;
        return new OllirExprResult(code);
    }

    private OllirExprResult parenExprVisit(JmmNode node, Void unused){
        return visit(node.getJmmChild(0));
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }



        return OllirExprResult.EMPTY;
    }

}
