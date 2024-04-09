package pt.up.fe.comp2024.optimization;

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
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(MAIN_METHOD_DECL, this::visitMainMethodDecl);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitImport(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(String.format("import %s;",node.get("ID"))).append(NL);
        return code.toString();
    }


    private String visitExprStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        String methodNodeName = node.getChildren().get(0).getChildren().get(0).get("name");
        List<String> imports = table.getImports();
        boolean isStatic = OptUtils.isStatic(methodNodeName, imports);
        String type_virtual = "";
        for(var locals: this.table.getLocalVariables(node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow())){
            var a  = locals.getName();
            if(Objects.equals(methodNodeName, locals.getName()))
                type_virtual= locals.getType().getName();
        }
        if(isStatic){
            code.append(String.format("invokestatic(%s, ", methodNodeName));
            code.append("\"").append(node.getChildren().get(0).get("name")).append("\", ");
        }
        else {
            //var a = this.table.getLocalVariables(node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow());

            code.append(String.format("invokevirtual(%s.%s, %s, ", methodNodeName, type_virtual, "\"" + node.getChildren().get(0).get("name") + "\""));
        }
        String type = "";
        var children = node.getChildren().get(0).getChildren().get(1).getChildren();
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);

            for(var locals : this.table.getLocalVariables(node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow())) {
                if (Objects.equals(locals.getName(), child.get("name")))
                    type = locals.getType().getName();
            }
            code.append(String.format("%s%s", child.get("name"), OptUtils.toOllirType(type)));
            if (i < children.size() - 1) {
                code.append(", ");
            }
        }

        if(isStatic){
            code.append(").V;").append(NL);
        }
        else{
            code.append(")."+type_virtual+";").append(NL);
        }


        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    private String visitMainMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(".method public static main(args.array.String).V {").append(NL);
        code.append("ret.V ;").append(NL);
        code.append(R_BRACKET).append(NL);
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

        //Get Class fields
        code.append(OptUtils.getFields(this.table.getFields()));
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


    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();


        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
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
