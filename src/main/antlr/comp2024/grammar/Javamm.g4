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
LBRACK : '[' ;
RBRACK : ']' ;
COMMA : ',' ;
DOT : '.' ;
DIV : '/' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
AND : '&&' ;
LT : '<' ;
OR : '||';
GT : '>' ;
LE : '<=' ;
GE : '>=' ;
NOT: '!' ;
VARAGS: '...' ;

CLASS : 'class' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
IMPORT : 'import';
RETURN : 'return' ;
EXTENDS : 'extends' ;
IF : 'if' ;
ELSE : 'else' ;
ELSEIF : 'else if' ;
WHILE : 'while' ;
NEW : 'new' ;
STATIC : 'static' ;
VOID : 'void' ;
MAIN : 'main' ;
LENGTH : 'length' ;
THIS : 'this' ;

INTEGER : '0' | [1-9] [0-9]*;
BOOLEAN_VALUE : 'true' | 'false' ;

ID : [a-zA-Z$_] [a-zA-Z0-9$_]*;

COMMENT : '//' ~[\r\n]* -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;


WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : IMPORT value+=ID (DOT value+=ID)* SEMI
    ;

classDecl
    : CLASS name=ID (EXTENDS parent=ID)?
        LCURLY
        varDecl*
        methodDecl*
        mainMethodDecl?
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    | type name='main' SEMI
    ;

type
    : type LBRACK RBRACK
    | name= INT
    | name= BOOLEAN
    | name= ID
    | name= 'String';

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN paramList RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

mainMethodDecl
    : PUBLIC? STATIC VOID MAIN LPAREN ('String' LBRACK RBRACK arg=ID)? RPAREN LCURLY varDecl* stmt* RCURLY
    ;

paramList
    : (param (COMMA param)*)?
    ;

param
    : type VARAGS? name=ID?
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    | expr SEMI #ExprStmt
    | LCURLY stmt* RCURLY #BlockStmt
    | ifStmt #IfElseStmt
    | whileStmt #WhileCondition
    ;

ifStmt
    : IF LPAREN expr RPAREN stmt ELSE stmt
    ;

whileStmt
    : WHILE LPAREN expr RPAREN stmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr
    | THIS DOT expr #ThisExpr1
    | THIS expr  #ThisExpr2
    | name=ID LPAREN args? RPAREN #MethodCallExpr
    | NEW type LBRACK expr RBRACK #NewArrayExpression
    | NEW name=ID LPAREN RPAREN #NewClassExpression
    | LBRACK (expr (COMMA expr)*)? RBRACK #ArrayInitExpression
    | expr op=(AND | OR) expr #BinaryExpr
    | expr op=(LT | LE | GT | GE) expr #BinaryExpr
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | op=NOT expr #NotExpr
    | value=INTEGER #IntegerLiteral
    | value=BOOLEAN_VALUE #BooleanLiteral
    | name=ID #VarRefExpr
    | LPAREN? ID RPAREN? (LBRACK expr RBRACK)+ #ArrayAccessExpr
    | expr DOT LENGTH #ArrayLengthExpr
    | expr DOT name=ID LPAREN args? RPAREN #MethodCallExpr
    | expr LBRACK index=expr RBRACK #ArrayAccessExpr
    ;

args
    : expr (COMMA expr)*
    ;

