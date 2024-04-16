package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.MAIN_METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";

    private static final String VOID = "void";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }
    public static String getBooleanTypeName() {return BOOLEAN_TYPE_NAME;}

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case ARRAY_ACCESS_EXPR -> getArrayAccessExprType(expr, table);
            case METHOD_CALL_EXPR -> new Type(VOID, false);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case THIS_EXPR -> getThisType(expr,table);
            case NEW_CLASS_EXPR -> new Type(expr.get("name"),false);
            case NEW_ARRAY_EXPR -> new Type(expr.getChild(0).get("name"), true);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded ( already expanded x1)

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "/", "-" -> new Type(INT_TYPE_NAME, false);
            case "&&", "<", "||", ">", "<=", ">=", "!" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {

        String id = varRefExpr.get("name");
        var type = new Type("",false);
        List<Symbol> parent = null;
        if(varRefExpr.getAncestor(MAIN_METHOD_DECL).isPresent()){
            parent = table.getLocalVariables("main");
        }
        else{
            parent = table.getLocalVariables(varRefExpr.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow());
        }

        //Variavel Local
        for(var locals: parent){
            if(locals.getName().equals(id)){
                type = locals.getType();
                break;
            }
        }
        if(type.getName().isEmpty() && !type.isArray()){
            //Variavel Param
            if(!varRefExpr.getAncestor(MAIN_METHOD_DECL).isPresent()) {
                var parent_param = table.getParameters(varRefExpr.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow());
                for (var param : parent_param) {
                    if (param.getName().equals(id)) {
                        type = param.getType();
                        break;
                    }
                }
            }
            if(type.getName().isEmpty() && !type.isArray()){
                //Variavel Field
                for(var field: table.getFields()) {
                    if(field.getName().equals(id)){
                        type = field.getType();
                        break;
                    }
                }
                if(type.getName().isEmpty() && !type.isArray()){
                    for(var imports: table.getImports()){
                        if (imports.contains(id)) {
                            type = new Type("import", false);
                            break;
                        }
                    }
                }
            }
        }
        return type;
    }

    private static Type getArrayAccessExprType(JmmNode arrayAccessExpr, SymbolTable table) {
        return getVarExprType(arrayAccessExpr.getJmmChild(0), table);
    }

    private static Type getThisType(JmmNode binaryExpr,SymbolTable table) {
        return new Type(table.getClassName(),false);
    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
