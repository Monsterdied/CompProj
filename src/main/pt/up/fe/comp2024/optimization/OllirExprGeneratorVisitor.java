package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";



    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(METHOD_CALL_EXPR, this::visitMethodCall);
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(ARRAY_ACCESS_EXPR, this::visitVarRef);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitMethodCall(JmmNode nodeMethodCall, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String objectName = nodeMethodCall.getDescendants(VAR_REF_EXPR).get(0).get("name");

        List<String> imports = table.getImports();

        boolean isStatic = OptUtils.isStatic(objectName, imports);

        String type_virtual = "";

        //Main decl doesn't have a name, so we need to check if the parent is a main method decl
        List<Symbol> localVariables = null;
        if(nodeMethodCall.getAncestor(MAIN_METHOD_DECL).isPresent()){
            localVariables = this.table.getLocalVariables("main");
        }
        else{
            localVariables = this.table.getLocalVariables(nodeMethodCall.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow());
        }
        if(isStatic){
            code.append(String.format("invokestatic(%s, ", objectName));
            code.append("\"").append(nodeMethodCall.get("name")).append("\"");
        }
        else{
            for(var locals: localVariables){
                var a  = locals.getName();
                if(Objects.equals(objectName, a)){
                    type_virtual= locals.getType().getName();
                    break;
                }
            }
            code.append(String.format("invokevirtual(%s.%s, %s ", objectName, type_virtual, "\"" + nodeMethodCall.get("name") + "\""));

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
                    arg_type = visit(arg).getCode();
                    code.append(arg_type);
                }
                //Other case
                else {
                    var expr = visit(arg);

                    computation.append(expr.getComputation());

                    if(arg.isInstance(BOOLEAN_LITERAL) || arg.isInstance(INTEGER_LITERAL)){
                        code.append(expr.getCode());
                    }
                    else {
                        Type resType = TypeUtils.getExprType(arg, table);
                        String resOllirType = OptUtils.toOllirType(resType);
                        String tempVar = OptUtils.getTemp() + resOllirType;

                        computation.append(tempVar).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(expr.getCode());
                        code.append(tempVar);
                    }
                }
                if (i < args_nodes.size() - 1) {
                    code.append(", ");
                }
            }
        }
        if(isStatic){
            code.append(").V;").append("\n");
        }
        else{
            code.append(").").append(type_virtual).append(";").append("\n");
        }
        return new OllirExprResult(code.toString(), computation);
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

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
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

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        String code;


        if(node.getKind().equals("ArrayAccessExpr")) {
            String id = node.getChild(0).get("name");
            var index = visit(node.getChildren().get(1)).getCode();
            code = id + "[" + index + "]" + OptUtils.toOllirType(TypeUtils.getExprType(node,table));
            return new OllirExprResult(code);
        }

        for (var field : table.getFields()) {
            if (field.getName().equals(node.get("name"))) {
                if(node.getParent().getKind().equals("AssignStmt")  ){
                    String name = node.get("name");
                    String type = OptUtils.toOllirType(field.getType().getName());
                    var computation = visit(node.getAncestor("AssignStmt").orElseThrow().getChild(1));
                    code = String.format("putfield(this, %s, %s).V;", name + type, computation.getCode());
                    return new OllirExprResult(computation.getComputation(),code);
                }
                else{
                    Type fieldType = TypeUtils.getExprType(node, table);
                    String fieldOllirType = OptUtils.toOllirType(fieldType);
                    String tempVar = OptUtils.getTemp() + fieldOllirType;
                    String fieldName = node.get("name");

                    code = tempVar + SPACE + ASSIGN + fieldOllirType + SPACE + String.format("getfield(this, %s)", fieldName+fieldOllirType) + fieldOllirType + END_STMT;
                    return new OllirExprResult(tempVar, code);
                }
            }
        }
        String id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        code = id + ollirType;

        return new OllirExprResult(code);
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
