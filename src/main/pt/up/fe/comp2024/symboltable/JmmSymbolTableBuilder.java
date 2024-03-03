package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {
        int i = 0;
        var importsNodes = root.getChildren("ImportDecl");
        List<String> imports = buildImports(importsNodes);
        var classDecl = root.getJmmChild(importsNodes.size());
        //SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String[] superClass = {""};
        //kinda works
        classDecl.getAttributes().stream().forEach(token ->{
            if(token == "parent"){
                superClass[0] = classDecl.get("parent");
            }}
        );



        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);


        return new JmmSymbolTable(className, superClass[0],imports,fields, methods, returnTypes, params, locals);
    }
    private static List<String> buildImports(List<JmmNode> importsNodes) {
        List<String> imports = new ArrayList<>();
        importsNodes.stream()
                .forEach(var ->
                        imports.add(var.get("ID"))
                );
        return imports;
    }
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).stream()//not works with arrays
                .forEach(method -> {
                    var type = "";
                    var is_array = false;
                    if(method.getChildren("Type").get(0).getChildren().size() == 0){
                        type = method.getChildren("Type").get(0).get("name");
                    }else{
                        type = method.getChildren("Type").get(0).getChildren().get(0).get("name");
                        is_array = true;
                    }
                    map.put(method.get("name"), new Type(type,is_array));});
        if(classDecl.getChildren("MainMethodDecl").size() >0){
            map.put("MainMethodDecl",new Type("void",false));
        }

        return map;
    }
    private static List<Symbol> buildFields(JmmNode classDecl){
            List<Symbol> symbols = new ArrayList<>();
        classDecl.getChildren(VAR_DECL).stream()
                .forEach(var ->
                        symbols.add(new Symbol(new Type(var.getChildren().get(0).get("name"),false),var.get("name")))
                );
        return symbols;
    }
    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded
        Map<String, List<Symbol>> map = new HashMap<>();
        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> {
                            List<Symbol> symbols= new ArrayList<>();
                            method.getChildren().get(1).getChildren().stream()// não sei se funciona com array
                                    .forEach(type -> symbols.add(new Symbol(new Type(type.getChildren().get(0).get("name"),false),type.get("name"))));
                            map.put(method.get("name"),symbols);
                        }
                        );
        if(classDecl.getChildren("MainMethodDecl").size() >0){
            List<Symbol> symbols= new ArrayList<>();
            classDecl.getChildren("MainMethodDecl").get(0).getAttributes().stream().forEach(token -> {if(token =="arg"){
                symbols.add(new Symbol(new Type("String",true),classDecl.getChildren("MainMethodDecl").get(0).get("arg")));
            }
            });

            map.put("MainMethodDecl",symbols);
        }
        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));
        if(classDecl.getChildren("MainMethodDecl").size() >0){

        }
        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList <>();
        List<String> methods1 = classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
        methods.addAll(methods1);
        var test = classDecl.getChildren("MainMethodDecl");
        if(classDecl.getChildren("MainMethodDecl").size() >0){
            methods.add("MainMethodDecl");
        }
        return methods;
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
