grammar SimpleRegexp;

@header {
package regexlang;

import regexlang.UnsupportedRegexException;
}

@members {
// ---------------------------------------------------------------------
// Members defined in SimpleRegexp.g4
// ---------------------------------------------------------------------
public static enum Eagerness {
	GREEDY,
	LAZY,
	NEUTRAL
}

public final static int UNBOUNDED = -1;

// Used as arguments to closeCounter
private final static boolean ALLOW_LAZINESS = true;
private final static boolean DISALLOW_LAZINESS = false;

// ---------------------------------------------------------------------
}

// Parser rules

regexp: (body | emptyString) EOF;

group: openGroup (body | emptyString) closeGroup;

anchor: '^' | '$';

body:
	alt
	| concat
	| lookaround body; // throws UnsupportedRegexException

exactCounter
	returns[int exactBound, Eagerness egrns]:
	openCounter bound {$exactBound = $bound.val;} closeCounter[DISALLOW_LAZINESS] {$egrns = Eagerness.NEUTRAL;
		};

boundedCounter
	returns[int lowerBound, int upperBound, Eagerness egrns]:
	openCounter lwr = bound {$lowerBound = $lwr.val;} ',' upr = bound {$upperBound = $upr.val;} (
		cc = closeCounter[ALLOW_LAZINESS]
	) {$egrns = $cc.egrns;};

unboundedCounter
	returns[int lowerBound, Eagerness egrns]:
	openCounter lwr = bound {$lowerBound = $lwr.val;} ',' cc = closeCounter[ALLOW_LAZINESS] {$egrns = $cc.egrns;
		};

openCounter: '{';

closeCounter[boolean allowLaziness]
	returns[Eagerness egrns]:
	{$allowLaziness}? '}?' {$egrns=Eagerness.LAZY;}
	| '}' {$egrns=Eagerness.GREEDY;};

bound
	returns[int val]:
	s = integer {$val = Integer.parseInt($s.ctx.getText());};

quantExpr
	returns[QuantifiableContext quantifiableCtx, QuantifierContext quantifierCtx]:
	q1 = quantifiable q2 = quantifier { $quantifiableCtx = $q1.ctx; $quantifierCtx = $q2.ctx;};

quantifier
	returns[Eagerness egrns, int lowerBound, int upperBound, int exactBound]:
	// Neither greedy nor lazy
	ec = exactCounter {$egrns = Eagerness.NEUTRAL; $lowerBound = $ec.exactBound; $upperBound = $ec.exactBound; $exactBound = $ec.exactBound;
			}
	// Either greedy or lazy
	| bc = boundedCounter {$egrns = $bc.egrns; $lowerBound=$bc.lowerBound; $upperBound=$bc.upperBound; $exactBound=UNBOUNDED;
			}
	| uc = unboundedCounter {$egrns = $uc.egrns; $lowerBound=$uc.lowerBound; $upperBound=UNBOUNDED; $exactBound=UNBOUNDED;
			}
	| s = star {$egrns = $s.egrns; $lowerBound=0; $upperBound=UNBOUNDED; $exactBound=UNBOUNDED;
			}
	| p = plus {$egrns = $p.egrns; $lowerBound=1; $upperBound=UNBOUNDED; $exactBound=UNBOUNDED;
			}
	| q = ques {$egrns = $q.egrns; $lowerBound=0; $upperBound=1; $exactBound=UNBOUNDED;};

star
	returns[Eagerness egrns]:
	LAZY_STAR {$egrns=Eagerness.LAZY;}
	| GREEDY_STAR {$egrns=Eagerness.GREEDY;};

// TODO: possesive plus
plus
	returns[Eagerness egrns]:
	LAZY_PLUS {$egrns=Eagerness.LAZY;}
	| GREEDY_PLUS {$egrns=Eagerness.GREEDY;};

ques
	returns[Eagerness egrns]:
	LAZY_QUES {$egrns=Eagerness.LAZY;}
	| GREEDY_QUES {$egrns=Eagerness.GREEDY;};

quantifiable: (group | nonEmptyTerminal);

concat: (quantExpr | group | nonEmptyTerminal) (
		concat
		| emptyString
	);

alt: (concat | emptyString) (
		'|' (body | emptyString)
	);

charCls:
	CHAR_RANGE
	| CHAR_SET
	| DIGIT_CLS
	| WORD_CLS
	| SPACE_CLS
	| ANY_CLS;

nonEmptyTerminal:
	anchor
	| charCls
	| LETTER
	| NON_DIGIT_CHAR
	| DIGIT
	| ESCAPED_CHAR;

openGroup: '(?:' | '(<' groupName '>' | '(';

closeGroup: ')';

groupName: LETTER+;

emptyString:;

// Unsupported

lookaround:
	openLookaround body closeLookaround {throw new UnsupportedRegexException("Lookaround is not supported");
		};

openLookaround: '(?' | '(?<=' | '(?<!' | '?=' | '(?<!';

closeLookaround: ')';

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

// Used for the bounds of quantifiers
integer: DIGIT+;

DIGIT: [0-9];

LETTER: [a-zA-Z];

// XXX If any named character classes are missed by the lexer, they will be treated as escaped characters
ESCAPED_CHAR: '\\' .;

NON_DIGIT_CHAR: LETTER | ~[0-9];