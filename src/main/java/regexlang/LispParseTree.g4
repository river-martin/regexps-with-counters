grammar LispParseTree;

@header {
package regexlang;
}

@members {
// Used as arguments to closeCounter
private final static boolean ALLOW_LAZINESS = true;
private final static boolean DISALLOW_LAZINESS = false;
}

root: regexp;

regexp: LPAR KEYWORD_REGEXP body KEYWORD_EOF RPAR;

anchor: LPAR KEYWORD_ANCHOR ('^' | '$') RPAR;

group: LPAR KEYWORD_GROUP openGroup body closeGroup RPAR;

body:
	LPAR KEYWORD_BODY (
		quantExpr
		| group
		| concat
		| alt
		| nonEmptyTerminal
		| emptyString
	) RPAR;

openGroup: LPAR KEYWORD_OPEN_GROUP LPAR RPAR;

closeGroup: LPAR KEYWORD_CLOSE_GROUP RPAR RPAR;

exactCounter:
	LPAR KEYWORD_EXACT_COUNTER openCounter bound closeCounter[DISALLOW_LAZINESS] RPAR;

boundedCounter:
	LPAR KEYWORD_BOUNDED_COUNTER openCounter bound ',' bound closeCounter[ALLOW_LAZINESS] RPAR;

unboundedCounter:
	LPAR KEYWORD_UNBOUNDED_COUNTER openCounter bound ',' closeCounter[ALLOW_LAZINESS] RPAR;

openCounter: LPAR KEYWORD_OPEN_COUNTER LBRACK RPAR;

closeCounter[boolean allowLaziness]:
	LPAR KEYWORD_CLOSE_COUNTER (
		{$allowLaziness}? LAZY_RBRACK
		| RBRACK
	) RPAR;

bound: LPAR KEYWORD_BOUND INT RPAR;

quantExpr: LPAR KEYWORD_QUANT_EXPR quantifiable quantifier RPAR;

quantifier:
	LPAR KEYWORD_QUANTIFIER (
		exactCounter
		| boundedCounter
		| unboundedCounter
		| star
		| plus
		| ques
	) RPAR;

star: LPAR KEYWORD_STAR (LAZY_STAR | GREEDY_STAR) RPAR;

// TODO: possessive plus
plus: LPAR KEYWORD_PLUS (LAZY_PLUS | GREEDY_PLUS) RPAR;

ques: LPAR KEYWORD_QUES (LAZY_QUES | GREEDY_QUES) RPAR;

quantifiable:
	LPAR KEYWORD_QUANTIFIABLE (group | nonEmptyTerminal) RPAR;

concat:
	LPAR KEYWORD_CONCAT (quantExpr | group | nonEmptyTerminal) (
		quantExpr
		| group
		| nonEmptyTerminal
		| emptyString
		| concat
	) RPAR;

alt:
	LPAR (concat | emptyString) ('|' (concat | emptyString)) RPAR;

charCls:
	LPAR KEYWORD_CHAR_CLS (
		CHAR_RANGE
		| CHAR_SET
		| DIGIT_CLS
		| WORD_CLS
		| SPACE_CLS
		| ANY_CLS
	) RPAR;

nonEmptyTerminal:
	LPAR KEYWORD_NON_EMPTY_TERMINAL (
		charCls
		| NON_DIGIT_CHAR
		| DIGIT
		| anchor
	) RPAR;

emptyString: LPAR KEYWORD_EMPTY_STRING RPAR;

// Lexer rules

KEYWORD_EMPTY_STRING: 'emptyString';

KEYWORD_NON_EMPTY_TERMINAL: 'nonEmptyTerminal';

KEYWORD_CHAR_CLS: 'charCls';

KEYWORD_ANCHOR: 'anchor';

KEYWORD_QUANTIFIABLE: 'quantifiable';

KEYWORD_QUANT_EXPR: 'quantExpr';

KEYWORD_QUANTIFIER: 'quantifier';

KEYWORD_CONCAT: 'concat';

KEYWORD_GROUP: 'group';

KEYWORD_EXACT_COUNTER: 'exactCounter';

KEYWORD_BOUNDED_COUNTER: 'boundedCounter';

KEYWORD_UNBOUNDED_COUNTER: 'unboundedCounter';

KEYWORD_OPEN_COUNTER: 'openCounter';

KEYWORD_CLOSE_COUNTER: 'closeCounter';

KEYWORD_BOUND: 'bound';

KEYWORD_STAR: 'star';

KEYWORD_PLUS: 'plus';

KEYWORD_QUES: 'ques';

KEYWORD_REGEXP: 'regexp';

KEYWORD_EOF: '<EOF>';

KEYWORD_OPEN_GROUP: 'openGroup';

KEYWORD_CLOSE_GROUP: 'closeGroup';

KEYWORD_BODY: 'body';

LPAR: '(';
RPAR: ')';

LBRACK: '{';
LAZY_RBRACK: '}?';
RBRACK: '}';

// Lexer rules

WS: [\n \t] -> skip;

LAZY_STAR: '*?';
GREEDY_STAR: '*';

LAZY_PLUS: '+?';
GREEDY_PLUS: '+';

LAZY_QUES: '??';
GREEDY_QUES: '?';

CHAR_RANGE: '[' '^'? (~[-] '-' ~[\]])+ ']';

CHAR_SET: '[' '^'? ~[\]]+ ']';

DIGIT_CLS: '\\d';

WORD_CLS: '\\w';

SPACE_CLS: '\\s';

ANY_CLS: '.';

// Used for the bounds of quantifiers
INT: DIGIT+;

DIGIT: [0-9];

NON_DIGIT_CHAR: ~[0-9];