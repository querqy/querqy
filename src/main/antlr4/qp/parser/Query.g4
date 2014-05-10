grammar Query;

query: (booleanQuery | noopQuery)+ ;

noopQuery:
	clause+
	;
	
booleanQuery: 
    
	clause (opAnd clause)+ 
	|
	clause (opOr clause)+
	|
	booleanQuery (opAnd clause)+
	|
	booleanQuery (opOr clause)+
    
	;
	
clause: 
	(booleanPrefix? '('+ booleanQuery ')')
	| (booleanPrefix? '('+ termQuery ')')
	| (booleanPrefix? '('+ noopQuery ')')
	| (booleanPrefix? termQuery)
	| (booleanPrefix? '('+ booleanQuery ')'*)
	| (booleanPrefix? '('+ termQuery ')'*)
	| (booleanPrefix? '('+ noopQuery ')'*)
	| (booleanPrefix? termQuery ')'+)
	;

booleanOperator: opAnd | opOr;
booleanPrefix: must | mustNot;

opOr : OR;
opAnd: AND;
must:  PLUS;
mustNot: MINUS;

termQuery: TERMSTRING;

fragment
TermChar :   [:a-zA-Z];

AND 	: 'AND';
OR  	: 'OR';
PLUS	: '+';
MINUS	: '-';

TERMSTRING : TermChar+ ;

WS : [ \t]+ -> skip;


