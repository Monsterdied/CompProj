package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL,this::visitImport);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(MAIN_METHOD_DECL, this::visitMainMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(WHILE_CONDITION, this::visitWhileStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();


        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    private String visitImport(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var importStmt = node.get("value");
        StringBuilder result = new StringBuilder();
        String[] importParts = importStmt.substring(1, importStmt.length() - 1).split(", ");
        for (String part : importParts) {
            result.append(part).append(".");
        }
        result.deleteCharAt(result.length() - 1);
        code.append(String.format("import %s;", result)).append(NL);
        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        //Get class name
        code.append(table.getClassName());

        //Get super class if it exists
        if (this.table.getSuper() != null && !Objects.equals(this.table.getSuper(), "")) {
            code.append(" extends ").append(this.table.getSuper());
        }
        code.append(L_BRACKET);
        code.append(NL);


        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var parent = node.getParent();
        var id = node.get("name");

        //Declared in class
        if(parent.getKind().equals("ClassDecl")){
            code.append(String.format(".field public %s",id));

            for(var field: this.table.getFields()){
                if(field.getName().equals(id)){
                    //Array
                    if(field.getType().isArray()){
                        code.append(".array");
                    }
                    //Other types
                    code.append(OptUtils.toOllirType(field.getType().getName()));
                    break;
                }
            }
            code.append(";\n");
        }



        return code.toString();
    }

    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        StringBuilder paramCode = new StringBuilder();
        for (var param : node.getJmmChild(1).getChildren()) {
            paramCode.append(visitParam(param, unused));
            paramCode.append(", ");
        }
        if (paramCode.length() > 0) {
            paramCode.delete(paramCode.length() - 2, paramCode.length());
        }
        code.append("(").append(paramCode).append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var afterParam = 2;
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);

        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitMainMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(".method public static main(args.array.String).V {").append(NL);
        for(var child: node.getChildren()){
            code.append(visit(child));
        }
        code.append("ret.V ;").append(NL);
        code.append(R_BRACKET).append(NL);
        return code.toString();
    }

    private String visitParam(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var parent = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        var id = node.get("name");
        for(var param: this.table.getParameters(parent)) {
            if(param.getName().equals(id)){
                if(param.getType().isArray()){
                    code.append(String.format("%s.%s%s",id,"array",OptUtils.toOllirType(param.getType().getName())));
                    break;
                }
                else{
                    code.append(String.format("%s%s",id,OptUtils.toOllirType(param.getType().getName())));
                    break;
                }
            }
        }



        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var lhs_node = node.getJmmChild(0);
        var rhs_node = node.getJmmChild(1);
        var lhs = exprVisitor.visit(lhs_node);
        var rhs = OllirExprResult.EMPTY;
        if(lhs.getComputation().contains("putfield")){
            code.append(lhs.getCode());
            code.append(lhs.getComputation()).append(NL);
            return code.toString();
        }
        rhs = exprVisitor.visit(rhs_node);

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());


        // code to compute self
        String tempVar = "";


        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);


        code.append(rhs.getCode());


        if(!rhs.getCode().contains(";")){
            code.append(END_STMT);
        }


        return code.toString();
    }

    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        OllirExprResult expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            var exp = node.getChildren().get(0);

            expr = exprVisitor.visit(exp);

        }
        code.append(expr.getComputation());

        if(node.getNumChildren() == 1)
            if(node.getJmmChild(0).getKind().equals("MethodCallExpr")){
                Type resType = TypeUtils.getExprType(node.getJmmChild(0), table);
                String resOllirType = OptUtils.toOllirType(resType);
                String tmp =OptUtils.getTemp() + resOllirType;
                code.append(tmp).append(SPACE)
                        .append(ASSIGN).append(resOllirType).append(SPACE)
                        .append(expr.getCode());

                code.append("ret").append(resOllirType).append(SPACE).append(tmp).append(END_STMT);

                return code.toString();
            }

        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        var expr = node.getJmmChild(0);
        var exprResult = exprVisitor.visit(expr);
        code.append(exprResult.getComputation());

        var variable_type = TypeUtils.getExprType(node.getJmmChild(0).getJmmChild(0),table);
        if(variable_type.getName().equals(table.getClassName())){
            var tmp = OptUtils.getTemp();
            Type function_type = table.getReturnType(node.getJmmChild(0).get("name"));
            String ollir_type = OptUtils.toOllirType(function_type);
            code.append(String.format("%s%s :=%s %s",tmp,ollir_type,ollir_type,exprResult.getCode()));
            return code.toString();
        }
        code.append(exprResult.getCode());
        return code.toString();

    }

    private String visitIfElseStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var conditionExpr = node.getJmmChild(0).getJmmChild(0);
        var conditionExprResult = exprVisitor.visit(conditionExpr);
        code.append(conditionExprResult.getComputation());
        var ifcondition = OptUtils.getIf();
        code.append(String.format("if (%s) goto %s;",conditionExprResult.getCode(),ifcondition)).append(NL);

        var blockStmt2 = node.getJmmChild(0).getJmmChild(2);
        for(var child: blockStmt2.getChildren()){
            code.append(visit(child));
        }
        code.append(String.format("goto end%s;",ifcondition)).append(NL);

        code.append(String.format("%s:",ifcondition)).append(NL);

        var blockStmt1 = node.getJmmChild(0).getJmmChild(1);
        for(var child: blockStmt1.getChildren()){
            code.append(visit(child));
        }
        code.append(String.format("end%s:",ifcondition)).append(NL);


        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var whilecounter = OptUtils.getWhile();

        code.append(String.format("whileCond%s:",whilecounter)).append(NL);

        var whilecondition = exprVisitor.visit(node.getJmmChild(0).getJmmChild(0));

        code.append(whilecondition.getComputation());

        code.append(String.format("if (%s) goto whileLoop%s;",whilecondition.getCode(),whilecounter)).append(NL);
        code.append(String.format("goto whileEnd%s;",whilecounter)).append(NL);

        code.append(String.format("whileLoop%s:",whilecounter)).append(NL);

        for(var child: node.getJmmChild(0).getJmmChild(1).getChildren()){
            code.append(visit(child));
        }

        code.append(String.format("goto whileCond%s;",whilecounter)).append(NL);
        code.append(String.format("whileEnd%s:",whilecounter)).append(NL);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }




    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
