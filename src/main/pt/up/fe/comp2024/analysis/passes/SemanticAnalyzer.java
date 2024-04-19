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

        // Checks for no returns
        if(!method.getDescendants("ReturnStmt").isEmpty()){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Found a return statement inside main method declaration.",
                    null)
            );
        }

        if(method.getDescendants("ThisExpr").size() > 0) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Found \"this\" keyword inside main method.",
                    null)
            );
        }

        for(var variables: method.getDescendants(Kind.VAR_REF_EXPR)){
           for(var field : table.getFields()){
               if(field.getName().equals(variables.get("name"))){
                   addReport(Report.newError(
                           Stage.SEMANTIC,
                           NodeUtils.getLine(method),
                           NodeUtils.getColumn(method),
                           "Cannot use field \"" + variables.get("name") + "\" inside a main decl.",
                           null)
                   );
               }
           }
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        CurrentMethod = method;

        var methodType = table.getReturnType(currentMethod);

        List<JmmNode> returnStatements = method.getDescendants("ReturnStmt");

        if(returnStatements.size() > 1){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Method \"" + currentMethod + "\" has more than one return statement.",
                    null)
            );
        }

        // Check if there are no return statements
        if (returnStatements.isEmpty()) {
            if (!methodType.getName().equals("void")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        "Method must have a return type of void since it has no return statements.",
                        null)
                );
            }
        } else {
            // Check if the return type matches the method's declared return type
            JmmNode ReturnStatement = returnStatements.get(0);
            Type returnType = TypeUtils.getExprType(ReturnStatement.getChild(0), table);
            if (!methodType.equals(returnType)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(ReturnStatement),
                        NodeUtils.getColumn(ReturnStatement),
                        "Return type of method does not match the declared return type.",
                        null)
                );
            }
        }

        if(!methodType.getName().equals("void")) {
            var retur = method.getChildren(Kind.RETURN_STMT);

            if(retur.size() == 1) {
                Type type = table.getReturnType(currentMethod);

                Type returnType = getTypeFromExprSpeculation(retur.get(0).getChildren().get(0), table);
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
                var children = method.getChildren();
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


        // Check if variable reference exists in the symbol table
        String varRefName = varRefExpr.get("name");


        if (table.getFields().stream().anyMatch(field -> field.getName().equals(varRefName))) {
            return null;
        }

        if (table.getParameters(currentMethod).stream().anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        if (table.getLocalVariables(currentMethod).stream().anyMatch(local -> local.getName().equals(varRefName))) {
            return null;
        }

        if (table.getImports().contains(varRefName)) {
            return null;
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

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        // Get the operator
        String operator = binaryExpr.get("op");

        // Get the left and right operands
        JmmNode leftOperand = binaryExpr.getChildren().get(0);
        JmmNode rightOperand = binaryExpr.getChildren().get(1);

        // Get the types of the left and right operands
        Type leftType = getExprType(leftOperand, table, currentMethod);
        Type rightType = getExprType(rightOperand, table, currentMethod);
        if (leftType.getName() == null || rightType.getName() == null) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    "Variable not defined",
                    null)

            );
        }
        else{
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
            case "<":
            case "<=":
            case ">":
            case ">=":
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
            case "&&":
            case "||":
                if (rigthAny && leftAny) {
                    return true;
                } else if (rigthAny) {
                    return rightType.getName().equals("int") && !rightIsArray;
                } else if (leftAny) {
                    return !leftIsArray && leftType.getName().equals("int");
                }
                // For this type of operations, both operands must be of type boolean
                return leftType.getName().equals("boolean") && rightType.getName().equals("boolean") &&
                        !leftIsArray && !rightIsArray;
            // Add cases for other operators as needed
            default:
                return true; // Default to true for operators not explicitly handled
        }
    }

    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, SymbolTable table) {
        // Check if array access is done over an array
        JmmNode arrayExpr = arrayAccessExpr.getChild(0);
        Type arrayType = getExprType(arrayExpr, table, currentMethod);

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
        JmmNode indexExpr = arrayAccessExpr.getChild(1);
        Type indexType = getExprType(indexExpr, table, currentMethod);

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
        // Get the left operand (the variable being assigned to) and check if it's a variable or array access
        JmmNode assignee = assignStmt.getChildren().get(0);
        if (!Objects.equals(assignee.getKind(), "VarRefExpr") && !Objects.equals(assignee.getKind(), "ArrayAccessExpr")) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignee),
                    NodeUtils.getColumn(assignee),
                    "Left operand of assignment is not a variable.",
                    null)
            );
        }

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

        //Check if variables are defined
        var rightNode = assignStmt.getChildren().get(1);
        //checkUndefinedVariables(rightNode,table);



        return null;
    }

    private Void visitIfStmt(JmmNode ifElseStmt, SymbolTable table) {
        // Get the condition expression
        JmmNode conditionExpr = ifElseStmt.getChildren().get(0);

        // Check if the condition expression returns a boolean
        if (!isValidConditionExpr(conditionExpr, table)) {
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

        // Check if the condition expression returns a boolean
        if (!isValidConditionExpr(conditionExpr, table)) {
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

    private Boolean isValidConditionExpr(JmmNode conditionExpr, SymbolTable table) {
        Type conditionType = getExprType(conditionExpr, table, currentMethod);

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

        // Check if method is assumed in extends
        if (Objects.equals(table.getClassName(), typeName) && !table.getSuper().isEmpty()) {
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
            return null;
        }

        // Check if method has arguments
        if (table.getParameters(expr.get("name")).isEmpty()) {
            return null;
        } else {
            // Checks if function is vararg
            List<JmmNode> functionsExpr = expr.getAncestor("ClassDecl").get().getDescendants("MethodDecl");
            for (JmmNode function : functionsExpr) {
                if (Objects.equals(function.get("name"), expr.get("name"))) {
                    JmmNode paramList = function.getDescendants("ParamList").get(0);
                    if (!paramList.getDescendants("VarArgArray").isEmpty()) {
                        // TODO: Check if arguments are correct
                        return null;
                    }
                    break;
                }
            }

            // Not vararg, check types and amount of arguments
            List<JmmNode> arguments = expr.getDescendants("Args").get(0).getChildren();
            List<Symbol> expectedParams = table.getParameters(expr.get("name"));

            // Check if number of arguments is correct
            if (arguments.size() != expectedParams.size()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(expr),
                        NodeUtils.getColumn(expr),
                        "Invalid number of arguments.",
                        null)
                );
                return null;
            }

            // Check if types of arguments are correct
            for (int i = 0; i < arguments.size(); i++) {
                Type argType = getExprType(arguments.get(i), table, currentMethod);
                Type expectedType = expectedParams.get(i).getType();
                if (!argType.equals(expectedType)) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(arguments.get(i)),
                            NodeUtils.getColumn(arguments.get(i)),
                            "Invalid argument type.",
                            null)
                    );
                    return null;
                }
            }
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

