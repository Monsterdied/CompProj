package pt.up.fe.comp2024.analysis.passes;

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

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class SemanticAnalyzer extends AnalysisVisitor  {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("ArrayAccessExpr", this::visitArrayAccessExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit(Kind.ARRAY_INIT_EXPRESSION, this::visitArrayInitExpr);
        addVisit(Kind.VAR_ARG_ARRAY ,this::visitVarArgArray);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
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
        Type leftType = getExprType(leftOperand, table);
        Type rightType = getExprType(rightOperand, table);

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

        // Check if both types are arrays
        if (leftIsArray && rightIsArray) {
            // Reject array operations
            return false;
        }

        // Perform type compatibility check based on the operator
        switch (operator) {
            case "+":
            case "-":
            case "*":
            case "/":
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
        Type arrayType = TypeUtils.getExprType(arrayExpr, table);

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
        Type indexType = TypeUtils.getExprType(indexExpr, table);

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
        Type assigneeType = TypeUtils.getExprType(assignee, table);

        // Get the right operand (the value being assigned)
        JmmNode valueExpr = assignStmt.getChildren().get(1);
        Type valueType = TypeUtils.getExprType(valueExpr, table);

        // Pass if both variables are imports
        if (table.getImports().contains(assigneeType.getName()) && table.getImports().contains(valueType.getName())) {
            return null;
        }

        // Pass if assignee extends value
        if (Objects.equals(table.getSuper(), assigneeType.getName()) && Objects.equals(table.getClassName(), valueType.getName())) {
            return null;
        }

        // Don't know return value of function
        if (Objects.equals(valueType.getName(), "null")) {
            return null;
        }

        // Check if the types are compatible
        if (!assigneeType.equals(valueType)) {
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
        Type conditionType = TypeUtils.getExprType(conditionExpr, table);

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
        Type conditionType = TypeUtils.getExprType(conditionExpr, table);

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

    private Boolean isValidConditionType(Type conditionType) {
        return conditionType.getName().equals("boolean");
    }

    private Void visitMethodCallExpr(JmmNode expr, SymbolTable table) {
        // Get type
        Type childType = getExprType(expr.getChild(0), table);
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

        Type arrayType = getExprType(expr, table);

        // Checks if values of array have different types
        for (JmmNode child : expr.getChildren()) {
            Type childType = getExprType(child, table);
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

        for (JmmNode siblingExpr : expr.getParent().getChildren()) {
            String siblingKind = siblingExpr.getKind();
            if (Objects.equals(siblingKind, "VarArgArray")) {
                varArgCounter++;
            }
        }

        if (varArgCounter > 1) {
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
}
