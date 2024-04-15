package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static void decreaseTemp() {
        tempNumber -= 1;

    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.get("name");

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
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
