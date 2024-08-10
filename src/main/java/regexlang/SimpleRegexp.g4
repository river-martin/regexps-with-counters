grammar SimpleRegexp;

@header {
package regexlang;

enum Eagerness {
	GREEDY,
	LAZY,
	NEUTRAL
};

public final int NONE = -1;
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

exactCounter
	returns[int exactBound]:
	'{' bound '}' {$exactBound = $bound.val;};

boundedCounter
	returns[int lowerBound, int upperBound, Eagerness egrns]:
	'{' lwr = bound ',' upr = bound {$lowerBound = $lwr.val; $upperBound = $upr.val;} cc =
		closeCounter {$egrns = $cc.egrns;};

unboundedCounter
	returns[int lowerBound, Eagerness egrns]:
	'{' lwr = bound ',' {$lowerBound = $lwr.val;} cc = closeCounter {$egrns = $cc.egrns
		};

closeCounter
	returns[Eagerness egrns]:
	'}?' {$egrns=LAZY}
	| '}' {$egrns=GREEDY};

bound
	returns[int val]:
	s = INT {$val = Integer.parseInt($s.getText());};

quantifier
	returns[Eagerness egrns, int lowerBound, int upperBound, int exactBound]:
	quantifiable (
		// Neither greedy nor lazy
		ec = exactCounter {$egrns = NEUTRAL; $lowerBound = $ec.exactBound; $upperBound = $ec.exactBound; $exactBound = $ec.exactBound;
			}
		// Either greedy or lazy
		| uc = unboundedCounter {$egrns = uc.egrns; $lowerBound=uc.lowerBound; $upperBound=NONE; $exactBound=NONE;
			}
		| bc = boundedCounter {$egrns = bc.egrns; $lowerBound=bc.lowerBound; $upperBound=bc.upperBound; $exactBound=NONE;
			}
		| s = star {$egrns = $s.egrns; $lowerBound=0; $upperBound=NONE; $exactBound=NONE;}
		| p = plus {$egrns = $p.egrns; $lowerBound=1; $upperBound=NONE; $exactBound=NONE;}
		| q = ques {$egrns = $q.egrns; $lowerBound=0; $upperBound=1; $exactBound=NONE;}
	);

star
	returns[Eagerness egrns]:
	LAZY_STAR {$egrns=LAZY;}
	| GREEDY_STAR {$egrns=GREEDY;};
plus
	returns[Eagerness egrns]:
	LAZY_PLUS {$egrns=LAZY;}
	| GREEDY_PLUS {$egrns=GREEDY;};
ques
	returns[Eagerness egrns]:
	LAZY_QUES {$egrns=LAZY;}
	| GREEDY_QUES {$egrns=GREEDY;};

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

CHAR_RANGE: '[' '^'? (~[-] '-' ~[\]])+ ']';

CHAR_SET: '[' '^'? ~[\]]+ ']';

DIGIT_CLS: '\\d';

WORD_CLS: '\\w';

SPACE_CLS: '\\s';

ANY_CLS: '.';

DIGIT: [0-9];

NON_DIGIT_CHAR: [~0-9];

INT: [0-9]+;