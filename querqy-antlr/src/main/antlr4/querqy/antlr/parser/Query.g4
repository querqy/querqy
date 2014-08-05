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
	(booleanPrefix? termQuery )
	| (booleanPrefix? '('+ booleanQuery ')')
	| (booleanPrefix? '('+ termQuery ')')
	| (booleanPrefix? '('+ noopQuery ')')
	| (booleanPrefix? '('+ booleanQuery ')'*)
	| (booleanPrefix? '('+ termQuery ')'*)
	| (booleanPrefix? '('+ noopQuery ')'*)
	| (booleanPrefix? termQuery ')'+)
	//| (booleanPrefix? booleanQuery)
	;

opOr : OR;
opAnd: AND;
must:  PLUS;
mustNot: MINUS;

// TODO: check against a set of valid field names and
// make "<fieldName>:" part of the term if it is invalid or make "<fieldName>:"
// a term on its own if it is not followed by a term
termQuery:
	(fieldName ':')? term
	;

//booleanOperator: opAnd | opOr;
booleanPrefix: must | mustNot;


AND 	: 'AND';
OR  	: 'OR';
PLUS	: '+';
MINUS	: '-';

term: STRING|STRING_EXT ;
fieldName : STRING;

STRING: STRING_CHAR+;
STRING_EXT: (STRING_CHAR_EXT | STRING_CHAR)+;

fragment
STRING_CHAR  : [a-zA-Z0-9_];

fragment
STRING_CHAR_EXT   : 
	'!'
	| '\u0023' .. '\u0027'
	| '*'
	| ','
	| '\u002e' .. '\u002f'
	| '\u003b' .. '\u0040'
	| '\u005b' .. '\u005e'
	| '\u0060'
	| '\u007b' .. '\u007e'
	| '\u0080' .. '\uffff'

;


WS : [ \t]+ -> skip;


