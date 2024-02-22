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
DIV : '/' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
AND : '&&' ;
LT : '<' ;

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : '0' | [1-9] [0-9]*;

ID : [a-zA-Z$_] [a-zA-Z0-9$_]* ;

COMMENT : '//' ~[\r\n]* -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;


WS : [ \t\n\r\f]+ -> skip ;

program
    : classDecl EOF
    ;


classDecl
    : CLASS name=ID
        LCURLY
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= INT ;

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
    ;

expr
    : LPAREN expr RPAREN #ParenExpr
    | expr AND expr #LogicalOpExpr
    | expr LT expr #RelationalOpExpr
    | expr op=(MUL | DIV) expr #MulDivExpr
    | expr op=(ADD | SUB) expr #AddSubExpr
    | value=INTEGER #IntegerLiteral
    | name=ID #VarRefExpr
    ;




