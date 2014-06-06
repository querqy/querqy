grammar Query;

query: (booleanQuery | noopQuery)+ ;

noopQuery:
	clause+
	;
	
booleanQuery: 
    
	clause (opAnd clause)+
	|
	clause (opOr clause)+
//	|
//	booleanQuery (opAnd clause)+
//	|
//	booleanQuery (opOr clause)+
    
	;
	
clause: 
	 (booleanPrefix? termQuery)
	| (booleanPrefix? '('+ booleanQuery ')')
	| (booleanPrefix? '('+ termQuery ')')
	| (booleanPrefix? '('+ noopQuery ')')
	| (booleanPrefix? '('+ booleanQuery ')'*)
	| (booleanPrefix? '('+ termQuery ')'*)
	| (booleanPrefix? '('+ noopQuery ')'*)
	| (booleanPrefix? termQuery ')'+)
	//| (booleanPrefix? booleanQuery)
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
// this is the BMP in Unicode, excl. punctuation that has a meaning in our grammar 
// http://en.wikipedia.org/wiki/Unicode_block, http://en.wikipedia.org/wiki/Basic_Latin_(Unicode_block)
TermChar :  
	'!'
	| '\u0023' .. '\u0027'
	| '*'
	| ','
	| '\u002e' .. '\uffff'
	;

fragment
FieldNameChar : [a-zA-Z0-9_];

AND 	: 'AND';
OR  	: 'OR';
PLUS	: '+';
MINUS	: '-';

TERMSTRING : TermChar+ ;
FIELDNAME : FieldNameChar+ ;

WS : [ \t]+ -> skip;


