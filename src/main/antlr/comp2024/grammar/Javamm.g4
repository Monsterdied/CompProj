grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}


COMMENT : '//' ~[\r\n]* -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACK : '[' ;
RBRACK : ']' ;
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
WHILE : 'while' ;
NEW : 'new' ;
STATIC : 'static' ;
VOID : 'void' ;
THIS : 'this' ;

INTEGER : '0' | [1-9] [0-9]*;
BOOLEAN_VALUE : 'true' | 'false' ;

ID : [a-zA-Z$_] [a-zA-Z0-9$_]*;





WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : IMPORT value+=ID ('.' value+=ID)* SEMI
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
    | name= VOID
    | name= 'String'
    | name= ID;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN paramList RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

mainMethodDecl
    : PUBLIC? STATIC VOID name='main' LPAREN ('String' LBRACK RBRACK arg=ID)? RPAREN LCURLY varDecl* stmt* RCURLY
    ;

paramList
    : (param (',' param)*)?
    ;

param
    : type name=ID? #NormalParam
    | type VARAGS name=ID? #VarArgArray
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt
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
    : op=NOT expr #NotExpr
    | expr '.' name=ID LPAREN args? RPAREN #MethodCallExpr
    | name=ID LPAREN args? RPAREN #MethodCallExpr
    | expr op=(AND | OR) expr #BinaryExpr
    | expr op=(LT | LE | GT | GE) expr #BinaryExpr
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | value=INTEGER #IntegerLiteral
    | name=ID #VarRefExpr
    | LPAREN expr RPAREN #ParenExpr
    | expr '.' 'length' #ArrayLengthExpr
    | expr LBRACK index=expr RBRACK #ArrayAccessExpr
    | value=BOOLEAN_VALUE #BooleanLiteral
    | NEW type LBRACK expr RBRACK #NewArrayExpr
    | NEW name=ID LPAREN RPAREN #NewClassExpr
    | LBRACK (expr (',' expr)*)? RBRACK #ArrayInitExpression
    | LPAREN? ID RPAREN? (LBRACK expr RBRACK)+ #ArrayAccessExpr
    | THIS  #ThisExpr
    ;


args
    : expr (',' expr)*
    ;
