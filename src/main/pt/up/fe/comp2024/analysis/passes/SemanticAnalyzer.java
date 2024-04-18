package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pt.up.fe.comp2024.ast.Kind;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class SemanticAnalyzer extends AnalysisVisitor {

    private String currentMethod;
    private JmmNode CurrentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.MAIN_METHOD_DECL, this::visitMainMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("ArrayAccessExpr", this::visitArrayAccessExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit(Kind.ARRAY_INIT_EXPRESSION, this::visitArrayInitExpr);
        addVisit(Kind.VAR_ARG_ARRAY, this::visitVarArgArray);
        addVisit(Kind.PROGRAM, this::visitProgram);
    }

    private Void visitMainMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = "main";
        CurrentMethod = method;

        // Checks for 0 or 1 return
        int returnCounter = 0;
        boolean hasReturn = false;
        List<JmmNode> children = method.getChildren();
        for (JmmNode child : children) {
            if (Objects.equals(child.getKind(), "ReturnStmt")) {
                if (!Objects.equals(child.getChild(0).get("name"), "ReturnStmt")) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(method),
                            NodeUtils.getColumn(method),
                            "Return inside main is not void",
                            null)
                    );
                    return null;
                }

                hasReturn = true;
                returnCounter++;
            }
        }

        if (!(returnCounter == 0 || returnCounter == 1)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Number of returns inside main function is not zero or one",
                    null)
            );
            return null;
        }

        // Checks if last expression inside function is a return
        if (hasReturn) {
            if (!Objects.equals(children.get(children.size() - 1).getKind(), "ReturnStmt")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Last expression inside function is not ReturnStmt",
                        null)
                );
            }
        }

        if(!method.getDescendants("ThisExpr").isEmpty()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Found \"this\" keyword inside main method.",
                    null)
            );
        }

        if(!method.getDescendants("ReturnStmt").isEmpty()){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Found a return statement inside main method.",
                    null)
            );
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        CurrentMethod = method;

        // Checks for only 1 return
        int returnCounter = 0;
        List<JmmNode> children = method.getChildren();
        for (JmmNode child : children) {
            if (Objects.equals(child.getKind(), "ReturnStmt")) {
                returnCounter++;
            }
        }

        /*
        if (returnCounter != 1) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Number of returns inside function is not one",
                    null)
            );
            return null;
        }
        */






        Type type = table.getReturnType(currentMethod);
        var retur = method.getChildren(Kind.RETURN_STMT).get(0).getChildren().get(0);
        Type returnType = getTypeFromExprSpeculation(retur, table);
        if (retur == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Return type of method does not match the declared return type.",
                    null)
            );
        }
        if (!type.equals(returnType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Return type of method does not match the declared return type.",
                    null)
            );
        }

        // Checks if last expression inside function is a return
        if (!Objects.equals(children.get(children.size() - 1).getKind(), "ReturnStmt")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Last expression inside function is not ReturnStmt",
                    null)
            );
        }

        return null;
    }

    public Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {

        var imports = table.getImports();
        String name = varRefExpr.get("name");
        for (String a : imports) {
            if (a.endsWith(name)) {
                return new Type(name, false);
            }
        }

        for (var symbol : table.getFields()) {
            if (symbol.getName().equals(varRefExpr.get("name"))) {
                return symbol.getType();
            }
        }
        for (var param : table.getParameters(currentMethod)) {
            if (param.getName().equals(varRefExpr.get("name"))) {
                return param.getType();
            }
        }
        for (var local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(varRefExpr.get("name"))) {
                return local.getType();
            }
        }
        return new Type(null, false);
    }

    //this method just gives the expected from a given expression carefull
    private Type getTypeFromExprSpeculation(JmmNode expr, SymbolTable table) {
        switch (expr.getKind()) {
            case "ThisExpr":
                return new Type("int", false);
            case "VarRefExpr":
                return getVarExprType(expr, table);
            case "NewArrayExpr":
                return new Type("int", true);
            case "NotExpr":
                return new Type("boolean", false);
            case "IntegerLiteral":
                return new Type("int", false);
            case "BooleanLiteral":
                return new Type("boolean", false);
            case "BinaryExpr":
                var op = expr.get("op");

                if (op == "&&" || op == "||" || op == "<" || op == ">" || op == "<=" || op == ">=") {
                    return new Type("boolean", false);
                }
                if (op == "+" || op == "-" || op == "*" || op == "/") {
                    return new Type("int", false);
                }
                break;
            case "ArrayAccessExpr":
                return new Type("int", false);
            case "MethodCallExpr":
                var tmp = table.getReturnType(expr.get("name"));
                if (tmp == null) {
                    return new Type("null", false);
                }
                return tmp;
            case "ArrayInitExpression":
                return new Type("int", true);
            case "NewClassExpr":
                return new Type(expr.get("name"), false);


            default:
                break;
            // code block
        }

        return new Type(null, false);
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if variable reference exists in the symbol table
        var varRefName = varRefExpr.get("name");
        if (varRefExists(varRefName, table)) {
            return null; // Variable reference exists, return
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist or is not accessible in this scope.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }

    private boolean varRefExists(String varRefName, SymbolTable table) {
        // Check if the variable reference exists in the symbol table
        return table.getFields().stream()
                .anyMatch(field -> field.getName().equals(varRefName)) ||  // Check if the variable reference is a field
                table.getParameters(currentMethod).stream()
                        .anyMatch(param -> param.getName().equals(varRefName)) || // Check if the variable reference is a parameter of the current method
                table.getLocalVariables(currentMethod).stream()
                        .anyMatch(varDecl -> varDecl.getName().equals(varRefName)) || // Check if the variable reference is a local variable of the current method
                table.getImports().stream()
                        .anyMatch(importName -> importName.equals(varRefName)); // Check if the variable reference is an import
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        // Get the operator
        String operator = binaryExpr.get("op");

        // Get the left and right operands
        JmmNode leftOperand = binaryExpr.getChildren().get(0);
        JmmNode rightOperand = binaryExpr.getChildren().get(1);

        // Get the types of the left and right operands
        Type leftType = getExprType(leftOperand, table, currentMethod);
        Type rightType = getExprType(rightOperand, table, currentMethod);
        if (leftType == null || rightType == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    "Class Not imported",
                    null)

            );
            return null;
        }
        // Perform type compatibility check based on the operator
        if (!isCompatible(operator, leftType, rightType)) {
            String leftTypeName = leftType.print();
            String rightTypeName = rightType.print();

            String message;
            if (leftType.isArray() || rightType.isArray()) {
                message = "Array cannot be used in arithmetic operations.";
            } else {
                message = String.format("Incompatible types for operation '%s': %s and %s.", operator, leftTypeName, rightTypeName);
            }

            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null)
            );
        }


        return null;
    }

    private boolean isCompatible(String operator, Type leftType, Type rightType) {
        // Check if either operand is an array
        boolean leftIsArray = leftType.isArray();
        boolean rightIsArray = rightType.isArray();
        boolean leftAny = false;
        boolean rigthAny = false;
        // Check if both types are arrays
        if (leftIsArray && rightIsArray) {
            // Reject array operations
            return false;
        }
        if (leftType.getName() == null) {
            leftAny = true;
        }
        if (rightType.getName() == null) {
            rigthAny = true;
        }
        // Perform type compatibility check based on the operator
        switch (operator) {
            case "+":
            case "-":
            case "*":
            case "/":
                if (rigthAny && leftAny) {
                    return true;
                } else if (rigthAny) {
                    return rightType.getName().equals("int") && !rightIsArray;
                } else if (leftAny) {
                    return !leftIsArray && leftType.getName().equals("int");
                }
                // For arithmetic operations, both operands must be of type int
                return leftType.getName().equals("int") && rightType.getName().equals("int") &&
                        !leftIsArray && !rightIsArray;
            case "==":
            case "!=":
                // For equality operations, both operands must have the same type
                return leftType.equals(rightType);
            // Add cases for other operators as needed
            default:
                return true; // Default to true for operators not explicitly handled
        }
    }

    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, SymbolTable table) {
        // Check if array access is done over an array
        JmmNode arrayExpr = arrayAccessExpr.getChildren().get(0);
        Type arrayType = TypeUtils.getExprType(arrayExpr, table, currentMethod);

        if (!arrayType.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayAccessExpr),
                    NodeUtils.getColumn(arrayAccessExpr),
                    "Array access is done over a non-array.",
                    null)
            );
        }

        // Check if array access index is an expression of type integer
        JmmNode indexExpr = arrayAccessExpr.getChildren().get(1);
        Type indexType = TypeUtils.getExprType(indexExpr, table, currentMethod);

        if (!indexType.getName().equals("int")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(indexExpr),
                    NodeUtils.getColumn(indexExpr),
                    "Array access index is not an expression of type integer.",
                    null)
            );
        }

        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        // Get the left operand (the variable being assigned to)
        JmmNode assignee = assignStmt.getChildren().get(0);
        Type assigneeType = TypeUtils.getExprType(assignee, table, currentMethod);

        // Get the right operand (the value being assigned)
        JmmNode valueExpr = assignStmt.getChildren().get(1);
        Type valueType = TypeUtils.getExprType(valueExpr, table, currentMethod);

        if (valueType != null) {
            if (valueType.getName() == null) { // Function is imported
                return null;
            }
        }

        // Pass if both variables are imports
        if (table.getImports().contains(assigneeType.getName()) && table.getImports().contains(valueType.getName())) {
            return null;
        }

        // Pass if assignee extends value
        if (Objects.equals(table.getSuper(), assigneeType.getName()) && Objects.equals(table.getClassName(), valueType.getName())) {
            return null;
        }
        if (valueType == null || assigneeType == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    "Class Not imported",
                    null)
            );
            return null;
        }

        // Don't know return value of function
        if (Objects.equals(valueType.getName(), "null")) {
            return null;
        }
        // Check if the types are compatible
        if (!assigneeType.equals(valueType) && valueType.getName() != null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    "Incompatible types for assignment: " + assigneeType.print() + " and " + valueType.print(),
                    null)
            );
        }

        return null;
    }

    private Void visitIfStmt(JmmNode ifElseStmt, SymbolTable table) {
        // Get the condition expression
        JmmNode conditionExpr = ifElseStmt.getChildren().get(0);

        // Get the type of the condition expression
        Type conditionType = TypeUtils.getExprType(conditionExpr, table, currentMethod);

        // Check if the condition expression returns a boolean
        if (!isValidConditionType(conditionType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(conditionExpr),
                    NodeUtils.getColumn(conditionExpr),
                    "Expression in condition must return a boolean.",
                    null)
            );
        }

        return null;
    }

    private Void visitWhileStmt(JmmNode ifElseStmt, SymbolTable table) {
        // Get the condition expression
        JmmNode conditionExpr = ifElseStmt.getChildren().get(0);

        // Get the type of the condition expression
        Type conditionType = TypeUtils.getExprType(conditionExpr, table, currentMethod);

        // Check if the condition expression returns a boolean
        if (!isValidConditionType(conditionType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(ifElseStmt),
                    NodeUtils.getColumn(ifElseStmt),
                    "Expression in condition must return a boolean.",
                    null)
            );
        }

        return null;
    }

    private Boolean isValidConditionType(Type conditionType) {
        return conditionType.getName().equals("boolean");
    }

    private Void visitMethodCallExpr(JmmNode expr, SymbolTable table) {
        // Get type
        Type childType = getExprType(expr.getChild(0), table, currentMethod);
        String typeName = childType.getName();

        // Class is not super class, so it is import
        if (!Objects.equals(typeName, table.getClassName())) {
            return null;
        }

        // Method does not belong to super class
        if (!table.getMethods().contains(expr.get("name")) && table.getSuper().isEmpty()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Invalid method call.",
                    null)
            );
        }

        return null;
    }

    private Void visitArrayInitExpr(JmmNode expr, SymbolTable table) {
        boolean validExpr = true;

        Type arrayType = getExprType(expr, table, currentMethod);

        // Checks if values of array have different types
        for (JmmNode child : expr.getChildren()) {
            Type childType = getExprType(child, table, currentMethod);
            if (!Objects.equals(childType.getName(), arrayType.getName())) {
                validExpr = false;
                break;
            }
        }

        if (!validExpr) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Invalid array initialization expression",
                    null)
            );
        }

        return null;
    }

    private Void visitVarArgArray(JmmNode expr, SymbolTable table) {
        int varArgCounter = 0;
        boolean found_var_arg = false;
        boolean error = false;
        for (JmmNode siblingExpr : expr.getParent().getChildren()) {
            String siblingKind = siblingExpr.getKind();
            if (found_var_arg) {
                error = true;
                break;
            }
            if (Objects.equals(siblingKind, "VarArgArray")) {
                found_var_arg = true;
            }
        }

        if (error) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    "Too many varargs",
                    null)
            );
        }

        return null;
    }

    private Void visitProgram(JmmNode expr, SymbolTable table) {
        // Checks duplicated imports
        List<String> imports = table.getImports();
        for (String import1 : imports) {
            int counter = 0;
            for (String import2 : imports) {
                if (Objects.equals(import1, import2)) {
                    counter++;
                }
                if (counter > 1) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(expr),
                            NodeUtils.getColumn(expr),
                            "Duplicated imports",
                            null)
                    );
                }
            }
        }

        // Checks duplicated fields
        List<Symbol> fields = table.getFields();
        for (Symbol field1 : fields) {
            int counter = 0;
            for (Symbol field2 : fields) {
                if (Objects.equals(field1, field2)) {
                    counter++;
                }
                if (counter > 1) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(expr),
                            NodeUtils.getColumn(expr),
                            "Duplicated fields",
                            null)
                    );
                    return null;
                }
            }
        }

        // Checks duplicated locals
        for (String methodName : table.getMethods()) {
            List<Symbol> localList = table.getLocalVariables(methodName);
            for (Symbol local1 : localList) {
                int counter = 0;
                for (Symbol local2 : localList) {
                    if (Objects.equals(local1, local2)) {
                        counter++;
                    }
                }
                if (counter > 1) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(expr),
                            NodeUtils.getColumn(expr),
                            "Duplicated locals",
                            null)
                    );
                    return null;
                }
            }
        }

        // Checks duplicated parameter
        for (String methodName : table.getMethods()) {
            List<Symbol> paramList = table.getParameters(methodName);
            for (Symbol param1 : paramList) {
                int counter = 0;
                for (Symbol param2 : paramList) {
                    if (Objects.equals(param1, param2)) {
                        counter++;
                    }
                }
                if (counter > 1) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(expr),
                            NodeUtils.getColumn(expr),
                            "Duplicated parameters",
                            null)
                    );
                    return null;
                }
            }
        }

        // Checks duplicated parameter
        for (String methodName : table.getMethods()) {
            List<Symbol> paramList = table.getParameters(methodName);
            for (Symbol param1 : paramList) {
                int counter = 0;
                for (Symbol param2 : paramList) {
                    if (Objects.equals(param1, param2)) {
                        counter++;
                    }
                }
                if (counter > 1) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(expr),
                            NodeUtils.getColumn(expr),
                            "Duplicated parameters",
                            null)
                    );
                    return null;
                }
            }
        }

        // Checks duplicate methods
        List<String> methodList = table.getMethods();
        for (String method1 : methodList) {
            int counter = 0;
            for (String method2 : methodList) {
                if (Objects.equals(method1, method2)) {
                    counter++;
                }
            }
            if (counter > 1) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(expr),
                        NodeUtils.getColumn(expr),
                        "Duplicated methods",
                        null)
                );
                return null;
            }
        }

        return null;
    }
}
