package nca;

/**
 * Reads characters and constructs Tokens, which are convenient to work with in
 * other parts of the project.
 */
public class MyScanner {
    private static int pos = 0;
    private static char[] s;
    private static int id = 1;
    private static int counterID = 1;

    public static void initScanner(char[] line) {
        s = line;
        pos = 0;
        id = 1;
        counterID = 1;
    }

    private static char nextChar() {
        if (pos < s.length) {
            return s[pos++];
        } else {
            return Character.MIN_VALUE;
        }
    }

    private static boolean isPredefinedCharacterClass(char c) {
        char[] classes = new char[] { 'd', 'D', 's', 'S', 'v', 'V', 'w', 'W' };
        for (char cl : classes) {
            if (cl == c) {
                return true;
            }
        }
        return false;
    }

    private static Token scanCharClass() {
        StringBuilder charClass = new StringBuilder("[");
        char c = '[';
        while (c != ']') {
            c = nextChar();
            charClass.append(c);
        }
        return new Token(charClass.toString(), id++, TokenType.CHAR_CLASS);
    }

    private static Token scanPredefinedCharacterClass(char c2) {
        String charClass = "\\" + c2;
        return new Token(charClass, id++, TokenType.CHAR_CLASS);
    }

    private static Token scanEscapedChar(char escapedChar) {
        return new Token(escapedChar + "", id++, TokenType.CHAR);
    }

    private static Token scanDot() {
        return new Token(".", id++, TokenType.CHAR_CLASS);
    }

    private static Token scanCounter() {
        char c = '{';
        StringBuilder counterString = new StringBuilder("{");
        while (c != '}') {
            c = nextChar();
            counterString.append(c);
        }
        int lowerBound;
        int upperBound = -1;
        if (counterString.toString().contains(",")) {
            lowerBound = Integer.parseInt(counterString.substring(1, counterString.indexOf(",")));
            String upperBoundString = counterString.substring(counterString.indexOf(",") + 1,
                    counterString.indexOf("}"));
            if (upperBoundString.equals("")) {
                // The counter is unbounded (above)
                upperBound = -1;
            } else {
                upperBound = Integer.parseInt(upperBoundString);
            }
        } else {
            lowerBound = Integer.parseInt(counterString.substring(1, counterString.length() - 1));
            upperBound = lowerBound;
        }
        CounterRange counter = new CounterRange(lowerBound, upperBound, counterID++);
        return new Token(counterString.toString(), TokenType.COUNTER, counter);
    }

    private static Token scanChar(char c) {
        return new Token(c + "", id++, TokenType.CHAR);
    }

    public static Token getToken() {
        char c = nextChar();
        if (c == Character.MIN_VALUE) {
            return null;
        }
        switch (c) {
            case '*':
                return new Token(c + "", counterID++, TokenType.STAR);
            case '+':
                return new Token(c + "", counterID++, TokenType.PLUS);
            case '|':
                return new Token(c + "", TokenType.BAR);
            case '(':
                return new Token(c + "", TokenType.L_PAR);
            case ')':
                return new Token(c + "", TokenType.R_PAR);
            case '[':
                return scanCharClass();
            case '.':
                return scanDot();
            case '{':
                return scanCounter();
            case '\\':
                char next = nextChar();
                if (isPredefinedCharacterClass(next)) {
                    return scanPredefinedCharacterClass(next);
                } else {
                    return scanEscapedChar(next);
                }
            default:
                return scanChar(c);
        }
    }

}
