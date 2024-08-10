grammar SimpleRegexp;

@header {
package regexlang;
}

// Parser rules

regexp: (concat | alt | nonEmptyTerminal | emptyString) EOF;

charCls:
	CHAR_RANGE
	| CHAR_SET
	| DIGIT_CLS
	| WORD_CLS
	| SPACE_CLS
	| ANY_CLS;

anchor: '^' | '$';

nonEmptyTerminal: charCls | NON_DIGIT_CHAR | DIGIT | anchor;

group: '(' (concat | alt | nonEmptyTerminal | emptyString) ')';

unboundedLazyCounter: '{' lowerBound ',' '}?';

boundedLazyCounter: '{' lowerBound ',' upperBound '}?';

exactCounter: '{' lowerBound '}';

unboundedGreedyCounter: '{' lowerBound ',' '}';

boundedGreedyCounter: '{' lowerBound ',' upperBound '}';

lowerBound: DIGIT+;

upperBound: DIGIT*;

quantifier:
	quantifiable (
		// Neither greedy nor lazy
		exactCounter
		// Lazy quantifiers
		| LAZY_STAR
		| LAZY_PLUS
		| LAZY_QUES
		| unboundedLazyCounter
		| boundedLazyCounter
		// Greedy quantifiers
		| GREEDY_STAR
		| GREEDY_PLUS
		| GREEDY_QUES
		| unboundedGreedyCounter
		| boundedGreedyCounter
	);

quantifiable: (nonEmptyTerminal | group);

concat: (group | quantifier | nonEmptyTerminal) (
		group
		| quantifier
		| nonEmptyTerminal
		| emptyString
		| concat
	);

alt: (concat | emptyString) ( '|' (concat | emptyString));

right_alt: alt;

emptyString:;

// Lexer rules

WS: [\n] -> skip;

LAZY_STAR: '*?';
GREEDY_STAR: '*';

LAZY_PLUS: '+?';
GREEDY_PLUS: '+';

LAZY_QUES: '??';
GREEDY_QUES: '?';

//LAZY_COUNTER: '{' [0-9]+ (',' [0-9]*)? '}?'; GREEDY_COUNTER: '{' [0-9]+ (',' [0-9]*)? '}';

CHAR_RANGE: '[' '^'? (~[-] '-' ~[\]])+ ']';

CHAR_SET: '[' '^'? ~[\]]+ ']';

DIGIT_CLS: '\\d';

WORD_CLS: '\\w';

SPACE_CLS: '\\s';

ANY_CLS: '.';

DIGIT: [0-9];

NON_DIGIT_CHAR: [~0-9];