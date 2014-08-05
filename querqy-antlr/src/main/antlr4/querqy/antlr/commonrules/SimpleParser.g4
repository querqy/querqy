grammar SimpleParser;

line:
	boostInstruction | synonymInstruction | filterInstruction | deleteInstruction | input;

boostInstruction:
	BOOST ':' boostValue
	;
	
boostValue:
	termExpr+;

synonymInstruction:
	SYNONYM ':' synonymValue
	;
	
synonymValue:
	termExpr+;
	
filterInstruction:
	FILTER ':' filterValue
	;
	
filterValue:
	termExpr+;

deleteInstruction:
	DELETE (':' deleteValue)?
	;
	
deleteValue:
	termExpr+;
	
input: termExpr+ '=>';

termExpr: 
	fieldNameClause? termClause;
		
fieldNameClause: (fieldName | '{' fieldName (',' fieldName)* '}') ':'
	;
	
fieldName: STRING;
	
termClause: termValue | ('(' termValue+ ')');

termValue: STRING;

fragment
STRING_CHAR  : [a-zA-Z0-9_];

fragment
STRING_CHAR_EXT   : 
	/*'!'
	| '\u0023' .. '\u0027'
	| '*'
	| ','
	| '\u002e' .. '\u002f'
	| '\u003b' .. '\u0040'
	| '\u005b' .. '\u005e'
	| '\u0060'
	| '\u007b' .. '\u007e'
	| '\u0080' .. '\uffff'*/
	[a-z]
;
	
BOOST: [Bb][Oo][Oo][Ss][Tt];
SYNONYM: [Ss][Yy][Nn][Oo][Nn][Yy][Mm];
FILTER: [Ff][Ii][Ll][Tt][Ee][Rr];
DELETE: [Dd][Ee][Ll][Ee][Tt][Ee];

STRING: STRING_CHAR+;
STRING_EXT: (STRING_CHAR_EXT | STRING_CHAR)+;

WS : [ \t\n\r]+ -> skip;