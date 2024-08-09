grammar SimpleRegexp;

// Parser rules

start: (concat | alt | non_empty_terminal | empty_string) EOF;

char_cls:
	CHAR_RANGE
	| CHAR_SET
	| DIGIT_CLS
	| WORD_CLS
	| SPACE_CLS
	| ANY_CLS;

anchor: '^' | '$';

non_empty_terminal: char_cls | CHAR | anchor;

group:
	'(' (concat | alt | non_empty_terminal | empty_string) ')';

reluctant_op: SHY_STAR | SHY_PLUS | SHY_QUES | SHY_COUNTER;

greedy_op:
	GREEDY_STAR
	| GREEDY_PLUS
	| GREEDY_QUES
	| GREEDY_COUNTER;

quantifier: (non_empty_terminal | group) (
		reluctant_op
		| greedy_op
	);

concat: (group | quantifier | non_empty_terminal) (
		group
		| quantifier
		| non_empty_terminal
		| empty_string
		| concat
	);

alt: (concat | empty_string) ( '|' (concat | empty_string));

right_alt: alt;

empty_string:;

// Lexer rules

WS: [\n] -> skip;

SHY_STAR: '*?';
GREEDY_STAR: '*';

SHY_PLUS: '+?';
GREEDY_PLUS: '+';

SHY_QUES: '??';
GREEDY_QUES: '?';

SHY_COUNTER: '{' [0-9]+ (',' [0-9]*)? '}?';
GREEDY_COUNTER: '{' [0-9]+ (',' [0-9]*)? '}';

CHAR_RANGE: '[' '^'? (~[-] '-' ~[\]])+ ']';

CHAR_SET: '[' '^'? ~[\]]+ ']';

DIGIT_CLS: '\\d';

WORD_CLS: '\\w';

SPACE_CLS: '\\s';

ANY_CLS: '.';

CHAR: .;