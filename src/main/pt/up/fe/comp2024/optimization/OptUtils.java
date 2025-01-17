package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;
    private static int ifNumber = -1;
    private static int whileNumber = -1;
    private static int varargNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String getIf(){
        return getIf("if");
    }

    public static String getIf(String prefix) {
        return prefix + getNextIfNum();
    }

    public static int getNextIfNum() {
        ifNumber += 1;
        return ifNumber;
    }

    public static String getWhile(){
        return getWhile("");
    }

    public static String getWhile(String prefix) {
        return prefix + getNextWhileNum();
    }

    public static int getNextWhileNum() {
        whileNumber += 1;
        return whileNumber;
    }

    public static String getVararg(){
        return getVararg("__varargs_array_");
    }

    public static String getVararg(String prefix) {
        return prefix + getNextVarArgNum() + ".array.i32";
    }
    public static int getNextVarArgNum() {
        varargNumber += 1;
        return varargNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        if(!typeNode.getChildren().isEmpty()){
            String typeName = typeNode.getJmmChild(0).get("name");
            return ".array" + toOllirType(typeName);
        }
        String typeName = typeNode.get("name");

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        if(type.isArray()){
            return ".array" + toOllirType(type.getName());
        }
        return toOllirType(type.getName());
    }

    public static String toOllirType(String typeName) {

        String type = switch (typeName) {
            case "int" -> ".i32";
            case "boolean" -> ".bool";
            case "void" -> ".V";
            case "import" -> "";
            default -> "." + typeName;
        };

        return type;
    }

    public static boolean isStatic(String name, List<String> imports) {
        return imports.contains(name);
    }
}
