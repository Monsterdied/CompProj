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
    : CLASS name=(ID | 'length' |'main') (EXTENDS parent=ID)?
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
    | type name='length' SEMI
    ;

type
    : type LBRACK RBRACK
    | name= INT
    | name= BOOLEAN
    | name= VOID
    | name= 'String'
    | name= (ID|'main');

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN paramList RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

mainMethodDecl
    : PUBLIC? STATIC VOID name='main' LPAREN ('String' LBRACK RBRACK arg=(ID|'length'|'main'))? RPAREN LCURLY varDecl* stmt* RCURLY
    ;

paramList
    : (param (',' param)*)?
    ;

param
    : type name=(ID|'length'|'main')? #NormalParam
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
    : LPAREN expr RPAREN #ParenExpr
    | value=INTEGER #IntegerLiteral
    | value=BOOLEAN_VALUE #BooleanLiteral
    | name=(ID|'length'|'main') #VarRefExpr
    | THIS  #ThisExpr
    | LBRACK (expr (',' expr)*)? RBRACK #ArrayInitExpression
    | expr LBRACK index=expr RBRACK #ArrayAccessExpr
    | expr '.' name=(ID|'main') LPAREN args? RPAREN #MethodCallExpr
    | name=ID LPAREN args? RPAREN #MethodCallExpr
    | NEW type LBRACK expr RBRACK #NewArrayExpr
    | NEW name=(ID|'main') LPAREN RPAREN #NewClassExpr
    | expr '.' 'length' #ArrayLengthExpr
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | op=NOT expr #NotExpr
    | expr op=(LT | LE | GT | GE) expr #BinaryExpr
    | expr op=(AND | OR) expr #BinaryExpr
    ;



args
    : expr (',' expr)*
    ;
