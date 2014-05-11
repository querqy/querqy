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

termQuery:
	(fieldName ':')? term
	;

booleanOperator: opAnd | opOr;
booleanPrefix: must | mustNot;

opOr : OR;
opAnd: AND;
must:  PLUS;
mustNot: MINUS;

term: TERMSTRING;
fieldName : FIELDNAME;

fragment
TermChar :   [a-zA-Z];

fragment
FieldNameChar : [a-zA-Z0-9_];

AND 	: 'AND';
OR  	: 'OR';
PLUS	: '+';
MINUS	: '-';

TERMSTRING : TermChar+ ;
FIELDNAME : FieldNameChar+ ;

WS : [ \t]+ -> skip;


