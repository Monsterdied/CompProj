grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
COMMA : ',' ;
DOT : '.' ;
DIV : '/' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
AND : '&&' ;
LT : '<' ;
NOT: '!' ;

CLASS : 'class' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
IMPORT : 'import';
RETURN : 'return' ;
EXTENDS : 'extends' ;

INTEGER : '0' | [1-9] [0-9]*;
BOOLEAN_VALUE : 'true' | 'false' ;

ID : [a-zA-Z$_] [a-zA-Z0-9$_]* ;

COMMENT : '//' ~[\r\n]* -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;


WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl | classDecl)* EOF
    ;

importDecl
    : IMPORT ID (DOT ID)* SEMI
    ;

classDecl
    : CLASS name=ID (EXTENDS parent=ID)?
        LCURLY
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= INT
    | name= BOOLEAN
    | name= ID;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    | expr SEMI #ExprStmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr
    | methodCall #MethodCallExpr
    | expr op=AND expr #LogicalOpExpr
    | expr op=LT expr #RelationalOpExpr
    | expr op=(MUL | DIV) expr #MulDivExpr
    | expr op=(ADD | SUB) expr #AddSubExpr
    | op=NOT expr #NotExpr
    | value=INTEGER #IntegerLiteral
    | value=BOOLEAN_VALUE #BooleanLiteral
    | name=ID #VarRefExpr
    ;

methodCall
    : name=ID LPAREN args? RPAREN
    ;

args
    : expr (COMMA expr)*
    ;
