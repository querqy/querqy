grammar Query;

//@header {
//    package qp.parser;
//}

query: booleanClause booleanOperator booleanClause
		| query booleanOperator booleanClause
		| query booleanClause+
		| booleanClause+ ;

booleanClause: 
	booleanPrefix? 
	(
		'(' query ')'
		|
		termQuery
	);

booleanOperator: opAnd | opOr;
booleanPrefix: must | mustNot;

opOr : OR;
opAnd: AND;
must:  PLUS;
mustNot: MINUS;

termQuery: TERMSTRING ;

fragment
TermChar :   [:a-zA-Z];

AND 	: 'AND';
OR  	: 'OR';
PLUS	: '+';
MINUS	: '-';

TERMSTRING : TermChar+ ;

WS : [ \t]+ -> skip;


